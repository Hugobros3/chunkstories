package io.xol.chunkstories.entity;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.inventory.Inventory;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.DefferedLight;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.ChunkHolder;
import io.xol.chunkstories.world.World;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Entity
{
	public long entityID;

	public World world;
	public double posX, posY, posZ;
	public double velX, velY, velZ;
	public Vector3d accelerationVector;
	public float rotH, rotV;

	public boolean collision_top = false;
	public boolean collision_bot = false;
	public boolean collision_left = false;
	public boolean collision_right = false;
	public boolean collision_north = false;
	public boolean collision_south = false;

	public Vector3d blockedMomentum = new Vector3d();
	
	public boolean inWater = false;
	
	public Inventory inventory;
	
	public ChunkHolder parentHolder;

	protected boolean flying = false;
	
	//Flag set when deleted from world entities list ( to report to other refering places )
	public boolean deleteFlag = false;
	
	public Entity(World w, double x, double y, double z)
	{
		world = w;
		posX = x;
		posY = y;
		posZ = z;
		accelerationVector = new Vector3d();
		setHolder();
	}
	
	public void setPosition(double x, double y, double z)
	{
		posX = x;
		posY = y;
		posZ = z;
		setHolder();
	}

	public void setVelocity(double x, double y, double z)
	{
		velX = x;
		velY = y;
		velZ = z;
	}

	public void applyExternalForce(double x, double y, double z)
	{
		velX += x;
		velY += y;
		velZ += z;
	}

	// Server-side updating
	public void update()
	{
		posX %= world.getSizeSide();
		posZ %= world.getSizeSide();
		
		Voxel voxelIn = VoxelTypes.get(VoxelFormat.id(world.getDataAt((int) (posX), (int) (posY + 1), (int) (posZ))));
		inWater = voxelIn.isVoxelLiquid();

		// velZ=Math.cos(a)*hSpeed*0.1;
		if (collision_left || collision_right)
			velX = 0;
		if (collision_north || collision_south)
			velZ = 0;
		// Stap it
		if (collision_bot && velY < 0)
			velY = 0;
		else if (collision_top)
			velY = 0;
		// Gravity

		if(!flying)
		{
			double terminalVelocity = inWater ? -0.02 : -0.5;
			if (velY > terminalVelocity)
				velY -= 0.008;
			if (velY < terminalVelocity)
				velY = terminalVelocity;
		}

		// Acceleration
		velX += accelerationVector.x;
		velY += accelerationVector.y;
		velZ += accelerationVector.z;
		
		blockedMomentum = moveWithCollisionRestrain(velX, velY, velZ, true);
		
		changeChunk();
	}

	public boolean changeChunk()
	{
		posX %= world.getSizeSide();
		posZ %= world.getSizeSide();
		if(posX < 0)
			posX += world.getSizeSide();
		if(posZ < 0)
			posZ += world.getSizeSide();
		int regionX = (int) (posX / (32 * 8));
		int regionY = (int) (posY / (32 * 8));
		if(regionY < 0)
			regionY = 0;
		if(regionY > world.getMaxHeight() / (32 * 8 ))
			regionY = world.getMaxHeight() / (32 * 8 );
		int regionZ = (int) (posZ / (32 * 8));
		if(parentHolder != null && parentHolder.regionX == regionX && parentHolder.regionY == regionY && parentHolder.regionZ == regionZ)
		{
			return false; // Nothing to do !
		}
		else
		{
			//if(parentHolder != null)
			//	parentHolder.removeEntity(this);
			parentHolder = world.chunksHolder.getChunkHolder(regionX * 8, regionY * 8, regionZ * 8, true);
			//parentHolder.addEntity(this);
			/*System.out.println("Had to move entity "+this+" to a new holder :");
			System.out.println("RegionX : "+regionX+" PH: "+parentHolder.regionX);
			System.out.println("RegionY : "+regionY+" PH: "+parentHolder.regionY);
			System.out.println("RegionZ : "+regionZ+" PH: "+parentHolder.regionZ);*/
			return true;
		}
	}

	public void setHolder()
	{
		posX %= world.getSizeSide();
		posZ %= world.getSizeSide();
		int regionX = (int) (posX / (32 * 8));
		int regionY = (int) (posY / (32 * 8));
		if(regionY < 0)
			regionY = 0;
		if(regionY > world.getMaxHeight() / (32 * 8 ))
			regionY = world.getMaxHeight() / (32 * 8 );
		int regionZ = (int) (posZ / (32 * 8));
		parentHolder = world.chunksHolder.getChunkHolder(regionX * 8, regionY * 8, regionZ * 8, true);
	}
	
	public void moveWithoutCollisionRestrain(double mx, double my, double mz)
	{
		posX += mx;
		posY += my;
		posZ += mz;
	}

	public String toString()
	{
		return "['"+this.getClass().getName()+"'] PosX : "+clampDouble(posX)+" PosY"+clampDouble(posY)+" PosZ"+clampDouble(posZ) +" UUID : "+entityID+" EID : "+this.getEID()+" Holder:"+this.parentHolder;
	}
	
	double clampDouble(double d)
	{
		d *= 100;
		d = Math.floor(d);
		d /= 100.0;
		return d;
	}
	
	public Vector3d moveWithCollisionRestrain(Vector3d vec)
	{
		return moveWithCollisionRestrain(vec.x, vec.y, vec.z, false);
	}
	
	// Convinience method
	public Vector3d moveWithCollisionRestrain(double mx, double my, double mz, boolean writeCollisions)
	{
		int id, data;

		boolean collision = false;
		if(writeCollisions)
		{
			collision_top = false;
			collision_bot = false;
			collision_left = false;
			collision_right = false;
			collision_north = false;
			collision_south = false;
		}
		// Make a normalized double vector and keep the original length
		Vector3d vec = new Vector3d(mx, my, mz);
		Vector3d distanceToTravel = new Vector3d(mx, my, mz);
		double len = vec.length();
		vec.normalize();
		vec.scale(0.25d);
		// Do it block per block, face per face
		double distanceTraveled = 0;
		// CollisionBox checker = getCollisionBox().translate(posX, posY, posZ);

		CollisionBox checkerX = getCollisionBox().translate(posX, posY, posZ);
		CollisionBox checkerY = getCollisionBox().translate(posX, posY, posZ);
		CollisionBox checkerZ = getCollisionBox().translate(posX, posY, posZ);

		double pmx, pmy, pmz;

		while (distanceTraveled < len)
		{
			if (len - distanceTraveled > 0.25)
			{
				distanceTraveled += 0.25;
			}
			else
			{
				vec = new Vector3d(mx, my, mz);
				vec.normalize();
				vec.scale(len - distanceTraveled);
				distanceTraveled = len;
			}

			pmx = vec.x;
			pmy = vec.y;
			pmz = vec.z;

			int radius = 2;

			// checkerX = getCollisionBox().translate(posX+pmx, posY, posZ);
			Voxel vox;
			checkerZ = getCollisionBox().translate(posX, posY, posZ + pmz);
			// Z part
			for (int i = ((int) posX) - radius; i <= ((int) posX) + radius; i++)
				for (int j = ((int) posY); j <= ((int) posY) + (int) Math.ceil(checkerY.h) + 1; j++)
					for (int k = ((int) posZ) - radius; k <= ((int) posZ) + radius; k++)
					{
						data = Client.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						vox = VoxelTypes.get(id);
						if (vox.isVoxelSolid())
						{
							CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
							if (boxes != null)
								for (CollisionBox b : boxes)
								{
									b.translate(i, j, k);
									if (mz != 0.0)
									{
										if (checkerZ.collidesWith(b))
										{
											collision = true;
											if (collision == false)
												break;
											pmz = 0;
											if (mz < 0)
											{
												double south = Math.min((b.zpos + b.zw / 2.0 + checkerZ.zw / 2.0) - (posZ), 0.0d);
												// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
												pmz = south;
												if(writeCollisions)
												collision_south = true;
											}
											else
											{
												double north = Math.max((b.zpos - b.zw / 2.0 - checkerZ.zw / 2.0) - (posZ), 0.0d);
												// System.out.println(right);
												pmz = north;
												if(writeCollisions)
												collision_north = true;
											}
											vec.z = 0;
											checkerZ = getCollisionBox().translate(posX, posY, posZ + pmz);
										}
									}
								}
						}
					}
			distanceToTravel.z -= pmz;
			posZ += pmz;
			checkerX = getCollisionBox().translate(posX + pmx, posY, posZ);
			// X-part
			for (int i = ((int) posX) - radius; i <= ((int) posX) + radius; i++)
				for (int j = ((int) posY); j <= ((int) posY) + (int) Math.ceil(checkerY.h) + 1; j++)
					for (int k = ((int) posZ) - radius; k <= ((int) posZ) + radius; k++)
					{
						data = Client.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						vox = VoxelTypes.get(id);
						if (vox.isVoxelSolid())
						{
							CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
							if (boxes != null)
								for (CollisionBox b : boxes)
								{
									b.translate(i, j, k);

									if (mx != 0.0)
									{
										if (checkerX.collidesWith(b))
										{
											collision = true;
											if (collision == false)
												break;
											pmx = 0;
											if (mx < 0)
											{
												double left = Math.min((b.xpos + b.xw / 2.0 + checkerX.xw / 2.0) - (posX), 0.0d);
												// System.out.println(left+" : "+(b.xpos+b.xw/2.0+checkerX.xw/2.0)+" : "+((b.xpos+b.xw/2.0+checkerX.xw/2.0)-(checkerX.xpos)));
												pmx = left;
												if(writeCollisions)
												collision_left = true;
											}
											else
											{
												double right = Math.max((b.xpos - b.xw / 2.0 - checkerX.xw / 2.0) - (posX), 0.0d);
												// System.out.println(right);
												pmx = right;
												if(writeCollisions)
												collision_right = true;
											}
											vec.x = 0;
											checkerX = getCollisionBox().translate(posX + pmx, posY, posZ);
										}
									}
								}
						}
					}
			posX += pmx;
			distanceToTravel.x -= pmx;

			checkerY = getCollisionBox().translate(posX, posY + pmy, posZ);
			for (int i = ((int) posX) - radius; i <= ((int) posX) + radius; i++)
				for (int j = ((int) posY); j <= ((int) posY) + (int) Math.ceil(checkerY.h) + 1; j++)
					for (int k = ((int) posZ) - radius; k <= ((int) posZ) + radius; k++)
					{
						data = Client.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						vox = VoxelTypes.get(id);
						if (vox.isVoxelSolid())
						{
							CollisionBox[] boxes = vox.getCollisionBoxes(new BlockRenderInfo(world, i, j, k));
							if (boxes != null)
								for (CollisionBox b : boxes)
								{
									b.translate(i, j, k);
									if (my != 0.0)
									{
										if (checkerY.collidesWith(b))
										{
											collision = true;
											pmy = 0;
											if (my < 0)
											{
												double top = Math.min((b.ypos + b.h) - posY, 0.0d);
												// System.out.println(top);
												pmy = top;
												if(writeCollisions)
												collision_bot = true;
											}
											else
											{
												double bot = Math.max((b.ypos) - (posY + checkerY.h), 0.0d);
												// System.out.println(bot);
												pmy = bot;
												if(writeCollisions)
												collision_top = true;
											}
											vec.y = 0;
											checkerY = getCollisionBox().translate(posX, posY + pmy, posZ);
										}
									}

								}
						}
					}
			posY += pmy;
			distanceToTravel.y -= pmy;
		}
		return distanceToTravel;
	}

	public DefferedLight[] getLights()
	{
		return null;
	}
	
	public CollisionBox[] getTranslatedCollisionBoxes()
	{
		return new CollisionBox[] { getCollisionBox().translate(posX, posY, posZ) };
	}

	private CollisionBox getCollisionBox()
	{
		return new CollisionBox(0.75, 1.80, 0.75);
	}

	public boolean renderable()
	{
		return false;
	}

	public void render()
	{
		// Do nothing.
	}

	public void debugDraw()
	{
		// Do nothing.
	}
	
	public void setupCamera(Camera camera)
	{
		synchronized(this)
		{
			camera.camPosX = (float) -posX;
			camera.camPosY = (float) -posY;
			camera.camPosZ = (float) -posZ;
	
			camera.view_rotx = rotV;
			camera.view_roty = rotH;
	
			camera.fov = FastConfig.fov;
	
			camera.alUpdate();
		}
	}
	
	public short getEID()
	{
		return EntitiesList.getIdForClass(getClass().getName());
		//return allocatedID;
	}
	
	public static short allocatedID = 0;

	public long getUUID()
	{
		return entityID;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Entity))
			return false;
		return ((Entity)o).entityID == entityID;
	}
	
	public void delete()
	{
		deleteFlag = true;
	}
}
