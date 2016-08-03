package io.xol.engine.model;

import java.nio.FloatBuffer;
import java.util.Map;

import io.xol.engine.graphics.geometry.VerticesObject;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ObjMeshComplete extends ObjMeshRenderable
{
	ObjMeshComplete(int verticesCount, FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normalMapCoordinates, Map<String, Integer> boneGroups)
	{
		super();
		this.verticesCount = verticesCount;
		this.vertices = vertices;
		this.textureCoordinates = textureCoordinates;
		this.normals = normalMapCoordinates;
		this.boneGroups = boneGroups;
	}
	
	private FloatBuffer vertices;
	private FloatBuffer textureCoordinates;
	private FloatBuffer normals;
	
	public VerticesObject getDrawableModel()
	{
		if(texCoordDataOnGpu == null)
			this.uploadFloatBuffers(vertices, textureCoordinates, normals);
		
		return texCoordDataOnGpu;
	}
}
