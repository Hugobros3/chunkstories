package io.xol.engine.graphics.geometry;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.nio.FloatBuffer;

import io.xol.chunkstories.api.rendering.pipeline.AttributeSource;

@Deprecated
public class FloatBufferAttributeSource implements AttributeSource
{
	static VerticesObject sorryILied = new VerticesObject();
	
	FloatBuffer buffer;
	int dimensions, stride;

	public FloatBufferAttributeSource(FloatBuffer buffer, int dimensions, int stride)
	{
		this.buffer = buffer;
		this.dimensions = dimensions;
		this.stride = stride;
	}

	public FloatBufferAttributeSource(FloatBuffer buffer, int dimensions)
	{
		this(buffer, dimensions, 0);
	}

	@Override
	public void setup(int gl_AttributeLocation)
	{
		sorryILied.uploadData(buffer);
		sorryILied.asAttributeSource(VertexFormat.FLOAT, dimensions, stride, 0L).setup(gl_AttributeLocation);
		//VerticesObject.unbind();
		//glVertexAttribPointer(gl_AttributeLocation, dimensions, false, stride, buffer);
	}

}
