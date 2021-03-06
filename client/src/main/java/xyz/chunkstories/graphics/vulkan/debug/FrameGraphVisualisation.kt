package xyz.chunkstories.graphics.vulkan.debug

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.*
import xyz.chunkstories.graphics.vulkan.graph.FrameGraphNode
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderTaskInstance
import java.io.File

fun exportRenderGraphPng(frameGraph: VulkanFrameGraph) {
    fun node2viz(node: FrameGraphNode) = node("${node.hashCode()}").let {
        when (node) {
            is VulkanPassInstance -> {
                it.with(Label.of("passInstance(pass=${node.declaration.name})")).with(Color.BLUE1)
            }
            is VulkanRenderTaskInstance -> {
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