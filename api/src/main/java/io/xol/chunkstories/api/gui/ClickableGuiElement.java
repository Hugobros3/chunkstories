package io.xol.chunkstories.api.gui;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.input.Mouse.MouseButton;

public interface ClickableGuiElement {
	public boolean handleClick(MouseButton mouseButton);
}
