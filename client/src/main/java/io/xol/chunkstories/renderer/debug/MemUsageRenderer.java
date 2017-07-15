package io.xol.chunkstories.renderer.debug;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture1DGL;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MemUsageRenderer
{
	//Draws a fps graph of frameTime
	static ByteBuffer dataBB = MemoryUtil.memCalloc(4 * 1024);
	
	static Texture1DGL texture = new Texture1DGL(TextureFormat.RED_32F);
	static int lel = 0;
	static long lastTime;
	
	public static void draw(RenderingContext renderingContext)
	{
		lel++;
		lel%=1024;
		long elapsedTime = (System.nanoTime() - lastTime);
		lastTime = System.nanoTime();
		
		float totalMemoryMB = Runtime.getRuntime().totalMemory() / 1024 / 1024;
		float freeMemoryMB = Runtime.getRuntime().freeMemory() / 1024 / 1024;
		
		float usedMemoryMB = totalMemoryMB - freeMemoryMB;
		
		dataBB.putFloat(lel * 4, usedMemoryMB / totalMemoryMB * 192f);
		
		//System.out.println(usedMemoryMB + " / " + totalMemoryMB);
		
		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.MIX);
		renderingContext.setCullingMode(CullingMode.DISABLED);

		ShaderInterface overlayProgram = renderingContext.useShader("fps_graph");
		
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
