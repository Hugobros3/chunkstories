package io.xol.chunkstories.graphics.common

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import kotlin.reflect.KClass

class ShaderMetadata(shaderString: String, customClassToStealLoader: Class<*>?) {
    /** interfaceblock structs yet to include (declared via #include struct but not reached yet) */
    internal val todo = mutableListOf<KClass<InterfaceBlock>>()
    internal val done = mutableListOf<KClass<InterfaceBlock>>()

    /** Stack of what interface block classes we're making representations for so we can catch loops */
    internal val stack = mutableListOf<KClass<InterfaceBlock>>()

    /** All the data structure that are explicitely included in the shader string *or* implicitely included due to use in an included struct */
    val structures = mutableListOf<InterfaceBlockRepresentation>()

    val glslWithAddedStructs: String

    init {
        val mappedLines = shaderString.lines().map {

            if (it.startsWith("#include struct")) {
                it.split(' ').getOrNull(2)?.replace("<", "")?.replace(">", "")?.let {
                    val classByThatName = (Class.forName(it, true, customClassToStealLoader?.classLoader ?: javaClass.classLoader)
                            ?: throw Exception("Couldn't find the class $it"))
                            .kotlin as? KClass<InterfaceBlock> ?: throw Exception("Specified class $it does not implement InterfaceBlock")
                    todo.add(classByThatName)
                    return@map classByThatName
                }
            }
            it
        }

        while (todo.isNotEmpty()) {
            val interfaceBlockClass = todo.removeAt(0)

            val representation = InterfaceBlockRepresentation(interfaceBlockClass, this)
            structures += representation
            done += interfaceBlockClass

            if (stack.size > 0) {
                println("Stack size > 0 after a conversion... hmmm")
                stack.clear()
            }
        }

        val noDuplicates = mutableListOf<KClass<InterfaceBlock>>()
        fun recursivelyAddWithRequirements(klass: KClass<InterfaceBlock>) : List<String> {
            if(noDuplicates.contains(klass))
                return emptyList()

            val ibRepresentation = structures.find { it.klass == klass } ?: throw Exception("Assertion failed catastrophically")
            noDuplicates.add(klass)

            val defs = mutableListOf<String>()
            ibRepresentation.requirements.forEach { defs.addAll(recursivelyAddWithRequirements(it)) }

            defs.add(ibRepresentation.generateGLSL())
            return defs
        }

        glslWithAddedStructs = mappedLines.joinToString(separator = "\n") { line ->
            when (line) {
                is String -> line
                is KClass<*> -> recursivelyAddWithRequirements(line as KClass<InterfaceBlock>).joinToString(separator = "\n")
                else -> throw Exception()
            }
        }

        //println(structures.map { it.toString() })
    }
}