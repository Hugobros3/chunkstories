package io.xol.chunkstories.renderer.decals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.joml.Vector4f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.world.VoxelContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Responsible for drawing the bounding box of the voxels */
public class VoxelOverlays {

	VertexBuffer vertexBuffer;

	public void drawSelectionBox(RenderingInterface renderingInterface, Location location)
	{
		if(vertexBuffer == null)
			vertexBuffer = renderingInterface.newVertexBuffer();
		
		int x = (int)(double) location.x();
		int y = (int)(double) location.y();
		int z = (int)(double) location.z();
		
		VoxelContext peek = location.getWorld().peekSafely(x, y, z);
		if(peek.getVoxel() == null)
			return;
		
		CollisionBox[] boxes = peek.getVoxel().getTranslatedCollisionBoxes(location.getWorld(), x, y, z);
		//TODO: getTranslatedCollisionBoxes(voxelContext)
		if(boxes == null)
			return;
		
		renderingInterface.setCullingMode(CullingMode.DISABLED);
		renderingInterface.setBlendMode(BlendMode.MIX);
		renderingInterface.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		
		ShaderInterface overlayProgram = renderingInterface.useShader("overlay");//ShadersLibrary.getShaderProgram("overlay");
		renderingInterface.getCamera().setupShader(overlayProgram);
		overlayProgram.setUniform4f("colorIn", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		
		ByteBuffer data = ByteBuffer.allocateDirect(4 * 3 * 8 * 3 * boxes.length);
		data.order(ByteOrder.nativeOrder());
		//FloatBuffer fb = data.asFloatBuffer();
		//fb.order(ByteOrder.nativeOrder());
		//float[] data = new float[3 * 8 * 6 * boxes.length];
		
		int i = 0;
		for(CollisionBox box : boxes) {
			i = cubeVertices(data, i, (float) box.xpos, (float) box.ypos, (float) box.zpos, (float) box.xw, (float) box.h, (float) box.zw);
		}
		
		data.flip();
		vertexBuffer.uploadData(data);
		
		renderingInterface.bindAttribute("vertexIn", vertexBuffer.asAttributeSource(VertexFormat.FLOAT, 3));
		//renderingContext.bindAttribute("vertexIn", new FloatBufferAttributeSource(data, 3));
		
		renderingInterface.draw(Primitive.LINE, 0, boxes.length * 6 * 8);
		renderingInterface.setBlendMode(BlendMode.DISABLED);
		renderingInterface.flush();
		
		//System.out.println("k");
		/*
		glColor4f(1, 1, 1, 1.0f);
		//GL11.glBlendFunc(GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ZERO);
		//GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		//GL11.glBlendEquation(GL11.);
		glBegin(GL_LINES);
		VoxelContext bri = new VoxelContextOlder(world, x, y, z);
		if (bri.getVoxel() == null)
		{
			System.out.println(bri.getData());
			return;
		}
		for (CollisionBox box : bri.getVoxel().getTranslatedCollisionBoxes(world, x, y, z))
			cubeVertices((float) box.xpos, (float) box.ypos, (float) box.zpos, (float) box.xw, (float) box.h, (float) box.zw);
		glEnd();
		glColor4f(1, 1, 1, 1);*/
	}

	private int cubeVertices(ByteBuffer data, int i, float x, float y, float z, float xw, float h, float zw)
	{
		i += addPoints(data, i, + x, 0 + y, + z);
		i += addPoints(data, i, xw + x, 0 + y, + z);
		i += addPoints(data, i,  + x, 0 + y, zw + z);
		i += addPoints(data, i, xw + x, 0 + y, zw + z);
		i += addPoints(data, i, xw + x, 0 + y, zw + z);
		i += addPoints(data, i, xw + x, 0 + y, + z);
		i += addPoints(data, i,  + x, 0 + y, + z);
		i += addPoints(data, i,  + x, 0 + y, zw + z);

		i += addPoints(data, i,  + x, +h + y, + z);
		i += addPoints(data, i, xw + x, +h + y, + z);
		i += addPoints(data, i,  + x, +h + y, zw + z);
		i += addPoints(data, i, xw + x, +h + y, zw + z);
		i += addPoints(data, i, xw + x, +h + y, zw + z);
		i += addPoints(data, i, xw + x, +h + y, + z);
		i += addPoints(data, i,  + x, +h + y, + z);
		i += addPoints(data, i,  + x, +h + y, zw + z);

		i += addPoints(data, i,  + x, 0 + y, + z);
		i += addPoints(data, i,  + x, +h + y, + z);
		i += addPoints(data, i,  + x, 0 + y, zw + z);
		i += addPoints(data, i,  + x, +h + y, zw + z);
		i += addPoints(data, i, xw + x, 0 + y, + z);
		i += addPoints(data, i, xw + x, +h + y, + z);
		i += addPoints(data, i, xw + x, 0 + y, zw + z);
		i += addPoints(data, i, xw + x, +h + y, zw + z);
		
		return i;
	}

	private int addPoints(ByteBuffer data, int i, float x, float y, float z) {
		/*data[i] = x;
		i++;
		data[i] = y;
		i++;
		data[i] = z;
		i++;*/
		data.putFloat(x);
		data.putFloat(y);
		data.putFloat(z);
		//System.out.println("x:"+x+"y:"+y+"z:"+z);
		i+=3;
		return i;
	}
}
