package io.xol.chunkstories.renderer.opengl.shader;

import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

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
