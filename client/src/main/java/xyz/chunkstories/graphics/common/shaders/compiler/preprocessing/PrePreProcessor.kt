package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

//inlineifdef
sealed class PreProcessorBlock {
    class AcceptableBlock : PreProcessorBlock()
    class UnacceptableBlock : PreProcessorBlock()
}

fun runPrePreProcessor(shaderCode: String): String {
    val stack = mutableListOf<PreProcessorBlock>()
    val defines = mutableMapOf<String, String>()

    val processedLines = shaderCode.lines().mapNotNull { line ->
        val trimmedLine = line.trim()
        when {
            trimmedLine.startsWith("#define") -> {
                val key = trimmedLine.split(" ")[1]
                val value = trimmedLine.removePrefix("#define $key")
                println("found define '$key' => '$value'")
                defines.put(key, value)

                line
            }
            trimmedLine.startsWith("#ifdef") -> {
                val key = trimmedLine.split(" ")[1]

                if(defines.containsKey(key))
                    stack.add(0, PreProcessorBlock.AcceptableBlock())
                else
                    stack.add(0, PreProcessorBlock.UnacceptableBlock())

                null
            }
            trimmedLine.startsWith("#else") -> {
                val was = stack.removeAt(0)

                if(was is PreProcessorBlock.UnacceptableBlock)
                    stack.add(0, PreProcessorBlock.AcceptableBlock())
                else
                    stack.add(0, PreProcessorBlock.UnacceptableBlock())

                null
            }
            trimmedLine.startsWith("#endif") -> {
                stack.removeAt(0)

                null
            }
            else -> {
                if(stack.size == 0 || stack.find { it is PreProcessorBlock.UnacceptableBlock } == null)
                    line
                else
                    null
            }
        }
    }

    val processedText = processedLines.joinToString(separator = "\n")
    //println(processedText)

    return processedText
}