//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.hppc.FloatArrayList;

import assimp.AiBone;
import assimp.AiMaterial;
import assimp.AiMesh;
import assimp.AiScene;
import assimp.AiVertexWeight;
import assimp.Importer;
import glm_.vec3.Vec3;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.AnimatableMesh;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.api.mesh.MeshMaterial;
import io.xol.chunkstories.util.FoldersUtils;

public class AssimpMeshLoader {

	private static final Logger logger = LoggerFactory.getLogger("content.meshes.assimp-kotlin");

	final MeshLibrary store;

	public AssimpMeshLoader(MeshLibrary meshStore) {
		store = meshStore;
	}

	class VertexBoneWeights {
		float[] weights = new float[4];
		int[] bones = new int[4];
		int slot = 0;
		float totalWeight = 0.0f;
	}

	public Mesh load(Asset mainAsset) throws MeshLoadException {
		if (mainAsset == null)
			throw new MeshLoadException(mainAsset);

		Importer im = new Importer();
		assimp.SettingsKt.setASSIMP_LOAD_TEXTURES(false);

		im.setIoHandler(new AssetIOSystem(store.parent()));
		AiScene scene = im.readFile(mainAsset.getName(), im.getIoHandler(), 0);

		if (scene == null) {
			logger.error("Could not load meshes from asset: " + mainAsset);
			throw new MeshLoadException(mainAsset);
		}
		/*
		 * for(AiMesh mesh : scene.getMeshes()) { System.out.println(mesh.getName());
		 * System.out.println(mesh.getFaces().size());
		 * 
		 * AiMaterial material = scene.getMaterials().get(mesh.getMaterialIndex());
		 * System.out.println("mat: "+material.getName());
		 * System.out.println(material.getTextures());
		 * 
		 * System.out.println("bones: "+mesh.getNumBones()); for(AiBone bone :
		 * mesh.getBones()) {
		 * System.out.println(bone.getName().substring(bone.getName().lastIndexOf('_') +
		 * 1)); System.out.println(bone.getNumWeights());
		 * System.out.println(tomat4(bone.getOffsetMatrix())); } }
		 */
		if (scene.getMeshes() == null || scene.getMeshes().size() == 0) {
			logger.error("Loaded mesh did not contain any mesh data.");
			return null;
		}

		FloatArrayList vertices = new FloatArrayList();
		FloatArrayList normals = new FloatArrayList();
		FloatArrayList texcoords = new FloatArrayList();

		Map<String, Integer> boneNames = new HashMap<>();
		ByteArrayList boneIds = new ByteArrayList();
		ByteArrayList boneWeights = new ByteArrayList();

		List<MeshMaterialLoaded> meshMaterials = new ArrayList<>();

		boolean has_bones = scene.getMeshes().get(0).getHasBones();
		Map<Integer, VertexBoneWeights> boneWeightsForeachVertex = null;
		if (has_bones)
			boneWeightsForeachVertex = new HashMap<>();

		String assetFolder = mainAsset.getName().substring(0, mainAsset.getName().lastIndexOf('/') + 1);
		// System.out.println("asset folder: "+assetFolder);

		int[] order = { 0, 1, 2 };
		for (AiMesh mesh : scene.getMeshes()) {
			int existing_vertices = vertices.size() / 3;

			AiMaterial material = scene.getMaterials().get(mesh.getMaterialIndex());
			MeshMaterialLoaded mml = new MeshMaterialLoaded(null, material.getName(), existing_vertices, -1,
					"./textures/notex.png", "./textures/normalnormal.png", "./textures/defaultmaterial.png");
			for (AiMaterial.Texture tex : material.getTextures()) {
				switch (tex.getType()) {
				case ambient:
					break;
				case diffuse:
					mml.albedoTextureName = FoldersUtils.combineNames(assetFolder, tex.getFile());
					break;
				case displacement:
					break;
				case emissive:
					break;
				case height:
					break;
				case lightmap:
					break;
				case none:
					break;
				case normals:
					mml.normalTextureName = FoldersUtils.combineNames(assetFolder, tex.getFile());
					break;
				case opacity:
					break;
				case reflection:
					break;
				case shininess:
					break;
				case specular:
					mml.specularTextureName = FoldersUtils.combineNames(assetFolder, tex.getFile());
					break;
				case unknown:
					break;
				default:
					break;

				}
				// System.out.println(tex.getFile()+":"+tex.getType());
			}

			meshMaterials.add(mml);

			if (has_bones) {
				for (int i = 0; i < mesh.getNumVertices(); i++) {
					boneWeightsForeachVertex.put(i + existing_vertices, new VertexBoneWeights());
				}
				for (AiBone bone : mesh.getBones()) {
					String boneName = bone.getName().substring(bone.getName().lastIndexOf('_') + 1);

					int boneId = boneNames.getOrDefault(boneName, -1);

					if (boneId == -1) {
						boneId = boneNames.size();
						boneNames.put(boneName, boneId);
					}
					for (AiVertexWeight weight : bone.getWeights()) {
						int vid = existing_vertices + weight.getVertexId();
						VertexBoneWeights vw = boneWeightsForeachVertex.get(vid);

						vw.bones[vw.slot] = boneId;
						vw.weights[vw.slot] = weight.getWeight();
						vw.slot++;
						vw.totalWeight += weight.getWeight();
						if (vw.totalWeight > 1.0f) {
							logger.warn("Total weight > 1 for vertex #" + vid);
						}
						if (vw.slot >= 4) {
							logger.error("More than 4 bones weighted against vertex #" + vid);
							return null;
						}
					}
				}
			}
			for (List<Integer> face : mesh.getFaces()) {
				if (face.size() == 3) {
					for (int i : order) { // swap
						Vec3 vertex = mesh.getVertices().get(face.get(i));
						Vec3 normal = mesh.getNormals().get(face.get(i));
						float[] texcoord = mesh.getTextureCoords().get(0).get(face.get(i));

						if (mainAsset.getName().endsWith("dae")) {
							// swap Y and Z axises
							vertices.add(vertex.x, vertex.z, -vertex.y);
							normals.add(normal.x, normal.z, -normal.y);
						} else {
							vertices.add(vertex.x, vertex.y, vertex.z);
							normals.add(normal.x, normal.y, normal.z);
						}
						texcoords.add(texcoord[0], 1.0f - texcoord[1]);

						if (has_bones) {
							VertexBoneWeights boned = boneWeightsForeachVertex.get(existing_vertices + face.get(i));
							boneIds.add((byte) boned.bones[0]);
							boneIds.add((byte) boned.bones[1]);
							boneIds.add((byte) boned.bones[2]);
							boneIds.add((byte) boned.bones[3]);

							boneWeights.add((byte) (boned.weights[0] * 255));
							boneWeights.add((byte) (boned.weights[1] * 255));
							boneWeights.add((byte) (boned.weights[2] * 255));
							boneWeights.add((byte) (boned.weights[3] * 255));
						}
					}
				} else
					logger.warn("Should triangulate! (face=" + face.size() + ")");
			}

			mml.lastVertex = vertices.size() / 3 - 1;
		}

		FloatBuffer verticesBuffer = toFloatBuffer(vertices);
		FloatBuffer normalsBuffer = toFloatBuffer(normals);
		FloatBuffer texcoordsBuffer = toFloatBuffer(texcoords);

		ByteBuffer boneIdsBuffer = toByteBuffer(boneIds);
		ByteBuffer boneWeightsBuffer = toByteBuffer(boneWeights);

		String[] boneNamesArray = new String[boneNames.size()];
		for (Entry<String, Integer> e : boneNames.entrySet()) {
			boneNamesArray[e.getValue()] = e.getKey();
		}

		Mesh mesh;

		// Hacky but whatever you get the point, we fill the data structure
		MeshMaterial materialsArray[] = new MeshMaterial[meshMaterials.size()];

		if (has_bones)
			mesh = new AnimatableMesh(verticesBuffer, texcoordsBuffer, normalsBuffer, boneNamesArray, boneIdsBuffer,
					boneWeightsBuffer, materialsArray);
		else
			mesh = new Mesh(verticesBuffer, texcoordsBuffer, normalsBuffer, materialsArray);

		for (int i = 0; i < materialsArray.length; i++) {
			meshMaterials.get(i).mesh = mesh;
			materialsArray[i] = meshMaterials.get(i);
		}

		return mesh;
	}

	private FloatBuffer toFloatBuffer(FloatArrayList array) {
		FloatBuffer fb = ByteBuffer.allocateDirect(array.size() * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < array.size(); i++) {
			fb.put(i, array.get(i));
		}
		fb.position(0);
		fb.limit(fb.capacity());
		return fb;
	}

	private ByteBuffer toByteBuffer(ByteArrayList array) {
		ByteBuffer bb = ByteBuffer.allocateDirect(array.size()).order(ByteOrder.nativeOrder());
		for (int i = 0; i < array.size(); i++) {
			bb.put(i, array.get(i));
		}
		bb.position(0);
		bb.limit(bb.capacity());
		return bb;
	}

	/*
	 * private Matrix4f tomat4(Mat4 mat4) { Matrix4f mat = new Matrix4f();
	 * mat.m00(mat4.v00()); mat.m01(mat4.v01()); mat.m02(mat4.v02());
	 * mat.m03(mat4.v03());
	 * 
	 * mat.m10(mat4.v10()); mat.m11(mat4.v11()); mat.m12(mat4.v12());
	 * mat.m13(mat4.v13());
	 * 
	 * mat.m20(mat4.v20()); mat.m21(mat4.v21()); mat.m22(mat4.v22());
	 * mat.m23(mat4.v23());
	 * 
	 * mat.m30(mat4.v30()); mat.m31(mat4.v31()); mat.m32(mat4.v32());
	 * mat.m33(mat4.v33());
	 * 
	 * mat.transpose(); return mat; }
	 */
}
