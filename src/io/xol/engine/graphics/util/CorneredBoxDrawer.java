package io.xol.engine.graphics.util;

public class CorneredBoxDrawer
{

	public static void drawCorneredBoxTiled(float posx, float posy, int width, int height, int cornerSize, String texture, int textureSize, int scale)
	{
		int maxCoverage = textureSize / 2 * scale;

		// corner up-left
		ObjectRenderer.renderTexturedRotatedRect(posx - width / 2, posy + height / 2, cornerSize * scale, cornerSize * scale, 0, 0, 0f, 1 / 4f, 1 / 4f, texture);
		// corner up-right
		ObjectRenderer.renderTexturedRotatedRect(posx + width / 2, posy + height / 2, cornerSize * scale, cornerSize * scale, 0, 3 / 4f, 0f, 1f, 1 / 4f, texture);

		// left
		for (int b = (int) (posy - height / 2); b < (int) (posy + height / 2); b += maxCoverage)
		{
			int spaceLeftToCoverY = Math.min((int) (posy + height / 2) - b, maxCoverage);
			ObjectRenderer.renderTexturedRotatedRect(posx - width / 2, b + spaceLeftToCoverY / 2, cornerSize * scale, spaceLeftToCoverY, 0, 0f, 1 / 4f, 1 / 4f, 1 / 4f + 1f * spaceLeftToCoverY / (textureSize * scale), texture);
		}
		// ObjectRenderer.renderTexturedRotatedRect(posx-width/2,
		// posy,cornerSize*scale, height-cornerSize*scale, 0, 0f, 1/4f, 1/4f,
		// 3/4f, texture);
		// up
		for (int a = (int) (posx - width / 2 + maxCoverage / scale / 2); a < (int) (posx + width / 2); a += maxCoverage)
		{
			int spaceLeftToCoverX = Math.min((int) (posx + width / 2) - a, maxCoverage);
			ObjectRenderer.renderTexturedRotatedRect(a + spaceLeftToCoverX / 2, posy + height / 2, spaceLeftToCoverX, cornerSize * scale, 0, 1 / 4f, 0f, 1 / 4f + 1f * spaceLeftToCoverX / (textureSize * scale), 1 / 4f, texture);
		}
		// center
		for (int a = (int) (posx - width / 2); a < (int) (posx + width / 2); a += maxCoverage)
		{
			int spaceLeftToCoverX = Math.min((int) (posx + width / 2) - a, maxCoverage);
			// ObjectRenderer.renderTexturedRotatedRect(a + spaceLeftToCoverX/2,
			// posy-height/2, spaceLeftToCoverX, cornerSize*scale, 0, 1/4f,
			// 3/4f, 1/4f + 1f * spaceLeftToCoverX / (textureSize * scale) , 1f,
			// texture);
			for (int b = (int) (posy - height / 2 + maxCoverage / 2 / scale); b < (int) (posy + height / 2); b += maxCoverage)
			{
				int spaceLeftToCoverY = Math.min((int) (posy + height / 2) - b, maxCoverage);
				ObjectRenderer.renderTexturedRotatedRect(a + spaceLeftToCoverX / 2, b + spaceLeftToCoverY / 2, spaceLeftToCoverX, spaceLeftToCoverY, 0, 1 / 4f, 1 / 4f, 1 / 4f + 1f * spaceLeftToCoverX / (textureSize * scale), 1 / 4f + 1f
						* spaceLeftToCoverY / (textureSize * scale), texture);
			}
		}
		// ObjectRenderer.renderTexturedRotatedRect(posx,
		// posy,width-cornerSize*scale, height-cornerSize*scale, 0, 1/4f, 1/4f,
		// 3/4f, 3/4f, texture);
		// back
		for (int a = (int) (posx - width / 2 + maxCoverage / 2 / scale); a < (int) (posx + width / 2); a += maxCoverage)
		{
			int spaceLeftToCoverX = Math.min((int) (posx + width / 2) - a, maxCoverage);
			ObjectRenderer.renderTexturedRotatedRect(a + spaceLeftToCoverX / 2, posy - height / 2, spaceLeftToCoverX, cornerSize * scale, 0, 1 / 4f, 3 / 4f, 1 / 4f + 1f * spaceLeftToCoverX / (textureSize * scale), 1f, texture);
		}
		// ObjectRenderer.renderTexturedRotatedRect(posx,
		// posy-height/2,width-cornerSize*scale, cornerSize*scale, 0, 1/4f,
		// 3/4f, 3/4f, 1f, texture);
		// right
		for (int b = (int) (posy - height / 2 + maxCoverage / 2 / scale); b < (int) (posy + height / 2); b += maxCoverage)
		{
			int spaceLeftToCoverY = Math.min((int) (posy + height / 2) - b, maxCoverage);
			ObjectRenderer.renderTexturedRotatedRect(posx + width / 2, b + spaceLeftToCoverY / 2, cornerSize * scale, spaceLeftToCoverY, 0, 3 / 4f, 1 / 4f, 1f, 1 / 4f + 1f * spaceLeftToCoverY / (textureSize * scale), texture);
		}
		// ObjectRenderer.renderTexturedRotatedRect(posx+width/2,
		// posy,cornerSize*scale, height-cornerSize*scale, 0, 3/4f, 1/4f, 1f,
		// 3/4f, texture);

		// corner up-left
		ObjectRenderer.renderTexturedRotatedRect(posx - width / 2, posy - height / 2, cornerSize * scale, cornerSize * scale, 0, 0, 3 / 4f, 1 / 4f, 1f, texture);
		// corner up-right
		ObjectRenderer.renderTexturedRotatedRect(posx + width / 2, posy - height / 2, cornerSize * scale, cornerSize * scale, 0, 3 / 4f, 3 / 4f, 1f, 1f, texture);

	}

	public static void drawCorneredBox(float posx, float posy, int width, int height, int cornerSize, String texture)
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
