package io.xol.chunkstories.api.rendering.text;

import io.xol.chunkstories.api.rendering.Renderable;

public interface TextMesh extends Renderable {
	public String getText();
	
	public void destroy();
}
