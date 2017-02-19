package io.xol.chunkstories.renderer.decals;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.operations.VectorCrossProduct;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.renderer.VoxelContextOlder;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.renderer.WorldRendererOld;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.voxel.models.VoxelModelsStore;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.MatrixHelper;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsRenderer implements DecalsManager
{
	int MAX_CONCURRENT_DECAL_TYPES = 16;
	int DECALS_BUFFER_SIZE = 4 * (3 + 2) * (3 * 2) * 4096;
	Map<Texture2D, DecalType> decalsTypes = new HashMap<Texture2D, DecalType>();
	
	WorldRendererImplementation worldRenderer;
	World world;

	class DecalType {
		
		Texture2D texture;
		
		DecalType(Texture2D texture)
		{
			this.texture = texture;
		}
		
		ByteBuffer decalsByteBuffer = BufferUtils.createByteBuffer(DECALS_BUFFER_SIZE);
		VerticesObject verticesObject = new VerticesObject();
		int kount = 0;
		long lastDecalAdded;
		
		Texture2D getTexture()
		{
			return texture;
		}
		
		void addDecal(Vector3dm position, Vector3dm orientation, Vector3dm size)
		{
			decalsByteBuffer.limit(decalsByteBuffer.capacity());
			
			ByteBuffer bbuf = BufferUtils.createByteBuffer(128 * 1024 * 4 * (3 + 3));
			
			orientation.normalize();

			Vector3<Float> lookAt = orientation.castToSinglePrecision();
			
			Vector3fm up = new Vector3fm(0.0f, 1.0f, 0.0f);
			VectorCrossProduct.cross33(lookAt, up, up);
			VectorCrossProduct.cross33(up, lookAt, up);
			
			Matrix4f rotationMatrix = MatrixHelper.getLookAtMatrix(new Vector3fm(0.0f), lookAt, up);
			
			VoxelBakerHighPoly virtualRenderBytebuffer = new DecalsVoxelBaker(bbuf);
			Vector3dm size2 = new Vector3dm(size);
			size2.scale(1.5);
			size2.add(new Vector3dm(0.5));
			//TODO use proper dda ?
			try{
			for (int x = 0; x < size2.getX(); x++)
				for (int y = 0; y < size2.getY(); y++)
					for (int z = 0; z < size2.getZ(); z++)
					{
						float dx = (float) (x - size2.getX() / 2.0);
						float dy = (float) (y - size2.getY() / 2.0);
						float dz = (float) (z - size2.getZ() / 2.0);

						Vector4fm rotateMe = new Vector4fm(dx, dy, dz, 1.0f);
						//Matrix4f.transform(rotationMatrix, rotateMe, rotateMe);

						Location location = new Location(world, position);
						location.add(new Vector3dm(rotateMe.getX(), rotateMe.getY(), rotateMe.getZ()));
						location.add(new Vector3dm(0.5));

						int idThere = VoxelFormat.id(world.getVoxelData(location));

						Voxel voxel = VoxelsStore.get().getVoxelById(idThere);
						if (voxel != null && idThere > 0 && !voxel.getType().isLiquid() && voxel.getType().isSolid())
						{
							VoxelContext bri = new VoxelContextOlder(location);
							VoxelRenderer model = voxel.getVoxelRenderer(bri);

							if (model == null)
								model = voxel.store().models().getVoxelModelByName("default");

							//TODO
							System.out.println("FIXME LATER REEEE");
							//model.renderInto(virtualRenderBytebuffer, bri, world.getChunkWorldCoordinates(location), (int)(double) location.getX(), (int)(double) location.getY(), (int)(double) location.getZ());
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
	
	public DecalsRenderer(WorldRendererImplementation worldRenderer)
	{
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
	}
	
	public void drawDecal(Vector3dm position, Vector3dm orientation, Vector3dm size, String decalName)
	{
		Texture2D texture = TexturesHandler.getTexture("./textures/decals/"+decalName+".png");
		drawDecal(position, orientation, size, texture);
	}

	public void drawDecal(Vector3dm position, Vector3dm orientation, Vector3dm size, Texture2D texture)
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
			
			renderingInterface.draw(Primitive.TRIANGLE, 0, decalType.kount);
			
			renderingInterface.flush();
			//decalType.verticesObject.drawElementsTriangles(decalType.kount);
		}

		renderingInterface.getRenderTargetManager().setDepthMask(true);
		//glDepthMask(true);
	}
}
