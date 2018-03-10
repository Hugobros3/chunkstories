//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import static io.xol.chunkstories.api.rendering.textures.TextureFormat.DEPTH_RENDERBUFFER;

import org.joml.Vector3d;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.Shader;
import io.xol.chunkstories.api.rendering.pipeline.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.engine.graphics.fbo.FrameBufferObjectGL;
import io.xol.engine.graphics.textures.Texture2DRenderTargetGL;

public class GiRenderer {
	public final WorldRendererImplementation worldRenderer;
	
	private Texture2DRenderTargetGL accumulationA, accumulationB;
	private Texture2DRenderTargetGL zBufferA, zBufferB;
	private FrameBufferObjectGL fboAccumulationA, fboAccumulationB;
	
	private NearbyVoxelsVolumeTexture voxels4gi;
	
	public int accumulatedSamples = 0;

	public GiRenderer(WorldRendererImplementation worldRenderer, NearbyVoxelsVolumeTexture voxels4gi) {
		this.worldRenderer = worldRenderer;
		this.voxels4gi = voxels4gi;

		float giScale = 2.0f;
		accumulationA = new Texture2DRenderTargetGL(TextureFormat.RGBA_16F, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		accumulationB = new Texture2DRenderTargetGL(TextureFormat.RGBA_16F, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		
		zBufferA = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		zBufferB = new Texture2DRenderTargetGL(DEPTH_RENDERBUFFER, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		
		fboAccumulationA = new FrameBufferObjectGL(zBufferA, accumulationA);
		fboAccumulationB = new FrameBufferObjectGL(zBufferB, accumulationB);
	}
	
	Vector3d cameraPosition = new Vector3d();
	Vector3d cameraDirection = new Vector3d();
	
	Vector3d oldCameraPosition = new Vector3d();
	Vector3d oldCameraDirection = new Vector3d();
	
	boolean renderingToA = false;
	
	public void resize() {

		float giScale = 2.0f;
		fboAccumulationA.resize((int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		fboAccumulationB.resize((int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
	}
	
	public Texture2DRenderTargetGL giTexture() {
		return (!renderingToA ? accumulationA : accumulationB);
	}
	
	public void render(RenderingInterface renderer) {
		cameraPosition.set(renderer.getCamera().getCameraPosition());
		cameraDirection.set(renderer.getCamera().getViewDirection());

		Shader giShader = renderer.useShader("gi");
		
		renderer.getRenderTargetManager().setConfiguration(renderingToA ? fboAccumulationA : fboAccumulationB);
		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.DISABLED);

		renderer.bindTexture2D("albedoBuffer", worldRenderer.renderBuffers.rbAlbedo);
		renderer.bindTexture2D("depthBuffer", worldRenderer.renderBuffers.rbZBuffer);
		renderer.bindTexture2D("normalBuffer", worldRenderer.renderBuffers.rbNormal);
		
		renderer.getRenderTargetManager().clearBoundRenderTargetAll();

		renderer.bindTexture2D("previousBuffer", giTexture());
		
		if(cameraPosition.distance(oldCameraPosition) != 0.0f || cameraDirection.distance(oldCameraDirection) != 0.0f) {
			//System.out.println("moved! : " + cameraPosition.distance(oldCameraPosition) + "or " + cameraDirection.distance(oldCameraDirection));
			giShader.setUniform1i("keepPreviousData", 0);
			accumulatedSamples = 0;
		} else {
			giShader.setUniform1i("keepPreviousData", 1);
		}
		
		accumulatedSamples++;

		voxels4gi.update(renderer);
		voxels4gi.setupForRendering(renderer);
		

		giShader.setUniform1f("animationTimer", worldRenderer.animationTimer + accumulatedSamples);
		giShader.setUniform1f("overcastFactor", worldRenderer.getWorld().getWeather());
		giShader.setUniform1f("wetness", worldRenderer.getWorld().getGenerator().getEnvironment().getWorldWetness(cameraPosition));

		
		worldRenderer.getSkyRenderer().setupShader(giShader);
		renderer.getCamera().setupShader(giShader);
		
		renderer.drawFSQuad();
		
		renderingToA = !renderingToA;
		
		oldCameraPosition.set(cameraPosition);
		oldCameraDirection.set(cameraDirection);
	}
}
