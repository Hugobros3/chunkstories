package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import kotlin.reflect.KClass

fun ShaderCompiler.processFileIncludes(shaderBaseDir: String, shaderCode: String): String = shaderCode.lines().map { line ->
    when {
        line.startsWith("#include") -> line.split(' ')[1].let { fileToInclude ->
            var includePath = fileToInclude

            //TODO this bit of code is duplicated and there is an util function for that
            var resolvedDir = shaderBaseDir
            while (includePath.startsWith("../")) {
                resolvedDir = resolvedDir.substring(0, resolvedDir.lastIndexOf('/'))
                includePath = includePath.substring(3)
            }
            resolvedDir = "$resolvedDir/$includePath"

            val referencedAsset = this.readShaderFile(resolvedDir) ?: throw Exception("Couldn't include file $fileToInclude")

            referencedAsset.reader().readText()
        }
        else -> line
    }
}.joinToString(separator = "\n")

fun ShaderCompiler.findUsedJvmClasses(shaderCode: String) : List<KClass<InterfaceBlock>>{
    val includedStructsList = mutableListOf<KClass<InterfaceBlock>>()

    for(line in shaderCode.lines()) {
        if(line.startsWith("#using struct")) {
            val className = line.split(" ").getOrNull(2) ?: throw Exception("Missing class name for #include struct statement")

            val classByThatName = (Class.forName(className, true, classLoader) ?: throw Exception("Couldn't find the class $className"))

            if(!InterfaceBlock::class.java.isAssignableFrom(classByThatName))
                throw Exception("Specified class $className does not implement InterfaceBlock")

            val kotlinClass = classByThatName.kotlin as KClass<InterfaceBlock>

            val interfaceBlockClass = (kotlinClass)

            includedStructsList.add(interfaceBlockClass)
        }
    }

    return includedStructsList
}

fun ShaderCompiler.addStructsDeclaration(shaderCode: String, list: List<GLSLType.JvmStruct>) : String {
    val dependenciesInOrder = mutableListOf<GLSLType.JvmStruct>()
    val toWriteQueue = mutableListOf<GLSLType.JvmStruct>()

    fun GLSLType.extractDeps() {
        when (this) {
            is GLSLType.JvmStruct -> {
                for (field in fields) {
                    field.type.extractDeps()
                }
                if (!dependenciesInOrder.contains(this)) {
                    dependenciesInOrder.add(this)
                    toWriteQueue.add(this)
                }
            }
            is GLSLType.Array -> this.baseType.extractDeps()
        }
    }

    for(usedStruct in list)
        usedStruct.extractDeps()

    return shaderCode.lines().mapNotNull {
        if(it.startsWith("#using struct")){
            val structName = it.split(" ")[2]
            val struct = list.find { it.kClass.qualifiedName == structName }!!
            struct.extractDeps()
            val txt = toWriteQueue.map { it.generateStructGLSL() }.joinToString(separator = "\n")
            toWriteQueue.clear()
            txt
        }
        else
            it
    }.joinToString(separator = "\n")
}

fun ShaderCompiler.inlineUniformStructs(shaderCode: String, structs: List<GLSLType.JvmStruct>) : String = shaderCode.lines().map {line ->
    if(line.startsWith("uniform")) {
        val structName = line.split(' ').getOrNull(1)
        val uniformName = line.split(' ').getOrNull(2)?.trimEnd(';')

        structs.find { it.glslToken == structName }?.run {
            "layout(std140) uniform _inlined${structName}_$uniformName {\n" + this.innerGLSLCode() + "} $uniformName;\n"
        } ?: line
    } else
        line
}.joinToString(separator = "\n")