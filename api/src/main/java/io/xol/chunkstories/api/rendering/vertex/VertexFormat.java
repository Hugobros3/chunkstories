package io.xol.chunkstories.api.rendering.vertex;

import static io.xol.chunkstories.api.rendering.vertex.VertexFormat.GL_Abstraction.*;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public enum VertexFormat
{
	FLOAT(GL_FLOAT, 4), 
	HALF_FLOAT(GL_HALF_FLOAT, 2), 
	INTEGER(GL_INT, 4), 
	USHORT(GL_UNSIGNED_SHORT, 2), 
	NORMALIZED_USHORT(GL_UNSIGNED_SHORT, 2, true),
	UBYTE(GL_UNSIGNED_BYTE, 1), 
	NORMALIZED_UBYTE(GL_UNSIGNED_BYTE, 2, true),
	U1010102(GL_UNSIGNED_INT_2_10_10_10_REV, 1, true),//<On average, based on a vec4>
	SHORT(GL_SHORT, 2), 
	BYTE(GL_BYTE, 1),
	;

	//Removes LWJGL dependency
	interface GL_Abstraction {
		public static final int GL_FLOAT = 0x1406;
		public static final int GL_HALF_FLOAT = 0x140b;
		public static final int GL_INT = 0x1404;
		public static final int GL_UNSIGNED_SHORT = 0x1403;
		public static final int GL_UNSIGNED_BYTE = 0x1401;
		public static final int GL_UNSIGNED_INT_2_10_10_10_REV = 0x8368;
		public static final int GL_SHORT = 0x1402;
		public static final int GL_BYTE = 0x1400;
	}
	
	
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
