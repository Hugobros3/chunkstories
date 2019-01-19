package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.api.graphics.structs.UpdateFrequency
import xyz.chunkstories.graphics.common.shaders.GLSLType
import kotlin.reflect.KClass

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

fun KClass<InterfaceBlock>.updateFrequency(): UniformUpdateFrequency =
        this.annotations.filterIsInstance<UpdateFrequency>().firstOrNull()?.frequency ?: UniformUpdateFrequency.ONCE_PER_FRAME
