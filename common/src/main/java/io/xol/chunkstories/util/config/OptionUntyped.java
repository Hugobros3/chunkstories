package io.xol.chunkstories.util.config;

import java.io.BufferedReader;
import java.io.IOException;

public class OptionUntyped extends OptionImplementation {

	public OptionUntyped(ConfigurationImplementation config, String name, BufferedReader reader) throws IOException {
		super(config, name, reader);
	}

}
