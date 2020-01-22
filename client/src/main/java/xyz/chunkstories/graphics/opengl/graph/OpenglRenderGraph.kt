package xyz.chunkstories.graphics.opengl.graph

import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.ARBDirectStateAccess.*

import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclaration
import xyz.chunkstories.api.graphics.rendergraph.RenderTaskInstance
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.representations.gatherRepresentations
import xyz.chunkstories.graphics.opengl.OpenglFrame
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem

class OpenglRenderGraph(val backend: OpenglGraphicsBackend, val dslCode: RenderGraphDeclaration.() -> Unit): Cleanable {
    val declaration = RenderGraphDeclaration().also(dslCode)

    val tasks: Map<String, OpenglRenderTask>
    val dispatchingSystems = mutableListOf<OpenglDispatchingSystem<*>>()

    init {
        tasks = declaration.renderTasks.values.map {
            val openglRenderTask = OpenglRenderTask(backend, this, it)
            Pair(it.name, openglRenderTask)
        }.toMap()
    }

    fun renderFrame(frame: OpenglFrame) {
        val mainCamera = backend.window.client.ingame?.player?.controlledEntity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val mainTaskName = "main"
        val mainTask = tasks[mainTaskName]!!
        val map = mutableMapOf<String, Any>()
        map["camera"] = mainCamera

        val graph = OpenglFrameGraph(frame, this, mainTask, mainCamera, map)
        val sequencedGraph = graph.sequenceGraph()

        val passInstances: List<PassInstance> = sequencedGraph.filterIsInstance<PassInstance>()
        val renderingContexts: List<RenderTaskInstance> = sequencedGraph.filterIsInstance<RenderTaskInstance>()
        val gathered = backend.graphicsEngine.gatherRepresentations(frame, passInstances, renderingContexts)

        // Fancy preparing of the representations to render
        val jobs = passInstances.map {
            mutableMapOf<OpenglDispatchingSystem.Drawer<*>, ArrayList<*>>()
        }

        for ((index, passInstance) in passInstances.withIndex()) {
            val openglPassInstance = passInstance as OpenglPassInstance

            val renderContextIndex = renderingContexts.indexOf(openglPassInstance.taskInstance)
            val ctxMask = 1 shl renderContextIndex
            val jobsForPassInstance = jobs[index]

            for (bucket in gathered.buckets.values) {
                val responsibleSystem = dispatchingSystems.find { it.representationName == bucket.representationName } ?: continue

                val drawers = openglPassInstance.pass.dispatchingDrawers.filter {
                    it.system == responsibleSystem
                }
                val drawersArray = drawers.toTypedArray()

                val allowedOutputs = drawers.map {
                    jobsForPassInstance.getOrPut(it) {
                        arrayListOf<Any>()
                    }
                }

                TODO()
                /*for (i in 0 until bucket.representations.size) {
                    val item = bucket.representations[i]
                    val mask = bucket.masks[i]

                    if (mask and ctxMask == 0)
                        continue

                    (responsibleSystem as OpenglDispatchingSystem<Representation>).sort(item, drawersArray, allowedOutputs as List<MutableList<Any>>)
                }*/
            }
        }

        var rootFbo = -1

        var passIndex = 0
        for (graphNodeIndex in 0 until sequencedGraph.size) {
            val graphNode = sequencedGraph[graphNodeIndex]

            when (graphNode) {
                is OpenglPassInstance -> {
                    val pass = graphNode.pass
                    val fboUsed = pass.render(frame, graphNode, jobs[passIndex])

                    if(graphNode == graph.rootNode.rootPassInstance)
                        rootFbo = fboUsed

                    passIndex++
                }
                is OpenglRenderTaskInstance -> {
                    graphNode.callbacks.forEach { it.invoke(graphNode) }
                }
            }
        }

        if(backend.openglSupport.dsaSupport) {
            glBlitNamedFramebuffer(rootFbo, 0,
                    0, backend.window.height, backend.window.width, 0,
                    0, 0, backend.window.width, backend.window.height,
                    GL_COLOR_BUFFER_BIT, GL_NEAREST)
        } else {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, rootFbo)
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0)
            glBlitFramebuffer(
                    0, backend.window.height, backend.window.width, 0,
                    0, 0, backend.window.width, backend.window.height,
                    GL_COLOR_BUFFER_BIT, GL_NEAREST)
        }
    }

    fun resizeBuffers() {
        tasks.values.forEach {
            it.buffers.values.forEach { it.resize() }
            it.passes.values.forEach { it.dumpFramebuffers() }
        }
    }

    override fun cleanup() {
        dispatchingSystems.forEach(Cleanable::cleanup)
        tasks.values.forEach(Cleanable::cleanup)
    }
}