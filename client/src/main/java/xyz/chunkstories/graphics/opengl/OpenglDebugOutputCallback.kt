package xyz.chunkstories.graphics.opengl

//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

import org.lwjgl.opengl.GLDebugMessageARBCallbackI
import org.slf4j.LoggerFactory

/** Recycled class from the legacy GL renderer, does nothing special other than pretty-printing debug messages */
class OpenGLDebugOutputCallback : GLDebugMessageARBCallbackI {

    override fun invoke(source: Int, type: Int, id: Int, severity: Int, length: Int, message: Long, userParam: Long)
    {
        //Don't need nvidia spam
        //if(source == GL_DEBUG_SOURCE_API_ARB && type == GL_DEBUG_TYPE_OTHER_ARB)
        //	return;

        var debugString = "GL:"
        debugString += when (source) {
            GL_DEBUG_SOURCE_API_ARB -> "API"
            GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB -> "WINDOW SYSTEM"
            GL_DEBUG_SOURCE_SHADER_COMPILER_ARB -> "SHADER COMPILER"
            GL_DEBUG_SOURCE_THIRD_PARTY_ARB -> "THIRD PARTY"
            GL_DEBUG_SOURCE_APPLICATION_ARB -> "APPLICATION"
            GL_DEBUG_SOURCE_OTHER_ARB -> "OTHER"
            else -> "Unknown (0x" + Integer.toHexString(source).toUpperCase() + ")"
        }

        debugString += ":"

        debugString += when (type) {
            GL_DEBUG_TYPE_ERROR_ARB -> "ERROR"
            GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB -> "DEPRECATED BEHAVIOR"
            GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB -> "UNDEFINED BEHAVIOR"
            GL_DEBUG_TYPE_PORTABILITY_ARB -> "PORTABILITY"
            GL_DEBUG_TYPE_PERFORMANCE_ARB -> "PERFORMANCE"
            GL_DEBUG_TYPE_OTHER_ARB -> "OTHER"
            else -> "Unknown (0x" + Integer.toHexString(type).toUpperCase() + ")"
        }

        debugString += ":"

        debugString += when (severity) {
            GL_DEBUG_SEVERITY_HIGH_ARB -> "HIGH"
            GL_DEBUG_SEVERITY_MEDIUM_ARB -> "MEDIUM"
            GL_DEBUG_SEVERITY_LOW_ARB -> "LOW"
            else -> "Unknown (0x" + Integer.toHexString(severity).toUpperCase() + ")"
        }

        debugString += ": $message"

        logger.info(debugString)

        if (type == GL_DEBUG_TYPE_ERROR_ARB) {
            Thread.dumpStack()
            errorHappened = true
        }

    }

    companion object {
        /** Severity levels.  */
        private val GL_DEBUG_SEVERITY_HIGH_ARB = 0x9146
        private val GL_DEBUG_SEVERITY_MEDIUM_ARB = 0x9147
        private val GL_DEBUG_SEVERITY_LOW_ARB = 0x9148

        /** Sources.  */
        private val GL_DEBUG_SOURCE_API_ARB = 0x8246
        private val GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB = 0x8247
        private val GL_DEBUG_SOURCE_SHADER_COMPILER_ARB = 0x8248
        private val GL_DEBUG_SOURCE_THIRD_PARTY_ARB = 0x8249
        private val GL_DEBUG_SOURCE_APPLICATION_ARB = 0x824A
        private val GL_DEBUG_SOURCE_OTHER_ARB = 0x824B

        /** Types.  */
        private val GL_DEBUG_TYPE_ERROR_ARB = 0x824C
        private val GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB = 0x824D
        private val GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB = 0x824E
        private val GL_DEBUG_TYPE_PORTABILITY_ARB = 0x824F
        private val GL_DEBUG_TYPE_PERFORMANCE_ARB = 0x8250
        private val GL_DEBUG_TYPE_OTHER_ARB = 0x8251

        private var errorHappened = false

        private val logger = LoggerFactory.getLogger("rendering.opengl.debug")

        fun didErrorHappen(): Boolean {
            if (errorHappened) {
                errorHappened = false
                return true
            }
            return false
        }
    }

}
