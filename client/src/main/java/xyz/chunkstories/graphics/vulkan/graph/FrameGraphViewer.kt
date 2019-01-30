package xyz.chunkstories.graphics.vulkan.graph

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.attribute.RankDir
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.*
import java.io.File

fun lookIDontCare(frameGraph: FrameGraph) {
    fun node2viz(node: FrameGraph.FrameGraphNode)
        = node("$node").let {
            when(node) {
                is FrameGraph.FrameGraphNode.PassNode -> {
                    it.with(Label.of("passNode(pass=${node.pass.declaration.name})")).with(Color.BLUE1)
                }
                is FrameGraph.FrameGraphNode.RenderingContextNode -> {
                    it.with(Label.of("renderContext(task=${node.renderContext.renderTask.declaration.name})")).with(Color.RED)
                }
            }
        }


    //val nodes = frameGraph.allNodes.map { node(Label.of("$it"))}
    val allLinks = frameGraph.allNodes.flatMap {fgNode ->
        val node = node2viz(fgNode)
        fgNode.depends.map { it2 -> node.link(to(node2viz(it2))) }
    }

    val g = graph("example1").directed()
            .graphAttr().with(RankDir.TOP_TO_BOTTOM)
            .with(
                    /*node("a").with(Color.RED).link(node("b")),
                    node("b").link(to(node("c")).with(Style.DASHED))*/
                    *(allLinks.toTypedArray())
            )
    Graphviz.fromGraph(g).render(Format.PNG).toFile(File("rendergraph.png"))
}