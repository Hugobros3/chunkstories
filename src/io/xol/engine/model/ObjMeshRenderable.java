package io.xol.engine.model;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.rendering.RenderingInterface.Primitive;
import io.xol.engine.animation.SkeletonAnimator;
import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.RenderableAnimatable;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.GBufferTexture;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TextureFormat;
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

		verticesDataOnGpu.uploadData(vertices);
		texCoordDataOnGpu.uploadData(textureCoordinates);
		normalsDataOnGpu.uploadData(normals);
	}

	protected int verticesCount;
	protected Map<String, Integer> boneGroups = new HashMap<String, Integer>();
	protected VerticesObject verticesDataOnGpu;
	protected VerticesObject texCoordDataOnGpu;
	protected VerticesObject normalsDataOnGpu;

	@Override
	public void render(RenderingContext renderingContext)
	{
		internalRenderer(renderingContext, null, 0.0, false, (String[]) null);

	}

	@Override
	public void render(RenderingContext renderingContext, SkeletonAnimator skeleton, double animationTime)
	{
		internalRenderer(renderingContext, skeleton, animationTime, false, (String[]) null);
	}

	/*@Override
	public void renderParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts)
	{
		internalRenderer(renderingContext, skeleton, animationTime, false, parts);
	}
	
	@Override
	public void renderButParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts)
	{
		internalRenderer(renderingContext, skeleton, animationTime, true, parts);
	}*/

	private void internalRenderer(RenderingContext renderingContext, SkeletonAnimator skeleton, double animationTime, boolean exclude, String... parts)
	{
		prepareDraw(renderingContext);

		Matrix4f currentObjectMatrix = renderingContext.getObjectMatrix();
		Matrix4f matrix;

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
				matrix = skeleton.getBoneHierarchyTransformationMatrixWithOffset(currentVertexGroup, animationTime < 0 ? 0 : animationTime);
				
				
				//Send the transformation
				Matrix4f.mul(matrix, currentObjectMatrix, matrix);
				
				renderingContext.setObjectMatrix(matrix);
				//renderingContext.sendBoneTransformationMatrix(matrix);
				//Only what we can care about

				renderingContext.draw(Primitive.TRIANGLE, totalSize * 3, i * 3);
				//GLCalls.drawArrays(GL_TRIANGLES, totalSize * 3, i * 3);
				//GLCalls.drawArraysInstanced(GL_TRIANGLES, totalSize * 3, i * 3, 50);
				totalSize += i;
			}
		}
		else
		{
			/*System.out.println("verticesCount" + verticesCount);
			System.out.println("renderingContext" + renderingContext.toString());
			System.out.println(verticesDataOnGpu);
			System.out.println(texCoordDataOnGpu);
			System.out.println(normalsDataOnGpu);*/
			renderingContext.draw(Primitive.TRIANGLE, 0, verticesCount);
			//GLCalls.drawArrays(GL_TRIANGLES, 0, verticesCount);
		}

		renderingContext.setObjectMatrix(currentObjectMatrix);
		//renderingContext.sendBoneTransformationMatrix(null);

		//renderInstanciated(renderingContext, Arrays.asList(new AnimatableData(new Vector3f(0.0, 2.0f, 0.5f), skeleton, animationTime)));
		//glCullFace(GL_FRONT);
	}

	private void prepareDraw(RenderingContext renderingContext)
	{
		//System.out.println("slt");
		
		//renderingContext.resetAllVertexAttributesLocations();
		//renderingContext.disableUnusedVertexAttributes();

		/*int vertexIn = renderingContext.currentShader().getVertexAttributeLocation("vertexIn");
		int texCoordIn = renderingContext.currentShader().getVertexAttributeLocation("texCoordIn");
		int normalIn = renderingContext.currentShader().getVertexAttributeLocation("normalIn");

		renderingContext.enableVertexAttribute(vertexIn);
		renderingContext.enableVertexAttribute(texCoordIn);
		if (normalIn != -1)
			renderingContext.enableVertexAttribute(normalIn);*/

		//System.out.println("ColorIn in is at :"+renderingContext.getCurrentShader().getVertexAttributeLocation("colorIn"));

		renderingContext.currentShader().setUniform1f("useColorIn", 0.0f);
		renderingContext.currentShader().setUniform1f("useNormalIn", 1.0f);

		//Make sure vertex data is avaible
		
		renderingContext.bindAttribute("vertexIn", verticesDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 3));
		//verticesDataOnGpu.bind();
		//renderingContext.setVertexAttributePointerLocation(vertexIn, 3, GL_FLOAT, false, 0, 0);
		
		renderingContext.bindAttribute("texCoordIn", texCoordDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 2));
		//texCoordDataOnGpu.bind();
		//renderingContext.setVertexAttributePointerLocation(texCoordIn, 2, GL_FLOAT, false, 0, 0);
		
		//if (normalIn != -1)
		{

			renderingContext.bindAttribute("normalIn", normalsDataOnGpu.asAttributeSource(VertexFormat.FLOAT, 2));
			//normalsDataOnGpu.bind();
			//renderingContext.setVertexAttributePointerLocation(normalIn, 3, GL_FLOAT, true, 0, 0);
		}

	}

	Texture2D instancesDataTexture = new GBufferTexture(TextureFormat.RGBA_32F, 32, 32);
	ByteBuffer instancesDataBuffer = BufferUtils.createByteBuffer(32 * 32 * 4 * 4);

	@Override
	public void renderInstanciated(RenderingContext renderingContext, Collection<AnimatableData> instancesData)
	{
		prepareDraw(renderingContext);
		
		int totalTriangles = 0;
		for (String currentVertexGroup : boneGroups.keySet())
		{
			int trianglesInThisGroup = boneGroups.get(currentVertexGroup);

			int dataInInstancesBuffer = 0;
			Iterator<AnimatableData> iterator = instancesData.iterator();
			instancesDataBuffer.clear();

			//Fill the buffer
			while (iterator.hasNext())
			{
				AnimatableData data = iterator.next();

				if (!data.skeleton.shouldHideBone(renderingContext, currentVertexGroup))
				{
					Matrix4f animation = data.skeleton.getBoneHierarchyTransformationMatrixWithOffset(currentVertexGroup, data.animationTime);
					
					Matrix4f mat = new Matrix4f();
					
					mat.translate(data.position);
					
					Matrix4f.mul(mat, animation, mat);
					
					//Matrix4f.transpose(mat, mat);
					
					//System.out.println(mat);
					
					instancesDataBuffer.putFloat(mat.m00);
					instancesDataBuffer.putFloat(mat.m01);
					instancesDataBuffer.putFloat(mat.m02);
					instancesDataBuffer.putFloat(mat.m03);
					instancesDataBuffer.putFloat(mat.m10);
					instancesDataBuffer.putFloat(mat.m11);
					instancesDataBuffer.putFloat(mat.m12);
					instancesDataBuffer.putFloat(mat.m13);
					instancesDataBuffer.putFloat(mat.m20);
					instancesDataBuffer.putFloat(mat.m21);
					instancesDataBuffer.putFloat(mat.m22);
					instancesDataBuffer.putFloat(mat.m23);
					instancesDataBuffer.putFloat(mat.m30);
					instancesDataBuffer.putFloat(mat.m31);
					instancesDataBuffer.putFloat(mat.m32);
					instancesDataBuffer.putFloat(mat.m33);
					
					instancesDataBuffer.putFloat(data.sunLight);
					instancesDataBuffer.putFloat(data.blockLight);

					for (int f = 0; f < 14; f++)
						instancesDataBuffer.putFloat(0.0f);
						
					dataInInstancesBuffer += 8;
				}
				
				//Don't overfill the buffer
				while (dataInInstancesBuffer == 1024)
				{
					instancesDataBuffer.flip();
					instancesDataTexture.uploadTextureData(32, 32, instancesDataBuffer);
					instancesDataBuffer.clear();
					drawInstanceBufferContents(renderingContext, totalTriangles * 3, trianglesInThisGroup * 3, dataInInstancesBuffer / 4);
					dataInInstancesBuffer = 0;
				}
			}

			//Render in bulk
			if (dataInInstancesBuffer > 0)
			{
				//Pad the buffer
				int k = dataInInstancesBuffer;
				while (k < 1024)
				{
					for (int f = 0; f < 32; f++)
						instancesDataBuffer.putFloat(0.0f);
					k += 8;
				}

				instancesDataBuffer.flip();
				instancesDataTexture.uploadTextureData(32, 32, instancesDataBuffer);
				instancesDataBuffer.clear();

				drawInstanceBufferContents(renderingContext, totalTriangles * 3, trianglesInThisGroup * 3, dataInInstancesBuffer / 4);
			}

			totalTriangles += trianglesInThisGroup;
		}
	}

	private void drawInstanceBufferContents(RenderingContext renderingContext, int start, int count, int dataInInstancesBuffer)
	{
		renderingContext.bindTexture2D("instancedDataSampler", instancesDataTexture);
		//renderingContext.currentShader().setUniformSampler(13, "instancedDataSampler", instancesDataTexture);

		//System.out.println("Drawing "+dataInInstancesBuffer+" instances of "+start);
		renderingContext.currentShader().setUniform1f("isUsingInstancedData", 1);
		GLCalls.drawArraysInstanced(GL_TRIANGLES, start, count, dataInInstancesBuffer);
		renderingContext.currentShader().setUniform1f("isUsingInstancedData", 0);
	}
}
