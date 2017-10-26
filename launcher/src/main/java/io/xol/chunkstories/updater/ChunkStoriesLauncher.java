package io.xol.chunkstories.updater;

//(c) 2015 XolioWare Interactive

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import io.xol.chunkstories.content.GameDirectory;

//import io.xol.chunkstories.content.GameDirectory;

public class ChunkStoriesLauncher{
	
	public static JLabel label = new JLabel();
	static JPanelBackground panel;
	
	static JButton update = new JButton("Update");
	static JButton play = new JButton("Play");
	
	public static JProgressBar progress = new JProgressBar();
	static UpdaterThread thread = new UpdaterThread();
	
	public static VersionFile localVersion;
	public static VersionFile latestVersion;
	//public static String version = "";
	//public static String lastVersion = "";
	
	public static JEditorPane browser = new JEditorPane();

	private static String launcherVersion() {
		
		//If compiled jar, it has this
		String ver = ChunkStoriesLauncher.class.getPackage().getImplementationVersion();
		if(ver != null)
			return ver;
		
		return "[local build]";
	}
	
	public static void main(String[] args) {
		panel = new JPanelBackground();
		GameDirectory.initClientPath();
		GameDirectory.check();
		
		JFrame window = new JFrame();
		//window.setUndecorated(true); // Remove title bar
		window.setSize(720, 480);
		window.setTitle("Chunk Stories Launcher " + launcherVersion());
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setContentPane(panel);
		window.setResizable(false);
		window.setLayout(null);
		try {
			window.setIconImage(ImageIO.read(ChunkStoriesLauncher.class.getResourceAsStream("/gfx/icon128.png")));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//
		browser.setContentType("text/html");
		browser.setBorder(null);
		browser.setEditable(false);
		try {
			browser.setPage(new URL("http://chunkstories.xyz/updates.php"));
		} catch (IOException e) {
			e.printStackTrace();
		}

        JScrollPane scrollPane = new JScrollPane(browser);
        panel.add(scrollPane);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBounds(0, 0, 720-8, 360);
		
		panel.add(update);
		update.setBounds(590, 360+12+8, 112, 24);
		panel.add(play);
		play.setBounds(590, 360+12+32+8, 112, 24);
		
		
		progress.setSize(390, 20);
		update.addActionListener(thread);
		play.addActionListener(new GameLauncher(window));
		
		progress.setVisible(false);
		
		progress.setStringPainted(true);
		label.setForeground(Color.white);
		panel.add(label);
		label.setBounds(240+16, 380+24, 320, 20);
		panel.add(progress);
		progress.setBounds(240+16, 380, 320, 20);
		
		window.setVisible(true);
		
		thread.checkVersion();

		if(args.length > 0 && args[0].equals("-auto"))
		{
			update.setEnabled(false);
			thread.start();
		}
		
		if(args.length > 0 && args[0].equals("-argtest"))
		{
			window.setTitle("argtest");
		}
	}
	
	public static String sendPost(String address, String params)
	{
		boolean https = false;
		try{
			URL url = new URL(address);
			https = address.startsWith("https://");
			if(!https)
			{
				HttpURLConnection htc = (HttpURLConnection) url.openConnection();
				htc.setRequestMethod("POST");
				htc.setRequestProperty("Accept-Charset", "UTF-8");
				htc.setRequestProperty("charset", "UTF-8");
				htc.setDoOutput(true);
				htc.setConnectTimeout(5000);
				htc.setReadTimeout(15000);
				DataOutputStream out = new DataOutputStream(htc.getOutputStream());
				out.writeBytes(params);
				out.flush();
				out.close();
				//get response
				BufferedReader in = new BufferedReader(new InputStreamReader(htc.getInputStream()));
				StringBuffer rslt = new StringBuffer();
				String line;
				while((line = in.readLine()) != null)
				{
					rslt.append(line);
				}
				in.close();
				return rslt.toString();
			}
			else
			{
				HttpsURLConnection htc = (HttpsURLConnection) url.openConnection();
				htc.setRequestMethod("POST");
				htc.setRequestProperty("Accept-Charset", "UTF-8");
				htc.setRequestProperty("charset", "UTF-8");
				htc.setDoOutput(true);
				htc.setConnectTimeout(5000);
				htc.setReadTimeout(15000);
				DataOutputStream out = new DataOutputStream(htc.getOutputStream());
				out.writeBytes(params);
				out.flush();
				out.close();
				//get response
				BufferedReader in = new BufferedReader(new InputStreamReader(htc.getInputStream()));
				StringBuffer rslt = new StringBuffer();
				String line;
				while((line = in.readLine()) != null)
				{
					rslt.append(line);
				}
				in.close();
				return rslt.toString();
			}
		}
		catch(Exception e)
		{
			System.out.println("Coudln't perform POST request on "+address+", ("+e.getMessage()+") stack trace above :");
			return "request failed : "+e.getMessage()+"";
		}
	}
}
