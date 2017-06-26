package io.xol.chunkstories.api.input;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Mouse {
	
	public MouseButton getMainButton();
	
	/** Returns true if the secondary mouse button is pressed */
	public MouseButton getSecondaryButton();
	
	/** Returns true if the middle mouse button is pressed */
	public MouseButton getMiddleButton();
	
	public interface MouseButton extends ClientInput {
		public Mouse getMouse();
	}
	
	/** Sent when the mouse scrolled (up or down) */
	public interface MouseScroll extends ClientInput {
		public int amount();
	}
	
	public float getCursorX();
	
	public float getCursorY();
	
	public void setMouseCursorLocation(double x, double y);
	
	public boolean isGrabbed();
	
	public void setGrabbed(boolean grabbed);
}
