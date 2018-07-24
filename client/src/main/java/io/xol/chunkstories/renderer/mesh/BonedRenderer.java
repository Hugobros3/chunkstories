//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.mesh;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.mesh.AnimatableMesh;
import io.xol.chunkstories.api.mesh.MeshMaterial;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.mesh.RenderableAnimatableMesh;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.vertex.Primitive;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.client.util.MemFreeByteBuffer;
import io.xol.chunkstories.renderer.opengl.vbo.VertexBufferGL;

public class BonedRenderer implements RenderableAnimatableMesh {
	protected final AnimatableMesh mesh;

	protected final int verticesCount;
	protected final String[] boneName;

	protected VertexBuffer meshDataOnGPU;

	public BonedRenderer(AnimatableMesh mesh) {
		this.mesh = mesh;

		this.boneName = mesh.getBoneNames();
		this.verticesCount = mesh.getVerticesCount();

		this.uploadFloatBuffers(mesh.getVertices(), mesh.getTextureCoordinates(), mesh.getNormals(), mesh.getBoneIds(),
				mesh.getBoneWeights());
	}

	public VertexBuffer getMeshDataOnGPU() {
		return meshDataOnGPU;
	}

	public AnimatableMesh getMesh() {
		return mesh;
	}

	final int FLOAT_SIZE = 4; // a float
	final int WEIGHT_SIZE = 1; // normalized byte 0-1
	final int BONE_ID_SIZE = 1; // uint byte
	final int VERTEX_STRUCT_SIZE = FLOAT_SIZE * 3;
	final int NORMALS_STRUCT_SIZE = FLOAT_SIZE * 3;
	final int TEXCOORD_STRUCT_SIZE = FLOAT_SIZE * 2;
	final int ANIMATIONS_STRUCT_SIZE = BONE_ID_SIZE * 4 + WEIGHT_SIZE * 4;

	final int TOTAL_STRUCT_SIZE = VERTEX_STRUCT_SIZE + NORMALS_STRUCT_SIZE + TEXCOORD_STRUCT_SIZE
			+ ANIMATIONS_STRUCT_SIZE;

	private void uploadFloatBuffers(FloatBuffer vertices, FloatBuffer textureCoordinates, FloatBuffer normals,
			ByteBuffer boneIds, ByteBuffer boneWeights) {
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
		// memfree(meshDataOnGPU) uneeded ^
	}

	@Override
	public void render(RenderingInterface renderer) {
		internalRenderer(renderer, null, 0.0, false);
	}

	public void render(RenderingInterface renderer, SkeletonAnimator skeleton, double animationTime) {
		internalRenderer(renderer, skeleton, animationTime, false);
	}

	@Override
	public void renderUsingMaterials(RenderingInterface renderer) throws RenderingException {
		internalRenderer(renderer, null, 0.0, true);
	}

	@Override
	public void renderUsingMaterials(RenderingInterface renderer, SkeletonAnimator skeleton, double animationTime)
			throws RenderingException {
		internalRenderer(renderer, skeleton, animationTime, true);
	}

	private void prepareDraw(RenderingInterface renderer) {
		// Make sure vertex data is avaible
		renderer.unbindAttributes();
		renderer.bindAttribute("vertexIn",
				meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 3, TOTAL_STRUCT_SIZE, 0));
		int offset = VERTEX_STRUCT_SIZE;
		renderer.bindAttribute("normalIn",
				meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 3, TOTAL_STRUCT_SIZE, offset));
		offset += NORMALS_STRUCT_SIZE;
		renderer.bindAttribute("texCoordIn",
				meshDataOnGPU.asAttributeSource(VertexFormat.FLOAT, 2, TOTAL_STRUCT_SIZE, offset));
		offset += TEXCOORD_STRUCT_SIZE;
		renderer.bindAttribute("boneIdIn",
				meshDataOnGPU.asIntegerAttributeSource(VertexFormat.BYTE, 4, TOTAL_STRUCT_SIZE, offset, 0));
		offset += BONE_ID_SIZE * 4;
		renderer.bindAttribute("boneWeightsIn",
				meshDataOnGPU.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, TOTAL_STRUCT_SIZE, offset));
	}

	private void internalRenderer(RenderingInterface renderer, SkeletonAnimator skeleton, double animationTime,
			boolean useMaterials) {
		prepareDraw(renderer);

		Shader shader = renderer.currentShader();

		if (skeleton != null) {
			for (int i = 0; i < boneName.length; i++) {
				if (skeleton.shouldHideBone(renderer, boneName[i])) {
					Matrix4f boneMatrix = new Matrix4f();
					boneMatrix.translate(50000, 50000, 50000);
					shader.setUniformMatrix4f("bones[" + i + "]", boneMatrix);
				} else {
					Matrix4fc boneMatrix = skeleton.getBoneHierarchyTransformationMatrixWithOffset(boneName[i],
							animationTime < 0 ? 0 : animationTime);
					shader.setUniformMatrix4f("bones[" + i + "]", boneMatrix);
				}
			}
		}

		if (useMaterials) {
			for (MeshMaterial material : mesh.getMaterials()) {
				renderer.bindAlbedoTexture(renderer.textures().getTexture(material.getAlbedoTextureName()));
				renderer.bindNormalTexture(renderer.textures().getTexture(material.getNormalTextureName()));
				renderer.bindTexture2D("specularTexture",
						renderer.textures().getTexture(material.getSpecularTextureName()));
				// renderer.bindSpecularTexture(renderer.textures().getTexture(material.getSpecularTextureName()));
				renderer.draw(Primitive.TRIANGLE, material.firstVertex(),
						material.lastVertex() - material.firstVertex());
			}
		} else
			renderer.draw(Primitive.TRIANGLE, 0, verticesCount);
	}
}
