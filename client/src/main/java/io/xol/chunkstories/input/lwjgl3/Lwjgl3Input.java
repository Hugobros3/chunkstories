//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.input.InputsLoaderHelper;

public abstract class Lwjgl3Input implements Input {
	
	public Lwjgl3Input(Lwjgl3ClientInputsManager im, String name) {
		this.im = im;
		this.name = name;
		
		computeHash(name);
	}
	
	protected final Lwjgl3ClientInputsManager im;
	protected final String name;
	
	private long hash;
	
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
	
	@Override
	public String getName()
	{
		return name;
	}
	
	public long getHash()
	{
		return hash;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == null)
			return false;
		else if(o instanceof Lwjgl3Input) {
			return ((Lwjgl3Input)o).getName().equals(getName());
		}
		else if(o instanceof String) {
			return ((String)o).equals(this.getName());
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}
	
	public abstract void reload();
}
