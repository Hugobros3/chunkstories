package io.xol.chunkstories.renderer.decals;

import static org.lwjgl.opengl.GL11.*;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.rendering.DecalsManager;
import io.xol.chunkstories.api.rendering.ShaderInterface;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.RenderingInterface.Primitive;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.chunkstories.voxel.models.VoxelRenderer;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.MatrixHelper;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsRenderer implements DecalsManager
{
	int MAX_CONCURRENT_DECAL_TYPES = 16;
	int DECALS_BUFFER_SIZE = 4 * (3 + 2) * (3 * 2) * 4096;
	Map<Texture2D, DecalType> decalsTypes = new HashMap<Texture2D, DecalType>();
	
	WorldRenderer worldRenderer;
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
		
		void addDecal(Vector3d position, Vector3d orientation, Vector3d size)
		{
			decalsByteBuffer.limit(decalsByteBuffer.capacity());
			
			ByteBuffer bbuf = BufferUtils.createByteBuffer(128 * 1024 * 4 * (3 + 3));
			
			orientation.normalize();

			Vector3f lookAt = orientation.castToSimplePrecision();
			
			Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
			Vector3f.cross(lookAt, up, up);
			Vector3f.cross(up, lookAt, up);
			
			Matrix4f rotationMatrix = MatrixHelper.getLookAtMatrix(new Vector3f(0.0f), lookAt, up);
			
			VoxelBaker virtualRenderBytebuffer = new DecalsVoxelBaker(bbuf);
			Vector3d size2 = new Vector3d(size);
			size2.scale(1.5);
			size2.add(new Vector3d(0.5));
			//TODO use proper dda ?
			try{
			for (int x = 0; x < size2.getX(); x++)
				for (int y = 0; y < size2.getY(); y++)
					for (int z = 0; z < size2.getZ(); z++)
					{
						float dx = (float) (x - size2.getX() / 2.0);
						float dy = (float) (y - size2.getY() / 2.0);
						float dz = (float) (z - size2.getZ() / 2.0);

						Vector4f rotateMe = new Vector4f(dx, dy, dz, 1.0f);
						//Matrix4f.transform(rotationMatrix, rotateMe, rotateMe);

						Location location = new Location(world, position);
						location.add(new Vector3d(rotateMe.x, rotateMe.y, rotateMe.z));
						location.add(new Vector3d(0.5));

						int idThere = VoxelFormat.id(world.getVoxelData(location));

						Voxel voxel = Voxels.get(idThere);
						if (voxel != null && idThere > 0 && !voxel.isVoxelLiquid() && voxel.isVoxelSolid())
						{
							VoxelContext bri = new VoxelContext(location);
							VoxelRenderer model = voxel.getVoxelRenderer(bri);

							if (model == null)
								model = VoxelModels.getVoxelModel("default");

							model.renderInto(virtualRenderBytebuffer, bri, world.getChunkWorldCoordinates(location), (int) location.getX(), (int) location.getY(), (int) location.getZ());
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
	
	public DecalsRenderer(WorldRenderer worldRenderer)
	{
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
	}
	
	public void drawDecal(Vector3d position, Vector3d orientation, Vector3d size, String decalName)
	{
		Texture2D texture = TexturesHandler.getTexture("./textures/decals/"+decalName+".png");
		drawDecal(position, orientation, size, texture);
	}

	public void drawDecal(Vector3d position, Vector3d orientation, Vector3d size, Texture2D texture)
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

	public void renderDecals(RenderingContext renderingContext)
	{
		ShaderInterface decalsShader = renderingContext.useShader("decals");

		renderingContext.getCamera().setupShader(decalsShader);
		
		renderingContext.bindTexture2D("zBuffer", worldRenderer.zBuffer);
		//decalsShader.setUniformSampler(1, "zBuffer", worldRenderer.zBuffer);

		renderingContext.setCullingMode(CullingMode.DISABLED);
		//glDisable(GL_CULL_FACE);
		
		//glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );
		
		renderingContext.setBlendMode(BlendMode.MIX);
		//glEnable(GL_BLEND);
		
		//glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		//glDepthFunc(GL_LEQUAL);

		for(DecalType decalType : decalsTypes.values())
		{
			Texture2D diffuseTexture = decalType.getTexture();
			diffuseTexture.bind();
			diffuseTexture.setTextureWrapping(false);
			diffuseTexture.setLinearFiltering(false);
			
			renderingContext.bindAlbedoTexture(diffuseTexture);
			//decalsShader.setUniformSampler(0, "diffuseTexture", diffuseTexture);
			
			//decalType.verticesObject.bind();
			renderingContext.bindAttribute("vertexIn", decalType.verticesObject.asAttributeSource(VertexFormat.FLOAT, 3, 4 * (3 + 2), 0));
			renderingContext.bindAttribute("texCoordIn", decalType.verticesObject.asAttributeSource(VertexFormat.FLOAT, 2, 4 * (3 + 2), 4 * 3));
			//renderingContext.setVertexAttributePointerLocation("vertexIn", 3, GL_FLOAT, false, 4 * (3 + 2), 0);
			//renderingContext.setVertexAttributePointerLocation("texCoordIn", 2, GL_FLOAT, false, 4 * (3 + 2), 4 * 3);
			
			renderingContext.draw(Primitive.TRIANGLE, 0, decalType.kount);
			//decalType.verticesObject.drawElementsTriangles(decalType.kount);
		}

		//glEnable(GL_DEPTH_TEST);
		glDepthMask(true);
		
		//glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );
	}
}
