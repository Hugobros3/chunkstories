package io.xol.chunkstories.tools;

import org.lwjgl.input.Keyboard;

public class KeysLister
{
	public static void main(String[] a)
	{
		for (int i = 0; i < Keyboard.getKeyCount(); i++)
		{
			System.out.println(i + " -> " + Keyboard.getKeyName(i));
		}
	}
}
