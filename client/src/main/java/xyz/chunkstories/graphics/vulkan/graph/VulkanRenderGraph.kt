package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.rendergraph.*
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.swapchain.Frame

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, val dslCode: RenderGraphDeclarationScript) : Cleanable {
    val taskDeclarations: List<RenderTaskDeclaration>
    val tasks: Map<String, VulkanRenderTask>

    init {
        taskDeclarations = RenderGraphDeclaration().also(dslCode).renderTasks.values.toList()
        tasks = taskDeclarations.map {
            val vulkanRenderTask = VulkanRenderTask(backend, it)
            Pair(it.name, vulkanRenderTask)
        }.toMap()
    }

    fun renderFrame(frame: Frame) {
        //TODO make that configurable
        val mainCamera = backend.window.client.ingame?.player?.controlledEntity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val mainTaskName = "main"

        val mainTask = tasks[mainTaskName]!!
        val graph = FrameGraph(mainTask, mainCamera)
    }

    sealed class Node {
        val depends = mutableListOf<Node>()

        data class PassNode(val contextNode: RenderContextNode, val pass: VulkanPass) : Node()
        data class RenderContextNode(val renderContext: FrameGraph.VulkanRenderingContext) : Node()
    }

    inner class FrameGraph(startTask: VulkanRenderTask, mainCamera: Camera) {
        val rootNode: Node
        val allNodes = mutableSetOf<Node>()

        init {
            rootNode = Node.RenderContextNode(VulkanRenderingContext(startTask, mainCamera, emptyMap(), null))
            rootNode.addDependencies()
        }

        inner class VulkanRenderingContext(val renderTask: VulkanRenderTask, override val camera: Camera, override val parameters: Map<String, Any>, val callback: (Map<String, Any>.() -> Unit)?) : RenderingContext {
            //override val bindings: ShaderBindingInterface
            //    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

            override val artifacts =  mutableMapOf<String, Any>()

            override fun dispatchRenderTask(camera: Camera, renderTaskName: String, parameters: Map<String, Any>, callback: Map<String, Any>.() -> Unit) {
                // Hack because we can't have the reference in both way :(
                val thisNode = allNodes.find { it is Node.RenderContextNode && it.renderContext == this }!!

                val task = tasks[renderTaskName]  ?: throw Exception("Can't find task $renderTaskName")
                val childNode = Node.RenderContextNode(VulkanRenderingContext(task, camera, parameters, callback))

                thisNode.depends.add(childNode)
                allNodes.add(childNode)

                childNode.addDependencies()
            }
        }

        private fun Node.addDependencies() {
            when (this) {
                is Node.PassNode -> {
                    for (dependency in this.pass.declaration.passDependencies) {
                        val passThatWeDependOn = this.contextNode.renderContext.renderTask.passes[dependency] ?: throw Exception("Can't find pass $dependency")

                        // Does a node for that pass in that context exist already
                        val existingDependencyNode = allNodes.find { it is Node.PassNode && it.pass == passThatWeDependOn && it.contextNode == contextNode }

                        val dependencyNode = existingDependencyNode ?: Node.PassNode(contextNode, passThatWeDependOn)
                        depends.add(dependencyNode)

                        if (existingDependencyNode == null) {
                            allNodes.add(dependencyNode)
                            dependencyNode.addDependencies()
                        }
                    }

                    //TODO here goes dynamic rendertask deps from systems
                    /*pass.drawingSystems.forEach {
                        it.registerAdditionalRenderTasks(this.contextNode.renderContext)
                    }*/
                }
                is Node.RenderContextNode -> {
                    val rootPass = this.renderContext.renderTask.rootPass
                    val passNode = Node.PassNode(this, rootPass)
                    depends.add(passNode)
                    allNodes.add(passNode)
                    passNode.addDependencies()
                }
            }
        }
    }

    fun resizeBuffers() {
        tasks.values.forEach {
            it.buffers.values.forEach { it.resize() }
            it.passes.values.forEach { it.recreateFramebuffer() }
        }
    }

    override fun cleanup() {
        tasks.values.forEach(Cleanable::cleanup)
    }
}