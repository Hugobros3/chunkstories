package xyz.chunkstories.graphics.common.shaders.compiler.postprocessing

fun annotateForNonUniformAccess(shaderCode: String): String {
    val annotated = shaderCode.replace(Regex("textures2D\\[(([a-z]|[A-Z]).*)\\]")) {
        "textures2D[nonuniformEXT(${it.groupValues[1]})]"
    }
    return addNonUniformExt(annotated)
}

private fun addNonUniformExt(shaderCode: String) = shaderCode.lines().mapNotNull { line ->
    if (line.startsWith("#version ")) {
        """
            $line
            #extension GL_EXT_nonuniform_qualifier : require
        """.trimIndent()
    } else line
}.joinToString(separator = "\n")

fun addVirtualTexturingHeader(shaderCode: String) = shaderCode.lines().mapNotNull { line ->
    if (line.startsWith("#version ")) {
        """
            $line
            // Magic !
            #extension GL_EXT_nonuniform_qualifier : require

            uniform sampler defaultSampler;
            uniform texture2D textures2D[];
            #define vtexture2D(texId,coords) texture(sampler2D(textures2D[nonuniformEXT(texId)], defaultSampler), coords)
        """.trimIndent()
    } else line
}.joinToString(separator = "\n")

fun removeVersionString(shaderCode: String) = shaderCode.lines().mapNotNull { line ->
    if (line.startsWith("#version "))
        null
    else line
}.joinToString(separator = "\n")