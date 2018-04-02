//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MeshMaterial;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.mesh.RenderableMesh;
import io.xol.chunkstories.api.rendering.vertex.Primitive;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.client.util.MemFreeByteBuffer;
import io.xol.chunkstories.renderer.opengl.vbo.VertexBufferGL;

public class MeshRenderer implements RenderableMesh {
	private final Mesh mesh;

	protected int verticesCount;
	protected VertexBuffer meshDataOnGPU;
	
	public MeshRenderer(Mesh mesh) {
		this.mesh = mesh;
		this.verticesCount = mesh.getVerticesCount();

		uploadFloatBuffers(mesh.getVertices(), mesh.getTextureCoordinates(), mesh.getNormals());
	}

	public VertexBuffer getMeshDataOnGPU() {
		return meshDataOnGPU;
	}

	public Mesh getMesh() {
		return mesh;
	}

	final int FLOAT_SIZE = 4; // a float
	final int VERTEX_STRUCT_SIZE = FLOAT_SIZE * 3;
	final int NORMALS_STRUCT_SIZE = FLOAT_SIZE * 3;
	final int TEXCOORD_STRUCT_SIZE = FLOAT_SIZE * 2;
	
	final int TOTAL_STRUCT_SIZE = VERTEX_STRUCT_SIZE + NORMALS_STRUCT_SIZE + TEXCOORD_STRUCT_SIZE;
	
	protected void uploadFloatBuffers(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals) {
		meshDataOnGPU = new VertexBufferGL();

		int buffer_size = verticesCount * TOTAL_STRUCT_SIZE;
		ByteBuffer buffer = MemoryUtil.memAlloc(buffer_size);
		for (int i = 0; i < verticesCount; i++) {
			buffer.putFloat(vertices.get(i * 3 + 0));
			buffer.putFloat(vertices.get(i * 3 + 1));
			buffer.putFloat(vertices.get(i * 3 + 2));

			buffer.putFloat(normals.get(i * 3 + 0));
			buffer.putFloat(normals.get(i * 3 + 1));
			buffer.putFloat(normals.get(i * 3 + 2));

			buffer.putFloat(textureCoordinates.get(i * 2 + 0));
			buffer.putFloat(textureCoordinates.get(i * 2 + 1));
		}

		buffer.flip();

		meshDataOnGPU.uploadData(new MemFreeByteBuffer(buffer));
	}

	@Override
	public void render(RenderingInterface renderer) {
		prepareDraw(renderer);
		renderer.draw(Primitive.TRIANGLE, 0, verticesCount);
	}
	
	private void prepareDraw(RenderingInterface renderer) {
		// Make sure vertex data is avaible
		renderer.unbindAttributes();
		renderer.bindAttribute("vertexIn", meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 3, TOTAL_STRUCT_SIZE, 0));
		int offset = VERTEX_STRUCT_SIZE;
		renderer.bindAttribute("normalIn", meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 3, TOTAL_STRUCT_SIZE, offset));
		offset += NORMALS_STRUCT_SIZE;
		renderer.bindAttribute("texCoordIn", meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 2, TOTAL_STRUCT_SIZE, offset));
	}

	@Override
	public void renderUsingMaterials(RenderingInterface renderer) throws RenderingException {
		prepareDraw(renderer);
		
		//Shader shader = renderer.currentShader();
		
		for(MeshMaterial material : mesh.getMaterials()) {
			renderer.bindAlbedoTexture(renderer.textures().getTexture(material.getAlbedoTextureName()));
			renderer.bindNormalTexture(renderer.textures().getTexture(material.getNormalTextureName()));
			renderer.bindTexture2D("specularTexture", renderer.textures().getTexture(material.getSpecularTextureName()));
			//renderer.bindSpecularTexture(renderer.textures().getTexture(material.getSpecularTextureName()));
			renderer.draw(Primitive.TRIANGLE, material.firstVertex(), material.lastVertex()-material.firstVertex());
		}
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
