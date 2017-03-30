package io.xol.chunkstories.world;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldCollisionsManager;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContextOlder;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.iterators.EntityRayIterator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class BuiltInWorldCollisionsManager implements WorldCollisionsManager
{
	private final WorldImplementation world;
	
	public BuiltInWorldCollisionsManager(WorldImplementation world) {
		this.world = world;
	}
	
	@Override
	public Location raytraceSolid(Vector3dm initialPosition, Vector3dm direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false, false);
	}

	@Override
	public Location raytraceSolidOuter(Vector3dm initialPosition, Vector3dm direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, true, false);
	}

	@Override
	public Location raytraceSelectable(Location initialPosition, Vector3dm direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false, true);
	}

	private Location raytraceSolid(Vector3dm initialPosition, Vector3dm direction, double limit, boolean outer, boolean selectable)
	{
		direction.normalize();
		//direction.scale(0.02);

		//float distance = 0f;
		Voxel vox;
		int x, y, z;
		x = (int) Math.floor(initialPosition.getX());
		y = (int) Math.floor(initialPosition.getY());
		z = (int) Math.floor(initialPosition.getZ());

		//DDA algorithm

		//It requires double arrays because it works using loops over each dimension
		double[] rayOrigin = new double[3];
		double[] rayDirection = new double[3];
		rayOrigin[0] = initialPosition.getX();
		rayOrigin[1] = initialPosition.getY();
		rayOrigin[2] = initialPosition.getZ();
		rayDirection[0] = direction.getX();
		rayDirection[1] = direction.getY();
		rayDirection[2] = direction.getZ();
		int voxelCoords[] = new int[] { x, y, z };
		int voxelDelta[] = new int[] { 0, 0, 0 };
		double[] deltaDist = new double[3];
		double[] next = new double[3];
		int step[] = new int[3];

		int side = 0;
		//Prepare distances
		for (int i = 0; i < 3; ++i)
		{
			double deltaX = rayDirection[0] / rayDirection[i];
			double deltaY = rayDirection[1] / rayDirection[i];
			double deltaZ = rayDirection[2] / rayDirection[i];
			deltaDist[i] = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
			if (rayDirection[i] < 0.f)
			{
				step[i] = -1;
				next[i] = (rayOrigin[i] - voxelCoords[i]) * deltaDist[i];
			}
			else
			{
				step[i] = 1;
				next[i] = (voxelCoords[i] + 1.f - rayOrigin[i]) * deltaDist[i];
			}
		}

		do
		{
			
			//DDA steps
			side = 0;
			for (int i = 1; i < 3; ++i)
			{
				if (next[side] > next[i])
				{
					side = i;
				}
			}
			next[side] += deltaDist[side];
			voxelCoords[side] += step[side];
			voxelDelta[side] += step[side];

			x = voxelCoords[0];
			y = voxelCoords[1];
			z = voxelCoords[2];
			vox = VoxelsStore.get().getVoxelById(world.getVoxelData(x, y, z));
			if (vox.getType().isSolid() || (selectable && vox.isVoxelSelectable()))
			{
				boolean collides = false;
				for (CollisionBox box : vox.getTranslatedCollisionBoxes(world, x, y, z))
				{
					//System.out.println(box);
					Vector3dm collisionPoint = box.lineIntersection(initialPosition, direction);
					if (collisionPoint != null)
					{
						collides = true;
						//System.out.println("collides @ "+collisionPoint);
					}
				}
				if (collides)
				{
					if (!outer)
						return new Location(world, x, y, z);
					else
					{
						//Back off a bit
						switch (side)
						{
						case 0:
							x -= step[side];
							break;
						case 1:
							y -= step[side];
							break;
						case 2:
							z -= step[side];
							break;
						}
						return new Location(world, x, y, z);
					}
				}
			}
			
			//distance += deltaDist[side];

		}
		while (voxelDelta[0] * voxelDelta[0] + voxelDelta[1] * voxelDelta[1] + voxelDelta[2] * voxelDelta[2] < limit * limit);
		return null;
	}

	@Override
	public Iterator<Entity> rayTraceEntities(Vector3dm initialPosition, Vector3dm direction, double limit)
	{
		double blocksLimit = limit;

		Vector3dm blocksCollision = this.raytraceSolid(initialPosition, direction, limit);
		if (blocksCollision != null)
			blocksLimit = blocksCollision.distanceTo(initialPosition);

		return raytraceEntitiesIgnoringVoxels(initialPosition, direction, Math.min(blocksLimit, limit));
	}

	@Override
	public Iterator<Entity> raytraceEntitiesIgnoringVoxels(Vector3dm initialPosition, Vector3dm direction, double limit)
	{
		return new EntityRayIterator(world, initialPosition, direction, limit);
	}
	
	/** 
	 * Does a complicated check to see how far the entity can go using the delta direction, from the 'start' position.
	 * Does not actually move anything
	 * Returns the remaining distance in each dimension if it got stuck ( with vec3(0.0, 0.0, 0.0) meaning it can safely move without colliding with anything )
	 */
	public Vector3dm runEntityAgainstWorldVoxels(Entity entity, Vector3dm from, Vector3dm delta)
	{
		int id, data;

		boolean collision = false;
		
		//Extract the current position
		Vector3dm pos = new Vector3dm(from);

		//Keep biggest distanceToTravel for each dimension collisionBox of our entity
		Vector3dm maxDistanceToTravel = new Vector3dm(0.0);

		Vector3dm direction = new Vector3dm(delta);
		direction.normalize();

		//Iterate over every box
		for (int r = 0; r < entity.getCollisionBoxes().length; r++)
		{
			// Make a normalized double vector and keep the original length
			Vector3dm vec = new Vector3dm(delta);
			Vector3dm distanceToTravel = new Vector3dm(delta);
			double len = vec.length();
			vec.normalize();
			vec.scale(0.25d);
			
			// Do it block per block, face per face
			double distanceTraveled = 0;
			CollisionBox checkerX = entity.getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ());
			CollisionBox checkerY = entity.getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ());
			CollisionBox checkerZ = entity.getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ());

			double stepDistanceX, stepDistanceY, stepDistanceZ;

			while (distanceTraveled < len)
			{
				if (len - distanceTraveled > 0.25)
				{
					//DistanceTraveled is incremented no matter what, for momentum loss while sliding on walls
					distanceTraveled += 0.25;
				}
				else
				{
					vec = new Vector3dm(delta);
					vec.normalize();
					vec.scale(len - distanceTraveled);
					distanceTraveled = len;
				}

				stepDistanceX = vec.getX();
				stepDistanceY = vec.getY();
				stepDistanceZ = vec.getZ();

				Voxel vox;

				// Z part
				checkerZ = entity.getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ() + stepDistanceZ);
				for(int i = (int)Math.floor(pos.getX()) - 1; i < (int)Math.ceil(pos.getX() + checkerX.xw); i++)
					for(int j = (int)Math.floor(pos.getY()) - 1; j < (int)Math.ceil(pos.getY() + checkerX.h); j++)
						for(int k = (int)Math.floor(pos.getZ()) - 1; k < (int)Math.ceil(pos.getZ() + checkerX.zw); k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.getType().isSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContextOlder(world, i, j, k));
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										box.translate(i, j, k);
										if (delta.getZ() != 0.0)
										{
											if (checkerZ.collidesWith(box))
											{
												collision = true;
												if (collision == false)
													break;
												stepDistanceZ = 0;
												if (delta.getZ() < 0)
												{
													double south = Math.min((box.zpos + box.zw + checkerZ.zw) - (pos.getZ()), 0.0d);
													// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
													//System.out.println("south:"+south);
													stepDistanceZ = south;
												}
												else
												{
													double north = Math.max((box.zpos) - (pos.getZ() + checkerZ.zw), 0.0d);
													//System.out.println("north:"+north);
													stepDistanceZ = north;
												}
												vec.setZ(0d);
												checkerZ = entity.getCollisionBoxes()[r].translate(pos.getX(), pos.getY(), pos.getZ() + stepDistanceZ);
											}
										}
									}
							}
						}
				distanceToTravel.setZ(distanceToTravel.getZ() - stepDistanceZ);
				pos.setZ(pos.getZ() + stepDistanceZ);

				// X-part
				checkerX = entity.getCollisionBoxes()[r].translate(pos.getX() + stepDistanceX, pos.getY(), pos.getZ());
				for(int i = (int)Math.floor(pos.getX()) - 1; i < (int)Math.ceil(pos.getX() + checkerY.xw); i++)
					for(int j = (int)Math.floor(pos.getY()) - 1; j < (int)Math.ceil(pos.getY() + checkerY.h); j++)
						for(int k = (int)Math.floor(pos.getZ()) - 1; k < (int)Math.ceil(pos.getZ() + checkerY.zw); k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.getType().isSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContextOlder(world, i, j, k));
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										box.translate(i, j, k);

										if (delta.getX() != 0.0)
										{
											if (checkerX.collidesWith(box))
											{
												collision = true;
												if (collision == false)
													break;
												stepDistanceX = 0;
												if (delta.getX() < 0)
												{
													double left = Math.min((box.xpos + box.xw + checkerX.xw) - (pos.getX()), 0.0d);
													//System.out.println("left:"+left);
													stepDistanceX = left;
												}
												else
												{
													double right = Math.max((box.xpos) - (pos.getX() + checkerX.xw), 0.0d);
													//System.out.println("right"+right);
													stepDistanceX = right;
												}
												vec.setX(0d);
												checkerX = entity.getCollisionBoxes()[r].translate(pos.getX() + stepDistanceX, pos.getY(), pos.getZ());
											}
										}
									}
							}
						}
				pos.setX(pos.getX() + stepDistanceX);
				distanceToTravel.setX(distanceToTravel.getX() - stepDistanceX);

				//Y-part
				checkerY = entity.getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + stepDistanceY, pos.getZ());
				for(int i = (int)Math.floor(pos.getX()) - 1; i < (int)Math.ceil(pos.getX() + checkerZ.xw); i++)
					for(int j = (int)Math.floor(pos.getY()) - 1; j < (int)Math.ceil(pos.getY() + checkerZ.h) + 1; j++)
						for(int k = (int)Math.floor(pos.getZ()) - 1; k < (int)Math.ceil(pos.getZ() + checkerZ.zw); k++)
						{
							data = this.world.getVoxelData(i, j, k);
							id = VoxelFormat.id(data);
							vox = VoxelsStore.get().getVoxelById(id);
							if (vox.getType().isSolid())
							{
								CollisionBox[] boxes = vox.getCollisionBoxes(new VoxelContextOlder(world, i, j, k));
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										box.translate(i, j, k);
										if (delta.getY() != 0.0)
										{
											if (checkerY.collidesWith(box))
											{
												collision = true;
												stepDistanceY = 0;
												if (delta.getY() < 0)
												{
													double top = Math.min((box.ypos + box.h) - pos.getY(), 0.0d);
													// System.out.println(top);
													stepDistanceY = top;
												}
												else
												{
													double bot = Math.max((box.ypos) - (pos.getY() + checkerY.h), 0.0d);
													// System.out.println(bot);
													stepDistanceY = bot;
												}
												vec.setY(0d);
												checkerY = entity.getCollisionBoxes()[r].translate(pos.getX(), pos.getY() + stepDistanceY, pos.getZ());
											}
										}

									}
							}
						}
				pos.setY(pos.getY() + stepDistanceY);
				distanceToTravel.setY(distanceToTravel.getY() - stepDistanceY);
			}

			if (Math.abs(distanceToTravel.getX()) > Math.abs(maxDistanceToTravel.getX()))
				maxDistanceToTravel.setX(distanceToTravel.getX());

			if (Math.abs(distanceToTravel.getY()) > Math.abs(maxDistanceToTravel.getY()))
				maxDistanceToTravel.setY(distanceToTravel.getY());

			if (Math.abs(distanceToTravel.getZ()) > Math.abs(maxDistanceToTravel.getZ()))
				maxDistanceToTravel.setZ(distanceToTravel.getZ());
		}
		//Set the new position after computations have been done

		//if (!onlyTest)
		//	this.moveWithoutCollisionRestrain(delta.getX() - maxDistanceToTravel.getX(), delta.getY() - maxDistanceToTravel.getY(), delta.getZ() - maxDistanceToTravel.getZ());
		
		//return new Vector3dm(delta.getX() - maxDistanceToTravel.getX(), delta.getY() - maxDistanceToTravel.getY(), delta.getZ() - maxDistanceToTravel.getZ());
		return maxDistanceToTravel;
	}
}
