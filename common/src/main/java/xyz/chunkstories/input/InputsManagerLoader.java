//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input;

import java.util.Collection;

import xyz.chunkstories.api.input.InputsManager;

public interface InputsManagerLoader extends InputsManager {
	public void insertInput(String type, String name, String value, Collection<String> arguments);
}
