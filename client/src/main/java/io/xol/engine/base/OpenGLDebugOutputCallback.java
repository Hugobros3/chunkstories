//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.base;

import org.lwjgl.opengl.GLDebugMessageARBCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenGLDebugOutputCallback implements GLDebugMessageARBCallbackI
{
	/** Severity levels. */
	private static final int
		GL_DEBUG_SEVERITY_HIGH_ARB = 0x9146,
		GL_DEBUG_SEVERITY_MEDIUM_ARB = 0x9147,
		GL_DEBUG_SEVERITY_LOW_ARB = 0x9148;

	/** Sources. */
	private static final int
		GL_DEBUG_SOURCE_API_ARB = 0x8246,
		GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB = 0x8247,
		GL_DEBUG_SOURCE_SHADER_COMPILER_ARB = 0x8248,
		GL_DEBUG_SOURCE_THIRD_PARTY_ARB = 0x8249,
		GL_DEBUG_SOURCE_APPLICATION_ARB = 0x824A,
		GL_DEBUG_SOURCE_OTHER_ARB = 0x824B;

	/** Types. */
	private static final int
		GL_DEBUG_TYPE_ERROR_ARB = 0x824C,
		GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB = 0x824D,
		GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB = 0x824E,
		GL_DEBUG_TYPE_PORTABILITY_ARB = 0x824F,
		GL_DEBUG_TYPE_PERFORMANCE_ARB = 0x8250,
		GL_DEBUG_TYPE_OTHER_ARB = 0x8251;
	
	@SuppressWarnings("unused")
	private final Thread mainGLThread;
	
	private static boolean errorHappened = false;
	
	public OpenGLDebugOutputCallback(Thread mainGLThread)
	{
		this.mainGLThread = mainGLThread;
		System.out.println("Callback initialized.");
	}

	@Override

	public void invoke(int source, int type, int id, int severity, int length, long message, long userParam)
	//public void handleMessage(int source, int type, int id, int severity, String message)
	{
		//Don't need nvidia spam
		//if(source == GL_DEBUG_SOURCE_API_ARB && type == GL_DEBUG_TYPE_OTHER_ARB)
		//	return;
		
		String debugString = "GL:";
		switch (source)
		{
		case GL_DEBUG_SOURCE_API_ARB:
			debugString += "API";
			break;
		case GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB:
			debugString += "WINDOW SYSTEM";
			break;
		case GL_DEBUG_SOURCE_SHADER_COMPILER_ARB:
			debugString += "SHADER COMPILER";
			break;
		case GL_DEBUG_SOURCE_THIRD_PARTY_ARB:
			debugString += "THIRD PARTY";
			break;
		case GL_DEBUG_SOURCE_APPLICATION_ARB:
			debugString += "APPLICATION";
			break;
		case GL_DEBUG_SOURCE_OTHER_ARB:
			debugString += "OTHER";
			break;
		default:
			debugString += printUnknownToken(source);
		}
		
		debugString += ":";

		switch (type)
		{
		case GL_DEBUG_TYPE_ERROR_ARB:
			debugString += "ERROR";
			break;
		case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB:
			debugString += "DEPRECATED BEHAVIOR";
			break;
		case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB:
			debugString += "UNDEFINED BEHAVIOR";
			break;
		case GL_DEBUG_TYPE_PORTABILITY_ARB:
			debugString += "PORTABILITY";
			break;
		case GL_DEBUG_TYPE_PERFORMANCE_ARB:
			debugString += "PERFORMANCE";
			break;
		case GL_DEBUG_TYPE_OTHER_ARB:
			debugString += "OTHER";
			break;
		default:
			debugString += printUnknownToken(type);
		}
		
		debugString += ":";

		switch (severity)
		{
		case GL_DEBUG_SEVERITY_HIGH_ARB:
			debugString += "HIGH";
			break;
		case GL_DEBUG_SEVERITY_MEDIUM_ARB:
			debugString += "MEDIUM";
			break;
		case GL_DEBUG_SEVERITY_LOW_ARB:
			debugString += "LOW";
			break;
		default:
			debugString += printUnknownToken(severity);
		}
		
		debugString += ":"+message;

		logger().info(debugString);
		
		if(type == GL_DEBUG_TYPE_ERROR_ARB)
		{
			Thread.dumpStack();
			
			errorHappened = true;
		}

	}
	
	private static final Logger logger = LoggerFactory.getLogger("rendering.opengl.debug");
	public Logger logger() {
		return logger;
	}
	
	public static boolean didErrorHappen()
	{
		if(errorHappened)
		{
			errorHappened = false;
			return true;
		}
		return false;
	}

	private String printUnknownToken(final int token)
	{
		return "Unknown (0x" + Integer.toHexString(token).toUpperCase() + ")";
	}

}
