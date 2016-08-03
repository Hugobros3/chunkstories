package io.xol.engine.model;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import io.xol.engine.animation.AnimationData;
import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.RenderableAnimatable;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ObjMeshRenderable implements RenderableAnimatable
{
	protected ObjMeshRenderable()
	{

	}

	ObjMeshRenderable(int verticesCount, FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normalMapCoordinates, Map<String, Integer> boneGroups)
	{
		this.verticesCount = verticesCount;
		this.boneGroups = boneGroups;

		uploadFloatBuffers(vertices, textureCoordinates, normalMapCoordinates);
	}

	ObjMeshRenderable(int verticesCount, VerticesObject verticesDataOnGpu, VerticesObject texCoordDataOnGpu, VerticesObject normalsDataOnGpu, Map<String, Integer> boneGroups)
	{
		this.verticesCount = verticesCount;
		this.verticesDataOnGpu = verticesDataOnGpu;
		this.texCoordDataOnGpu = texCoordDataOnGpu;
		this.normalsDataOnGpu = normalsDataOnGpu;
		this.boneGroups = boneGroups;
	}

	protected void uploadFloatBuffers(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals)
	{
		verticesDataOnGpu = new VerticesObject();
		texCoordDataOnGpu = new VerticesObject();
		normalsDataOnGpu = new VerticesObject();

		System.out.println("UPLOAD" + vertices);
		verticesDataOnGpu.uploadData(vertices);
		texCoordDataOnGpu.uploadData(textureCoordinates);
		normalsDataOnGpu.uploadData(normals);
	}

	protected int verticesCount;
	protected Map<String, Integer> boneGroups = new HashMap<String, Integer>();
	protected VerticesObject verticesDataOnGpu;
	protected VerticesObject texCoordDataOnGpu;
	protected VerticesObject normalsDataOnGpu;

	public VerticesObject getDrawableModel()
	{
		return verticesDataOnGpu;
	}

	@Override
	public void render(RenderingContext renderingContext)
	{
		internalRenderer(renderingContext, null, 0.0, false, (String[]) null);

	}

	@Override
	public void render(RenderingContext renderingContext, AnimationData skeleton, double animationTime)
	{
		internalRenderer(renderingContext, skeleton, animationTime, false, (String[]) null);
	}

	@Override
	public void renderParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts)
	{
		internalRenderer(renderingContext, skeleton, animationTime, false, parts);
	}

	@Override
	public void renderButParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts)
	{
		internalRenderer(renderingContext, skeleton, animationTime, true, parts);
	}

	private void internalRenderer(RenderingContext renderingContext, AnimationData skeleton, double animationTime, boolean exclude, String... parts)
	{
		//System.out.println("slt");

		int vertexIn = renderingContext.getCurrentShader().getVertexAttributeLocation("vertexIn");
		int texCoordIn = renderingContext.getCurrentShader().getVertexAttributeLocation("texCoordIn");
		int normalIn = renderingContext.getCurrentShader().getVertexAttributeLocation("normalIn");

		Matrix4f matrix;

		renderingContext.enableVertexAttribute(vertexIn);
		renderingContext.enableVertexAttribute(texCoordIn);
		if (normalIn != -1)
			renderingContext.enableVertexAttribute(normalIn);

		//Make sure vertex data is avaible
		getDrawableModel().bind();

		glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 0, 0);
		texCoordDataOnGpu.bind();
		glVertexAttribPointer(texCoordIn, 2, GL_FLOAT, false, 0, 0);
		if (normalIn != -1)
		{
			System.out.println("normal bound to " + normalIn);
			normalsDataOnGpu.bind();
			glVertexAttribPointer(normalIn, 3, GL_FLOAT, true, 0, 0);
		}

		int totalSize = 0;

		if (skeleton != null)
		{
			//Loop over groups
			boneGroup: for (String currentVertexGroup : boneGroups.keySet())
			{
				int i = boneGroups.get(currentVertexGroup);
				if (parts != null && parts.length > 0)
				{
					boolean found = false;
					for (String part : parts)
						if (part.equals(currentVertexGroup))
						{
							found = true;
							break;
						}

					//If we found or didn't found what we were looking for we skip this group
					if (exclude == found)
					{
						totalSize += i;
						continue boneGroup;
					}
				}

				//Get transformer matrix
				matrix = skeleton.getBoneHierarchyTransformationMatrixWithOffset(currentVertexGroup, animationTime);
				//Send the transformation

				//if(currentVertexGroup.equals("boneArmLD"))
				//	System.out.println(matrix);

				//System.out.println(currentVertexGroup);
				renderingContext.sendBoneTransformationMatrix(matrix);
				//Only what we can care about

				GLCalls.drawArrays(GL_TRIANGLES, totalSize * 3, i * 3);
				totalSize += i;
			}
		}
		else
		{
			System.out.println("verticesCount" + verticesCount);
			System.out.println("renderingContext" + renderingContext.toString());
			System.out.println(verticesDataOnGpu);
			System.out.println(texCoordDataOnGpu);
			System.out.println(normalsDataOnGpu);
			GLCalls.drawArrays(GL_TRIANGLES, 0, verticesCount);
		}

		renderingContext.sendBoneTransformationMatrix(null);
		//glCullFace(GL_FRONT);
	}
}
