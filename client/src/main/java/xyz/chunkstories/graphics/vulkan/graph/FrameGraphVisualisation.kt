package xyz.chunkstories.graphics.vulkan.graph

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.*
import java.io.File

fun exportRenderGraphPng(frameGraph: VulkanFrameGraph) {
    fun node2viz(node: VulkanFrameGraph.FrameGraphNode) = node("${node.hashCode()}").let {
        when (node) {
            is VulkanFrameGraph.FrameGraphNode.VulkanPassInstance -> {
                it.with(Label.of("passInstance(pass=${node.declaration.name})")).with(Color.BLUE1)
            }
            is VulkanFrameGraph.FrameGraphNode.VulkanRenderTaskInstance -> {
                it.with(Label.of("renderTaskInstance(name=${node.name} task=${node.declaration.name})")).with(Color.RED)
            }
        }
    }


    val vizNodes = frameGraph.nodes.associateWith { node2viz(it) }

    val g2 = graph("example1").directed()
            .with(
                    *(frameGraph.nodes.map { a->
                        val vizA = vizNodes[a]!!

                        vizA.link(*(a.dependencies.map { b ->
                            vizNodes[b]!!
                        }).toTypedArray())

                        }.toTypedArray())
            )
    Graphviz.fromGraph(g2).render(Format.PNG).toFile(File("rendergraph.png"))
}