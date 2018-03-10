//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.mesh;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;

import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.mesh.MultiPartMesh;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.mesh.RenderableMesh;
import io.xol.chunkstories.api.rendering.mesh.RenderableMultiPartAnimatableMesh;
import io.xol.chunkstories.api.rendering.mesh.RenderableMultiPartMesh;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.engine.graphics.geometry.VertexBufferGL;

public class MultiPartMeshRenderable implements RenderableMesh, RenderableMultiPartMesh, RenderableMultiPartAnimatableMesh
{
	public MultiPartMeshRenderable(MultiPartMesh mesh)
	{
		this(mesh.getVerticesCount(), mesh.getVertices(), mesh.getTextureCoordinates(), mesh.getNormals(), mesh.partsMap());
	}
	
	MultiPartMeshRenderable(int verticesCount, FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normalMapCoordinates, Map<String, Integer> boneGroups)
	{
		this.verticesCount = verticesCount;
		this.boneGroups = boneGroups;

		uploadFloatBuffers(vertices, textureCoordinates, normalMapCoordinates);
	}

	protected void uploadFloatBuffers(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals)
	{
		verticesDataOnGpu = new VertexBufferGL();
		texCoordDataOnGpu = new VertexBufferGL();
		normalsDataOnGpu = new VertexBufferGL();

		verticesDataOnGpu.uploadData(vertices);
		texCoordDataOnGpu.uploadData(textureCoordinates);
		normalsDataOnGpu.uploadData(normals);
	}

	protected int verticesCount;
	protected Map<String, Integer> boneGroups = new HashMap<String, Integer>();
	protected VertexBuffer verticesDataOnGpu;
	protected VertexBuffer texCoordDataOnGpu;
	protected VertexBuffer normalsDataOnGpu;

	@Override
	public void render(RenderingInterface renderingContext)
	{
		internalRenderer(renderingContext, null, 0.0, false, (String[]) null);
	}

	@Override
	public void render(RenderingInterface renderingInterface, String... parts) throws RenderingException
	{
		internalRenderer(renderingInterface, null, 0.0, false, parts);
	}

	public void render(RenderingInterface renderingContext, SkeletonAnimator skeleton, double animationTime)
	{
		internalRenderer(renderingContext, skeleton, animationTime, false, (String[]) null);
	}
	
	public void render(RenderingInterface renderingContext, SkeletonAnimator skeleton, double animationTime, String... parts)
	{
		internalRenderer(renderingContext, skeleton, animationTime, false, parts);
	}

	private void internalRenderer(RenderingInterface renderingContext, SkeletonAnimator skeleton, double animationTime, boolean exclude, String... parts)
	{
		prepareDraw(renderingContext);

		Matrix4f currentObjectMatrix = renderingContext.getObjectMatrix();
		Matrix4f matrix = new Matrix4f();

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
					if (exclude == found || skeleton.shouldHideBone(renderingContext, currentVertexGroup))
					{
						totalSize += i;
						continue boneGroup;
					}
				}
				
				if(skeleton.shouldHideBone(renderingContext, currentVertexGroup))
				{
					totalSize += i;
					continue boneGroup;
				}

				//Get transformer matrix
				matrix.set(skeleton.getBoneHierarchyTransformationMatrixWithOffset(currentVertexGroup, animationTime < 0 ? 0 : animationTime));
				
				if(currentObjectMatrix == null)
					currentObjectMatrix = new Matrix4f();
				
				//Send the transformation
				currentObjectMatrix.mul(matrix, matrix);
				//Matrix4f.mul(currentObjectMatrix, matrix, matrix);
				
				renderingContext.setObjectMatrix(matrix);
				
				//Only what we can care about
				renderingContext.draw(Primitive.TRIANGLE, totalSize * 3, i * 3);
				totalSize += i;
			}
		}
		else
		{
			renderingContext.draw(Primitive.TRIANGLE, 0, verticesCount);
		}

		renderingContext.setObjectMatrix(currentObjectMatrix);
	}

	private void prepareDraw(RenderingInterface renderingContext)
	{
		renderingContext.currentShader().setUniform1f("useColorIn", 0.0f);
		renderingContext.currentShader().setUniform1f("useNormalIn", 1.0f);

		//Make sure vertex data is avaible
		
		renderingContext.unbindAttributes();
		renderingContext.bindAttribute("vertexIn", verticesDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 3));
		renderingContext.bindAttribute("texCoordIn", texCoordDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 2));
		renderingContext.bindAttribute("normalIn", normalsDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 3));
	}

	@Override
	public Iterable<String> allParts() {
		return this.boneGroups.keySet();
	}
}
