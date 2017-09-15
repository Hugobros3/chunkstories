package io.xol.chunkstories.updater;

//(c) 2015 XolioWare Interactive

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

	public Map<String, String> localMap = new HashMap<String, String>();
	public Map<String, String> remoteMap = new HashMap<String, String>();

	public void checkVersion() {
		File f = new File(GameDirectory.getGameFolderPath() + "/version.txt");
		if (f.exists()) {
			try {
				InputStream ips = new FileInputStream(f);
				InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
				BufferedReader br = new BufferedReader(ipsr);
				String ligne;
				while ((ligne = br.readLine()) != null) {
					ChunkStoriesLauncher.version += ligne;
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			ChunkStoriesLauncher.version = "unknow";

		ChunkStoriesLauncher.lastVersion = ChunkStoriesLauncher
				.sendPost("http://chunkstories.xyz/api/updater/current/version.txt", "");
		ChunkStoriesLauncher.label.setText("Local version : " + ChunkStoriesLauncher.version + " - Last version : "
				+ ChunkStoriesLauncher.lastVersion);

		if (ChunkStoriesLauncher.version.equals("unknow")) {
			// Start DL immediately
			startDownloadingUpdate();
		}
		else if (!ChunkStoriesLauncher.version.equals(ChunkStoriesLauncher.lastVersion)) {

			int dialogButton = JOptionPane.YES_NO_OPTION;
			int dialogResult = JOptionPane.showConfirmDialog(null,
					"New version avaible (" + ChunkStoriesLauncher.lastVersion + ") do you want to download it ?",
					"Warning", dialogButton);
			if (dialogResult == JOptionPane.YES_OPTION) {
				startDownloadingUpdate();
			}
		}

		if (ChunkStoriesLauncher.version.equals(ChunkStoriesLauncher.lastVersion))
			ChunkStoriesLauncher.update.setText("Force update");
	}

	public void computeMaps() {
		ChunkStoriesLauncher.label.setText("Computing files hashes");
		localMap.clear();
		remoteMap.clear();
		browseDirsSafe(GameDirectory.getGameFolderPath());
		String rslt = ChunkStoriesLauncher.sendPost("http://chunkstories.xyz/api/updater/index.php", "");
		for (String part : rslt.split(";")) {
			if (part.contains(":")) {
				String data[] = part.split(":");
				remoteMap.put(data[0], data[1]);
			}
		}
		ChunkStoriesLauncher.progress.setMaximum(remoteMap.size());
		System.out.println(localMap.size() + " files in local map," + remoteMap.size() + " in remote one");
	}

	public UpdaterThread() {

		String[] ban = { "worlds", "screenshots", "logs", "skyboxscreens" };
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
					localMap.put(dir.replace(GameDirectory.getGameFolderPath(), "") + "/" + f2.getName(), getMD5(f3));
				/*
				 * if(f2.getName().endsWith(".sector"))
				 * System.out.println(dir+f2.getName()+":"+getMD5(f3)+":");
				 */
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
					localMap.put(dir.replace(GameDirectory.getGameFolderPath(), "") + "/" + f2.getName(), getMD5(f3));
				// System.out.println(dir.replace(GameDirectory.getGameFolderPath(),
				// "")+"/"+f2.getName());
				/*
				 * if(f2.getName().endsWith(".sector"))
				 * System.out.println(dir+f2.getName()+":"+getMD5(f3)+":");
				 */
			}
		}
	}

	public void run() {
		computeMaps();
		int i = 0;
		int initSize = remoteMap.size();
		/*
		 * for(String lel : remoteMap.keySet()) { System.out.println(lel+" -> "
		 * +localMap.get(lel)); }
		 */
		while (!isDone()) {
			if (remoteMap.size() == 0)
				setDone();
			else {
				String currentFile = (String) remoteMap.keySet().toArray()[0];
				ChunkStoriesLauncher.label
						.setText("Doing " + currentFile + " ( file " + i + " out of " + initSize + " )");
				// System.out.println(currentFile+":"+localMap.get(2));
				if (localMap.containsKey("/" + currentFile)) {
					if (!localMap.get("/" + currentFile).equals(remoteMap.get(currentFile))) {
						// System.out.println(currentFile+" ->
						// "+localMap.get("/"+currentFile)+" !=
						// "+remoteMap.get(currentFile));
						downloadFile(currentFile);
					} else
						ChunkStoriesLauncher.label.setText("File " + currentFile + " already ok.");
					remoteMap.remove(currentFile);
				} else
					downloadFile(currentFile);
				i++;
				ChunkStoriesLauncher.progress.setValue(i);
			}
		}
		ChunkStoriesLauncher.label.setText("Game updated to " + ChunkStoriesLauncher.lastVersion + " !");
	}

	private void downloadFile(String file) {
		ChunkStoriesLauncher.label.setText("Downloading  " + file + "...");
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			int size = 0;
			int dled = 0;
			URL url = new URL("http://chunkstories.xyz/api/updater/current/" + file.replace(" ", "%20"));
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
				ChunkStoriesLauncher.progress
						.setString("Downloading " + file + " " + dled / 1000 + "/" + size / 1000 + "kb");
			}
			in.close();
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		remoteMap.remove(file);
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
