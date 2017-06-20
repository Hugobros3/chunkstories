package io.xol.chunkstories.mesh;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MultiPartMesh;
import io.xol.chunkstories.api.rendering.mesh.ClientMeshLibrary;
import io.xol.chunkstories.api.rendering.mesh.RenderableMesh;
import io.xol.chunkstories.api.rendering.mesh.RenderableMultiPartAnimatableMesh;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer;
import io.xol.engine.model.MeshRenderableImpl;
import io.xol.engine.model.MultiPartMeshRenderableImpl;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientMeshStore implements ClientMeshLibrary {
	
	private final MeshStore store;
	private final ClientContent content;
	
	private final Map<String, RenderableMesh> renderableMeshes = new HashMap<String, RenderableMesh>();
	
	public ClientMeshStore(ClientContent content, MeshStore store)
	{
		this.store = store;
		this.content = content;
	}

	@Override
	public Mesh getMeshByName(String meshName) {
		return store.getMeshByName(meshName);
	}

	@Override
	public MultiPartMesh getMultiPartMeshByName(String meshName) {
		return store.getMultiPartMeshByName(meshName);
	}

	@Override
	public RenderableMesh getRenderableMeshByName(String meshName) {
		RenderableMesh rm = renderableMeshes.get(meshName);
		
		if(rm == null) {
			Mesh mesh = this.getMeshByName(meshName);
			if(mesh == null) {
				//Really not found!
				return getRenderableMeshByName("./models/error.obj");
			}
			
			if(mesh instanceof MultiPartMesh)
				rm = new MultiPartMeshRenderableImpl((MultiPartMesh)mesh);
			else
				rm = new MeshRenderableImpl(mesh);
			
			renderableMeshes.put(meshName, rm);
		}
		return rm;
	}

	@Override
	public RenderableMultiPartAnimatableMesh getRenderableMultiPartAnimatableMeshByName(String meshName) {
		RenderableMesh rm = this.getRenderableMeshByName(meshName);
		
		return rm instanceof RenderableMultiPartAnimatableMesh ? (RenderableMultiPartAnimatableMesh)rm : null;
	}

	@Override
	public ClientContent parent() {
		
		return content;
	}

	public void reload() {
		renderableMeshes.clear();
	}

	@Override
	public VertexBuffer getIdentityCube() {
		return FakeImmediateModeDebugRenderer.getCube();
	}
}
