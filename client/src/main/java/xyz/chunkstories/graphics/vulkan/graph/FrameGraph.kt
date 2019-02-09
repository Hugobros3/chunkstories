package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.graphics.rendergraph.RenderingContext
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.vulkan.swapchain.Frame

class FrameGraph(val frame: Frame, val renderGraph: VulkanRenderGraph, startTask: VulkanRenderTask, mainCamera: Camera, parameters: Map<String, Any>) {
    val rootFrameGraphNode: FrameGraphNode
    val allNodes = mutableSetOf<FrameGraphNode>()

    init {
        rootFrameGraphNode = FrameGraphNode.RenderingContextNode(this, startTask, mainCamera, parameters, null)
        allNodes.add(rootFrameGraphNode)
        rootFrameGraphNode.addDependencies()
    }

    sealed class FrameGraphNode(val frameGraph: FrameGraph) {
        val depends = mutableListOf<FrameGraphNode>()

        val renderGraph: VulkanRenderGraph
            get() = frameGraph.renderGraph

        class PassNode(graph: FrameGraph, val taskNode: RenderingContextNode, val pass: VulkanPass) : FrameGraphNode(graph) {
            fun dispatchRenderTask(taskInstanceName: String, camera: Camera, renderTaskName: String, parameters: Map<String, Any>, callback: (RenderingContextNode) -> Unit) {

                val task = renderGraph.tasks[renderTaskName] ?: throw Exception("Can't find task $renderTaskName")
                val childNode = FrameGraphNode.RenderingContextNode(frameGraph, task, camera, parameters, callback)

                this.depends.add(childNode)
                this.frameGraph.allNodes.add(childNode)

                with(frameGraph) {
                    childNode.addDependencies()
                }

                taskNode.artifacts.put(taskInstanceName, childNode)
            }

            val extraInputRenderBuffers = mutableListOf<VulkanRenderBuffer>()
            fun markRenderBufferAsInput(renderBuffer: VulkanRenderBuffer) {
                extraInputRenderBuffers.add(renderBuffer)
            }
        }

        class RenderingContextNode(graph: FrameGraph, val renderTask: VulkanRenderTask, override val camera: Camera, parameters: Map<String, Any>, val callback: ((RenderingContextNode) -> Unit)?) : FrameGraphNode(graph), RenderingContext {
            override val artifacts = mutableMapOf<String, Any>()
            override val parameters: MutableMap<String, Any> = parameters.toMutableMap()

            init {
                this.parameters["camera"] = camera // Implicitly part of the parameters //TODO should we
            }
        }
    }

    /*inner class VulkanPassInstance(val frameGraphNode: FrameGraphNode.PassNode) {



    }

    inner class VulkanRenderingContext(val frameGraphNode: FrameGraphNode.RenderingContextNode/*, val renderTask: VulkanRenderTask, override val camera: Camera, parameters: Map<String, Any>, val callback: (Map<String, Any>.() -> Unit)?*/) : RenderingContext {
        override val artifacts = mutableMapOf<String, Any>()

        override val parameters: MutableMap<String, Any> = parameters.toMutableMap()

        init {
            this.parameters["camera"] = camera // Implicitly part of the parameters //TODO should we
        }
    }*/

    /*inner class RenderTaskDispatching(private val passNode: FrameGraphNode.PassNode) {
        fun dispatchRenderTask(camera: Camera, renderTaskName: String, parameters: Map<String, Any>, callback: Map<String, Any>.() -> Unit) {

            val task = renderGraph.tasks[renderTaskName] ?: throw Exception("Can't find task $renderTaskName")
            val childNode = FrameGraphNode.RenderingContextNode(this@FrameGraph, task, camera, parameters, callback)

            passNode.depends.add(childNode)
            //println("NODE DEPENDENCY XD $passNode to $childNode")
            allNodes.add(childNode)

            with(passNode.frameGraph) {
                childNode.addDependencies()
            }

            passNode.taskNode.renderContext.artifacts.put("TENTATIVE_SYNTAX_SUBTASK", childNode.renderContext)
        }
    }*/

    fun FrameGraphNode.addDependencies() {
        when (this) {
            is FrameGraphNode.PassNode -> {
                for (dependency in this.pass.declaration.passDependencies) {
                    val passThatWeDependOn = this.taskNode.renderTask.passes[dependency] ?: throw Exception("Can't find pass $dependency")

                    // Does a node for that pass in that context exist already
                    val existingDependencyNode = allNodes.find { it is FrameGraphNode.PassNode && it.pass == passThatWeDependOn && it.taskNode == taskNode }

                    val dependencyNode = existingDependencyNode ?: FrameGraphNode.PassNode(frameGraph, taskNode, passThatWeDependOn)
                    depends.add(dependencyNode)

                    if (existingDependencyNode == null) {
                        allNodes.add(dependencyNode)
                        dependencyNode.addDependencies()
                    }
                }

                //TODO here goes dynamic rendertask deps from systems
                pass.drawingSystems.forEach {
                    it.registerAdditionalRenderTasks(this)
                }
            }
            is FrameGraphNode.RenderingContextNode -> {
                val rootPass = this.renderTask.rootPass
                val passNode = FrameGraphNode.PassNode(frameGraph, this, rootPass)
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