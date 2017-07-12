package io.xol.chunkstories.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.renderer.lights.ComputedShadowMap;
import io.xol.chunkstories.renderer.sky.DefaultSkyRenderer;

public class ShadowMapRenderer
{
	WorldRendererImplementation worldRenderer;
	WorldClient world;
	
	public ShadowMapRenderer(WorldRendererImplementation worldRendererImplementation)
	{
		this.worldRenderer = worldRendererImplementation;
		this.world = worldRendererImplementation.getWorld();
	}

	public ComputedShadowMap generateSunShadowMap(RenderingInterface renderingContext, DefaultSkyRenderer sky)
	{
		if (this.getShadowVisibility() == 0f)
			return null; // No shadows at night :)

		//Size dependant of end texture resolution
		int shadowMapTextureSize = worldRenderer.renderBuffers.shadowMapBuffer.getWidth();
		int shadowRange = 128;
		if (shadowMapTextureSize > 1024)
			shadowRange = 192;
		else if (shadowMapTextureSize > 2048)
			shadowRange = 256;
		int shadowDepthRange = 200;
		
		//Builds the shadow matrix
		Matrix4f depthProjectionMatrix = new Matrix4f().ortho(-shadowRange, shadowRange, -shadowRange, shadowRange, -shadowDepthRange, shadowDepthRange);//MatrixHelper.getOrthographicMatrix(-shadowRange, shadowRange, -shadowRange, shadowRange, -shadowDepthRange, shadowDepthRange);
		Matrix4f depthViewMatrix = new Matrix4f().lookAt(sky.getSunPosition(), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
		Matrix4f shadowMVP = new Matrix4f();
		
		depthProjectionMatrix.mul(depthViewMatrix, shadowMVP);
		//Matrix4f.mul(depthProjectionMatrix, depthViewMatrix, shadowMVP);
		
		Matrix4f actualMatrixReturned = new Matrix4f(shadowMVP);
		Vector3dc posd = renderingContext.getCamera().getCameraPosition();
		Vector3f pos = new Vector3f((float)posd.x(), (float)posd.y(), (float)posd.z());
		pos.negate();
		
		shadowMVP.translate(pos);

		//Set appropriate fixed function stuff
		renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		renderingContext.setBlendMode(BlendMode.DISABLED);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);

		//Bind relevant FBO and clear it
		renderingContext.getRenderTargetManager().setConfiguration(worldRenderer.renderBuffers.shadowMapFBO);
		renderingContext.getRenderTargetManager().clearBoundRenderTargetZ(1.0f);

		ShaderInterface shadowsPassShader = renderingContext.useShader("shadows");
		
		shadowsPassShader.setUniform1f("time", worldRenderer.animationTimer);
		shadowsPassShader.setUniformMatrix4f("depthMVP", shadowMVP);

		renderingContext.bindAlbedoTexture(worldRenderer.worldTextures.blocksAlbedoTexture);
		renderingContext.setObjectMatrix(null);
		
		//We render the world from that perspective
		shadowsPassShader.setUniform1f("allowForWavyStuff", 1); //Hackish way of enabling the shader input for the fake "wind" effect vegetation can have
		worldRenderer.getChunkMeshesRenderer().renderChunks(renderingContext, WorldRenderer.RenderingPass.SHADOW);
		shadowsPassShader.setUniform1f("allowForWavyStuff", 0); //In tern, disabling it while we do the entities
		worldRenderer.entitiesRenderer.renderEntities(renderingContext);
		
		//Returns a fancy object pointing to the data we just generated
		return new ComputedShadowMap() {

			@Override
			public Texture2D getShadowMap()
			{
				return worldRenderer.renderBuffers.shadedBuffer;
			}

			@Override
			public Matrix4f getShadowTransformationMatrix()
			{
				return actualMatrixReturned;
			}
			
		};
	}

	public float getShadowVisibility()
	{
		float worldTime = (world.getTime() % 10000 + 10000) % 10000;
		int start = 2500;
		int end = 7500;
		if (worldTime < start || worldTime > end + 500)
			return 0;
		else if (worldTime < start + 500)
			return (worldTime - start) / 500f;
		else if (worldTime > end)
			return 1 - (worldTime - end) / 500f;
		else
			return 1;
	}
}
