package io.xol.chunkstories.util.config;

import java.io.IOException;

import io.xol.chunkstories.api.util.Configuration.OptionDouble;

public class OptionDoubleImplementation extends OptionImplementation implements OptionDouble {

	public OptionDoubleImplementation(ConfigurationImplementation config, OptionUntyped loadFromThat) throws IOException {
		super(config, loadFromThat);
	}

	@Override
	public double getDoubleValue() {
		return parse(this.getValue());
	}

	protected double parse(String s) {
		try {
			return Double.parseDouble(s);
		} catch(NumberFormatException e) {
			return 0.0;
		}
	}

}
