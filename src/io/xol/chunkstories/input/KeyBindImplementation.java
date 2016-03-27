package io.xol.chunkstories.input;

import org.lwjgl.input.Keyboard;

import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.client.Client;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a key assignated to some action
 * @author Hugo
 *
 */
public class KeyBindImplementation implements KeyBind
{
	String name;
	
	//Specific to the LWJGL 2.x implementation
	//A port would need rework here
	int LWJGL2_key;
	
	/**
	 * Returns the name of the bind
	 * @return
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Internal to the engine, should not be interfered with by external mods
	 * @return
	 */
	public int getLWJGL2xKey()
	{
		return LWJGL2_key;
	}
	
	public boolean isPressed()
	{
		return Keyboard.isKeyDown(LWJGL2_key);
	}
	
	public KeyBindImplementation(String name, String defaultKeyName)
	{
		this.name = name;
		this.LWJGL2_key = Client.getConfig().getIntProp("bind."+name, Keyboard.getKeyIndex(defaultKeyName));
	}
	
	/**
	 * When reloading from the config file (options changed)
	 */
	public void reload()
	{
		this.LWJGL2_key = Client.getConfig().getIntProp("bind."+name, -1);
	}
	
	public boolean equals(Object o)
	{
		if(o != null && o instanceof KeyBind)
		{
			return ((KeyBind)o).getName().equals(getName());
		}
		return false;
	}
	
	public int hashCode()
	{
		return getName().hashCode();
	}
}
