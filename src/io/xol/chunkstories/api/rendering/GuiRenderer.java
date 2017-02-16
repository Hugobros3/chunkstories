package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.engine.graphics.textures.Texture2D;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface GuiRenderer
{

	void drawBoxWindowsSpace(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, Texture2D texture, boolean alpha, boolean textured, Vector4fm color);

	void drawBoxWindowsSpaceWithSize(float startX, float startY, float width, float height, float textureStartX, float textureStartY, float textureEndX, float textureEndY, Texture2D texture, boolean alpha, boolean textured, Vector4fm color);

	void drawBox(float startX, float startY, float endX, float endY, float textureStartX, float textureStartY, float textureEndX, float textureEndY, Texture2D texture, boolean alpha, boolean textured, Vector4fm color);

}