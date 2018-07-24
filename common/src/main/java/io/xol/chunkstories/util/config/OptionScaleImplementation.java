//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.config;

import java.io.IOException;

import io.xol.chunkstories.api.util.Configuration.ScaleOption;

public class OptionScaleImplementation extends OptionDoubleImplementation implements ScaleOption {

	double min, max, granularity;

	public OptionScaleImplementation(ConfigurationImplementation config, OptionUntyped loadFromThat)
			throws IOException {
		super(config, loadFromThat);

		this.min = parse(this.resolveProperty("min", "0.0"));
		this.max = parse(this.resolveProperty("max", "1.0"));
		this.granularity = parse(this.resolveProperty("granularity", "0.1"));
	}

	@Override
	public void trySetting(String value) {
		double val = parse(value);
		val -= val % granularity;
		super.trySetting(val + "");
	}

	@Override
	public double getMinimumValue() {
		return min;
	}

	@Override
	public double getMaximumValue() {
		return max;
	}

	@Override
	public double getGranularity() {
		return granularity;
	}
}
