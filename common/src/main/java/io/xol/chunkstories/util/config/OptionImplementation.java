//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map.Entry;

import io.xol.chunkstories.api.events.config.OptionSetEvent;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.util.Configuration.Option;
import io.xol.chunkstories.content.GenericNamedConfigurable;

public class OptionImplementation extends GenericNamedConfigurable implements Option {

	final ConfigurationImplementation config;
	
	public OptionImplementation(ConfigurationImplementation config, String name, BufferedReader reader) throws IOException {
		super(name, reader);
		this.config = config;
		
		if(config == null)
			throw new NullPointerException();
	}
	
	public OptionImplementation(ConfigurationImplementation config, OptionUntyped loadFromThat) throws IOException {
		super(loadFromThat.getName());
		this.config = config;

		if(config == null)
			throw new NullPointerException();
		
		for(Entry<String, String> e : loadFromThat.properties.entrySet()) {
			this.properties.put(e.getKey(), e.getValue());
		}
		
		this.defaultValue = this.resolveProperty("default", "undefined");
		this.value = null;
	}

	protected String defaultValue;
	protected String value;

	@Override
	public String getValue() {
		return value == null ? defaultValue : value;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void trySetting(String value) {
		String previousValue = value;
		this.value = value;
		config.bake();
		
		PluginManager pm = config.getContext().getPluginManager();
		if(pm != null) {
			OptionSetEvent event = new OptionSetEvent(this);
			pm.fireEvent(event);
			
			if(event.isCancelled()) {
				this.value = previousValue;
				config.bake();
				return;
			}
		}
		
		config.bake();
	}


}
