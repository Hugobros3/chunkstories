//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.model;

import java.util.Map;

import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import org.joml.Matrix4f;
import io.xol.chunkstories.api.mesh.MultiPartMesh;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.mesh.RenderableMultiPartAnimatableMesh;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;

public class MultiPartMeshRenderableImpl extends MeshRenderableImpl implements RenderableMultiPartAnimatableMesh {

	private final MultiPartMesh mpm;
	private final Map<String, Integer> boneGroups;// = new HashMap<String, Integer>();
	
	public MultiPartMeshRenderableImpl(MultiPartMesh mesh) {
		super(mesh);
		
		this.mpm = mesh;
		this.boneGroups = mesh.partsMap();
	}

	@Override
	public void render(RenderingInterface renderingInterface, String... parts) throws RenderingException {
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
		//verticesDataOnGpu.bind();
		//renderingContext.setVertexAttributePointerLocation(vertexIn, 3, GL_FLOAT, false, 0, 0);
		
		renderingContext.bindAttribute("texCoordIn", texCoordDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 2));
		renderingContext.bindAttribute("normalIn", normalsDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 3));
		
	}

	@Override
	public Iterable<String> allParts() {
		return mpm.parts();
	}

}
