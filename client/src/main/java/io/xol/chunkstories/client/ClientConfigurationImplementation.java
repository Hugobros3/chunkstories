//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import io.xol.chunkstories.util.config.ConfigurationImplementation;
import io.xol.chunkstories.util.config.OptionChoiceImplementation;
import io.xol.chunkstories.util.config.OptionUntyped;

public class ClientConfigurationImplementation extends ConfigurationImplementation {

	final Client client;
	
	public ClientConfigurationImplementation(Client client, File configFile) {
		super(client, configFile);
		
		this.client = client;
		this.addOption(getLanguageOption());
	}

	public OptionChoiceImplementation getLanguageOption() {
		String choices = "";
		
		Iterator<String> i = client.getContent().localization().listTranslations().iterator();
		while(i.hasNext()) {
			choices += i.next() + (i.hasNext() ? ", " : "");
		}
		
		String fakeDefinition = 
		//"option client.game.language\n" +
		"\ttype: choice\n" + 
		"\tdefault: en\n" + 
		"\tchoices: "+choices+"\n" +
		"end\n";
		
		StringReader sr = new StringReader(fakeDefinition);
		BufferedReader br = new BufferedReader(sr);
		
		try {
			OptionUntyped or = new OptionUntyped(this, "client.game.language", br);
			
			return new OptionChoiceImplementation(this, or);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
