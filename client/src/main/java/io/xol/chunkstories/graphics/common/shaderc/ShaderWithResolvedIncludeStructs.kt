package io.xol.chunkstories.graphics.common.shaderc

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import kotlin.reflect.KClass

//TODO move constructor to a ShaderFactory method
class ShaderWithResolvedIncludeStructs(val factory: ShaderFactory, shaderString: String) {
    /** interfaceblock structs yet to include (declared via #include struct but not reached yet) */
    internal val todo = mutableListOf<KClass<InterfaceBlock>>()
    //internal val done = mutableListOf<KClass<InterfaceBlock>>()

    /** Stack of what interface block classes we're making representations for so we can catch loops */
    internal val stack = mutableListOf<KClass<InterfaceBlock>>()

    val transformedCode: String

    init {
        val mappedLines = shaderString.lines().map {

            if (it.startsWith("#include struct")) {
                it.split(' ').getOrNull(2)?.replace("<", "")?.replace(">", "")?.let {
                    val classByThatName = (Class.forName(it, true, factory.classLoader)
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

            val representation = factory.structures.getOrPut(interfaceBlockClass) { InterfaceBlockGLSLMapping(interfaceBlockClass, this) }
            //done += interfaceBlockClass

            if (stack.size > 0) {
                println("Stack size > 0 after a conversion... hmmm")
                stack.clear()
            }
        }

        val noDuplicates = mutableListOf<KClass<InterfaceBlock>>()
        fun recursivelyAddWithRequirements(klass: KClass<InterfaceBlock>) : List<String> {
            if(noDuplicates.contains(klass))
                return emptyList()

            val ibRepresentation = factory.structures[klass] ?: throw Exception("Assertion failed catastrophically")
            noDuplicates.add(klass)

            val defs = mutableListOf<String>()
            ibRepresentation.requirements.forEach { defs.addAll(recursivelyAddWithRequirements(it)) }

            defs.add(ibRepresentation.generateGLSL())
            return defs
        }

        transformedCode = mappedLines.joinToString(separator = "\n") { line ->
            when (line) {
                is String -> line
                is KClass<*> -> recursivelyAddWithRequirements(line as KClass<InterfaceBlock>).joinToString(separator = "\n")
                else -> throw Exception()
            }
        }

        //println(structures.map { it.toString() })
    }
}