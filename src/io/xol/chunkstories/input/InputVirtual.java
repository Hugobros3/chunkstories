package io.xol.chunkstories.input;

import io.xol.chunkstories.api.input.KeyBind;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class InputVirtual implements KeyBind
{
	private String name;
	private long hash;
	
	private boolean pressed = false;

	public InputVirtual(String name)
	{
		this.name = name;
		computeHash();
	}

	public InputVirtual(String name, long hash)
	{
		this.name = name;
		this.hash = hash;
	}
	
	@Override
	public String getName()
	{
		return name;
	}

	public void setPressed(boolean pressed)
	{
		this.pressed = pressed;
	}
	
	@Override
	public boolean isPressed()
	{
		return pressed;
	}

	public String toString()
	{
		return "[KeyBindVirtual: " + name + "]";
	}
	
	public long getHash()
	{
		return hash;
	}
	
	private void computeHash()
	{
		byte[] digested = KeyBindsLoader.md.digest(name.getBytes());
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
}
