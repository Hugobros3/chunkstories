package io.xol.engine.gui;

import java.util.ArrayList;
import java.util.List;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class FocusableObjectsHandler
{
	List<Focusable> objects;
	int focusedObject;

	public FocusableObjectsHandler()
	{
		objects = new ArrayList<Focusable>();
	}

	public void add(Focusable f)
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
		Focusable obj = getFocusedObject();
		if (obj != null && obj instanceof InputText)
		{
			InputText in = (InputText) obj;
			in.input(k);
		}
		else if (obj instanceof ClickableButton)
		{
			//TODO hard-coded enter key
			if(k == /*FastConfig.ENTER_KEY*/ 28)
			{
				ClickableButton but = (ClickableButton) obj;
				but.clicked = true;
			}
		}
	}

	public void handleClick(int x, int y)
	{
		// int id = 0;
		for (Focusable obj : objects)
		{
			if (obj instanceof ClickableButton)
			{
				ClickableButton but = (ClickableButton) obj;
				if (but.isMouseOver())
				{
					// System.out.println("clik"+obj.toString());
					but.clicked = true;
				}
			}
			else if(obj instanceof InputText)
			{
				InputText inp = (InputText)obj;
				if(inp.isMouseOver())
				{
					changeFocus(objects.indexOf(inp));
				}
			}
		}
	}

	public List<Focusable> getAllObjects()
	{
		return objects;
	}

	public Focusable getFocusedObject()
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

	public Focusable get(int i)
	{
		return objects.get(i);
	}

	public ClickableButton getButton(int i)
	{
		return (ClickableButton) objects.get(i);
	}

	public InputText getInputText(int i)
	{
		return (InputText) objects.get(i);
	}
}
