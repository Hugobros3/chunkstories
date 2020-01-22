package xyz.chunkstories.graphics.vulkan.graph

import org.joml.Vector2i
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.shader.ShaderResources
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.resources.VulkanShaderResourcesContext
import xyz.chunkstories.graphics.vulkan.shaders.extractInto
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.world.ViewportSize

class VulkanFrameGraph(val frame: VulkanFrame, val renderGraph: VulkanRenderGraph, startTask: VulkanRenderTask, mainCamera: Camera, parameters: Map<String, Any>) {
    val rootNode: FrameGraphNode
    val nodes = mutableSetOf<FrameGraphNode>()

    init {
        rootNode = VulkanRenderTaskInstance( this, null, "main", startTask, mainCamera, parameters)
        nodes.add(rootNode)
        rootNode.addDependencies()

        renderGraph.declaration.frameSetupHooks.forEach {
            frame.it()
        }
    }

    fun sequenceGraph(): List<FrameGraphNode> {
        val sortedList = mutableListOf<FrameGraphNode>()
        val todo = nodes.toMutableSet()

        //TODO less naive graph sorting method
        outerWhile@
        while (todo.isNotEmpty()) {
            for (node in todo) {
                val outstandingDependencies = node.dependencies.count { !sortedList.contains(it) }
                //println("node $node has $outstandingDependencies unsolved deps")
                if (outstandingDependencies > 0)
                    continue
                else {
                    //println("node $node can execute, adding to list")
                    sortedList.add(node)
                    todo.remove(node)
                    continue@outerWhile
                }
            }

            throw Exception("Loop detected in graph :(")
        }

        return sortedList
    }

    fun FrameGraphNode.addDependencies() {
        when (this) {
            is VulkanPassInstance -> {
                for (dependency in this.declaration.passDependencies) {
                    val resolvedPass = this.taskInstance.renderTask.passes[dependency] ?: throw Exception("Can't find pass $dependency")

                    // Does a node for that pass in that context exist already
                    val existingDependencyNode = nodes.find { it is VulkanPassInstance && it.pass == resolvedPass && it.taskInstance == taskInstance }

                    val dependencyNode = existingDependencyNode ?: VulkanPassInstance(frameGraph, taskInstance, resolvedPass)
                    dependencies.add(dependencyNode)

                    if (existingDependencyNode == null) {
                        nodes.add(dependencyNode)
                        dependencyNode.addDependencies()
                    }
                }

                declaration.setupLambdas.forEach { this.it() }
            }
            is VulkanRenderTaskInstance -> {
                val rootPass = this.renderTask.rootPass
                val passNode = VulkanPassInstance(frameGraph, this, rootPass)
                this.rootPassInstance = passNode
                dependencies.add(passNode)
                nodes.add(passNode)
                passNode.addDependencies()
            }
        }
    }
}

sealed class FrameGraphNode(val frameGraph: VulkanFrameGraph) {
    val frame = frameGraph.frame
    val dependencies = mutableListOf<FrameGraphNode>()

    val renderGraph: VulkanRenderGraph
        get() = frameGraph.renderGraph
}

class VulkanPassInstance(graph: VulkanFrameGraph, override val taskInstance: VulkanRenderTaskInstance, val pass: VulkanPass) : FrameGraphNode(graph), PassInstance {
    override val declaration: PassDeclaration = pass.declaration
    override val shaderResources = ShaderResources(taskInstance.shaderResources)
    override var renderTargetSize: Vector2i = Vector2i(0) // late-defined

    lateinit var resolvedDepthBuffer: VulkanRenderBuffer
    lateinit var resolvedOutputs: Map<PassOutput, VulkanRenderBuffer>
    lateinit var resolvedDepthAndColorBuffers: MutableList<VulkanRenderBuffer>

    lateinit var commandBuffer: VkCommandBuffer

    fun postResolve(resolvedDepthAndColorBuffers: MutableList<VulkanRenderBuffer>) {
        val viewportSize = ViewportSize()
        viewportSize.size.set(resolvedDepthAndColorBuffers[0].textureSize)
        this.shaderResources.supplyUniformBlock("viewportSize", viewportSize)

        this.renderTargetSize.set(resolvedDepthAndColorBuffers[0].textureSize)
        this.resolvedDepthAndColorBuffers = resolvedDepthAndColorBuffers
    }

    fun getBindingContext(pipeline: Pipeline): VulkanShaderResourcesContext {
        val shaderBindingContext = VulkanShaderResourcesContext(frame, frameGraph.renderGraph.backend.descriptorMegapool, pipeline)
        shaderResources.extractInto(shaderBindingContext, this)
        return shaderBindingContext
    }

    override fun dispatchRenderTask(taskInstanceName: String, camera: Camera, renderTaskName: String, parameters: Map<String, Any>, callback: (RenderTaskInstance) -> Unit) {
        val taskToDispatch = renderGraph.tasks[renderTaskName] ?: throw Exception("Can't find task $renderTaskName")

        //TODO lookup taskInstanceName
        val childNode = VulkanRenderTaskInstance(frameGraph, this, taskInstanceName, taskToDispatch, camera, parameters)
        childNode.callbacks.add(callback)

        this.dependencies.add(childNode)
        this.frameGraph.nodes.add(childNode)

        with(frameGraph) {
            childNode.addDependencies()
        }

        taskInstance.artifacts[taskInstanceName] = childNode
    }
}

class VulkanRenderTaskInstance(graph: VulkanFrameGraph, override val requester: VulkanPassInstance?, override val name: String, val renderTask: VulkanRenderTask, override val camera: Camera, parameters: Map<String, Any>) : FrameGraphNode(graph), RenderTaskInstance {
    var inOrderId = -1
    var mask = 0

    override val declaration: RenderTaskDeclaration = renderTask.declaration

    override val artifacts = mutableMapOf<String, Any>()
    override val parameters: MutableMap<String, Any> = parameters.toMutableMap()

    override val shaderResources: ShaderResources = ShaderResources(requester?.shaderResources ?: frame.shaderResources)

    val callbacks = mutableListOf<(RenderTaskInstance) -> Unit>()

    //TODO unused ?
    lateinit var rootPassInstance: VulkanPassInstance

    init {
        this.parameters["camera"] = camera // Implicitly part of the parameters
        shaderResources.supplyUniformBlock("camera", camera)
    }
}