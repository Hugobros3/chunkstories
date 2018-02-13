package io.xol.chunkstories.voxel;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.exceptions.content.IllegalVoxelDeclarationException;
import io.xol.chunkstories.api.voxel.materials.Material;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.materials.GenericNamedConfigurable;

public class VoxelTypeImplementation extends GenericNamedConfigurable implements VoxelDefinition
{
	private final VoxelsStore store;
	
	private final Material material;
	private final VoxelModel model;
	private final VoxelTexture[] textures = new VoxelTexture[6];
	private final CollisionBox collisionBox;
	
	private final boolean solid;
	private final boolean opaque;
	private final boolean selfOpaque;
	private final boolean liquid;
	private final boolean billboard;
	private final boolean isSelectable;
	
	private final byte emittingLightLevel;
	private final byte shadingLightLevel;
	
	private final Voxel voxel;
	
	public VoxelTypeImplementation(VoxelsStore store, String name, BufferedReader reader) throws IOException, IllegalVoxelDeclarationException
	{
		super(name, reader);
		
		this.store = store;
		
		//If a specific material was given, use that one, else use the voxel name
		String matResolved = this.resolveProperty("material");
		this.material = store.parent().materials().getMaterialByName(matResolved != null ? matResolved : name);
		
		//If a special model is being used
		String modelName = this.resolveProperty("model");
		this.model = modelName == null ? null : store.models().getVoxelModelByName(modelName);
		
		//Is there an explicit list of textures ?
		String texturesResolved = this.resolveProperty("textures");
		int textureIndex = 0;
		if(texturesResolved != null)
		{
			String sides[] = texturesResolved.split(",");
			for (; textureIndex < sides.length; textureIndex++)
			{
				String textureName = sides[textureIndex].replace("[", "").replace("]", "").replace(" ", "");
				//System.out.println("Voxel "+name+" texture"+textureIndex+" ="+textureName);
				textures[textureIndex] = store.textures().getVoxelTextureByName(textureName);
			}
		}
		//Try the 'texture' (no s) tag and if all else fail, use the voxel name as the texture name
		String textureResolved = this.resolveProperty("texture");
		VoxelTexture textureToFill = store.textures().getVoxelTextureByName(textureResolved != null ? textureResolved : name);
		//Fill the remaining slots with whatever we found
		while(textureIndex < 6)
		{
			textures[textureIndex] = textureToFill;
			textureIndex++;
		}
		
		//Load the collision box if it's defined
		String collisionBoxResolved = this.resolveProperty("collisionBox");
		if(collisionBoxResolved != null)
		{
			String sizes[] = (collisionBoxResolved.replace("(", "").replace(")", "")).split(",");

			collisionBox = new CollisionBox(Float.parseFloat(sizes[3]), Float.parseFloat(sizes[4]), Float.parseFloat(sizes[5]));
			collisionBox.translate(Float.parseFloat(sizes[0]), Float.parseFloat(sizes[1]), Float.parseFloat(sizes[2]));
		}
		else
			collisionBox = new CollisionBox(1, 1, 1);
		
		//Load a few trivial properties
		solid = getBoolean("solid", true);
		opaque = getBoolean("opaque", true);
		selfOpaque = getBoolean("selfOpaque", false);
		liquid = getBoolean("liquid", false);
		billboard = getBoolean("billboard", false);
		isSelectable = getBoolean("isSelectable", solid && !liquid);
		
		emittingLightLevel = getByte("emitting", (byte)0);
		shadingLightLevel = getByte("shading", (byte)0);
		
		//Once ALL that is done, we call the proper class and make a Voxel object
		String className = this.resolveProperty("customClass", "io.xol.chunkstories.api.voxel.Voxel");
		try
		{
			Class<?> rawClass = store.parent().modsManager().getClassByName(className);
			if (rawClass == null)
				throw new IllegalVoxelDeclarationException("Voxel " + this.getName() + " does not exist in codebase.");
			else if (!(Voxel.class.isAssignableFrom(rawClass)))
				throw new IllegalVoxelDeclarationException("Voxel " + this.getName() + " is not extending the Voxel class.");
			else
			{
				@SuppressWarnings("unchecked")
				Class<? extends Voxel> voxelClass = (Class<? extends Voxel>) rawClass;
				Class<?>[] types = { VoxelDefinition.class };
				Constructor<? extends Voxel> constructor = voxelClass.getConstructor(types);

				if (constructor == null)
					throw new IllegalVoxelDeclarationException("Voxel " + this.getName() + " does not provide a valid constructor.");

				Object[] parameters = { this };
				voxel = (Voxel) constructor.newInstance(parameters);
			}
		}
		catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e)
		{
			e.printStackTrace();
			throw new IllegalVoxelDeclarationException("Voxel " + this.getName() + " has an issue with it's constructor: " + e.getMessage());
		}
	}

	private boolean getBoolean(String propertyName, boolean defaultValue)
	{
		String resolved = this.resolveProperty(propertyName);
		return resolved == null ? defaultValue : resolved.equals("true");
	}
	
	private byte getByte(String propertyName, byte defaultValue)
	{
		String resolved = this.resolveProperty(propertyName);
		return resolved == null ? defaultValue : Byte.parseByte(resolved);
	}

	@Override
	public Material getMaterial()
	{
		return material;
	}
	
	@Override
	public VoxelModel getVoxelModel()
	{
		return model;
	}

	@Override
	public VoxelTexture getVoxelTexture(VoxelSides side)
	{
		return textures[side.ordinal()];
	}

	@Override
	public CollisionBox getCollisionBox()
	{
		return collisionBox;
	}

	@Override
	public boolean isSolid()
	{
		return solid;
	}

	@Override
	public boolean isOpaque()
	{
		return opaque;
	}

	@Override
	public boolean isSelfOpaque()
	{
		return selfOpaque;
	}

	@Override
	public boolean isLiquid()
	{
		return liquid;
	}

	@Override
	public boolean isBillboard()
	{
		return billboard;
	}

	@Override
	public byte getEmittingLightLevel()
	{
		return emittingLightLevel;
	}

	@Override
	public byte getShadingLightLevel()
	{
		return shadingLightLevel;
	}

	@Override
	public Voxels store()
	{
		return store;
	}
	
	public Voxel getVoxelObject()
	{
		return voxel;
	}

	@Override
	public boolean isSelectable() {
		return isSelectable;
	}
}
