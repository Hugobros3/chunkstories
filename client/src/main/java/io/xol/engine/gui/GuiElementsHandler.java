package io.xol.engine.gui;

import io.xol.chunkstories.gui.ng.NgButton;
import io.xol.chunkstories.gui.ng.ScrollableContainer;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.GuiElement;
import io.xol.engine.gui.elements.InputText;

import java.util.ArrayList;
import java.util.List;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GuiElementsHandler
{
	List<GuiElement> objects;
	int focusedObject;

	public GuiElementsHandler()
	{
		objects = new ArrayList<GuiElement>();
	}

	public void add(GuiElement f)
	{
		objects.add(f);
	}

	public void next()
	{
		objects.get(focusedObject).setFocus(false);
		nextId();
		objects.get(focusedObject).setFocus(true);
	}

	public void handleInput(int k)
	{
		GuiElement obj = getFocusedObject();
		if (obj != null && obj instanceof InputText)
		{
			InputText in = (InputText) obj;
			in.input(k);
		}
		else if (obj instanceof Button)
		{
			//TODO hard-coded enter key
			if(k == /*FastConfig.ENTER_KEY*/ 28)
			{
				Button but = (Button) obj;
				but.clicked = true;
			}
		}
	}

	public void handleClick(int x, int y)
	{
		// int id = 0;
		for (GuiElement obj : objects)
		{
			if (obj instanceof Button)
			{
				Button but = (Button) obj;
				if (but.isMouseOver())
					but.clicked = true;
			}
			if (obj instanceof NgButton)
			{
				NgButton but = (NgButton) obj;
				if (but.isMouseOver())
					but.clicked = true;
			}
			if (obj instanceof ScrollableContainer)
			{
				ScrollableContainer but = (ScrollableContainer) obj;
				if (but.isMouseOver(x, y))
					but.handleClick(0, x, y);
			}
			else if(obj instanceof InputText)
			{
				InputText inp = (InputText)obj;
				if(inp.isMouseOver())
					changeFocus(objects.indexOf(inp));
			}
		}
	}

	public List<GuiElement> getAllObjects()
	{
		return objects;
	}

	public GuiElement getFocusedObject()
	{
		return objects.get(focusedObject);
	}

	void changeFocus(int id)
	{
		objects.get(focusedObject).setFocus(false);
		focusedObject = id;
		objects.get(focusedObject).setFocus(true);
	}

	void nextId()
	{
		focusedObject++;
		if (focusedObject == objects.size())
			focusedObject = 0;
	}

	public GuiElement get(int i)
	{
		return objects.get(i);
	}

	public Button getButton(int i)
	{
		return (Button) objects.get(i);
	}

	public InputText getInputText(int i)
	{
		return (InputText) objects.get(i);
	}

	public void rescaleGui(int scale)
	{
		for (GuiElement obj : objects)
		{
			obj.setScale(scale);
		}
	}
}
