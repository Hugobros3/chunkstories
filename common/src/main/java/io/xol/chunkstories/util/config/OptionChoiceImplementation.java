//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.xol.chunkstories.api.util.Configuration.ChoiceOption;

public class OptionChoiceImplementation extends OptionImplementation implements ChoiceOption {

	List<String> choices = new ArrayList<>();
	
	public OptionChoiceImplementation(ConfigurationImplementation config, OptionUntyped loadFromThat) throws IOException {
		super(config, loadFromThat);
		
		String choicesString = this.resolveProperty("choices");
		String choicesArray[] = choicesString.split(",");
		
		for(String choice : choicesArray) {
			this.choices.add(choice.replace(" ", ""));
		}
	}

	@Override
	public Collection<String> getPossibleChoices() {
		return choices;
	}
}
