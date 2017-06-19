package io.xol.chunkstories.input;

import java.util.Collection;

import io.xol.chunkstories.api.input.InputsManager;

public interface InputsManagerLoader extends InputsManager {
	public void insertInput(String type, String name, String value, Collection<String> arguments);
}
