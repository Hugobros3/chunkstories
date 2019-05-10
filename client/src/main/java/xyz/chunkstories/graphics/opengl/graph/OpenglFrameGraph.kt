package xyz.chunkstories.graphics.opengl.graph

import org.joml.Vector2i
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.shader.ShaderResources
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.opengl.OpenglFrame

class OpenglFrameGraph(val frame: OpenglFrame, val renderGraph: OpenglRenderGraph, startTask: OpenglRenderTask, mainCamera: Camera, parameters: Map<String, Any>) {
    val rootNode: FrameGraphNode
    val nodes = mutableSetOf<FrameGraphNode>()

    init {
        rootNode = FrameGraphNode.OpenglRenderTaskInstance( this, null, "main", startTask, mainCamera, parameters)
        nodes.add(rootNode)
        rootNode.addDependencies()
    }

    sealed class FrameGraphNode(val frameGraph: OpenglFrameGraph) {
        val dependencies = mutableListOf<FrameGraphNode>()

        val renderGraph: OpenglRenderGraph
            get() = frameGraph.renderGraph

        class OpenglPassInstance(graph: OpenglFrameGraph, override val taskInstance: OpenglRenderTaskInstance, val pass: OpenglPass) : FrameGraphNode(graph), PassInstance {
            override val declaration: PassDeclaration = pass.declaration
            override val shaderResources = ShaderResources(null)
            override var renderTargetSize: Vector2i = Vector2i(0) // late-defined

            lateinit var preparedDrawingSystemsContexts: List<SystemExecutionContext>
            lateinit var preparedDispatchingSystemsContexts: List<SystemExecutionContext>

            override fun dispatchRenderTask(taskInstanceName: String, camera: Camera, renderTaskName: String, parameters: Map<String, Any>, callback: (RenderTaskInstance) -> Unit) {
                val taskToDispatch = renderGraph.tasks[renderTaskName] ?: throw Exception("Can't find task $renderTaskName")

                //TODO lookup taskInstanceName
                val childNode = FrameGraphNode.OpenglRenderTaskInstance(frameGraph, this, taskInstanceName, taskToDispatch, camera, parameters)
                childNode.callbacks.add(callback)

                this.dependencies.add(childNode)
                this.frameGraph.nodes.add(childNode)

                with(frameGraph) {
                    childNode.addDependencies()
                }

                taskInstance.artifacts[taskInstanceName] = childNode
            }
        }

        class OpenglRenderTaskInstance(graph: OpenglFrameGraph, override val requester: OpenglPassInstance?, override val name: String, val renderTask : OpenglRenderTask, override val camera: Camera, parameters: Map<String, Any>) : FrameGraphNode(graph), RenderTaskInstance {
            override val declaration: RenderTaskDeclaration = renderTask.declaration
            override val frame = frameGraph.frame

            override val artifacts = mutableMapOf<String, Any>()
            override val parameters: MutableMap<String, Any> = parameters.toMutableMap()

            val callbacks = mutableListOf<(RenderTaskInstance) -> Unit>()

            init {
                this.parameters["camera"] = camera // Implicitly part of the parameters //TODO should we
            }
        }
    }

    fun FrameGraphNode.addDependencies() {
        when (this) {
            is FrameGraphNode.OpenglPassInstance -> {
                for (dependency in this.declaration.passDependencies) {
                    val resolvedPass = this.taskInstance.renderTask.passes[dependency] ?: throw Exception("Can't find pass $dependency")

                    // Does a node for that pass in that context exist already
                    val existingDependencyNode = nodes.find { it is FrameGraphNode.OpenglPassInstance && it.pass == resolvedPass && it.taskInstance == taskInstance }

                    val dependencyNode = existingDependencyNode ?: FrameGraphNode.OpenglPassInstance(frameGraph, taskInstance, resolvedPass)
                    dependencies.add(dependencyNode)

                    if (existingDependencyNode == null) {
                        nodes.add(dependencyNode)
                        dependencyNode.addDependencies()
                    }
                }

                //TODO here goes dynamic rendertask deps from systems
                preparedDrawingSystemsContexts = pass.drawingSystems.map {
                    val ctx = object: SystemExecutionContext {
                        override val shaderResources: ShaderResources = ShaderResources(this@addDependencies.shaderResources)
                        override val passInstance: PassInstance = this@addDependencies
                    }

                    it.executePerFrameSetup(ctx)
                    ctx
                }

                preparedDispatchingSystemsContexts = pass.dispatchingDrawers.map {
                    val ctx = object: SystemExecutionContext {
                        override val shaderResources: ShaderResources = ShaderResources(this@addDependencies.shaderResources)
                        override val passInstance: PassInstance = this@addDependencies
                    }

                    it.executePerFrameSetup(ctx)
                    ctx
                }
            }
            is FrameGraphNode.OpenglRenderTaskInstance -> {
                val rootPass = this.renderTask.rootPass
                val passNode = FrameGraphNode.OpenglPassInstance(frameGraph, this, rootPass)
                dependencies.add(passNode)
                nodes.add(passNode)
                passNode.addDependencies()
            }
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
}