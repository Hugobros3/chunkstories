package xyz.chunkstories.graphics.vulkan.graph

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.*
import java.io.File


fun lookIDontCare(frameGraph: FrameGraph) {
    fun node2viz(node: FrameGraph.FrameGraphNode) = node("${node.hashCode()}").let {
        when (node) {
            is FrameGraph.FrameGraphNode.PassNode -> {
                it.with(Label.of("passNode(pass=${node.pass.declaration.name})")).with(Color.BLUE1)
            }
            is FrameGraph.FrameGraphNode.RenderingContextNode -> {
                it.with(Label.of("renderContext(task=${node.renderContext.renderTask.declaration.name})")).with(Color.RED)
            }
        }
    }


    val vizNodes = frameGraph.allNodes.associateWith { node2viz(it) }

    val g2 = graph("example1").directed()
            .with(
                    *(frameGraph.allNodes.map {a->
                        val vizA = vizNodes[a]!!

                        vizA.link(*(a.depends.map { b ->
                            vizNodes[b]!!
                        }).toTypedArray())

                        }.toTypedArray())
            )
    Graphviz.fromGraph(g2).render(Format.PNG).toFile(File("rendergraph.png"))
}