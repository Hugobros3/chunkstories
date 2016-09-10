package io.xol.engine.graphics.geometry;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

public enum VertexFormat
{
	FLOAT(GL_FLOAT, 4), 
	HALF_FLOAT(GL_HALF_FLOAT, 2), 
	INTEGER(GL_INT, 4), 
	USHORT(GL_UNSIGNED_SHORT, 2), 
	NORMALIZED_USHORT(GL_UNSIGNED_SHORT, 2, true),
	UBYTE(GL_UNSIGNED_BYTE, 2), 
	NORMALIZED_UBYTE(GL_UNSIGNED_BYTE, 2, true),
	U1010102(GL_UNSIGNED_INT_2_10_10_10_REV, 1, true),//<On average, based on a vec4>
	SHORT(GL_SHORT, 2),
	;
	
	VertexFormat(int id, int bytes)
	{
		this(id, bytes, false);
	}
	
	VertexFormat(int gl_Id, int bytes, boolean normalized)
	{
		this.glId = gl_Id;
		this.bytesUsed = bytes;
		this.normalized = normalized;
	}
	
	public final int glId;
	public final boolean normalized;
	public final int bytesUsed;

	public int getBytesPerVertice()
	{
		return bytesUsed;
	}
}
