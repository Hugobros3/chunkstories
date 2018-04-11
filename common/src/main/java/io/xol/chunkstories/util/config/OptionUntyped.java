//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.config;

import java.io.BufferedReader;
import java.io.IOException;

public class OptionUntyped extends OptionImplementation {

	public OptionUntyped(ConfigurationImplementation config, String name, BufferedReader reader) throws IOException {
		super(config, name, reader);
	}

}
