package io.xol.chunkstories.updater;

//(c) 2015 XolioWare Interactive

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class GameLauncher implements ActionListener{

	@Override
	public void actionPerformed(ActionEvent ee) {
		System.out.println("Starting game");
		try {
			Runtime.getRuntime().exec("java -Xmx1G -jar chunkstories.jar -cd=2048", null, new File(GameDirectory.getGameFolderPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

}
