//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.config;

import java.io.IOException;

import io.xol.chunkstories.api.util.Configuration.OptionInt;

public class OptionIntImplementation extends OptionImplementation implements OptionInt {

	public OptionIntImplementation(ConfigurationImplementation config, OptionUntyped loadFromThat) throws IOException {
		super(config, loadFromThat);
	}

	@Override
	public int getIntValue() {
		return parse(this.getValue());
	}

	private int parse(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
