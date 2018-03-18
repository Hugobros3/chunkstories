//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.debug;

import java.util.Iterator;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4f;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.EntityLiving.HitBox;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.cell.CellData;

public class PhysicsWireframeDebugger {
	
	ClientInterface client;
	WorldClient world;
	
	public PhysicsWireframeDebugger(ClientInterface client, WorldClient world) {
		super();
		this.client = client;
		this.world = world;
	}

	public void render(RenderingInterface renderer) {
		
		Vector3dc cameraPosition = renderer.getCamera().getCameraPosition();
		
		//int id, data;
		int drawDebugDist = 6;

		//Iterate over nearby voxels
		for (int i = ((int)(double) cameraPosition.x()) - drawDebugDist; i <= ((int)(double) cameraPosition.x()) + drawDebugDist; i++)
			for (int j = ((int)(double) cameraPosition.y()) - drawDebugDist; j <= ((int)(double) cameraPosition.y()) + drawDebugDist; j++)
				for (int k = ((int)(double) cameraPosition.z()) - drawDebugDist; k <= ((int)(double) cameraPosition.z()) + drawDebugDist; k++) {
					//data = world.peekSimple(i, j, k);
					//id = VoxelFormat.id(data);
					CellData cell = world.peekSafely(i, j, k);
					//System.out.println(i+":"+j+":"+k);
					//System.out.println(cell.getX() + ":"+cell.getY()+":"+cell.getZ());
					CollisionBox[] tboxes = cell.getTranslatedCollisionBoxes();
					//System.out.println(tboxes.length);
					if (tboxes != null) {
						//Draw all their collision boxes
						for (CollisionBox box : tboxes) {
							//System.out.println(cell.getX() + ":"+cell.getY()+":"+cell.getZ());
							
							if (cell.getVoxel().getDefinition().isSolid())
								//Red if solid
								FakeImmediateModeDebugRenderer.renderCollisionBox(box, new Vector4f(1, 0, 0, 1.0f));
							else
								//Yellow otherwise
								FakeImmediateModeDebugRenderer.renderCollisionBox(box, new Vector4f(1, 1, 0, 0.25f));
						}
					}
				}

		//Iterate over each entity
		Iterator<Entity> ie = world.getAllLoadedEntities();
		while (ie.hasNext()) {
			Entity e = ie.next();
			
			//Entities with hitboxes see all of those being drawn
			if(e instanceof EntityLiving) {
				EntityLiving eli = (EntityLiving)e;
				for(HitBox hitbox: eli.getHitBoxes()) {
					hitbox.draw(renderer);
				}
			}
			
			//Get the entity bounding box
			if(e.getTranslatedBoundingBox().lineIntersection(cameraPosition, new Vector3d(renderer.getCamera().getViewDirection())) != null)
				FakeImmediateModeDebugRenderer.renderCollisionBox(e.getTranslatedBoundingBox(), new Vector4f(0, 0, 0.5f, 1.0f));
			else
				FakeImmediateModeDebugRenderer.renderCollisionBox(e.getTranslatedBoundingBox(), new Vector4f(0, 1f, 1f, 1.0f));
			
			//And the collision box
			for(CollisionBox box : e.getCollisionBoxes()) {
				box.translate(e.getLocation());
				FakeImmediateModeDebugRenderer.renderCollisionBox(box, new Vector4f(0, 1, 0.5f, 1.0f));
			}
		}
	}
}
