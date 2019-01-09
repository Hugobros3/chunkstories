//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.input.InputsManager;
import xyz.chunkstories.input.InputVirtual;
import xyz.chunkstories.input.InputsLoaderHelper;
import xyz.chunkstories.input.InputsManagerLoader;
import xyz.chunkstories.server.player.ServerPlayer;

public class ServerInputsManager implements InputsManager, InputsManagerLoader {
	Set<Input> inputs = new HashSet<Input>();
	Map<Long, Input> inputsMap = new HashMap<Long, Input>();

	private final ServerPlayer player;

	public ServerInputsManager(ServerPlayer serverPlayer) {
		this.player = serverPlayer;

		reload();
	}

	public Input getInputByName(String bindName) {
		for (Input keyBind : inputs) {
			if (keyBind.getName().equals(bindName))
				return keyBind;
		}
		return null;
	}

	@Override
	public Iterator<Input> getAllInputs() {
		return inputs.iterator();
	}

	public Input getInputFromHash(long hash) {
		return inputsMap.get(hash);
	}

	public void reload() {
		inputs.clear();
		inputsMap.clear();

		// Load all keys as virtual ones
		InputsLoaderHelper.loadKeyBindsIntoManager(this, player.getContext().getContent().modsManager());

		// Add virtual mouse buttons
		InputVirtual mouseLeft = new InputVirtual("mouse.left", 0);
		inputs.add(mouseLeft);
		inputsMap.put(mouseLeft.getHash(), mouseLeft);
		InputVirtual mouseRight = new InputVirtual("mouse.right", 1);
		inputs.add(mouseRight);
		inputsMap.put(mouseRight.getHash(), mouseRight);
		InputVirtual mouseMiddle = new InputVirtual("mouse.middle", 2);
		inputs.add(mouseMiddle);
		inputsMap.put(mouseMiddle.getHash(), mouseMiddle);
	}

	public void insertInput(String type, String name, String value, Collection<String> arguments) {
		Input input;
		// Server treats everything as a virtual bind, because everything is abstract
		// for him
		// 2deep4me
		if (type.equals("keyBind") || type.equals("virtual")) {
			input = new InputVirtual(name);
		} else
			return;

		inputs.add(input);
		inputsMap.put(input.getHash(), input);
	}

}
