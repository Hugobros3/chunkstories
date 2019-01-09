//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.content.mods.ModsManager;

/** Eases internal classes from the burden of dealing with the file formats */
public class InputsLoaderHelper {
	public static MessageDigest md;

	static {
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static void loadKeyBindsIntoManager(InputsManagerLoader inputManager, ModsManager modsManager) {
		for(Asset asset : modsManager.getAllAssetsByExtension("inputs")) {
			loadKeyBindsFile(asset, inputManager);
		}
	}

	private static void loadKeyBindsFile(Asset asset, InputsManagerLoader inputManager) {
		if (asset == null)
			return;

		BufferedReader reader = new BufferedReader(asset.reader());

		List<String> arguments = new ArrayList<String>();
		String inputName;
		String inputValue;
		String inputType;

		// Read until we get a good one
		String line = "";
		try {
			while ((line = reader.readLine()) != null) {
				// System.out.println("Reading " + line);
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				} else {
					String splitted[] = line.split(" ");
					if (splitted.length >= 2) {
						arguments.clear();
						inputValue = null;

						inputType = splitted[0];
						inputName = splitted[1];
						if (splitted.length >= 3) {
							inputValue = splitted[2];
							for (int i = 3; i < splitted.length; i++)
								arguments.add(splitted[i]);
						}

						inputManager.insertInput(inputType, inputName, inputValue, arguments);
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(asset);
		}
	}
}
