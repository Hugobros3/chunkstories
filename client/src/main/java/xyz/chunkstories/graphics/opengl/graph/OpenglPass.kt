package xyz.chunkstories.graphics.opengl.graph

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*

import xyz.chunkstories.api.graphics.rendergraph.PassDeclaration
import xyz.chunkstories.api.graphics.rendergraph.RenderTarget
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.api.util.kotlin.toVec4f
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglFrame
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem
import xyz.chunkstories.graphics.opengl.systems.OpenglDrawingSystem

class OpenglPass(val backend: OpenglGraphicsBackend, val renderTask: OpenglRenderTask, val declaration: PassDeclaration) : Cleanable {
    val drawingSystems: List<OpenglDrawingSystem>
    val dispatchingDrawers: List<OpenglDispatchingSystem.Drawer<*>>

    protected val fbos = mutableSetOf<FrameBuffer>()

    protected data class FrameBuffer(val depth: OpenglRenderBuffer?, val colors: List<OpenglRenderBuffer>) : Cleanable {
        val glId: Int

        init {
            glId = glCreateFramebuffers()
            if(depth != null)
                glNamedFramebufferTexture(glId, GL_DEPTH_ATTACHMENT, depth.texture.glTexId, 0)

            for ((i, colorTarget) in colors.withIndex())
                glNamedFramebufferTexture(glId, GL_COLOR_ATTACHMENT0 + i, colorTarget.texture.glTexId, 0)
        }

        override fun cleanup() {
            glDeleteFramebuffers(glId)
        }
    }

    init {
        drawingSystems = mutableListOf()
        dispatchingDrawers = mutableListOf()

        declaration.draws?.registeredSystems?.let {
            for (registeredSystem in it) {
                if (DrawingSystem::class.java.isAssignableFrom(registeredSystem.clazz)) {
                    val drawingSystem = backend.createDrawingSystem(this, registeredSystem as RegisteredGraphicSystem<DrawingSystem>)
                    drawingSystems.add(drawingSystem)
                } else if (DispatchingSystem::class.java.isAssignableFrom(registeredSystem.clazz)) {
                    val dispatchingSystem = backend.getOrCreateDispatchingSystem(renderTask.renderGraph.dispatchingSystems, registeredSystem as RegisteredGraphicSystem<DispatchingSystem>)
                    val drawer = dispatchingSystem.createDrawerForPass(this, registeredSystem.dslCode as OpenglDispatchingSystem.Drawer<*>.() -> Unit)

                    dispatchingSystem.drawersInstances.add(drawer)
                    dispatchingDrawers.add(drawer)
                } else {
                    throw Exception("What is this :$registeredSystem ?")
                }
            }
        }
    }

    fun render(frame: OpenglFrame,
               passInstance: OpenglFrameGraph.FrameGraphNode.OpenglPassInstance,
               representationsGathered: MutableMap<OpenglDispatchingSystem.Drawer<*>, ArrayList<*>>) : Int {

        declaration.setupLambdas.forEach { it.invoke(passInstance) }

        fun resolveRenderTarget(renderTarget: RenderTarget) : OpenglRenderBuffer = when(renderTarget) {
            RenderTarget.BackBuffer -> TODO()
            is RenderTarget.RenderBufferReference -> renderTask.buffers[renderTarget.renderBufferName]
                    ?: throw Exception("Missing render target: No render buffer named '${renderTarget.renderBufferName}' found in RenderTask ${renderTask.declaration.name}")
            is RenderTarget.TaskInput -> {
                val resolvedParameter = (passInstance.taskInstance.parameters[renderTarget.name]
                        ?: throw Exception("The parent context lacks a '${renderTarget.name}' parameter"))

                when (resolvedParameter) {
                    is OpenglRenderBuffer -> resolvedParameter
                    is RenderTarget.RenderBufferReference -> {
                        val localRenderBuffer = renderTask.buffers[resolvedParameter.renderBufferName]
                        if (localRenderBuffer != null)
                            localRenderBuffer
                        else {
                            val parentRenderTask = passInstance.taskInstance.requester!!.taskInstance
                            parentRenderTask.renderTask.buffers[resolvedParameter.renderBufferName]
                                    ?: throw Exception("Can't find render buffer named: ${resolvedParameter.renderBufferName}")
                        }
                    }
                    else -> throw Exception("The $resolvedParameter parameter is not a render buffer")
                }
            }
        }

        val resolvedColorOutputs = declaration.outputs.outputs.map { colorOutput ->
            resolveRenderTarget(colorOutput.target ?: RenderTarget.RenderBufferReference(colorOutput.name))
        }

        val resolvedDepth = declaration.depthTestingConfiguration.let {
            if(it.enabled)
                resolveRenderTarget(it.depthBuffer!!)
            else
                null
        }

        passInstance.resolvedColorOutputs = declaration.outputs.outputs.mapIndexed { i, o -> Pair(o, resolvedColorOutputs[i]) }.toMap()
        passInstance.resolvedDepth = resolvedDepth

        // Prepare FBO
        val fbo = findOrCreateFbo(resolvedDepth, resolvedColorOutputs)
        glBindFramebuffer(GL_FRAMEBUFFER, fbo.glId)
        when(glCheckFramebufferStatus(GL_FRAMEBUFFER)) {
            GL_FRAMEBUFFER_COMPLETE -> {}
            GL_FRAMEBUFFER_UNSUPPORTED -> println("unsupported")
            else -> println("smth else")
        }

        glDrawBuffers(resolvedColorOutputs.mapIndexed { i, _ -> GL_COLOR_ATTACHMENT0 + i}.toIntArray())

        val viewportSize = (resolvedColorOutputs.getOrNull(0) ?: resolvedDepth!!).textureSize
        glViewport(0, 0, viewportSize.x, viewportSize.y)

        // Apply clear operations
        for((i, colorOutput) in declaration.outputs.outputs.withIndex()) {
            if(colorOutput.clear) {
                val clearColor = colorOutput.clearColor.toVec4f().let { floatArrayOf(it.x, it.y, it.z, it.w) }
                glClearNamedFramebufferfv(fbo.glId, GL_COLOR, i, clearColor)
            }
        }
        declaration.depthTestingConfiguration.let {
            if(it.enabled && it.clear)
                glClearNamedFramebufferfv(fbo.glId, GL_DEPTH, 0, floatArrayOf(1f))
        }

        // Draw systems
        for ((i, drawingSystem) in drawingSystems.withIndex()) {
            drawingSystem.executeDrawingCommands(frame, passInstance.preparedDrawingSystemsContexts[i])
        }

        for ((i, drawer) in dispatchingDrawers.withIndex()) {
            val relevantBucket = representationsGathered[drawer] ?: continue
             drawer.executeDrawingCommands(frame, passInstance.preparedDispatchingSystemsContexts[i], relevantBucket.toList().asSequence() as Sequence<Nothing>)
        }

        return fbo.glId
    }

    private fun findOrCreateFbo(depth: OpenglRenderBuffer?, colorOutputs: List<OpenglRenderBuffer>): FrameBuffer {
        val match = fbos.find { it.depth == depth && it.colors == colorOutputs }
        if(match != null)
            return match
        val fbo = FrameBuffer(depth, colorOutputs)
        fbos.add(fbo)
        return fbo
    }

    fun dumpFramebuffers() {
        fbos.forEach(Cleanable::cleanup)
        fbos.clear()
    }

    override fun cleanup() {
        fbos.forEach(Cleanable::cleanup)

        drawingSystems.forEach(Cleanable::cleanup)
        dispatchingDrawers.forEach(Cleanable::cleanup)
    }
}