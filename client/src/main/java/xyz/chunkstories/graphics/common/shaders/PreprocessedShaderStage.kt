/*package xyz.chunkstories.graphics.common.shaders

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import kotlin.reflect.KClass

/** Backend-agnostic preprocessing of GLSL shader stage snippets, crunches the custom GLSL flavour used by the game into a common Vulkan/OpenGL GLSL
 * subset, and also extracts a bunch of usefull metadata
 */
class PreprocessedShaderStage(private val factory: ShaderCompiler, private val originalShaderCode: String, private val stage: ShaderStage, val content: Content? = null, val shadersAssetBaseDir : String? = null) {
    /** interfaceblock structs yet to include (declared via #include struct but not reached yet) */
    internal val todo = mutableListOf<KClass<InterfaceBlock>>()
    //internal val done = mutableListOf<KClass<InterfaceBlock>>()

    /** Stack of what interface block classes we're making representations for so we can catch loops */
    internal val stack = mutableListOf<KClass<InterfaceBlock>>()

    var transformedCode: String
        private set

    lateinit var uniformBlocks: List<Pair<String, InterfaceBlockGLSLMapping>>
        private set

    lateinit var vertexInputs: List<VertexInputDeclaration>
        private set

    init {
        transformedCode = originalShaderCode

        processIncludeStructs()
        findAndInlineUniformBlock()

        if (stage == ShaderStage.VERTEX)
            findAndInlineInstanceDataInputs()
    }

    private fun processIncludeStructs() {
        val mappedLines = transformedCode.lines().map {

            if (it.startsWith("#include struct")) {
                it.split(' ').getOrNull(2)?.replace("<", "")?.replace(">", "")?.let {
                    val classByThatName = (Class.forName(it, true, factory.classLoader)
                            ?: throw Exception("Couldn't find the class $it"))
                            .kotlin as? KClass<InterfaceBlock> ?: throw Exception("Specified class $it does not implement InterfaceBlock")
                    todo.add(classByThatName)
                    return@map classByThatName
                }
            }
            else if(it.startsWith("#include ")) {
                it.split(' ')[1].let {
                    //val parent = asset!!.name.substring(0, asset!!.name.lastIndexOf('/'))
                    var includeDir = it
                    var resolvedDir = shadersAssetBaseDir!!
                    while(includeDir.startsWith("../")) {
                        resolvedDir = resolvedDir.substring(0, resolvedDir.lastIndexOf('/'))
                        includeDir = includeDir.substring(3)
                    }
                    resolvedDir = resolvedDir + "/" + includeDir

                    println(resolvedDir)
                    val referencedAsset = content!!.getAsset(resolvedDir)!!
                    val shaderCode = referencedAsset.reader().readText()
                    return@map shaderCode
                }
            }

            it
        }

        while (todo.isNotEmpty()) {
            val interfaceBlockClass = todo.removeAt(0)

            val representation = factory.structures.getOrPut(interfaceBlockClass) { InterfaceBlockGLSLMapping(interfaceBlockClass, factory, this) }
            //done += interfaceBlockClass

            if (stack.size > 0) {
                println("Stack size > 0 after a conversion... hmmm")
                stack.clear()
            }
        }

        val noDuplicates = mutableListOf<KClass<InterfaceBlock>>()
        fun recursivelyAddWithRequirements(klass: KClass<InterfaceBlock>): List<String> {
            if (noDuplicates.contains(klass))
                return emptyList()

            val ibRepresentation = factory.structures[klass] ?: throw Exception("Assertion failed catastrophically")
            noDuplicates.add(klass)

            val defs = mutableListOf<String>()
            ibRepresentation.requirements.forEach { defs.addAll(recursivelyAddWithRequirements(it)) }

            defs.add(ibRepresentation.generateStructGLSL())
            return defs
        }

        transformedCode = mappedLines.joinToString(separator = "\n") { line ->
            when (line) {
                is String -> line
                is KClass<*> -> recursivelyAddWithRequirements(line as KClass<InterfaceBlock>).joinToString(separator = "\n")
                else -> throw Exception()
            }
        }
    }

    /**
     * GLSL for Vulkan doesn't allow you to directly declare a uniform block using the layout of a struct, you always
     * need to declare the block structure in-place. This sucks and so here is a preprocessor to fix that.
     */
    private fun findAndInlineUniformBlock() {
        val list = mutableListOf<Pair<String, InterfaceBlockGLSLMapping>>()
        var processed = ""

        for (line in this.transformedCode.lines()) {
            var layoutQualifier = ""
            var declarationStrippedOfLayoutPrefix = line

            var translated: String? = line

            if (line.startsWith("layout")) {
                val layoutEndIndex = line.indexOf(')') + 1
                layoutQualifier = line.substring(0, layoutEndIndex)
                declarationStrippedOfLayoutPrefix = line.substring(layoutEndIndex).trim()
                //println("layout: $layoutQualifier $declarationStrippedOfLayoutPrefix")

                // If the layout block is on a line of it's own it'll just get forgotten
                // translated = null
            }


            if (declarationStrippedOfLayoutPrefix.startsWith("uniform")) {
                val uniformTypeName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(1)
                val uniformName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(2)?.trimEnd(';')

                factory.structures.values.find { it.glslToken == uniformTypeName }?.apply {
                    //this.sampleInstance as? UniformBlock ?: throw Exception("You must declare your InterfaceBlock as UniformBlock to use it in a Shader !")

                    println("Found inlineable uniform using a InterfaceBlock-based struct: $uniformName")
                    list += Pair(uniformName!!, this)

                    val inlinedUniformInterfaceBlockDeclaration = "layout(std140) uniform _inlined${uniformTypeName}_$uniformName {\n" + this.generateInnerGLSL() + "} $uniformName;\n"
                    translated = inlinedUniformInterfaceBlockDeclaration
                }
            }

            if (translated != null)
                processed += translated + "\n"
        }

        this.transformedCode = processed

        uniformBlocks = list
    }

    /** A similar problem to before, we want the synctatic sugar of having instance data live in classes in the JVM side, but GLSL disallows structs
     * for vertex input data, so we have to break down the struct in-place. Note that we can't have composite data types as instance data.
     *
     * Also, while this method goes through every vertex input declaration we take that chance to build some metadata so the backend can later
     * figure out what to do with those inputs.
     * */
    private fun findAndInlineInstanceDataInputs() {
        val list = mutableListOf<Pair<String, InterfaceBlockGLSLMapping>>()
        var processed = ""

        val inputs = mutableListOf<VertexInputDeclaration>()

        //var previousLine = ""
        for (line in this.transformedCode.lines()) {
            var translatedLine: String? = line

            var bareDeclaration = line
            if (line.startsWith("layout")) {
                val layoutEndIndex = line.indexOf(')') + 1
                bareDeclaration = line.substring(layoutEndIndex).trim()
            }

            if (bareDeclaration.startsWith("in")) {
                val typeName = bareDeclaration.split(' ').getOrNull(1)
                val inputName = bareDeclaration.split(' ').getOrNull(2)?.trimEnd(';')

                if (inputName == null)
                    continue
                if (typeName == null)
                    continue

                val interfaceBlock = factory.structures.values.find { it.glslToken == typeName }
                val baseType = GLSLType.get(typeName)

                when {
                    interfaceBlock != null -> {
                        println("Found inlineable instance data input using a InterfaceBlock-based struct: $inputName")
                        list += Pair(inputName, interfaceBlock)

                        //TODO create multiple inputs for each field in the interface block
                        //TODO and add those to the vertex inputs
                    }

                    baseType != null -> {
                        //val instanced = previousLine == "#instanced"
                        inputs.add(VertexInputDeclaration(inputName, baseType))
                    }

                    else -> throw Exception("What do I do with input type $typeName ? ")
                }
            }

            if(line == "instanced")
                translatedLine = null

            if (translatedLine != null)
                processed += translatedLine + "\n"
        }

        this.vertexInputs = inputs
        this.transformedCode = processed
    }

    data class VertexInputDeclaration(val name: String, val type: GLSLType)
}*/