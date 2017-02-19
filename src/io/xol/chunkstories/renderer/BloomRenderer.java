package io.xol.chunkstories.renderer;

import java.nio.ByteBuffer;

import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.util.PBOPacker;

public class BloomRenderer
{
	private final WorldRendererImplementation worldRenderer;

	private PBOPacker illuminationDownloader;
	private PBOPacker.PBOPackerResult illuminationDownloadInProgress = null;
	float apertureModifier = 1f;

	private int frameLumaDownloadDelay = 0;
	private final static int FRAME_DELAY_FOR_LUMA_MIP_GRAB = 2;
	private Vector3fm averageColorForAllFrame = new Vector3fm(1.0);
	private int downloadSize;
	
	public BloomRenderer(WorldRendererImplementation worldRenderer)
	{
		this.worldRenderer = worldRenderer;
		
		illuminationDownloader = new PBOPacker();
	}
	
	public Texture2D renderBloom(RenderingInterface renderingContext) {
		this.computeAverageLuma();
		this.renderAndBlurBloom(renderingContext);
		
		return worldRenderer.renderBuffers.bloomBuffer;
	}
	
	private void computeAverageLuma() {
		worldRenderer.renderBuffers.shadedBuffer.setMipMapping(true);

		int maxMipLevel = worldRenderer.renderBuffers.shadedBuffer.getMaxMipmapLevel();
		worldRenderer.renderBuffers.shadedBuffer.setMipmapLevelsRange(0, maxMipLevel);

		int divisor = 1 << maxMipLevel;
		int mipWidth = worldRenderer.renderBuffers.shadedBuffer.getWidth() / divisor;
		int mipHeight = worldRenderer.renderBuffers.shadedBuffer.getHeight() / divisor;

		if (illuminationDownloadInProgress == null)
		{
			//Start mipmap calculation for next frame
			worldRenderer.renderBuffers.shadedBuffer.computeMipmaps();
			frameLumaDownloadDelay = FRAME_DELAY_FOR_LUMA_MIP_GRAB + 6;

			//To avoid crashing when the windows size change, we send the PBO copy request immediately and instead wait to look for it
			this.illuminationDownloadInProgress = this.illuminationDownloader.copyTexure(worldRenderer.renderBuffers.shadedBuffer, maxMipLevel);

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
				averageColorForAllFrame = new Vector3fm(0.0);
				for (int i = 0; i < downloadSize; i++)
					averageColorForAllFrame.add(minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat());

				averageColorForAllFrame.scale(1.0f / downloadSize);
				//System.out.println(averageColorForAllFrame);

				//Throw that out
				illuminationDownloadInProgress = null;
			}
		}

		//Do continous luma adapation
		float luma = averageColorForAllFrame.getX() * 0.2125f + averageColorForAllFrame.getY() * 0.7154f + averageColorForAllFrame.getZ() * 0.0721f;

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
		worldRenderer.renderBuffers.shadedBuffer.setMipmapLevelsRange(0, 0);
	}
	
	private void renderAndBlurBloom(RenderingInterface renderingContext)
	{
		worldRenderer.renderBuffers.shadedBuffer.setLinearFiltering(true);
		worldRenderer.renderBuffers.bloomBuffer.setLinearFiltering(true);
		worldRenderer.renderBuffers.blurIntermediateBuffer.setLinearFiltering(true);

		ShaderInterface bloomShader = renderingContext.useShader("bloom");

		renderingContext.bindTexture2D("shadedBuffer", worldRenderer.renderBuffers.shadedBuffer);
		bloomShader.setUniform1f("apertureModifier", apertureModifier);
		bloomShader.setUniform2f("screenSize", renderingContext.getWindow().getWidth() / 2f, renderingContext.getWindow().getHeight() / 2f);

		//int max_mipmap = (int) (Math.ceil(Math.log(Math.max(scrH, scrW)) / Math.log(2)));
		//bloomShader.setUniform1f("max_mipmap", max_mipmap);

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(worldRenderer.renderBuffers.fboBloom);
		//this.fboBloom.bind();
		worldRenderer.renderBuffers.fboBloom.setEnabledRenderTargets();
		renderingContext.drawFSQuad();

		// Blur bloom
		// Vertical pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(worldRenderer.renderBuffers.fboBlur);
		//fboBlur.bind();

		ShaderInterface blurV = renderingContext.useShader("blurV");
		blurV.setUniform2f("screenSize", renderingContext.getWindow().getWidth() / 2f, renderingContext.getWindow().getHeight() / 2f);
		blurV.setUniform1f("lookupScale", 1);
		renderingContext.bindTexture2D("inputTexture", worldRenderer.renderBuffers.bloomBuffer);
		renderingContext.drawFSQuad();

		// Horizontal pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(worldRenderer.renderBuffers.fboBloom);
		//this.fboBloom.bind();

		ShaderInterface blurH = renderingContext.useShader("blurH");
		blurH.setUniform2f("screenSize", renderingContext.getWindow().getWidth() / 2f, renderingContext.getWindow().getHeight() / 2f);
		renderingContext.bindTexture2D("inputTexture", worldRenderer.renderBuffers.blurIntermediateBuffer);
		renderingContext.drawFSQuad();

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(worldRenderer.renderBuffers.fboBlur);
		//fboBlur.bind();

		blurV = renderingContext.useShader("blurV");

		blurV.setUniform2f("screenSize", renderingContext.getWindow().getWidth() / 4f, renderingContext.getWindow().getHeight() / 4f);
		blurV.setUniform1f("lookupScale", 1);
		renderingContext.bindTexture2D("inputTexture", worldRenderer.renderBuffers.bloomBuffer);
		renderingContext.drawFSQuad();

		// Horizontal pass
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(worldRenderer.renderBuffers.fboBloom);

		blurH = renderingContext.useShader("blurH");
		blurH.setUniform2f("screenSize", renderingContext.getWindow().getWidth() / 4f, renderingContext.getWindow().getHeight() / 4f);
		renderingContext.bindTexture2D("inputTexture", worldRenderer.renderBuffers.blurIntermediateBuffer);
		renderingContext.drawFSQuad();
		
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(worldRenderer.renderBuffers.fboShadedBuffer);
	}
}
