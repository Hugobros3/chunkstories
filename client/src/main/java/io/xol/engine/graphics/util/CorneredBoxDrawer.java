//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.util;

import org.joml.Vector4f;

import io.xol.chunkstories.api.rendering.GuiRenderer;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.RenderingContext;
import io.xol.chunkstories.renderer.opengl.texture.TexturesHandler;

public class CorneredBoxDrawer
{
	public static void drawCorneredBoxTiled(float posx, float posy, float width, float height, int cornerSize, Texture2D texture, int textureSize, int scale)
	{
		RenderingContext renderingContext = Client.getInstance().getGameWindow().getRenderingContext();
		GuiRenderer guiRenderer = renderingContext.getGuiRenderer();
		
		float topLeftCornerX = posx - width / 2;
		float topLeftCornerY = posy - height / 2;

		float botRightCornerX = posx + width / 2;
		float botRightCornerY = posy + height / 2;
		
		//Debug helper
		//guiRenderer.drawBoxWindowsSpace(topLeftCornerX, topLeftCornerY, botRightCornerX, botRightCornerY, 0, 0, 0, 0, null, true, false, new Vector4f(1.0, 1.0, 0.0, 1.0));
		
		int cornerSizeScaled = scale * cornerSize;
		
		float textureSizeInternal = textureSize - cornerSize * 2;
		
		float insideWidth = width - cornerSizeScaled * 2;
		float insideHeight = height - cornerSizeScaled * 2;
		
		float texCoordInsideTopLeft = ((float)cornerSize)/textureSize;
		float texCoordInsideBottomRight = ((float)(textureSize - cornerSize)) / textureSize;
		
		//Fill the inside of the box
		for(int fillerX = 0; fillerX < insideWidth; fillerX += textureSizeInternal * scale)
			for(int fillerY = 0; fillerY < insideHeight; fillerY += textureSizeInternal * scale)
			{
				float toFillX = Math.min(textureSizeInternal * scale, insideWidth - fillerX);
				float toFillY = Math.min(textureSizeInternal * scale, insideHeight - fillerY);
				
				float startX = topLeftCornerX + cornerSizeScaled + fillerX;
				float startY = topLeftCornerY + cornerSizeScaled + fillerY;
				
				guiRenderer.drawBoxWindowsSpace(startX, startY, startX + toFillX, startY + toFillY
						, texCoordInsideTopLeft, texCoordInsideTopLeft + toFillY / textureSize / scale, texCoordInsideTopLeft + toFillX / textureSize / scale,  texCoordInsideTopLeft, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
			}
		
		//Fill the horizontal sides
		for(int fillerX = 0; fillerX < insideWidth; fillerX += textureSizeInternal * scale)
		{
			float toFillX = Math.min(textureSizeInternal * scale, insideWidth - fillerX);
			
			float startX = topLeftCornerX + cornerSizeScaled + fillerX;
			float startY = topLeftCornerY;
			
			guiRenderer.drawBoxWindowsSpace(startX, startY + height - cornerSizeScaled, startX + toFillX, startY + height
					, texCoordInsideTopLeft, texCoordInsideTopLeft, texCoordInsideTopLeft + toFillX / textureSize / scale,  0, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		
			guiRenderer.drawBoxWindowsSpace(startX, startY, startX + toFillX, startY + cornerSizeScaled
					, texCoordInsideTopLeft, 1.0f, texCoordInsideTopLeft + toFillX / textureSize / scale, texCoordInsideBottomRight, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		
		}
		
		//Fill the vertical sides
		for(int fillerY = 0; fillerY < insideHeight; fillerY += textureSizeInternal * scale)
		{
			float toFillY = Math.min(textureSizeInternal * scale, insideHeight - fillerY);
			
			float startY = topLeftCornerY + cornerSizeScaled + fillerY;
			float startX = topLeftCornerX;
			
			guiRenderer.drawBoxWindowsSpace(startX, startY, startX + cornerSizeScaled, startY + toFillY
					, 0, texCoordInsideBottomRight - (textureSizeInternal * scale - toFillY) / textureSize / scale, texCoordInsideTopLeft, texCoordInsideTopLeft, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
			
			guiRenderer.drawBoxWindowsSpace(startX + width - cornerSizeScaled, startY, startX + width, startY + toFillY
					, texCoordInsideBottomRight, texCoordInsideBottomRight - (textureSizeInternal * scale - toFillY) / textureSize / scale, 1.0f, texCoordInsideTopLeft, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		}
		
		//Fill the 4 corners
		guiRenderer.drawBoxWindowsSpace(topLeftCornerX, botRightCornerY - cornerSizeScaled, topLeftCornerX + cornerSizeScaled, botRightCornerY
				, 0, texCoordInsideTopLeft, texCoordInsideTopLeft, 0, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		
		guiRenderer.drawBoxWindowsSpace(topLeftCornerX, topLeftCornerY, topLeftCornerX + cornerSizeScaled, topLeftCornerY + cornerSizeScaled
				, 0, 1.0f, texCoordInsideTopLeft, texCoordInsideBottomRight, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		
		guiRenderer.drawBoxWindowsSpace(botRightCornerX - cornerSizeScaled, botRightCornerY - cornerSizeScaled, botRightCornerX, botRightCornerY
				, texCoordInsideBottomRight, texCoordInsideTopLeft, 1.0f, 0, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		
		guiRenderer.drawBoxWindowsSpace(botRightCornerX - cornerSizeScaled, topLeftCornerY, botRightCornerX, topLeftCornerY + cornerSizeScaled
				, texCoordInsideBottomRight, 1.0f, 1.0f, texCoordInsideBottomRight, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		
	}

	public static void drawCorneredBoxTiled(float posx, float posy, float width, float height, int cornerSize, String textureName, int textureSize, int scale)
	{
		Texture2D texture = TexturesHandler.getTexture(textureName);
		drawCorneredBoxTiled(posx, posy, width, height, cornerSize, texture, textureSize, scale);
	}

	public static void drawCorneredBox(float posx, float posy, float width, float height, float cornerSize, String texture)
	{
		// corner up-left
		ObjectRenderer.renderTexturedRotatedRect(posx - width / 2, posy + height / 2, cornerSize * 2, cornerSize * 2, 0, 0, 0f, 1 / 4f, 1 / 4f, texture);
		// corner up-right
		ObjectRenderer.renderTexturedRotatedRect(posx + width / 2, posy + height / 2, cornerSize * 2, cornerSize * 2, 0, 3 / 4f, 0f, 1f, 1 / 4f, texture);

		// left
		ObjectRenderer.renderTexturedRotatedRect(posx - width / 2, posy, cornerSize * 2, height - cornerSize * 2, 0, 0, 1 / 4f, 1 / 4f, 3 / 4f, texture);
		// up
		ObjectRenderer.renderTexturedRotatedRect(posx, posy + height / 2, width - cornerSize * 2, cornerSize * 2, 0, 1 / 4f, 0f, 3 / 4f, 1 / 4f, texture);
		// center
		ObjectRenderer.renderTexturedRotatedRect(posx, posy, width - cornerSize * 2, height - cornerSize * 2, 0, 1 / 4f, 1 / 4f, 3 / 4f, 3 / 4f, texture);
		// back
		ObjectRenderer.renderTexturedRotatedRect(posx, posy - height / 2, width - cornerSize * 2, cornerSize * 2, 0, 1 / 4f, 3 / 4f, 3 / 4f, 1f, texture);
		// left
		ObjectRenderer.renderTexturedRotatedRect(posx + width / 2, posy, cornerSize * 2, height - cornerSize * 2, 0, 3 / 4f, 1 / 4f, 1f, 3 / 4f, texture);

		// corner up-left
		ObjectRenderer.renderTexturedRotatedRect(posx - width / 2, posy - height / 2, cornerSize * 2, cornerSize * 2, 0, 0, 3 / 4f, 1 / 4f, 1f, texture);
		// corner up-right
		ObjectRenderer.renderTexturedRotatedRect(posx + width / 2, posy - height / 2, cornerSize * 2, cornerSize * 2, 0, 3 / 4f, 3 / 4f, 1f, 1f, texture);

	}
}
