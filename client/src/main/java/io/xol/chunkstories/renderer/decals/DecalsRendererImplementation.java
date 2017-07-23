package io.xol.chunkstories.renderer.decals;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.Location;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.effects.DecalsRenderer;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerCubic;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.voxel.VoxelContextOlder;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.textures.Texture2DGL;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsRendererImplementation implements DecalsRenderer
{
	int MAX_CONCURRENT_DECAL_TYPES = 16;
	int DECALS_BUFFER_SIZE = 4 * (3 + 2) * (3 * 2) * 4096;
	Map<Texture2DGL, DecalType> decalsTypes = new HashMap<Texture2DGL, DecalType>();
	
	WorldRendererImplementation worldRenderer;
	World world;

	class DecalType {
		
		Texture2D texture;
		
		DecalType(Texture2D texture)
		{
			this.texture = texture;
		}
		
		ByteBuffer decalsByteBuffer = BufferUtils.createByteBuffer(DECALS_BUFFER_SIZE);
		VertexBuffer verticesObject = new VertexBufferGL();
		int kount = 0;
		long lastDecalAdded;
		
		Texture2D getTexture()
		{
			return texture;
		}
		
		void addDecal(Vector3dc position, Vector3dc orientation, Vector3dc size)
		{
			decalsByteBuffer.limit(decalsByteBuffer.capacity());
			
			ByteBuffer bbuf = BufferUtils.createByteBuffer(128 * 1024 * 4 * (3 + 3));
			
			//orientation.normalize();

			Vector3f lookAt = new Vector3f((float)orientation.x(), (float)orientation.y(), (float)orientation.z());
			lookAt.normalize();
			//Vector3<Float> lookAt = orientation.castToSinglePrecision();
			
			Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
			
			//Fail-over when looking straight down
			if(lookAt.x == 0.0 && (lookAt.y == 1.0 || lookAt.y == -1.0) && lookAt.z == 0) {
				up.y = 0.0f;
				up.x = 1.0f;
			}
			
			lookAt.cross(up, up);
			//VectorCrossProduct.cross33(lookAt, up, up);
			
			up.cross(lookAt);
			//VectorCrossProduct.cross33(up, lookAt, up);
			
			Matrix4f rotationMatrix = new Matrix4f();
			rotationMatrix.setLookAt(new Vector3f(0.0f), lookAt, up);
			//MatrixHelper.getLookAtMatrix(new Vector3f(0.0f), lookAt, up);
			
			DecalsVoxelBaker virtualRenderBytebuffer = new DecalsVoxelBaker(bbuf);
			Vector3d size2 = new Vector3d(size);
			size2.mul(1.5);
			size2.add(new Vector3d(0.5));
			//TODO use proper dda ?
			try{
			for (int x = 0; x < size2.x(); x++)
				for (int y = 0; y < size2.y(); y++)
					for (int z = 0; z < size2.z(); z++)
					{
						float dx = (float) (x - size2.x() / 2.0);
						float dy = (float) (y - size2.y() / 2.0);
						float dz = (float) (z - size2.z() / 2.0);

						Vector4f rotateMe = new Vector4f(dx, dy, dz, 1.0f);
						//Matrix4f.transform(rotationMatrix, rotateMe, rotateMe);

						Location location = new Location(world, position);
						location.add(new Vector3d(rotateMe.x(), rotateMe.y(), rotateMe.z()));
						location.add(new Vector3d(0.5));

						int idThere = VoxelFormat.id(world.getVoxelData(location));

						Voxel voxel = VoxelsStore.get().getVoxelById(idThere);
						if (voxel != null && idThere > 0 && !voxel.getType().isLiquid() && voxel.getType().isSolid())
						{
							VoxelContext bri = new VoxelContextOlder(location);
							VoxelRenderer model = voxel.getVoxelRenderer(bri);

							if (model == null)
								model = voxel.store().models().getVoxelModelByName("default");

							ChunkRenderer chunkRenderer = new ChunkRenderer() {

								@Override
								public VoxelBakerHighPoly getHighpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
								{
									return virtualRenderBytebuffer;
								}

								@Override
								public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
								{
									return virtualRenderBytebuffer;
								}
								
							};
							
							ChunkRenderer.ChunkRenderContext o2 = new ChunkRenderer.ChunkRenderContext()
							{
								
								private VoxelLighter voxeLighter = new VoxelLighter() {

									@Override
									public byte getSunlightLevelForCorner(Corners corner)
									{
										return 0;
									}

									@Override
									public byte getBlocklightLevelForCorner(Corners corner)
									{
										return 0;
									}

									@Override
									public byte getAoLevelForCorner(Corners corner)
									{
										return 0;
									}

									@Override
									public byte getSunlightLevelInterpolated(float vertX, float vertY, float vertZ)
									{
										return 0;
									}

									@Override
									public byte getBlocklightLevelInterpolated(float vertX, float vertY, float vertZ)
									{
										return 0;
									}

									@Override
									public byte getAoLevelInterpolated(float vertX, float vertY, float vertZ)
									{
										return 0;
									}
									
								};

								@Override
								public boolean isTopChunkLoaded()
								{
									return false;
								}
								
								@Override
								public boolean isRightChunkLoaded()
								{
									return false;
								}
								
								@Override
								public boolean isLeftChunkLoaded()
								{
									return false;
								}
								
								@Override
								public boolean isFrontChunkLoaded()
								{
									return false;
								}
								
								@Override
								public boolean isBottomChunkLoaded()
								{
									return false;
								}
								
								@Override
								public boolean isBackChunkLoaded()
								{
									return false;
								}

								@Override
								public int getRenderedVoxelPositionInChunkX()
								{
									return bri.getX() & 0x1f;
								}

								@Override
								public int getRenderedVoxelPositionInChunkY()
								{
									return bri.getY() & 0x1f;
								}

								@Override
								public int getRenderedVoxelPositionInChunkZ()
								{
									return bri.getZ() & 0x1f;
								}

								@Override
								public VoxelLighter getCurrentVoxelLighter()
								{
									return voxeLighter;
								}
							};

							Chunk chunk = world.getChunkWorldCoordinates(location);
							virtualRenderBytebuffer.setChunk(chunk);
							
							model.renderInto(chunkRenderer, o2, chunk, world.peek(location));
						}

					}

			}
			catch(BufferOverflowException e)
			{
				
			}
			
			int c = bbuf.position() / 2;
			if (c > 0.0)
			{
				bbuf.flip();
				
				int actualCount = 0;
				
				actualCount += TrianglesClipper.clipTriangles(bbuf, decalsByteBuffer, rotationMatrix, position, lookAt, size);
				
				int p = decalsByteBuffer.position();
				decalsByteBuffer.rewind();

				verticesObject.uploadData(decalsByteBuffer.asReadOnlyBuffer());
				
				kount += actualCount;
				
				//Put byteBuffer2 
				decalsByteBuffer.position(p);
				
				//bbuf.clear();
				if(kount > decalsByteBuffer.capacity() / (4 * (3 + 2)))
					kount = decalsByteBuffer.capacity() / (4 * (3 + 2));
			}
		}
	}
	
	public DecalsRendererImplementation(WorldRendererImplementation worldRenderer)
	{
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
	}
	
	public void drawDecal(Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName)
	{
		Texture2DGL texture = TexturesHandler.getTexture("./textures/decals/"+decalName+".png");
		drawDecal(position, orientation, size, texture);
	}

	public void drawDecal(Vector3dc position, Vector3dc orientation, Vector3dc size, Texture2DGL texture)
	{
		if(texture == null)
			return;
		
		DecalType decalType = decalsTypes.get(texture);
		//Create the decal type if missing
		if(decalType == null)
		{
			if(decalsTypes.size() < MAX_CONCURRENT_DECAL_TYPES)
			{
				decalType = new DecalType(texture);
				decalsTypes.put(texture, decalType);
			}
			else
				return;
		}
		//Do the work there
		decalType.addDecal(position, orientation, size);
	}

	public void renderDecals(RenderingInterface renderingInterface)
	{
		ShaderInterface decalsShader = renderingInterface.useShader("decals");

		renderingInterface.getCamera().setupShader(decalsShader);
		
		renderingInterface.bindTexture2D("zBuffer", worldRenderer.renderBuffers.zBuffer);
		//decalsShader.setUniformSampler(1, "zBuffer", worldRenderer.zBuffer);

		renderingInterface.setCullingMode(CullingMode.DISABLED);
		//glDisable(GL_CULL_FACE);
		
		//glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );
		
		renderingInterface.setBlendMode(BlendMode.MIX);
		//glEnable(GL_BLEND);
		
		//glDisable(GL_DEPTH_TEST);
		renderingInterface.getRenderTargetManager().setDepthMask(false);
		//glDepthMask(false);
		//glDepthFunc(GL_LEQUAL);

		for(DecalType decalType : decalsTypes.values())
		{
			Texture2D diffuseTexture = decalType.getTexture();
			//diffuseTexture.bind();
			diffuseTexture.setTextureWrapping(false);
			diffuseTexture.setLinearFiltering(false);
			
			renderingInterface.bindAlbedoTexture(diffuseTexture);
			//decalsShader.setUniformSampler(0, "diffuseTexture", diffuseTexture);
			
			if(!decalType.verticesObject.isDataPresent())
				continue;
			
			//decalType.verticesObject.bind();
			renderingInterface.bindAttribute("vertexIn", decalType.verticesObject.asAttributeSource(VertexFormat.FLOAT, 3, 4 * (3 + 2), 0));
			renderingInterface.bindAttribute("texCoordIn", decalType.verticesObject.asAttributeSource(VertexFormat.FLOAT, 2, 4 * (3 + 2), 4 * 3));
			//renderingContext.setVertexAttributePointerLocation("vertexIn", 3, GL_FLOAT, false, 4 * (3 + 2), 0);
			//renderingContext.setVertexAttributePointerLocation("texCoordIn", 2, GL_FLOAT, false, 4 * (3 + 2), 4 * 3);
			
			//System.out.println("wtf son");
			
			renderingInterface.draw(Primitive.TRIANGLE, 0, decalType.kount);
			//System.out.println(decalType.kount);
			
			renderingInterface.flush();
			//decalType.verticesObject.drawElementsTriangles(decalType.kount);
		}

		renderingInterface.getRenderTargetManager().setDepthMask(true);
		//glDepthMask(true);
	}
}
