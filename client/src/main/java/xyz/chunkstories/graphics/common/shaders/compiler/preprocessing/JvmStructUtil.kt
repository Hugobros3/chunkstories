package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.graphics.common.shaders.GLSLType

fun GLSLType.JvmStruct.generateStructGLSL(): String {
    var glsl = "struct $glslToken {\n"

    glsl += innerGLSLCode()

    glsl += "};\n"

    return glsl
}

fun GLSLType.JvmStruct.innerGLSLCode() : String {
    var glsl = ""
    for (field in fields) {
        glsl += "\t${field.type.glslToken} ${field.name};\n"
    }
    return glsl
}