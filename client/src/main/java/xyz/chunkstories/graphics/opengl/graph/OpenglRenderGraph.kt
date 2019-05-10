package xyz.chunkstories.graphics.opengl.graph

import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclaration
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.rendergraph.RenderTaskInstance
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.representations.gatherRepresentations
import xyz.chunkstories.graphics.opengl.OpenglFrame
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem

class OpenglRenderGraph(val backend: OpenglGraphicsBackend, val dslCode: RenderGraphDeclarationScript): Cleanable {
    val tasks: Map<String, OpenglRenderTask>
    val dispatchingSystems = mutableListOf<OpenglDispatchingSystem<*>>()

    init {
        tasks = RenderGraphDeclaration().also(dslCode).renderTasks.values.map {
            val openglRenderTask = OpenglRenderTask(backend, this, it)
            Pair(it.name, openglRenderTask)
        }.toMap()
    }

    fun renderFrame(frame: OpenglFrame) {
        // Gather the base information we need to start rendering
        //TODO make that configurable
        val mainCamera = backend.window.client.ingame?.player?.controlledEntity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val mainTaskName = "main"
        val mainTask = tasks[mainTaskName]!!
        val map = mutableMapOf<String, Any>()
        map["camera"] = mainCamera

        val graph = OpenglFrameGraph(frame, this, mainTask, mainCamera, map)
        val sequencedGraph = graph.sequenceGraph()

        val passInstances: Array<PassInstance> = sequencedGraph.filterIsInstance<PassInstance>().toTypedArray()
        val renderingContexts: Array<RenderTaskInstance> = sequencedGraph.filterIsInstance<RenderTaskInstance>().toTypedArray()
        val gathered = backend.graphicsEngine.gatherRepresentations(frame, passInstances, renderingContexts)

        // Fancy preparing of the representations to render
        val jobs = passInstances.map {
            mutableMapOf<OpenglDispatchingSystem.Drawer<*>, ArrayList<*>>()
        }

        for ((index, pass) in passInstances.withIndex()) {
            val pass = pass as OpenglFrameGraph.FrameGraphNode.OpenglPassInstance

            val renderContextIndex = renderingContexts.indexOf(pass.taskInstance)
            val ctxMask = 1 shl renderContextIndex
            val jobsForPassInstance = jobs[index]

            for ((key, contents) in gathered.buckets) {
                val responsibleSystem = dispatchingSystems.find { it.representationName == key } ?: continue

                val drawers = pass.pass.dispatchingDrawers.filter {
                    it.system == responsibleSystem
                }
                val drawersArray = drawers.toTypedArray()

                val allowedOutputs = drawers.map {
                    jobsForPassInstance.getOrPut(it) {
                        arrayListOf<Any>()
                    }
                }

                for (i in 0 until contents.representations.size) {
                    val item = contents.representations[i]
                    val mask = contents.masks[i]

                    if (mask and ctxMask == 0)
                        continue

                    (responsibleSystem as OpenglDispatchingSystem<Representation>).sort(item, drawersArray, allowedOutputs as List<MutableList<Any>>)
                }
            }
        }

        var passIndex = 0
        for (graphNodeIndex in 0 until sequencedGraph.size) {
            val graphNode = sequencedGraph[graphNodeIndex]

            when (graphNode) {
                is OpenglFrameGraph.FrameGraphNode.OpenglPassInstance -> {
                    val pass = graphNode.pass
                    pass.render(frame, graphNode, jobs[passIndex])

                    passIndex++
                }
                is OpenglFrameGraph.FrameGraphNode.OpenglRenderTaskInstance -> {
                    graphNode.callbacks.forEach { it.invoke(graphNode) }
                }
            }
        }

        //TODO blit
    }

    override fun cleanup() {
        dispatchingSystems.forEach(Cleanable::cleanup)
        tasks.values.forEach(Cleanable::cleanup)
    }
}