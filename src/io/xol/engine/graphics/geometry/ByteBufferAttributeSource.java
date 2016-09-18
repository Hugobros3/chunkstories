package io.xol.engine.graphics.geometry;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.rendering.pipeline.AttributeSource;

import static org.lwjgl.opengl.GL20.*;

public class ByteBufferAttributeSource implements AttributeSource
{
	ByteBuffer byteBuffer;
	VertexFormat format;
	int dimensions, stride;
	
	public ByteBufferAttributeSource(ByteBuffer byteBuffer, VertexFormat format, int dimensions, int stride)
	{
		this.byteBuffer = byteBuffer;
		this.format = format;
		this.dimensions = dimensions;
		this.stride = stride;
	}

	@Override
	public void setup(int gl_AttributeLocation)
	{
		VerticesObject.unbind();
		glVertexAttribPointer(gl_AttributeLocation, dimensions, format.glId, format.normalized, stride, byteBuffer);
	}
}
