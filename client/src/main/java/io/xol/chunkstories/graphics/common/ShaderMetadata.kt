package io.xol.chunkstories.graphics.common

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import org.joml.Matrix3f
import kotlin.reflect.KClass

class ShaderMetadata(val shaderString: String, customClassToStealLoader: Class<*>?) {
    /** interfaceblock structs yet to include (declared via #include struct but not reached yet) */
    internal val todo = mutableListOf<KClass<InterfaceBlock>>()
    internal val done = mutableListOf<KClass<InterfaceBlock>>()

    /** Stack of what interface block classes we're making representations for so we can catch loops */
    internal val stack = mutableListOf<KClass<InterfaceBlock>>()

    /** All the data structure that are explicitely included in the shader string *or* implicitely included due to use in an included struct */
    val structures = mutableListOf<InterfaceBlockRepresentation>()

    init {
        shaderString.lines().filter { it.startsWith("#include struct") }.forEach {
            it.split(' ').getOrNull(2)?.let { it.replace("<", "").replace(">", "").let {
                println("wow")
                val classByThatName = Class.forName(it, true, customClassToStealLoader?.classLoader ?: javaClass.classLoader) ?: throw Exception("Couldn't find the class $it")
                todo.add(classByThatName.kotlin as? KClass<InterfaceBlock> ?: throw Exception("Specified class $it does not implement InterfaceBlock"))
            } }
        }

        while(todo.isNotEmpty()) {
            val interfaceBlockClass = todo.removeAt(0)

            val representation = InterfaceBlockRepresentation(interfaceBlockClass, this)
            structures += representation
            done += interfaceBlockClass

            if(stack.size > 0) {
                println("Stack size > 0 after a conversion... hmmm")
                stack.clear()
            }
        }

        println(structures.map { it.toString() })
    }
}