package xyz.chunkstories.graphics.vulkan.graph

import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.vulkan.swapchain.Frame

class VulkanFrameGraph(val frame: Frame, val renderGraph: VulkanRenderGraph, startTask: VulkanRenderTask, mainCamera: Camera, parameters: Map<String, Any>) {
    val rootFrameGraphNode: FrameGraphNode
    val allNodes = mutableSetOf<FrameGraphNode>()

    init {
        rootFrameGraphNode = FrameGraphNode.RenderingContextNode("main", this, startTask, mainCamera, parameters, null)
        allNodes.add(rootFrameGraphNode)
        rootFrameGraphNode.addDependencies()
    }

    sealed class FrameGraphNode(val frameGraph: VulkanFrameGraph) {
        val depends = mutableListOf<FrameGraphNode>()

        val renderGraph: VulkanRenderGraph
            get() = frameGraph.renderGraph

        class PassNode(graph: VulkanFrameGraph, override val context: RenderingContextNode, val vulkanPass: VulkanPass) : FrameGraphNode(graph), PassInstance {
            override val pass: PassDeclaration
                get() = vulkanPass.declaration

            lateinit var resolvedDepthBuffer: VulkanRenderBuffer
            lateinit var resolvedOutputs: Map<PassOutput, VulkanRenderBuffer>

            lateinit var commandBuffer: VkCommandBuffer

            override fun dispatchRenderTask(taskInstanceName: String, camera: Camera, renderTaskName: String, parameters: Map<String, Any>, callback: (RenderingContext) -> Unit) {

                val task = renderGraph.tasks[renderTaskName] ?: throw Exception("Can't find task $renderTaskName")
                val childNode = FrameGraphNode.RenderingContextNode(taskInstanceName, frameGraph, task, camera, parameters, callback)

                this.depends.add(childNode)
                this.frameGraph.allNodes.add(childNode)

                with(frameGraph) {
                    childNode.addDependencies()
                }

                context.artifacts.put(taskInstanceName, childNode)
            }

            val extraInputRenderBuffers = mutableListOf<VulkanRenderBuffer>()
            fun markRenderBufferAsInput(renderBuffer: VulkanRenderBuffer) {
                extraInputRenderBuffers.add(renderBuffer)
            }
        }

        class RenderingContextNode(override val name: String, graph: VulkanFrameGraph, val renderTask: VulkanRenderTask, override val camera: Camera, parameters: Map<String, Any>, val callback: ((RenderingContext) -> Unit)?) : FrameGraphNode(graph), RenderingContext {
            override val task: RenderTaskDeclaration
                get() = renderTask.declaration

            override val artifacts = mutableMapOf<String, Any>()
            override val parameters: MutableMap<String, Any> = parameters.toMutableMap()

            lateinit var rootPassInstance: PassNode

            init {
                this.parameters["camera"] = camera // Implicitly part of the parameters //TODO should we
            }
        }
    }

    fun FrameGraphNode.addDependencies() {
        when (this) {
            is FrameGraphNode.PassNode -> {
                for (dependency in this.pass.passDependencies) {
                    val passThatWeDependOn = this.context.renderTask.passes[dependency] ?: throw Exception("Can't find pass $dependency")

                    // Does a node for that pass in that context exist already
                    val existingDependencyNode = allNodes.find { it is FrameGraphNode.PassNode && it.vulkanPass == passThatWeDependOn && it.context == context }

                    val dependencyNode = existingDependencyNode ?: FrameGraphNode.PassNode(frameGraph, context, passThatWeDependOn)
                    depends.add(dependencyNode)

                    if (existingDependencyNode == null) {
                        allNodes.add(dependencyNode)
                        dependencyNode.addDependencies()
                    }
                }

                //TODO here goes dynamic rendertask deps from systems
                vulkanPass.drawingSystems.forEach {
                    it.registerAdditionalRenderTasks(this)
                }

                vulkanPass.dispatchingDrawers.forEach {
                    it.registerAdditionalRenderTasks(this)
                }
            }
            is FrameGraphNode.RenderingContextNode -> {
                val rootPass = this.renderTask.rootPass
                val passNode = FrameGraphNode.PassNode(frameGraph, this, rootPass)
                this.rootPassInstance = passNode
                depends.add(passNode)
                allNodes.add(passNode)
                passNode.addDependencies()
            }
        }
    }

    fun sequenceGraph(): List<FrameGraphNode> {
        val sortedList = mutableListOf<FrameGraphNode>()
        val todo = allNodes.toMutableSet()

        //TODO less naive graph sorting method
        outerWhile@
        while (todo.isNotEmpty()) {
            for (node in todo) {
                val outstandingDependencies = node.depends.count { !sortedList.contains(it) }
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
}