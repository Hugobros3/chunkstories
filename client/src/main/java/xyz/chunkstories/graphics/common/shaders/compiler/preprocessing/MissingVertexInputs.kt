package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.graphics.common.shaders.GLSLVertexInput
import xyz.chunkstories.graphics.common.shaders.compiler.AvailableVertexInput
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler

fun ShaderCompiler.removeMissingInputs(shaderCode: String, shaderInputs: List<GLSLVertexInput>, availableInputs: List<AvailableVertexInput>): String {

    var i = 0
    return shaderCode.lines().map { line ->
        val trimmed = line.trim()
        for(input in shaderInputs) {
            /*val match = "${output.name} ="
            val match2 = "${output.name}="

            if(trimmed.startsWith(match)) {
                val used = usedOutputs.outputs.find { it.name == output.name } != null
                if(!used) {
                    println("found unused shader input, killing it off !")
                    return@map line.replace(match, "//${output.format.glslToken} garbage${i++} =")
                }
            }*/

            val match3 = "in ${input.format.glslToken} ${input.name}"
            if(trimmed.startsWith(match3)) {
                val supplied = availableInputs.find { it.name == input.name } != null
                if(!supplied) {
                    //println("REPLACING $line with \n"+"#define ${input.name} ${input.format.glslToken}(0)")
                    return@map "#define ${input.name} ${input.format.glslToken}(0)"
                }
            }
        }
        line
    }.joinToString(separator = "\n")
}