//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import java.nio.FloatBuffer;

import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.mesh.RenderableMesh;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.renderer.opengl.vbo.VertexBufferGL;

public class MeshRenderableImpl implements RenderableMesh
{
	@SuppressWarnings("unused")
	private final Mesh mesh;
	
	protected int verticesCount;
	//protected Map<String, Integer> boneGroups = new HashMap<String, Integer>();
	protected VertexBuffer verticesDataOnGpu;
	protected VertexBuffer texCoordDataOnGpu;
	protected VertexBuffer normalsDataOnGpu;
	
	public MeshRenderableImpl(Mesh mesh) {
		this.mesh = mesh;
		
		this.verticesCount = mesh.getVerticesCount();
		//this.boneGroups = boneGroups;

		uploadFloatBuffers(mesh.getVertices(), mesh.getTextureCoordinates(), mesh.getNormals());
	}

	/*private MeshRenderableImpl(int verticesCount, FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normalMapCoordinates, Map<String, Integer> boneGroups)
	{
		this.verticesCount = verticesCount;
		this.boneGroups = boneGroups;

		uploadFloatBuffers(vertices, textureCoordinates, normalMapCoordinates);
	}*/

	/*ObjMeshRenderable(int verticesCount, VertexBuffer verticesDataOnGpu, VertexBuffer texCoordDataOnGpu, VertexBuffer normalsDataOnGpu, Map<String, Integer> boneGroups)
	{
		this.verticesCount = verticesCount;
		this.verticesDataOnGpu = verticesDataOnGpu;
		this.texCoordDataOnGpu = texCoordDataOnGpu;
		this.normalsDataOnGpu = normalsDataOnGpu;
		this.boneGroups = boneGroups;
	}*/

	protected void uploadFloatBuffers(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals)
	{
		verticesDataOnGpu = new VertexBufferGL();
		texCoordDataOnGpu = new VertexBufferGL();
		normalsDataOnGpu = new VertexBufferGL();

		verticesDataOnGpu.uploadData(vertices);
		texCoordDataOnGpu.uploadData(textureCoordinates);
		normalsDataOnGpu.uploadData(normals);
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		internalRenderer(renderingContext);
	}

	private void internalRenderer(RenderingInterface renderingContext)
	{
		prepareDraw(renderingContext);
		renderingContext.draw(Primitive.TRIANGLE, 0, verticesCount);
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

	/*Texture2D instancesDataTexture = new Texture2DRenderTarget(TextureFormat.RGBA_32F, 32, 32);
	ByteBuffer instancesDataBuffer = BufferUtils.createByteBuffer(32 * 32 * 4 * 4);

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
	}*/

	/*private void drawInstanceBufferContents(RenderingContext renderingContext, int start, int count, int dataInInstancesBuffer)
	{
		renderingContext.bindTexture2D("instancedDataSampler", instancesDataTexture);
		//renderingContext.currentShader().setUniformSampler(13, "instancedDataSampler", instancesDataTexture);

		//System.out.println("Drawing "+dataInInstancesBuffer+" instances of "+start);
		renderingContext.currentShader().setUniform1f("isUsingInstancedData", 1);
		GLCalls.drawArraysInstanced(GL_TRIANGLES, start, count, dataInInstancesBuffer);
		renderingContext.currentShader().setUniform1f("isUsingInstancedData", 0);
	}*/
}
