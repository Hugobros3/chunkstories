package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.api.graphics.rendergraph.PassOutputsDeclaration
import xyz.chunkstories.graphics.common.shaders.GLSLFragmentOutput
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler

fun ShaderCompiler.removeUnusedOutputs(shaderCode: String, shaderOutputs: List<GLSLFragmentOutput>, usedOutputs: PassOutputsDeclaration): String {

    var i = 0

        return shaderCode.lines().map { line ->
            val trimmed = line.trim()
            for(output in shaderOutputs) {
                val match = "${output.name} ="
                val match2 = "${output.name}="

                if(trimmed.startsWith(match)) {
                    val used = usedOutputs.outputs.find { it.name == output.name } != null
                    if(!used) {
                        println("found unused shader input, killing it off !")
                        return@map line.replace(match, "//${output.format.glslToken} garbage${i++} =")
                    }
                }

                val match3 = "out ${output.format.glslToken} ${output.name}"
                if(trimmed.startsWith(match3)) {
                    val used = usedOutputs.outputs.find { it.name == output.name } != null
                    if(!used) {
                        return@map "//unused output $match3"
                    }
                }
            }
                line
        }.joinToString(separator = "\n")
}