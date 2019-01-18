package xyz.chunkstories.graphics.common.shaders.compiler.postprocessing

fun annotateForNonUniformAccess(shaderCode: String): String =
        addNonUniformExt(shaderCode.replace(Regex("virtualTextures\\[(([a-z]|[A-Z]).*)\\]")) {
            "virtualTextures[nonuniformEXT(${it.groupValues[1]})]"
        })

private fun addNonUniformExt(shaderCode: String) = shaderCode.lines().mapNotNull { line ->
    if (line.startsWith("#version ")) {
        "$line\n#extension GL_EXT_nonuniform_qualifier : require\n"
    } else line
}.joinToString(separator = "\n")

fun removeVersionString(shaderCode: String) = shaderCode.lines().mapNotNull { line ->
    if (line.startsWith("#version "))
        null
    else line
}.joinToString(separator = "\n")