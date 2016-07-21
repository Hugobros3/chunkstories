package io.xol.chunkstories.renderer.decals;

import static org.lwjgl.opengl.GL11.*;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.chunkstories.voxel.models.VoxelRenderer;
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
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DecalsRenderer
{
	ByteBuffer decalsByteBuffer = BufferUtils.createByteBuffer(4 * (3 + 2) * (3 * 2) * 4096);
	
	VerticesObject verticesObject = new VerticesObject();
	int kount = 0;
	WorldRenderer worldRenderer;
	World world;

	public DecalsRenderer(WorldRenderer worldRenderer)
	{
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
	}

	public void drawDecal(Vector3d position, Vector3d orientation, Vector3d size, Texture2D texture)
	{
		
		//decalsByteBuffer.clear();
		//decalsByteBuffer.position(0);
		//decalsByteBuffer.limit();
		
		decalsByteBuffer.limit(decalsByteBuffer.capacity());
		
		/*if(decalsByteBuffer.position() > 4 * (3 + 2) * (3 * 2) * 3048)
		{
			System.out.println("full, clearing");
			//decalsByteBuffer.clear();
			decalsByteBuffer.position(0);
			kount = 0;
			//decalsByteBuffer.clear();
		}*/
		
		ByteBuffer bbuf = BufferUtils.createByteBuffer(128 * 1024 * 4 * (3 + 3));
		//ByteBuffer bbuf2 = BufferUtils.createByteBuffer(409600);
		orientation.normalize();

		Vector3f lookAt = orientation.castToSP();
		
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		Vector3f.cross(lookAt, up, up);
		Vector3f.cross(up, lookAt, up);
		
		Matrix4f rotationMatrix = MatrixHelper.getLookAtMatrix(new Vector3f(0.0f), lookAt, up);
		//rotationMatrix.transpose();

		//System.out.println(rotationMatrix);
		//System.out.println(orientation);
		
		//System.out.println("0 0 1  "+Matrix3d.transform(new Vector3d(0, 0, 1), rotationMatrix, null));
		//System.out.println("0 1 0  "+Matrix3d.transform(new Vector3d(0, 1, 0), rotationMatrix, null));
		//System.out.println("1 0 0  "+Matrix3d.transform(new Vector3d(1, 0, 0), rotationMatrix, null));

		VoxelBaker virtualRenderBytebuffer = new DecalsVoxelBaker(bbuf);
		Vector3d size2 = new Vector3d(size);
		size2.scale(1.5);
		size2.add(new Vector3d(0.5));
		//TODO use proper dda ?
		try{
		for (int x = 0; x < size2.x; x++)
			for (int y = 0; y < size2.y; y++)
				for (int z = 0; z < size2.z; z++)
				{
					float dx = (float) (x - size2.x / 2.0);
					float dy = (float) (y - size2.y / 2.0);
					float dz = (float) (z - size2.z / 2.0);

					Vector4f rotateMe = new Vector4f(dx, dy, dz, 1.0f);
					//Matrix4f.transform(rotationMatrix, rotateMe, rotateMe);
					
					//System.out.println(rotateMe);
					//Matrix3d.transform(rotationMatrix, rotateMe, rotateMe);

					Location location = new Location(world, position);
					location.add(new Vector3d(rotateMe.x, rotateMe.y, rotateMe.z));
					location.add(new Vector3d(0.5));

					int idThere = VoxelFormat.id(world.getVoxelData(location));

					Voxel voxel = VoxelTypes.get(idThere);
					if (voxel != null && idThere > 0 && !voxel.isVoxelLiquid())
					{
						BlockRenderInfo bri = new BlockRenderInfo(location);
						VoxelRenderer model = voxel.getVoxelModel(bri);

						if (model == null)
							model = VoxelModels.getVoxelModel("default");

						model.renderInto(virtualRenderBytebuffer, bri, world.getChunkWorldCoordinates(location, false), (int) location.getX(), (int) location.getY(), (int) location.getZ());
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
			
			System.out.println("uploaded "+ actualCount + " vertices ("+kount+") total");
		}
	}

	public void renderDecals(RenderingContext renderingContext)
	{
		ShaderProgram decalsShader = ShadersLibrary.getShaderProgram("decals");

		renderingContext.setCurrentShader(decalsShader);
		renderingContext.getCamera().setupShader(decalsShader);
		
		renderingContext.enableVertexAttribute(decalsShader.getVertexAttributeLocation("vertexIn"));
		renderingContext.enableVertexAttribute(decalsShader.getVertexAttributeLocation("texCoordIn"));
		
		Texture2D diffuseTexture = TexturesHandler.getTexture("decals/bullethole.png");
		diffuseTexture.setTextureWrapping(false);
		diffuseTexture.setLinearFiltering(false);
		
		decalsShader.setUniformSampler(0, "diffuseTexture", diffuseTexture);
		decalsShader.setUniformSampler(1, "zBuffer", worldRenderer.zBuffer);
		decalsShader.setUniformFloat("time", (world.getTime() % 10000) / 10000f);

		decalsShader.setUniformFloat("overcastFactor", world.getWeather());

		glDisable(GL_CULL_FACE);
		//glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );
		glEnable(GL_BLEND);

		glDepthFunc(GL_LEQUAL);

		verticesObject.bind();
		renderingContext.setVertexAttributePointer("vertexIn", 3, GL_FLOAT, false, 4 * (3 + 2), 0);
		renderingContext.setVertexAttributePointer("texCoordIn", 2, GL_FLOAT, false, 4 * (3 + 2), 4 * 3);

		glDepthMask(false);
		
		verticesObject.drawElementsTriangles(Math.min(kount, decalsByteBuffer.capacity() / (4 * (3 + 2))));
		glDisable(GL_BLEND);
		glDepthMask(true);
		glDepthFunc(GL_LEQUAL);

		//System.out.println(renderingContext.getCamera().modelViewMatrix4f);
		
		glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );

		renderingContext.disableVertexAttribute(decalsShader.getVertexAttributeLocation("vertexIn"));
		renderingContext.disableVertexAttribute(decalsShader.getVertexAttributeLocation("texCoordIn"));
	}
}
