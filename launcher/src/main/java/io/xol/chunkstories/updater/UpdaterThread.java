package io.xol.chunkstories.updater;

//(c) 2015 XolioWare Interactive

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import io.xol.chunkstories.content.GameDirectory;

public class UpdaterThread extends Thread implements ActionListener {

	public boolean done = false;

	public Map<String, String> localHashes = new HashMap<String, String>();
	public Map<String, String> remoteHashes = new HashMap<String, String>();
	
	public void checkVersion() {
		ChunkStoriesLauncher.localVersion = VersionFile.loadFromFile(new File(GameDirectory.getGameFolderPath()+"/version.txt"));
			
		try {
			ChunkStoriesLauncher.latestVersion = VersionFile.loadFromOnline();
		} catch (IOException e) {
			ChunkStoriesLauncher.latestVersion = new VersionFile("offline");
		}
		
		//System.out.println("Local version : " + ChunkStoriesLauncher.localVersion.version + " - Last version : " + ChunkStoriesLauncher.latestVersion.version);
		ChunkStoriesLauncher.label.setText("Local version : " + ChunkStoriesLauncher.localVersion.version + " - Last version : " + ChunkStoriesLauncher.latestVersion.version);

		if (ChunkStoriesLauncher.localVersion.version.equals("unknown")) {
			// Start DL immediately
			startDownloadingUpdate();
		}
		else if (!ChunkStoriesLauncher.localVersion.equals(ChunkStoriesLauncher.latestVersion)) {

			int dialogButton = JOptionPane.YES_NO_OPTION;
			int dialogResult = JOptionPane.showConfirmDialog(null,
					"New version avaible (" + ChunkStoriesLauncher.latestVersion.version + ") do you want to download it ?",
					"Warning", dialogButton);
			if (dialogResult == JOptionPane.YES_OPTION) {
				startDownloadingUpdate();
			}
		}

		if (ChunkStoriesLauncher.localVersion.equals(ChunkStoriesLauncher.latestVersion))
			ChunkStoriesLauncher.update.setText("Force update");
	}

	public void computeMaps() {
		ChunkStoriesLauncher.label.setText("Computing files hashes");
		localHashes.clear();
		remoteHashes.clear();
		browseDirsSafe(GameDirectory.getGameFolderPath());
		String remoteHashesText = ChunkStoriesLauncher.sendPost("https://chunkstories.xyz/api/updater/index.php", "");
		
		here:
		for (String part : remoteHashesText.split(";")) {
			if (part.contains(":")) {
				String data[] = part.split(":");
				
				for(String bannedWorld : banned) {
					if(data[0].startsWith(bannedWorld))
						continue here;
				}
				remoteHashes.put(data[0], data[1]);
			}
		}
		
		//Scale the progress bar to the amount of shit we have to download
		ChunkStoriesLauncher.progress.setMaximum(remoteHashes.size());
		System.out.println(localHashes.size() + " local tracked files, " + remoteHashes.size() + " in remote one");
	}

	public UpdaterThread() {
		String[] ban = { "worlds", "screenshots", "logs", "skyboxscreens", "plugins", "mods", "servermods", "config", "res", "lib", "players", "cache" };
		for (String s : ban)
			banned.add(s);
	}

	List<String> banned = new ArrayList<String>();

	private void browseDirsSafe(String dir) {
		File f = new File(dir);
		for (File f2 : f.listFiles()) {
			if (f2.isDirectory() && !banned.contains(f2.getName())) {
				browseDir(dir + "/" + f2.getName());
			} else if (!dir.startsWith("..")) {
				if (dir.equals("."))
					dir = "";
				File f3 = new File(dir + "/" + f2.getName());
				if (!f3.isDirectory())
					localHashes.put(dir.replace(GameDirectory.getGameFolderPath(), "") + "/" + f2.getName(), getMD5(f3));
			}
		}
	}

	private void browseDir(String dir) {
		File f = new File(dir);
		for (File f2 : f.listFiles()) {
			if (f2.isDirectory()) {
				browseDir(dir + "/" + f2.getName());
			} else if (!dir.startsWith("..")) {
				if (dir.equals("."))
					dir = "";
				File f3 = new File(dir + "/" + f2.getName());
				if (!f3.isDirectory())
					localHashes.put(dir.replace(GameDirectory.getGameFolderPath(), "") + "/" + f2.getName(), getMD5(f3));
			}
		}
	}

	public void run() {
		computeMaps();
		int i = 0;
		int initSize = remoteHashes.size();
		
		while (!isDone()) {
			if (remoteHashes.size() == 0)
				setDone();
			else {
				String currentFile = (String) remoteHashes.keySet().toArray()[0];
				ChunkStoriesLauncher.label.setText("Doing " + currentFile + " ( file " + i + " out of " + initSize + " )");
				
				if (localHashes.containsKey("/" + currentFile)) {
					if (!localHashes.get("/" + currentFile).equals(remoteHashes.get(currentFile))) {
						
						downloadFile(currentFile);
					} else
						ChunkStoriesLauncher.label.setText("File " + currentFile + " already ok.");
					remoteHashes.remove(currentFile);
				} else
					downloadFile(currentFile);
				i++;
				ChunkStoriesLauncher.progress.setValue(i);
			}
		}
		ChunkStoriesLauncher.label.setText("Game updated to " + ChunkStoriesLauncher.latestVersion.version + " !");
	}

	private void downloadFile(String file) {
		ChunkStoriesLauncher.label.setText("Downloading  " + file + "...");
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			int size = 0;
			int dled = 0;
			URL url = new URL("https://chunkstories.xyz/api/updater/current/" + file.replace(" ", "%20"));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.getInputStream();
			size = conn.getContentLength();

			InputStream inp = url.openStream();
			in = new BufferedInputStream(inp);
			File f = new File(GameDirectory.getGameFolderPath() + "/" + file);
			if (f.getParentFile() != null && !f.getParentFile().exists())
				f.getParentFile().mkdirs();
			fout = new FileOutputStream(f);

			final byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				fout.write(data, 0, count);
				dled += count;
				ChunkStoriesLauncher.progress.setString("Downloading " + file + " " + dled / 1000 + "/" + size / 1000 + "kb");
			}
			in.close();
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		remoteHashes.remove(file);
	}

	private synchronized boolean isDone() {
		return done;
	}

	private synchronized void setDone() {
		done = true;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		startDownloadingUpdate();
	}

	private void startDownloadingUpdate() {

		this.start();
		ChunkStoriesLauncher.update.setEnabled(false);
		ChunkStoriesLauncher.progress.setVisible(true);
	}

	public String getMD5(File file) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			FileInputStream fis = new FileInputStream(file);
			byte[] dataBytes = new byte[1024];

			int nread = 0;

			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			;

			byte[] mdbytes = md.digest();

			// convert the byte to hex format
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			fis.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
