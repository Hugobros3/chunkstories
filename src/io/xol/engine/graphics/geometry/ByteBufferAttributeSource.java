package io.xol.engine.graphics.geometry;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.rendering.pipeline.AttributeSource;

@Deprecated
public class ByteBufferAttributeSource implements AttributeSource
{
	static VerticesObject sorryILied = new VerticesObject();
	
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
		sorryILied.uploadData(byteBuffer);
		sorryILied.asAttributeSource(format, dimensions, stride, 0L).setup(gl_AttributeLocation);
		//VerticesObject.unbind();
		//glVertexAttribPointer(gl_AttributeLocation, dimensions, format.glId, format.normalized, stride, byteBuffer);
	}
}
