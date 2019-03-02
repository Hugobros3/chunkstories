package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler

fun ShaderCompiler.findAndReplaceMaterialBoundResources(shaderCode: String, updatedWithMaterials: MutableSet<String>): String = shaderCode.lines().map { line ->
    when {
        line.startsWith("#material") -> {
            val materialBound = line.split(" ")[2].removeSuffix(";")
            updatedWithMaterials.add(materialBound)

            line.replace("#material", "uniform")
        }
        else -> line
    }
}.joinToString(separator = "\n")