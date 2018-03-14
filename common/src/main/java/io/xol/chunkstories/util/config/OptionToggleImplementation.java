package io.xol.chunkstories.util.config;

import java.io.IOException;

import io.xol.chunkstories.api.util.Configuration.OptionBoolean;

public class OptionToggleImplementation extends OptionImplementation implements OptionBoolean {

	public OptionToggleImplementation(ConfigurationImplementation config, OptionUntyped loadFromThat) throws IOException {
		super(config, loadFromThat);
	}

	@Override
	public boolean getBoolValue() {
		return parse(this.getValue());
	}

	@Override
	public void toggle() {
		this.trySetting("" + !getBoolValue());
	}

	private boolean parse(String s) {
		try {
			return Boolean.parseBoolean(s);
		} catch(NumberFormatException e) {
			return false;
		}
	}
}
