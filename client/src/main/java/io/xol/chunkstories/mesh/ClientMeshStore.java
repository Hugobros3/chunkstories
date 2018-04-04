//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.mesh.AnimatableMesh;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.rendering.mesh.ClientMeshLibrary;
import io.xol.chunkstories.api.rendering.mesh.RenderableMesh;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer;
import io.xol.chunkstories.renderer.mesh.BonedRenderer;

public class ClientMeshStore implements ClientMeshLibrary {

	private final MeshStore store;
	private final ClientContent content;

	private final Map<String, RenderableMesh> renderableMeshes = new HashMap<String, RenderableMesh>();

	public ClientMeshStore(ClientContent content, MeshStore store) {
		this.store = store;
		this.content = content;
	}

	@Override
	public Mesh getMesh(String meshName) {
		return store.getMesh(meshName);
	}

	@Override
	public AnimatableMesh getAnimatableMesh(String meshName) {
		return store.getAnimatableMesh(meshName);
	}

	@Override
	public RenderableMesh getRenderableMesh(String meshName) {
		RenderableMesh rm = renderableMeshes.get(meshName);

		if (rm == null) {
			Mesh mesh = this.getMesh(meshName);
			if (mesh == null) {
				// Really not found!
				return getRenderableMesh("./models/error.obj");
			}

			if (mesh instanceof AnimatableMesh)
				rm = new BonedRenderer((AnimatableMesh) mesh);
			else
				rm = new MeshRenderer(mesh);

			renderableMeshes.put(meshName, rm);
		}
		return rm;
	}

	@Override
	public BonedRenderer getRenderableAnimatableMesh(String meshName) {
		RenderableMesh rm = this.getRenderableMesh(meshName);

		return rm instanceof BonedRenderer ? (BonedRenderer) rm : null;
	}

	@Override
	public ClientContent parent() {

		return content;
	}

	public void reloadAll() {
		store.reloadAll();
		renderableMeshes.clear();
	}

	@Override
	public VertexBuffer getIdentityCube() {
		return FakeImmediateModeDebugRenderer.getCube();
	}
}
