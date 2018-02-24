//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.content;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.LocalizationManager;
import io.xol.chunkstories.api.content.Content.Translation;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.content.GameContentStore;



public class LocalizationManagerActual implements LocalizationManager
{
	// This class holds static model info

	private final GameContentStore store;
	private final ModsManager modsManager;
	
	private Map<String, Asset> translations = new HashMap<String, Asset>();
	private Translation activeTranslation;
	
	private String defaultTranslation;
	
	private final static Logger logger = LoggerFactory.getLogger("content.localization");
	
	public LocalizationManagerActual(GameContentStore store, String defaultTranslation)
	{
		this.store = store;
		this.modsManager = store.modsManager();
		
		this.defaultTranslation = defaultTranslation;
		//reload();
		
		//loadTranslation(activeTranslation);
	}

	public void loadTranslation(String translation)
	{
		activeTranslation = new ActualTranslation(translations.get(translation));
	}

	public void reload()
	{
		translations.clear();
		
		Iterator<Asset> i = modsManager.getAllAssetsByPrefix("./lang/");
		while(i.hasNext())
		{
			Asset a = i.next();
			if(a.getName().endsWith("lang.info"))
			{
				String abrigedName = a.getName().substring(7, a.getName().length() - 10);
				if(abrigedName.contains("/"))
					continue;
				//System.out.println("Found translation: "+abrigedName);
				translations.put(abrigedName, a);
			}
		}
		
		if(activeTranslation != null)
			activeTranslation = new ActualTranslation(((ActualTranslation)activeTranslation).a);
		else
			activeTranslation = new ActualTranslation(translations.get(defaultTranslation));
	}
	
	public class ActualTranslation implements Translation {

		public Asset a;
		private Map<String, String> strings = new HashMap<String, String>();
		
		public ActualTranslation(Asset a)
		{
			this.a = a;
			logger.info("Loading translation from asset asset: "+a);
			
			String prefix = a.getName().substring(0, a.getName().length() - 9);
			Iterator<Asset> i = modsManager.getAllAssetsByPrefix(prefix);
			while(i.hasNext())
			{
				Asset a2 = i.next();
				if(a2.getName().endsWith(".lang"))
				{
					try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(a2.read(), "UTF8"));
					String line = "";

						while ((line = reader.readLine()) != null)
						{
							String name = line.split(" ")[0];
							
							int indexOf = line.indexOf(" ");
							if(indexOf == -1)
								continue;
							String text = line.substring(indexOf + 1);
							text = text.replace("\\n", "\n");
							
							//System.out.println("name: "+name+" text: "+text);
							strings.put(name, text);
						}	
						reader.close();
					}
					catch(IOException e)
					{
						
					}
					
				}
			}
		}

		@Override
		public String getLocalizedString(String stringName)
		{
			String locStr = strings.get(stringName);
			return locStr != null ? locStr : "#{"+stringName+"}";
		}

		@Override
		public String localize(String text)
		{
			char[] array = text.toCharArray();
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < array.length; i++)
			{
				char c = array[i];
				if(c == '#')
				{
					if(i < array.length - 1 && array[i + 1] == '{')
					{
						int endIndex = text.indexOf("}", i + 1);
						String word = text.substring(i + 2, endIndex);
						//System.out.println("Found word: "+word);
						
						String translated = getLocalizedString(word);
						sb.append(translated != null ? translated : word);
						i+=word.length() + 2;
					}
					else
						sb.append(c);
				}
				else
					sb.append(c);
			}
			return sb.toString();
		}
		
	}

	@Override
	public String getLocalizedString(String stringName)
	{
		return activeTranslation.getLocalizedString(stringName);
	}

	@Override
	public String localize(String text)
	{
		return activeTranslation.localize(text);
	}

	@Override
	public Content parent()
	{
		return this.store;
	}

	@Override
	public Collection<String> listTranslations()
	{
		return translations.keySet();
	}

	@Override
	public Logger logger() {
		return logger;
	}
}
