//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxels.textures;

import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL20.GL_MAX_TEXTURE_IMAGE_UNITS;
import static org.lwjgl.opengl.GL30.GL_MAX_ARRAY_TEXTURE_LAYERS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.AssetHierarchy;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.voxel.VoxelsStore;

public class VoxelTexturesArrays {
	
	private final GameContentStore content;
	@SuppressWarnings("unused")
	private final VoxelsStore voxels;
	
	private static final Logger logger = LoggerFactory.getLogger("content.voxels.textures");
	public Logger logger() {
		return logger;
	}
	
	public VoxelTexturesArrays(VoxelsStore voxels)
	{
		this.content = voxels.parent();
		this.voxels = voxels;

		this.buildTextureAtlas();
	}

	Map<String, VoxelTextureArrayed> textures = new HashMap<String, VoxelTextureArrayed>();
	AtlasElement defaultAlbedo, defaultNormal, defaultMaterial;
	
	public void buildTextureAtlas()
	{
		defaultAlbedo = null;
		defaultNormal = null;
		defaultMaterial = null;
		
		textures.clear();
		
		try
		{
			int gl_MaxTextureUnits = glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);
			int gl_MaxTextureArraySize = glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS);
			
			if(gl_MaxTextureArraySize < 2048) {
				logger().warn("Max texture array size < 2048. For ideal results please use a GPU from this geological era.");
			}
			if(gl_MaxTextureUnits < 32) {
				logger().warn("Max texture units < 32. This means your GPU is ancient and you'll run into a lot of issues!");
			}

			//We'll reserve 8 texture units for all the other fluff
			int maxTextureArrays = (gl_MaxTextureUnits - 8);

			List<AtlasElement> elements = new ArrayList<AtlasElement>();
			//Map<Integer, AtlasElement> sizeBuckets = new HashMap<Integer, AtlasElement>();
			
			//First we want to iterate over every file to get an idea of how many textures (and of how many sizes) we are dealing
			Iterator<AssetHierarchy> allFiles = content.modsManager().getAllUniqueEntries();
			AssetHierarchy entry;
			Asset f;
			while (allFiles.hasNext())
			{
				entry = allFiles.next();
				if (entry.getName().startsWith("./voxels/textures/"))
				{
					String name = entry.getName().replace("./voxels/textures/", "");
					
					Type type = Type.ALBEDO;
					if(name.startsWith("normal/")) {
						name = name.substring("normal/".length());
						type = Type.NORMAL;
					} else if(name.startsWith("material/")) {
						name = name.substring("material/".length());
						type = Type.MATERIAL;
					}
					
					//We are only interested in top-level textures.
					if (name.contains("/")) 
						continue;
					
					f = entry.topInstance();
					if (f.getName().endsWith(".png"))
					{
						String textureName = name.replace(".png", "");
						
						//VoxelTextureAtlased voxelTexture = new VoxelTextureAtlased(textureName, uniquesIds);

						int width, height;
						try
						{
							ImageReader reader = ImageIO.getImageReadersBySuffix("png").next();
							ImageInputStream stream = ImageIO.createImageInputStream(f.read());
							reader.setInput(stream);
							width = reader.getWidth(reader.getMinIndex());
							height = reader.getHeight(reader.getMinIndex());
						}
						catch (Exception e)
						{
							logger().warn("Could not obtain the size of the asset: "+f.getName());
							//e.printStackTrace(logger().getPrintWriter());
							//e.printStackTrace();
							continue;
						}
						
						//We want nice powers of two
						if((width & (width - 1)) != 0 || (height & (height - 1)) != 0) {
							logger().warn("Non pow2 texture size ("+width+":"+height+") for: "+f.getName()+", skipping.");
							continue;
						}
						
						//Width >= 16
						if(width < 16 || height < 16) {
							logger().warn("Too small (<16px) texture ("+width+":"+height+") for: "+f.getName()+", skipping.");
							continue;
						}
						
						int frames = height / width;
						
						AtlasElement texture = new AtlasElement(f, type, textureName, width, frames);
						elements.add(texture);
						
						if(textureName.equals("notex")) {
							switch(type) {
							case ALBEDO:
								defaultAlbedo = texture;
								break;
							case NORMAL:
								defaultNormal = texture;
								break;
							case MATERIAL:
								defaultMaterial = texture;
								break;
							}
						}
						
						//System.out.println(textureName + " : " + frames);
						
						//sizeBuckets.put(width, texture);
						//System.out.println("Added: "+texture);

					} else if(f.getName().endsWith(".jpg") || f.getName().endsWith(".tiff") || f.getName().endsWith(".bmp") || f.getName().endsWith(".gif")) {
						logger().warn("Found image file of unsupported format in voxels folder: "+f.getName()+", ignoring.");
						continue;
					}
				}
			}
			
			//Check we DID obtain default textures
			if(defaultAlbedo == null || defaultNormal == null || defaultMaterial == null) {
				logger().error("Missing 'notex.png' for one of the 3 texture types (albedo, normal, material), exiting !");
				System.exit(-602);
			}
			
			//Once that's done, try to fit all everything in the texture units constraints
			int[] sizes = new int[maxTextureArrays];
			int[] counts = new int[maxTextureArrays];
			AtlasElement[][] stuff = new AtlasElement[maxTextureArrays][gl_MaxTextureArraySize];
			int maxSize = 0;
			int maxDownscaled = 0;
			int downscaleTimes = 0;
			while(true) {
				//Check if everything could fit in N - 8 texture units
				for(int i = 0; i < maxTextureArrays; i++) { sizes[i] = 0; counts[i] = 0; }
				
				boolean ko = false;
				for(AtlasElement e : elements) {
					
					if(e.downscaleTo > maxSize)
						maxSize = e.downscaleTo;
					
					boolean foundSpace = false;
					for(int i = 0; i < maxTextureArrays; i++) {
						//Can create a new size
						if(sizes[i] == 0) {
							sizes[i] = e.downscaleTo;
							stuff[i][counts[i]] = e;
							counts[i]+=e.animationFrames;
							foundSpace = true;
							break;
						}
						//Size already exist
						else if(sizes[i] == e.downscaleTo) {
							//Has enough remaining space
							if(gl_MaxTextureArraySize - counts[i] >= e.animationFrames) {
								stuff[i][counts[i]] = e;
								counts[i]+=e.animationFrames;
								foundSpace = true;
								break;
							}
						}
					}
					
					if(foundSpace) {
						continue;
					}
					else {
						//We're out of space, cancel already
						ko = true;
						break;
					}
				}
				
				if(!ko) {
					//Everyone found a place! Go ahead it's cool
					break;
				}
				else {
					//Not enough space to fit everyone :(
					
					if(maxSize == 16) {
						JOptionPane.showMessageDialog(null, "Exceeded vram constraints :(");
					}
					
					//Shrink the biggest texture size and try again
					for(AtlasElement e : elements) {
						if(e.downscaleTo >= maxSize) {
							e.downscaleTo = e.downscaleTo / 2;
						}
					}
					downscaleTimes++;
					maxDownscaled = maxSize / 2;
					maxSize = 0;
				}
			}
			
			System.out.println("Found space OK for every "+elements.size()+" voxel texture, had to downscale "+downscaleTimes+" resolutions down to "+maxDownscaled);

			//Once we secured all the needed textures, let's assemble them in a big map
			for(int i = 0; i < maxTextureArrays; i++) {
				System.out.println("Array "+i+": Size:"+sizes[i] + " Count:"+counts[i]);
				sizes[i] = 0; 
				counts[i] = 0;
				for(int j = 0; j < counts[i]; j++) {
					AtlasElement element = stuff[i][j];
					VoxelTextureArrayed completedTexture = textures.get(element.name);
					if(completedTexture == null) {
						completedTexture = new VoxelTextureArrayed(element.name);
						textures.put(element.name, completedTexture);
					}
					
					switch(element.type) {
					case ALBEDO:
						completedTexture.albedo = element;
						break;
					case NORMAL:
						completedTexture.normal = element;
						break;
					case MATERIAL:
						completedTexture.material = element;
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			logger().error("Exception during loading of voxel textures: "+e.getMessage());
			//e.printStackTrace(logger().getPrintWriter());
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	class AtlasElement {
		Asset asset;
		String name;
		Type type;
		
		int originalSize;
		int animationFrames;

		int downscaleTo;
		
		//Which array and at what index is it at ?
		int array;
		int arrayIndex;
		
		public AtlasElement(Asset asset, Type type, String name, int originalSize, int animationFrames) {
			super();
			this.type = type;
			this.asset = asset;
			this.name = name;
			this.originalSize = originalSize;
			this.animationFrames = animationFrames;
			
			this.downscaleTo = originalSize;
		}
	}
	
	class VoxelTextureArrayed {
		String textureName;
		AtlasElement albedo, normal, material;
		
		VoxelTextureArrayed(String textureName) {
			this.textureName = textureName;
			this.albedo = defaultAlbedo;
			this.normal = defaultNormal;
			this.material = defaultMaterial;
		}
	}
	
	enum Type {
		ALBEDO, NORMAL, MATERIAL;
	}
}
