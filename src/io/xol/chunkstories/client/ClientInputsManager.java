package io.xol.chunkstories.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.api.input.MouseButton;
import io.xol.chunkstories.input.Inputs;
import io.xol.chunkstories.input.KeyBindImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientInputsManager implements InputsManager
{
	Set<Input> inputs = new HashSet<Input>();
	Map<Long, Input> inputsMap = new HashMap<Long, Input>();

	public ClientInputsManager()
	{
		reload();
	}
	
	public Iterator<Input> getAllInputs()
	{
		return inputs.iterator();
	}

	/**
	 * Returns null or a KeyBind matching the name
	 * 
	 * @param keyCode
	 * @return
	 */
	public Input getInputByName(String bindName)
	{
		for (Input keyBind : inputs)
		{
			if (keyBind.getName().equals(bindName))
				return keyBind;
		}
		return null;
	}

	/**
	 * Returns null or a KeyBind matching the pressed key
	 * 
	 * @param keyCode
	 * @return
	 */
	public KeyBind getKeyBoundForLWJGL2xKey(int keyCode)
	{
		for (Input keyBind : inputs)
		{
			if (keyBind instanceof KeyBindImplementation && ((KeyBindImplementation) keyBind).getLWJGL2xKey() == keyCode)
				return (KeyBind) keyBind;
		}
		return null;
	}
	
	public Input getInputFromHash(long hash)
	{
		if(hash == 0)
			return MouseButton.LEFT;
		else if(hash == 1)
			return MouseButton.RIGHT;
		else if(hash == 2)
			return MouseButton.MIDDLE;
		
		return inputsMap.get(hash);
	}
	
	//Debug
	public void reloadBoundKeysFromConfig()
	{
		for (Input keyBind : inputs)
		{
			if (keyBind instanceof KeyBindImplementation)
				((KeyBindImplementation) keyBind).reload();
		}
	}

	public void reload()
	{
		inputs.clear();
		inputsMap.clear();
		Iterator<Input> i = Inputs.loadKeyBindsIntoManager(this);
		while(i.hasNext())
		{
			Input input = i.next();
			System.out.println("Loading input : "+input);
			inputs.add(input);
			inputsMap.put(input.getHash(), input);
		}
	}
}
