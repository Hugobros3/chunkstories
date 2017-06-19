package io.xol.chunkstories.api.mesh;

import java.nio.FloatBuffer;
import java.util.Map;

public class MultiPartMesh extends Mesh {
	protected final Map<String, Integer> parts;
	
	public MultiPartMesh(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals, Map<String, Integer> parts) {
		super(vertices, textureCoordinates, normals);
		
		this.parts = parts;
	}

	public Iterable<String> parts()
	{
		return parts.keySet();
	}
	
	public Map<String, Integer> partsMap()
	{
		return parts;
	}
	
	/** Returns the vertices offset for the part or -1 if it's not present. */
	public int getOffsetForPart(String part)
	{
		Integer i = parts.get(part);
		if(i != null)
			return i;
		
		return -1;
	}
}
