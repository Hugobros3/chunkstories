//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import java.nio.ByteBuffer;

import org.joml.Vector3f;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.engine.graphics.textures.Texture2DGL;
import io.xol.engine.graphics.util.PBOPacker;

public class BloomRenderer
{
	private final WorldRendererImplementation worldRenderer;

	private PBOPacker illuminationDownloader;
	private PBOPacker.PBOPackerResult illuminationDownloadInProgress = null;
	float apertureModifier = 1f;

	private int frameLumaDownloadDelay = 0;
	private final static int FRAME_DELAY_FOR_LUMA_MIP_GRAB = 2;
	private Vector3f averageColorForAllFrame = new Vector3f(1.0f);
	private int downloadSize;
	
	public BloomRenderer(WorldRendererImplementation worldRenderer)
	{
		this.worldRenderer = worldRenderer;
		
		illuminationDownloader = new PBOPacker();
	}
	
	public Texture2DGL renderBloom(RenderingInterface renderingContext) {
		this.computeAverageLuma();
		this.renderAndBlurBloom(renderingContext);
		
		return worldRenderer.renderBuffers.rbBloom;
	}
	
	private void computeAverageLuma() {
		worldRenderer.renderBuffers.rbShaded.setMipMapping(true);

		int maxMipLevel = worldRenderer.renderBuffers.rbShaded.getMaxMipmapLevel();
		worldRenderer.renderBuffers.rbShaded.setMipmapLevelsRange(0, maxMipLevel);

		int divisor = 1 << maxMipLevel;
		int mipWidth = worldRenderer.renderBuffers.rbShaded.getWidth() / divisor;
		int mipHeight = worldRenderer.renderBuffers.rbShaded.getHeight() / divisor;

		if (illuminationDownloadInProgress == null)
		{
			//Start mipmap calculation for next frame
			worldRenderer.renderBuffers.rbShaded.computeMipmaps();
			frameLumaDownloadDelay = FRAME_DELAY_FOR_LUMA_MIP_GRAB + 6;

			//To avoid crashing when the windows size change, we send the PBO copy request immediately and instead wait to look for it
			this.illuminationDownloadInProgress = this.illuminationDownloader.copyTexure(worldRenderer.renderBuffers.rbShaded, maxMipLevel);

			//We remember the size of the data to expect since that may change due to windows resizes and whatnot
			downloadSize = mipWidth * mipHeight;
		}

		if (illuminationDownloadInProgress != null && illuminationDownloadInProgress.isTraversable())
		{
			//Wait a few frames before actually reading the PBO
			if (frameLumaDownloadDelay > 0)
				frameLumaDownloadDelay--;
			else
			{

				ByteBuffer minMipmapBuffer = illuminationDownloadInProgress.readPBO();
				minMipmapBuffer.flip();

				//System.out.println("Obtained: "+minMipmapBuffer);
				averageColorForAllFrame = new Vector3f(0.0f);
				for (int i = 0; i < downloadSize; i++)
					averageColorForAllFrame.add(minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat());

				averageColorForAllFrame.mul(1.0f / downloadSize);
				//System.out.println(averageColorForAllFrame);

				//Throw that out
				illuminationDownloadInProgress = null;
			}
		}

		//Do continous luma adapation
		float luma = averageColorForAllFrame.x() * 0.2125f + averageColorForAllFrame.y() * 0.7154f + averageColorForAllFrame.z() * 0.0721f;

		luma *= apertureModifier;
		luma = (float) Math.pow(luma, 1d / 2.2);

		float targetLuma = 0.65f;
		float lumaMargin = 0.15f;

		if (luma < targetLuma - lumaMargin)
		{
			if (apertureModifier < 2.0)
				apertureModifier *= 1.001;
		}
		else if (luma > targetLuma + lumaMargin)
		{
			if (apertureModifier > 0.99)
				apertureModifier *= 0.999;
		}
		else
		{
			float clamped = (float) Math.min(Math.max(1 / apertureModifier, 0.998), 1.002);
			apertureModifier *= clamped;
		}
		worldRenderer.renderBuffers.rbShaded.setMipmapLevelsRange(0, 0);
	}
	
	private void renderAndBlurBloom(RenderingInterface renderer)
	{
		worldRenderer.renderBuffers.rbShaded.setLinearFiltering(true);
		worldRenderer.renderBuffers.rbBloom.setLinearFiltering(true);
		worldRenderer.renderBuffers.rbBlurTemp.setLinearFiltering(true);

		Shader bloomShader = renderer.useShader("bloom");

		renderer.bindTexture2D("shadedBuffer", worldRenderer.renderBuffers.rbShaded);
		bloomShader.setUniform1f("apertureModifier", apertureModifier);
		bloomShader.setUniform2f("screenSize", renderer.getWindow().getWidth() / 2f, renderer.getWindow().getHeight() / 2f);

		//int max_mipmap = (int) (Math.ceil(Math.log(Math.max(scrH, scrW)) / Math.log(2)));
		//bloomShader.setUniform1f("max_mipmap", max_mipmap);

		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboBloom);
		//this.fboBloom.bind();
		worldRenderer.renderBuffers.fboBloom.setEnabledRenderTargets();
		renderer.drawFSQuad();

		// Blur bloom
		// Vertical pass
		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboBlur);
		//fboBlur.bind();

		Shader blurV = renderer.useShader("blurV");
		blurV.setUniform2f("screenSize", renderer.getWindow().getWidth() / 2f, renderer.getWindow().getHeight() / 2f);
		blurV.setUniform1f("lookupScale", 1);
		renderer.bindTexture2D("inputTexture", worldRenderer.renderBuffers.rbBloom);
		renderer.drawFSQuad();

		// Horizontal pass
		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboBloom);
		//this.fboBloom.bind();

		Shader blurH = renderer.useShader("blurH");
		blurH.setUniform2f("screenSize", renderer.getWindow().getWidth() / 2f, renderer.getWindow().getHeight() / 2f);
		renderer.bindTexture2D("inputTexture", worldRenderer.renderBuffers.rbBlurTemp);
		renderer.drawFSQuad();

		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboBlur);
		//fboBlur.bind();

		blurV = renderer.useShader("blurV");

		blurV.setUniform2f("screenSize", renderer.getWindow().getWidth() / 4f, renderer.getWindow().getHeight() / 4f);
		blurV.setUniform1f("lookupScale", 1);
		renderer.bindTexture2D("inputTexture", worldRenderer.renderBuffers.rbBloom);
		renderer.drawFSQuad();

		// Horizontal pass
		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboBloom);

		blurH = renderer.useShader("blurH");
		blurH.setUniform2f("screenSize", renderer.getWindow().getWidth() / 4f, renderer.getWindow().getHeight() / 4f);
		renderer.bindTexture2D("inputTexture", worldRenderer.renderBuffers.rbBlurTemp);
		renderer.drawFSQuad();
		
		renderer.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboShadedBuffer);
	}
}
