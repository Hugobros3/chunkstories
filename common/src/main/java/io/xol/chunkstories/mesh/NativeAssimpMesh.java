//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memUTF8;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMaterialProperty;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.content.AssetToByteBufferHelper;

/** Experimental loader test for LWJGL3's assimp bindings (unused) */
public class NativeAssimpMesh {
	protected final Asset mainAsset;
	protected final MeshLibrary store;

	public NativeAssimpMesh(Asset mainAsset, MeshLibrary store) throws MeshLoadException {
		this.mainAsset = mainAsset;
		this.store = store;

		int flags = Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_Triangulate
				| Assimp.aiProcess_FixInfacingNormals | Assimp.aiProcess_LimitBoneWeights;

		flags = 0;
		AIScene aiScene;
		// No LWJGL Java bindings to do this cleanly :'(
		// aiScene = Assimp.aiImportFileEx(mainAsset.getName(), flags, new AIFileIO() );

		// File cachedFile = AssetAsFileHelper.cacheAsset(mainAsset,
		// store.parent().getContext());
		// aiScene = Assimp.aiImportFile(cachedFile.getAbsolutePath(), flags);

		// oof http://forum.lwjgl.org/index.php?topic=6704.0
		try (MemoryStack stack = MemoryStack.stackPush()) {
			aiScene = Assimp.aiImportFileEx(mainAsset.getName(), flags,
					AIFileIO.callocStack(stack).OpenProc((pFileIO, fileName, openMode) -> {
						String assetName = memUTF8(fileName);
						Asset asset = store.parent().getAsset(assetName);
						if (asset == null)
							throw new RuntimeException("Assimp was looking for an asset we don't have");

						ByteBuffer data = AssetToByteBufferHelper.loadIntoByteBuffer(asset);

						return AIFile.callocStack(stack).ReadProc((pFile, pBuffer, size, count) -> {
							long remaining = data.remaining();
							long requested = size * count;

							long elements = Long.compareUnsigned(requested, remaining) <= 0 ? count
									: Long.divideUnsigned(remaining, size);

							memCopy(memAddress(data), pBuffer, (int) (size * elements));
							data.position(data.position() + (int) ((size * elements) & 0xFFFFFFFF));

							return elements;
						}).TellProc(pFile -> Integer.toUnsignedLong(data.position()))
								.FileSizeProc(pFile -> Integer.toUnsignedLong(data.capacity()))
								.SeekProc((pFile, offset, origin) -> {
									long position;
									switch (origin) {
									case Assimp.aiOrigin_SET:
										position = offset;
										break;
									case Assimp.aiOrigin_CUR:
										position = data.position() + offset;
										break;
									case Assimp.aiOrigin_END:
										position = data.capacity() - offset;
										break;
									default:
										throw new IllegalArgumentException();
									}

									try {
										data.position((int) (position & 0xFFFFFFFF));
									} catch (IllegalArgumentException e) {
										return -1;
									}
									return 0;
								}).WriteProc((pFile, pBuffer, memB, count) -> {
									throw new UnsupportedOperationException();
								}).FlushProc(pFile -> {
									throw new UnsupportedOperationException();
								}).UserData(-1L).address();
					}).CloseProc((pFileIO, pFile) -> {
					}).UserData(-1L));

			if (aiScene == null) {
				System.out.println(Assimp.aiGetErrorString());
				throw new MeshLoadException(mainAsset);
			}

			int meshesN = aiScene.mNumMeshes();
			for (int mesh = 0; mesh < meshesN; mesh++) {
				AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(mesh));
				System.out.println("Found mesh: " + aiMesh.mName().dataString());
				System.out.println(aiMesh.mNumVertices());
				System.out.println(aiMesh.mNumFaces());

				/*
				 * AIFace.Buffer buf = aiMesh.mFaces(); for(int j = 0; j < aiMesh.mNumFaces();
				 * j++) { IntBuffer indices = buf.get(j).mIndices(); for(int k = 0; k <
				 * buf.get(j).mNumIndices(); k++) { System.out.println(indices.get(k)); }
				 * 
				 * System.out.println("-"); }
				 */

				System.out.println("bones: " + aiMesh.mNumBones());
				int bonesN = aiMesh.mNumBones();
				for (int j = 0; j < bonesN; j++) {
					AIBone bone = AIBone.create(aiMesh.mBones().get(j));
					System.out.println(bone.mName().dataString());
					int weightsN = bone.mNumWeights();
					System.out.println(weightsN);
					System.out.println(tomat4(bone.mOffsetMatrix()));
					/*
					 * for(int k = 0; k < weightsN; k++) { AIVertexWeight vertexWeight =
					 * bone.mWeights().get(k); System.out.println(vertexWeight.mWeight() +
					 * ": "+vertexWeight.mVertexId()); }
					 */
				}

			}

			System.out.println("animations: " + aiScene.mNumAnimations());
		}

		int materialsNum = aiScene.mNumMaterials();
		for (int i = 10000; i < materialsNum; i++) {
			AIMaterial material = AIMaterial.create(aiScene.mMaterials().get(i));
			System.out.println("Reading material: " + i);
			int propertiesNum = material.mNumProperties();
			for (int j = 0; j < propertiesNum; j++) {
				AIMaterialProperty property = AIMaterialProperty.create(material.mProperties().get(j));
				int propertyType = property.mType();
				int semantic = property.mSemantic();

				System.out.println(propertyType + " " + semantic + " " + property.mKey().dataString());// +":
																										// "+property.mData().toString());

				/*
				 * switch(propertyType) { case Assimp.AI_BOOL: boolean boolValue =
				 * property.mData().get(0) == 0 ? false : true;
				 * System.out.println("Bool: "+boolValue); break; case Assimp.AI_INT: int
				 * intValue = property.mData().getInt(0); System.out.println("Int: "+intValue);
				 * break; case Assimp.AI_UINT64: long longValue = property.mData().getLong(0);
				 * System.out.println("Long: "+longValue); break; case Assimp.AI_FLOAT: float
				 * floatValue = property.mData().getFloat(0);
				 * System.out.println("Float: "+floatValue); break; case Assimp.AI_AIVECTOR3D:
				 * Vector3f vec3Value = new Vector3f(); vec3Value.x =
				 * property.mData().getFloat(0); vec3Value.y = property.mData().getFloat(4);
				 * vec3Value.z = property.mData().getFloat(8);
				 * System.out.println("Vec3: "+vec3Value); break; case Assimp.AI_AISTRING:
				 * String stringValue = memUTF8(property.mData());
				 * System.out.println("String: "+stringValue); break; }
				 */
			}
		}
	}

	private Matrix4f tomat4(AIMatrix4x4 m) {
		Matrix4f mat = new Matrix4f();
		mat.m00(m.a1());
		mat.m01(m.a2());
		mat.m02(m.a3());
		mat.m03(m.a4());

		mat.m10(m.b1());
		mat.m11(m.b2());
		mat.m12(m.b3());
		mat.m13(m.b4());

		mat.m20(m.c1());
		mat.m21(m.c2());
		mat.m22(m.c3());
		mat.m23(m.c4());

		mat.m30(m.d1());
		mat.m31(m.d2());
		mat.m32(m.d3());
		mat.m33(m.d4());
		return mat;
	}

}
