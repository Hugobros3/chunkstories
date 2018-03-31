//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.mesh;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.mesh.AnimatableMesh;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.mesh.RenderableAnimatableMesh;
import io.xol.chunkstories.api.rendering.mesh.RenderableMesh;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.vertex.Primitive;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.client.util.MemFreeByteBuffer;
import io.xol.chunkstories.renderer.opengl.vbo.VertexBufferGL;

public class BonedRenderer implements RenderableAnimatableMesh
{
	protected final int verticesCount;
	//protected Map<String, Integer> boneGroups = new HashMap<String, Integer>();
	
	/*protected VertexBuffer verticesDataOnGpu;
	protected VertexBuffer texCoordDataOnGpu;
	protected VertexBuffer normalsDataOnGpu;*/
	protected VertexBuffer meshDataOnGPU;
	
	public BonedRenderer(AnimatableMesh mesh) {
		this.boneName = mesh.getBoneNames();
		this.verticesCount = mesh.getVerticesCount();
		
		this.uploadFloatBuffers(mesh.getVertices(), mesh.getTextureCoordinates(), mesh.getNormals(), mesh.getBoneIds(), mesh.getBoneWeights());
	}
	
	/*public BonedRenderer(AnimatableMesh mesh)
	{
		this(mesh.getVerticesCount(), mesh.getVertices(), mesh.getTextureCoordinates(), mesh.getNormals(), mesh.partsMap());
	}
	
	BonedRenderer(int verticesCount, FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normalMapCoordinates, Map<String, Integer> boneGroups)
	{
		this.verticesCount = verticesCount;
		this.boneGroups = boneGroups;

		//VERY BAD CODE ACTUALLY >:(
		boneName = new String[boneGroups.size()];
		boneStartVertex = new int[boneGroups.size()];
		boneGroupSize = new int[boneGroups.size()];
		
		//this is absolute garbage because it relies on the keySet being ordered
		//which it is apparently but trash design anyway
		int currentTotalSize = 0;
		int groupId = 0;
		for(String k : boneGroups.keySet()) {
			boneName[groupId] = k;
			boneStartVertex[groupId] = currentTotalSize;
			boneGroupSize[groupId] = boneGroups.get(k).intValue() * 3;
			currentTotalSize += boneGroupSize[groupId];
			groupId++;
		}
		
		ByteBuffer boneIds = MemoryUtil.memAlloc(verticesCount * 4);
		ByteBuffer boneWeights = MemoryUtil.memAlloc(verticesCount * 4);
		for(int i = 0; i < verticesCount; i++) {
			//Foreach vertex, find it's group
			int gId = 0;
			int j = 0;
			while(j < boneGroupSize.length) {
				if(i >= boneStartVertex[j] && i < boneStartVertex[j] + boneGroupSize[j]) {
					gId = j;
					break;
				}
				j++;
			}
			
			//Currently we only support meshes with one bone for testing, so only one id is filled and it's weight is 1
			boneIds.put((byte)gId);
			boneIds.put((byte)0x0);
			boneIds.put((byte)0x0);
			boneIds.put((byte)0x0);
			
			boneWeights.put((byte)0xFF);
			boneWeights.put((byte)0x0);
			boneWeights.put((byte)0x0);
			boneWeights.put((byte)0x0);
		}
		
		uploadFloatBuffers(vertices, textureCoordinates, normalMapCoordinates, boneIds, boneWeights);

		MemoryUtil.memFree(boneIds);
		MemoryUtil.memFree(boneWeights);
	}

	final int[] boneStartVertex;
	final int[] boneGroupSize;
	*/
	
	final String[] boneName;
	
	final int FLOAT_SIZE = 4; //a float
	final int WEIGHT_SIZE = 1; //normalized byte 0-1
	final int BONE_ID_SIZE = 1; //uint byte
	final int VERTEX_STRUCT_SIZE = FLOAT_SIZE * 3;
	final int NORMALS_STRUCT_SIZE = FLOAT_SIZE * 3;
	final int TEXCOORD_STRUCT_SIZE = FLOAT_SIZE * 2;
	final int ANIMATIONS_STRUCT_SIZE = BONE_ID_SIZE * 4 + WEIGHT_SIZE * 4;
	
	final int TOTAL_STRUCT_SIZE = VERTEX_STRUCT_SIZE + NORMALS_STRUCT_SIZE + TEXCOORD_STRUCT_SIZE + ANIMATIONS_STRUCT_SIZE;
	
	private void uploadFloatBuffers(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals, ByteBuffer boneIds, ByteBuffer boneWeights)
	{
		meshDataOnGPU = new VertexBufferGL();
		
		int buffer_size = verticesCount * TOTAL_STRUCT_SIZE;
		ByteBuffer buffer = MemoryUtil.memAlloc(buffer_size);
		for(int i = 0; i < verticesCount; i++) {
			buffer.putFloat(vertices.get(i * 3 + 0));
			buffer.putFloat(vertices.get(i * 3 + 1));
			buffer.putFloat(vertices.get(i * 3 + 2));

			buffer.putFloat(normals.get(i * 3 + 0));
			buffer.putFloat(normals.get(i * 3 + 1));
			buffer.putFloat(normals.get(i * 3 + 2));
			
			buffer.putFloat(textureCoordinates.get(i * 2 + 0));
			buffer.putFloat(textureCoordinates.get(i * 2 + 1));
			
			buffer.put(boneIds.get(i * 4 + 0));
			buffer.put(boneIds.get(i * 4 + 1));
			buffer.put(boneIds.get(i * 4 + 2));
			buffer.put(boneIds.get(i * 4 + 3));
			
			buffer.put(boneWeights.get(i * 4 + 0));
			buffer.put(boneWeights.get(i * 4 + 1));
			buffer.put(boneWeights.get(i * 4 + 2));
			buffer.put(boneWeights.get(i * 4 + 3));
		}
		
		buffer.flip();
		
		meshDataOnGPU.uploadData(new MemFreeByteBuffer(buffer));
		//memfree(meshDataOnGPU) uneeded ^
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		internalRenderer(renderingContext, null, 0.0);
	}

	public void render(RenderingInterface renderingContext, SkeletonAnimator skeleton, double animationTime)
	{
		internalRenderer(renderingContext, skeleton, animationTime);
	}

	private void internalRenderer(RenderingInterface renderingContext, SkeletonAnimator skeleton, double animationTime) {
		prepareDraw(renderingContext);

		Shader shader = renderingContext.currentShader();
		
		if (skeleton != null)
		{
			for(int i = 0; i < boneName.length; i++) {
				if(skeleton.shouldHideBone(renderingContext, boneName[i])) {
					Matrix4f boneMatrix = new Matrix4f();
					boneMatrix.translate(50000, 50000, 50000);
					shader.setUniformMatrix4f("bones["+i+"]", boneMatrix);
				} else {
					Matrix4fc boneMatrix = skeleton.getBoneHierarchyTransformationMatrixWithOffset(boneName[i], animationTime < 0 ? 0 : animationTime);
					shader.setUniformMatrix4f("bones["+i+"]", boneMatrix);
				}
			}
		}
		renderingContext.draw(Primitive.TRIANGLE, 0, verticesCount);
	}

	private void prepareDraw(RenderingInterface renderer) {
		renderer.currentShader().setUniform1f("useColorIn", 0.0f);
		renderer.currentShader().setUniform1f("useNormalIn", 1.0f);

		//Make sure vertex data is avaible
		renderer.unbindAttributes();
		renderer.bindAttribute("vertexIn", meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 3, TOTAL_STRUCT_SIZE, 0));
		int offset = VERTEX_STRUCT_SIZE;
		renderer.bindAttribute("normalIn", meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 3, TOTAL_STRUCT_SIZE, offset));
		offset += NORMALS_STRUCT_SIZE;
		renderer.bindAttribute("texCoordIn", meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 2, TOTAL_STRUCT_SIZE, offset));
		offset += TEXCOORD_STRUCT_SIZE;
		renderer.bindAttribute("boneIdIn", meshDataOnGPU.asIntegerAttributeSource(VertexFormat.BYTE, 4, TOTAL_STRUCT_SIZE, offset, 0));
		offset += BONE_ID_SIZE * 4;
		renderer.bindAttribute("boneWeightsIn", meshDataOnGPU.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, TOTAL_STRUCT_SIZE, offset));
		//System.out.println("angery"+offset+" "+TOTAL_STRUCT_SIZE);
	}
}
