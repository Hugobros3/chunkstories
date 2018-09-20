//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.scrap;

import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.GuiRenderer;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.renderer.OpenGLRenderingContext;

public class CorneredBoxDrawer {
	public static void drawCorneredBoxTiled_(float posx, float posy, float width, float height, int cornerSize,
			Texture2D texture, int textureSize, int scale) {
		OpenGLRenderingContext renderingContext = ClientImplementation.getInstance().getGameWindow().getRenderingInterface();
		GuiRenderer guiRenderer = renderingContext.getGuiRenderer();

		float topLeftCornerX = posx - width / 2;
		float topLeftCornerY = posy - height / 2;

		float botRightCornerX = posx + width / 2;
		float botRightCornerY = posy + height / 2;

		// Debug helper
		// guiRenderer.drawBoxWindowsSpace(topLeftCornerX, topLeftCornerY,
		// botRightCornerX, botRightCornerY, 0, 0, 0, 0, null, true, false, new
		// Vector4f(1.0, 1.0, 0.0, 1.0));

		int cornerSizeScaled = scale * cornerSize;

		float textureSizeInternal = textureSize - cornerSize * 2;

		float insideWidth = width - cornerSizeScaled * 2;
		float insideHeight = height - cornerSizeScaled * 2;

		float texCoordInsideTopLeft = ((float) cornerSize) / textureSize;
		float texCoordInsideBottomRight = ((float) (textureSize - cornerSize)) / textureSize;

		// Fill the inside of the box
		for (int fillerX = 0; fillerX < insideWidth; fillerX += textureSizeInternal * scale) {
			for (int fillerY = 0; fillerY < insideHeight; fillerY += textureSizeInternal * scale) {
				float toFillX = Math.min(textureSizeInternal * scale, insideWidth - fillerX);
				float toFillY = Math.min(textureSizeInternal * scale, insideHeight - fillerY);

				float startX = topLeftCornerX + cornerSizeScaled + fillerX;
				float startY = topLeftCornerY + cornerSizeScaled + fillerY;

				guiRenderer.drawBoxWindowsSpace(startX, startY, startX + toFillX, startY + toFillY,
						texCoordInsideTopLeft, texCoordInsideTopLeft + toFillY / textureSize / scale,
						texCoordInsideTopLeft + toFillX / textureSize / scale, texCoordInsideTopLeft, texture, true,
						false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
			}
		}

		// Fill the horizontal sides
		for (int fillerX = 0; fillerX < insideWidth; fillerX += textureSizeInternal * scale) {
			float toFillX = Math.min(textureSizeInternal * scale, insideWidth - fillerX);

			float startX = topLeftCornerX + cornerSizeScaled + fillerX;
			float startY = topLeftCornerY;

			guiRenderer.drawBoxWindowsSpace(startX, startY + height - cornerSizeScaled, startX + toFillX,
					startY + height, texCoordInsideTopLeft, texCoordInsideTopLeft,
					texCoordInsideTopLeft + toFillX / textureSize / scale, 0, texture, true, false,
					new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

			guiRenderer.drawBoxWindowsSpace(startX, startY, startX + toFillX, startY + cornerSizeScaled,
					texCoordInsideTopLeft, 1.0f, texCoordInsideTopLeft + toFillX / textureSize / scale,
					texCoordInsideBottomRight, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		}

		// Fill the vertical sides
		for (int fillerY = 0; fillerY < insideHeight; fillerY += textureSizeInternal * scale) {
			float toFillY = Math.min(textureSizeInternal * scale, insideHeight - fillerY);

			float startY = topLeftCornerY + cornerSizeScaled + fillerY;
			float startX = topLeftCornerX;

			guiRenderer.drawBoxWindowsSpace(startX, startY, startX + cornerSizeScaled, startY + toFillY, 0,
					texCoordInsideBottomRight - (textureSizeInternal * scale - toFillY) / textureSize / scale,
					texCoordInsideTopLeft, texCoordInsideTopLeft, texture, true, false,
					new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

			guiRenderer.drawBoxWindowsSpace(startX + width - cornerSizeScaled, startY, startX + width, startY + toFillY,
					texCoordInsideBottomRight,
					texCoordInsideBottomRight - (textureSizeInternal * scale - toFillY) / textureSize / scale, 1.0f,
					texCoordInsideTopLeft, texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
		}

		// Fill the 4 corners
		guiRenderer.drawBoxWindowsSpace(topLeftCornerX, botRightCornerY - cornerSizeScaled,
				topLeftCornerX + cornerSizeScaled, botRightCornerY, 0, texCoordInsideTopLeft, texCoordInsideTopLeft, 0,
				texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

		guiRenderer.drawBoxWindowsSpace(topLeftCornerX, topLeftCornerY, topLeftCornerX + cornerSizeScaled,
				topLeftCornerY + cornerSizeScaled, 0, 1.0f, texCoordInsideTopLeft, texCoordInsideBottomRight, texture,
				true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

		guiRenderer.drawBoxWindowsSpace(botRightCornerX - cornerSizeScaled, botRightCornerY - cornerSizeScaled,
				botRightCornerX, botRightCornerY, texCoordInsideBottomRight, texCoordInsideTopLeft, 1.0f, 0, texture,
				true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

		guiRenderer.drawBoxWindowsSpace(botRightCornerX - cornerSizeScaled, topLeftCornerY, botRightCornerX,
				topLeftCornerY + cornerSizeScaled, texCoordInsideBottomRight, 1.0f, 1.0f, texCoordInsideBottomRight,
				texture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

	}
}
