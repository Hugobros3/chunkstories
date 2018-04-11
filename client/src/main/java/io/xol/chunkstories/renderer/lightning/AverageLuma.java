//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.lightning;

import java.nio.ByteBuffer;

import org.joml.Vector3f;

import io.xol.chunkstories.renderer.opengl.texture.Texture2DRenderTargetGL;
import io.xol.chunkstories.renderer.opengl.util.PBOPacker;

public class AverageLuma
{
	private PBOPacker illuminationDownloader;
	private PBOPacker.PBOPackerResult illuminationDownloadInProgress = null;
	float apertureModifier = 1f;

	private int frameLumaDownloadDelay = 0;
	private final static int FRAME_DELAY_FOR_LUMA_MIP_GRAB = 2;
	private Vector3f averageColorForAllFrame = new Vector3f(1.0f);
	private int downloadSize;
	
	public AverageLuma()
	{
		illuminationDownloader = new PBOPacker();
	}
	
	public void computeAverageLuma(Texture2DRenderTargetGL finalBuffer) {
		finalBuffer.setMipMapping(true);

		int maxMipLevel = finalBuffer.getMaxMipmapLevel();
		finalBuffer.setMipmapLevelsRange(0, maxMipLevel);

		int divisor = 1 << maxMipLevel;
		int mipWidth = finalBuffer.getWidth() / divisor;
		int mipHeight = finalBuffer.getHeight() / divisor;

		if (illuminationDownloadInProgress == null)
		{
			//Start mipmap calculation for next frame
			finalBuffer.computeMipmaps();
			frameLumaDownloadDelay = FRAME_DELAY_FOR_LUMA_MIP_GRAB + 16;

			//To avoid crashing when the windows size change, we send the PBO copy request immediately and instead wait to look for it
			this.illuminationDownloadInProgress = this.illuminationDownloader.copyTexure(finalBuffer, maxMipLevel);

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
				if(minMipmapBuffer != null) {
				
					minMipmapBuffer.flip();
	
					//System.out.println("Obtained: "+minMipmapBuffer);
					averageColorForAllFrame = new Vector3f(0.0f);
					for (int i = 0; i < downloadSize; i++)
						averageColorForAllFrame.add(minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat(), minMipmapBuffer.getFloat());
	
					averageColorForAllFrame.mul(1.0f / downloadSize);
					//System.out.println(averageColorForAllFrame.length());
				}
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

		float change_speed = 0.007f;
		
		if (luma < targetLuma - lumaMargin)
		{
			if (apertureModifier < 10.0)
				apertureModifier *= (1.0 + change_speed);
		}
		else if (luma > targetLuma + lumaMargin)
		{
			if (apertureModifier >= 1.0)
				apertureModifier *= (1.0 - change_speed);
		}
		else
		{
			float clamped = (float) Math.min(Math.max(1 / apertureModifier, 0.998), 1.002);
			apertureModifier *= clamped;
		}
		
		//System.out.println("luma:"+luma+" am:"+apertureModifier);
		
		finalBuffer.setMipmapLevelsRange(0, 0);
	}

	public float getApertureModifier() {
		return apertureModifier;
	}

	public void destroy() {
		illuminationDownloader.destroy();
	}
}
