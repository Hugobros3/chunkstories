//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.util.Configuration;

public class ConfigurationImplementation implements Configuration {

	Map<String, Option> options = new HashMap<>();
	Map<String, Integer> bakedInts = new HashMap<>();
	Map<String, Double> bakedDoubles = new HashMap<>();
	Map<String, Boolean> bakedBools = new HashMap<>();
	Map<String, String> bakedString = new HashMap<>();

	private final GameContext context;
	private final Content content;
	private final File configFile;

	private static final Logger logger = LoggerFactory.getLogger("configuration");

	public Logger logger() {
		return logger;
	}

	public ConfigurationImplementation(GameContext context, File configFile) {
		this.context = context;
		this.content = context.getContent();
		this.configFile = configFile;

		reload();
		load();
	}

	public GameContext getContext() {
		return context;
	}

	public void reload() {
		options.clear();

		Iterator<Asset> i = content.modsManager().getAllAssetsByExtension("options");
		while (i.hasNext()) {
			Asset f = i.next();
			logger().debug("Reading options definitions in : " + f);
			readOptionsDefinitions(f);
		}

		options.values().forEach(o -> System.out.println(o.getName()));

		bake();
	}

	private void readOptionsDefinitions(Asset f) {
		if (f == null)
			return;
		try {
			BufferedReader reader = new BufferedReader(f.reader());
			String line = "";

			// ItemTypeImpl currentItemType = null;
			while ((line = reader.readLine()) != null) {
				line = line.replace("\t", "");
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				} else if (line.startsWith("end")) {
					// if (currentItemType == null)
					{
						logger().warn("Syntax error in file : " + f + " : ");
						continue;
					}
				} else if (line.startsWith("option")) {
					if (line.contains(" ")) {
						String[] split = line.split(" ");
						String itemName = split[1];

						try {
							OptionUntyped fakeOption = new OptionUntyped(this, itemName, reader);
							OptionImplementation option;

							if (fakeOption.resolveProperty("type").equals("scale"))
								option = new OptionScaleImplementation(this, fakeOption);
							else if (fakeOption.resolveProperty("type").equals("toggle"))
								option = new OptionToggleImplementation(this, fakeOption);
							else if (fakeOption.resolveProperty("type").equals("choice"))
								option = new OptionChoiceImplementation(this, fakeOption);
							else if (fakeOption.resolveProperty("type").equals("int"))
								option = new OptionIntImplementation(this, fakeOption);
							else if (fakeOption.resolveProperty("type").equals("double"))
								option = new OptionDoubleImplementation(this, fakeOption);
							// else if(option.resolveProperty("type").equals("string"))
							else
								option = new OptionImplementation(this, fakeOption);

							options.put(option.getName(), option);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load() {
		if (!configFile.exists())
			return;
		try {
			InputStream ips = new FileInputStream(configFile);
			InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("=") && !line.endsWith("=")) {
					Option option = (Option) this.getOption(line.split("=")[0]);
					if (option != null) {
						option.trySetting(line.split("=")[1]);
						System.out.println("set " + option.getName() + "to " + line.split("=")[1]);
					}
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.bake();
	}

	public void save() {
		File parentFolder = this.configFile.getParentFile();
		parentFolder.mkdirs();

		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));

			Map<String, String> props = new HashMap<>();
			this.allOptions().forEach(o -> props.put(o.getName(), o.getValue()));

			Set<String> unsortedKeys = props.keySet();
			List<String> sortedKeys = new ArrayList<String>(unsortedKeys);
			sortedKeys.sort(new Comparator<String>() {
				@Override
				public int compare(String arg0, String arg1) {
					return arg0.compareTo(arg1);
				}

			});
			for (String key : sortedKeys) {
				out.write(key + "=" + props.get(key) + "\n");
			}
			out.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Option getOption(String optionName) {
		return options.get(optionName);
	}

	@Override
	public boolean getBooleanOption(String optionName) {
		return this.bakedBools.getOrDefault(optionName, false);
	}

	@Override
	public int getIntOption(String optionName) {
		return this.bakedInts.getOrDefault(optionName, 0);
	}

	@Override
	public double getDoubleOption(String optionName) {
		return this.bakedDoubles.getOrDefault(optionName, 0.0);
	}

	@Override
	public String getStringOption(String optionName) {
		return this.bakedString.getOrDefault(optionName, "undefined");
	}

	protected void bake() {
		this.bakedBools.clear();
		this.bakedInts.clear();
		this.bakedDoubles.clear();
		this.bakedString.clear();

		for (Option o : allOptions()) {
			try {
				boolean casted = Boolean.parseBoolean(o.getValue());
				bakedBools.put(o.getName(), casted);
			} catch (NumberFormatException nfe) {
			}

			try {
				int casted = Integer.parseInt(o.getValue());
				bakedInts.put(o.getName(), casted);
			} catch (NumberFormatException nfe) {
			}

			try {
				double casted = Double.parseDouble(o.getValue());
				bakedDoubles.put(o.getName(), casted);
			} catch (NumberFormatException nfe) {
			}

			bakedString.put(o.getName(), o.getValue());
		}
	}

	@Override
	public Collection<Option> allOptions() {
		return options.values();
	}

	public void addOption(Option option) {
		this.options.put(option.getName(), option);
		bake();
	}

}
