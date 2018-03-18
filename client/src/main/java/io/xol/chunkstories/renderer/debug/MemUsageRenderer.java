//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.debug;

import java.nio.ByteBuffer;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.renderer.OpenGLRenderingContext;
import io.xol.chunkstories.renderer.opengl.texture.Texture1DGL;

public class MemUsageRenderer
{
	//Draws a fps graph of frameTime
	static ByteBuffer dataBB = MemoryUtil.memCalloc(4 * 1024);
	
	static Texture1DGL texture = new Texture1DGL(TextureFormat.RED_32F);
	static int lel = 0;
	static long lastTime;
	
	public static void draw(OpenGLRenderingContext renderingContext)
	{
		lel++;
		lel%=1024;
		//long elapsedTime = (System.nanoTime() - lastTime);
		lastTime = System.nanoTime();
		
		float totalMemoryMB = Runtime.getRuntime().totalMemory() / 1024 / 1024;
		float freeMemoryMB = Runtime.getRuntime().freeMemory() / 1024 / 1024;
		
		float usedMemoryMB = totalMemoryMB - freeMemoryMB;
		
		dataBB.putFloat(lel * 4, usedMemoryMB / totalMemoryMB * 192f);
		
		//System.out.println(usedMemoryMB + " / " + totalMemoryMB);
		
		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.MIX);
		renderingContext.setCullingMode(CullingMode.DISABLED);

		Shader overlayProgram = renderingContext.useShader("fps_graph");
		
		overlayProgram.setUniform1f("currentTiming", lel);
		overlayProgram.setUniform2f("screenSize", renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());
		
		overlayProgram.setUniform1f("sizeInPixels", 768);
		overlayProgram.setUniform1f("heightInPixels", 192);
		
		overlayProgram.setUniform1f("xPosition", 0);
		overlayProgram.setUniform1f("yPosition", 192 + 192);
		
		overlayProgram.setUniform3f("graphColour", 1, 1, 0);
		
		overlayProgram.setUniform1f("shade", 1.0f);
		
		dataBB.rewind();
		
		texture.uploadTextureData(1024, dataBB);
		texture.setLinearFiltering(false);
		texture.setTextureWrapping(true);
		
		renderingContext.bindTexture1D("frametimeData", texture);
		renderingContext.drawFSQuad();
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().defaultFont(), 4, 192 + 192 + 192 - 30, "Memory used (%"+Runtime.getRuntime().totalMemory()/1024/1024+"Mb)", 2, 2, new Vector4f(1.0f, 1.0f, 0.0f, 1.0f));
	}
}
