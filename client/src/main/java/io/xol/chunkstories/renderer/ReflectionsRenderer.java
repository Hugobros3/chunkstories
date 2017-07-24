package io.xol.chunkstories.renderer;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;

public class ReflectionsRenderer {

	private final WorldRendererImplementation worldRenderer;

	public ReflectionsRenderer(WorldRendererImplementation worldRenderer) {
		super();
		this.worldRenderer = worldRenderer;
	}

	/** Only ran if realtime screen-space reflections are enabled, otherwise a flag is raised and a baked envmap is used. */
	public void renderRealtimeScreenSpaceReflections(RenderingInterface renderingContext) {
		ShaderInterface reflectionsShader = renderingContext.useShader("reflections");

		//This isn't a depth-buffered pass.
		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);

		renderingContext.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboSSR);
		renderingContext.getRenderTargetManager().clearBoundRenderTargetAll();

		//Required to execute SSR
		renderingContext.bindTexture2D("shadedBuffer", worldRenderer.renderBuffers.shadedBuffer);
		renderingContext.bindTexture2D("depthBuffer", worldRenderer.renderBuffers.zBuffer);
		renderingContext.bindTexture2D("normalBuffer", worldRenderer.renderBuffers.normalBuffer);
		renderingContext.bindTexture2D("metaBuffer", worldRenderer.renderBuffers.materialBuffer);

		//Required to shade the sky
		renderingContext.bindTexture2D("sunSetRiseTexture", worldRenderer.worldTextures.sunGlowTexture);
		renderingContext.bindTexture2D("skyTextureSunny", worldRenderer.worldTextures.skyTextureSunny);
		renderingContext.bindTexture2D("skyTextureRaining", worldRenderer.worldTextures.skyTextureRaining);
		reflectionsShader.setUniform1f("dayTime", worldRenderer.skyRenderer.time);

		//Texture2D lightColors = TexturesHandler.getTexture("./textures/environement/lightcolors.png");
		//renderingContext.bindTexture2D("lightColors", lightColors);

		renderingContext.bindCubemap("environmentCubemap", worldRenderer.renderBuffers.environmentMap);

		// Matrices for screen-space transformations
		renderingContext.getCamera().setupShader(reflectionsShader);
		worldRenderer.skyRenderer.setupShader(reflectionsShader);

		//Disable depth writing and run the deal
		renderingContext.getRenderTargetManager().setDepthMask(false);
		renderingContext.drawFSQuad();
		renderingContext.getRenderTargetManager().setDepthMask(true);

		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		
		renderingContext.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.fboShadedBuffer);
	}
}
