package io.xol.chunkstories.input.lwjgl2;

import org.lwjgl.input.Keyboard;

import io.xol.chunkstories.api.input.KeyboardKeyInput;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.input.InputsLoaderHelper;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a key assignated to some action
 */
public class KeyBindImplementation implements KeyboardKeyInput, LWJGLPollable
{
	String name;
	
	//Specific to the LWJGL 2.x implementation
	//A port would need rework here
	int LWJGL2_key;
	
	long hash;
	
	boolean isDown;
	
	boolean editable = true;
	
	/**
	 * Returns the name of the bind
	 * @return
	 */
	@Override
	public String getName()
	{
		return name;
	}
	
	public long getHash()
	{
		return hash;
	}
	
	/**
	 * Internal to the engine, should not be interfered with by external mods
	 * @return
	 */
	public int getLWJGL2xKey()
	{
		return LWJGL2_key;
	}
	
	@Override
	public boolean isPressed()
	{
		return isDown;
	}
	
	public KeyBindImplementation(String name, String defaultKeyName)
	{
		this.name = name;
		this.LWJGL2_key = Client.getConfig().getInteger("bind."+name, Keyboard.getKeyIndex(defaultKeyName));
		computeHash(name);
	}
	
	private void computeHash(String name2)
	{
		byte[] digested = InputsLoaderHelper.md.digest(name2.getBytes());
		hash = (hash & 0x0FFFFFFFFFFFFFFFL) | (((long) digested[0] & 0xF) << 60);
		hash = (hash & 0xF0FFFFFFFFFFFFFFL) | (((long) digested[1] & 0xF) << 56);
		hash = (hash & 0xFF0FFFFFFFFFFFFFL) | (((long) digested[2] & 0xF) << 52);
		hash = (hash & 0xFFF0FFFFFFFFFFFFL) | (((long) digested[3] & 0xF) << 48);
		hash = (hash & 0xFFFF0FFFFFFFFFFFL) | (((long) digested[4] & 0xF) << 44);
		hash = (hash & 0xFFFFF0FFFFFFFFFFL) | (((long) digested[5] & 0xF) << 40);
		hash = (hash & 0xFFFFFF0FFFFFFFFFL) | (((long) digested[6] & 0xF) << 36);
		hash = (hash & 0xFFFFFFF0FFFFFFFFL) | (((long) digested[7] & 0xF) << 32);
		hash = (hash & 0xFFFFFFFF0FFFFFFFL) | (((long) digested[8] & 0xF) << 28);
		hash = (hash & 0xFFFFFFFFF0FFFFFFL) | (((long) digested[9] & 0xF) << 24);
		hash = (hash & 0xFFFFFFFFFF0FFFFFL) | (((long) digested[10] & 0xF) << 20);
		hash = (hash & 0xFFFFFFFFFFF0FFFFL) | (((long) digested[11] & 0xF) << 16);
		hash = (hash & 0xFFFFFFFFFFFF0FFFL) | (((long) digested[12] & 0xF) << 12);
		hash = (hash & 0xFFFFFFFFFFFFF0FFL) | (((long) digested[13] & 0xF) << 8);
		hash = (hash & 0xFFFFFFFFFFFFFF0FL) | (((long) digested[14] & 0xF) << 4);
		hash = (hash & 0xFFFFFFFFFFFFFFF0L) | (((long) digested[15] & 0xF) << 0);
	}

	/**
	 * When reloading from the config file (options changed)
	 */
	public void reload()
	{
		this.LWJGL2_key = Client.getConfig().getInteger("bind."+name, -1);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o != null && o instanceof KeyboardKeyInput)
		{
			return ((KeyboardKeyInput)o).getName().equals(getName());
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}
	
	public void updateStatus()
	{
		isDown = Keyboard.isKeyDown(LWJGL2_key);
	}
	
	/**
	 * Is this key bind editable in the controls
	 */
	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable(boolean editable)
	{
		this.editable = editable;
	}
}
