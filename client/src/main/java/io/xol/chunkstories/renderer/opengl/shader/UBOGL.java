package io.xol.chunkstories.renderer.opengl.shader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.rendering.shader.ShaderBuffer;

public class UBOGL implements ShaderBuffer {
	protected int glId = -1;
	
	public UBOGL() {
		glId = glGenBuffers();
	}
	
	private void bind() {
		glBindBuffer(GL_UNIFORM_BUFFER, glId);
	}
	
	@Override
	public void upload(ByteBuffer data) {
		bind();
		data.flip();
		glBufferData(GL_UNIFORM_BUFFER, data, GL_STATIC_DRAW);
	}
	
	@Override
	public void destroy() {
		glDeleteBuffers(glId);
	}
}
