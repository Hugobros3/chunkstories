package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.graphics.common.shaders.GLSLResource
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.GLSLVertexInput

/* Very crude (but thus simple to understand) reflection of the shader resources by analysing their source code */
fun analyseVertexShaderInputs(shaderCode: String) : List<GLSLVertexInput> {
    val inputs = mutableListOf<GLSLVertexInput>()

    var i = 0
    for(line in shaderCode.lines()) {
        if(line.startsWith("in")) {
            val glslType = line.split(" ")[1]
            val name = line.split(" ")[2]

            inputs.add(GLSLVertexInput(name, GLSLType.BaseType.get(glslType)!!, i++))
        }
    }

    return inputs
}

//TODO fragment outputs