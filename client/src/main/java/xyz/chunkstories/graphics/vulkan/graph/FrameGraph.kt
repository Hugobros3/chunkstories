package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.graphics.rendergraph.RenderingContext
import xyz.chunkstories.api.graphics.structs.Camera

class FrameGraph(val renderGraph: VulkanRenderGraph, startTask: VulkanRenderTask, mainCamera: Camera, parameters: Map<String, Any>) {
    val rootFrameGraphNode: FrameGraphNode
    val allNodes = mutableSetOf<FrameGraphNode>()

    init {
        rootFrameGraphNode = FrameGraphNode.RenderingContextNode(this, startTask, mainCamera, parameters, null)
        allNodes.add(rootFrameGraphNode)
        rootFrameGraphNode.addDependencies()
    }

    sealed class FrameGraphNode(val frameGraph: FrameGraph) {
        val depends = mutableListOf<FrameGraphNode>()

        class PassNode(graph: FrameGraph, val taskNode: RenderingContextNode, val pass: VulkanPass) : FrameGraphNode(graph)

        class RenderingContextNode(graph: FrameGraph, renderTask: VulkanRenderTask, camera: Camera, parameters: Map<String, Any>, callback: (Map<String, Any>.() -> Unit)?) : FrameGraphNode(graph) {
            val renderContext: FrameGraph.VulkanRenderingContext = VulkanRenderingContext(this, renderTask, camera, parameters, callback)
        }
    }

    class VulkanRenderingContext(val frameGraphNode: FrameGraphNode.RenderingContextNode, val renderTask: VulkanRenderTask, override val camera: Camera, parameters: Map<String, Any>, val callback: (Map<String, Any>.() -> Unit)?) : RenderingContext {
        override val artifacts = mutableMapOf<String, Any>()

        override val parameters: MutableMap<String, Any>

        init {
            this.parameters = parameters.toMutableMap()
            this.parameters["camera"] = camera
        }

        override fun dispatchRenderTask(camera: Camera, renderTaskName: String, parameters: Map<String, Any>, callback: Map<String, Any>.() -> Unit) {
            throw Exception("actuall no")
        }
    }

    inner class RenderTaskDispatching(private val passNode: FrameGraphNode.PassNode) {
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
    }

    fun FrameGraphNode.addDependencies() {
        when (this) {
            is FrameGraphNode.PassNode -> {
                for (dependency in this.pass.declaration.passDependencies) {
                    val passThatWeDependOn = this.taskNode.renderContext.renderTask.passes[dependency] ?: throw Exception("Can't find pass $dependency")

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
                val dispatching = RenderTaskDispatching(this)
                pass.drawingSystems.forEach {
                    it.registerAdditionalRenderTasks(this.taskNode.renderContext, dispatching)
                }
            }
            is FrameGraphNode.RenderingContextNode -> {
                val rootPass = this.renderContext.renderTask.rootPass
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