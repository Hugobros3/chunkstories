package io.xol.chunkstories.api.gui;

import java.util.LinkedList;
import java.util.List;

import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;

public class Layer {
	protected final GameWindow gameWindow;
	protected Layer parentLayer;
	
	protected float xPosition, yPosition;
	protected float width, height;
	
	protected List<GuiElement> elements = new LinkedList<GuiElement>();
	protected FocusableGuiElement focusedElement = null;
	
	/** parentLayer might be null, but gameWindow can't be ! */
	public Layer(GameWindow gameWindow, Layer parentLayer) {
		this.gameWindow = gameWindow;
		this.parentLayer = parentLayer;
		
		if(gameWindow == null)
			throw new RuntimeException("Fuck off");
		
		xPosition = 0;
		yPosition = 0;
		width = gameWindow.getWidth();
		height = gameWindow.getHeight();
	}

	public GameWindow getGameWindow() {
		return gameWindow;
	}

	public Layer getParentLayer() {
		return parentLayer;
	}
	
	/** Draws to the screen (preferably using the defined borders!) 
	 *  You may render the parent layer to have an overlay effect, but it's not mandatory. */
	public void render(RenderingInterface renderer) {
		
	}
	
	public boolean handleInput(Input input) {
		if(focusedElement != null)
			if(focusedElement.handleInput(input))
				return true;
		
		if(input instanceof MouseButton) {
			MouseButton mb = (MouseButton)input;
			//System.out.println(mb.getMouse().getCursorX());
			for(GuiElement ge : elements) {
				if(ge.isMouseOver()) {

					if(ge instanceof FocusableGuiElement)
						this.setFocusedElement((FocusableGuiElement) ge);
					
					if(ge instanceof ClickableGuiElement && ((ClickableGuiElement) ge).handleClick(mb))
						return true;
				}
			}
		}
		
		//Forward to parent if not handled
		/*Layer parent = this.parentLayer;
		if(parent != null)
			return parent.handleInput(input);*/
		
		return false;
	}
	
	public boolean handleTextInput(char c) {
		if(focusedElement != null && focusedElement instanceof TextInputGuiElement)
			return ((TextInputGuiElement) focusedElement).handleTextInput(c);

		//Forward to parent if not handled
		/*Layer parent = this.parentLayer;
		if(parent != null)
			return parent.handleTextInput(c);*/
		
		return false;
	}
	
	/** Frees and closes ressources */
	public void destroy() {
		
	}

	public float getxPosition() {
		return xPosition;
	}

	public void setxPosition(float xPosition) {
		this.xPosition = xPosition;
	}

	public float getyPosition() {
		return yPosition;
	}

	public void setyPosition(float yPosition) {
		this.yPosition = yPosition;
	}

	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public FocusableGuiElement getFocusedElement() {
		return focusedElement;
	}

	public void setFocusedElement(FocusableGuiElement focusedElement) {
		this.focusedElement = focusedElement;
	}

	public final Layer getRootLayer() {
		if(parentLayer == null)
			return this;
		else
			return parentLayer.getRootLayer();
	}

	public void onResize(int newWidth, int newHeight) {
		if(parentLayer != null)
			parentLayer.onResize(newWidth, newHeight);
	}
}
