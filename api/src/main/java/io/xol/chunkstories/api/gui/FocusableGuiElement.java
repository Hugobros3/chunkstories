package io.xol.chunkstories.api.gui;

import io.xol.chunkstories.api.input.Input;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Elements that can be focused, either by tab-ing on them, or by clicking */
public abstract class FocusableGuiElement extends GuiElement {
	
	protected FocusableGuiElement(Layer layer) {
		super(layer);
	}

	public boolean isFocused() {
		return this.equals(layer.getFocusedElement());
	}
	
	/** When focused an element receives input from the keyboard */
	public boolean handleInput(Input input) {
		return false;
	}
}
