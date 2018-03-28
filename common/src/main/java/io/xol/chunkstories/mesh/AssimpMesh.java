package io.xol.chunkstories.mesh;

import org.joml.Matrix4f;

import assimp.AiBone;
import assimp.AiMaterial;
import assimp.AiMesh;
import assimp.AiScene;
import assimp.Importer;
import glm_.mat4x4.Mat4;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.MeshLibrary;

public class AssimpMesh {
	protected final Asset mainAsset;
	protected final MeshLibrary store;

	public AssimpMesh(Asset mainAsset, MeshLibrary store) throws MeshLoadException {
		this.mainAsset = mainAsset;
		this.store = store;
		
		Importer im = new Importer();
		assimp.SettingsKt.setASSIMP_LOAD_TEXTURES(false);
		
		im.setIoHandler(new AssetIOSystem(store.parent()));
		AiScene scene = im.readFile(mainAsset.getName(), im.getIoHandler(), 0);
		System.out.println(scene);
		
		for(AiMesh mesh : scene.getMeshes()) {
			System.out.println(mesh.getName());
			System.out.println(mesh.getFaces().size());
			
			AiMaterial material = scene.getMaterials().get(mesh.getMaterialIndex());
			System.out.println("mat: "+material.getName());
			System.out.println(material.getTextures());
			
			System.out.println("bones: "+mesh.getNumBones());
			for(AiBone bone : mesh.getBones()) {
				System.out.println(bone.getName());
				System.out.println(bone.getNumWeights());
				System.out.println(tomat4(bone.getOffsetMatrix()));
			}
			
		}
	}

	private Matrix4f tomat4(Mat4 mat4) {
		Matrix4f mat = new Matrix4f();
		mat.m00(mat4.v00());
		mat.m01(mat4.v01());
		mat.m02(mat4.v02());
		mat.m03(mat4.v03());
		
		mat.m10(mat4.v10());
		mat.m11(mat4.v11());
		mat.m12(mat4.v12());
		mat.m13(mat4.v13());
		
		mat.m20(mat4.v20());
		mat.m21(mat4.v21());
		mat.m22(mat4.v22());
		mat.m23(mat4.v23());
		
		mat.m30(mat4.v30());
		mat.m31(mat4.v31());
		mat.m32(mat4.v32());
		mat.m33(mat4.v33());
		
		mat.transpose();
		return mat;
	}
}	
