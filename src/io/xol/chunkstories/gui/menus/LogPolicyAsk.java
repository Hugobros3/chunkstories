package io.xol.chunkstories.gui.menus;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.font.BitmapFont;
import io.xol.engine.font.FontRenderer2;
import io.xol.engine.font.TrueTypeFont;
import io.xol.engine.gui.GuiElementsHandler;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LogPolicyAsk extends Overlay
{
	public LogPolicyAsk(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(acceptButton);
		guiHandler.add(denyButton);
	}

	GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button acceptButton = new Button(0, 0, 300, 32, ("I'm ok with this"), BitmapFont.SMALLFONTS, 1);
	Button denyButton = new Button(0, 0, 300, 32, ("No thanks."), BitmapFont.SMALLFONTS, 1);
	
	String message = "English: \n"
			+ "Welcome to the indev version of Chunk Stories !\n"
			+ "The whole point of having an early access title is finding and fixing bugs and crashes, and this "
			+ "often requires you sending us informations about your computer and how the game runs on it.\n"
			+ "We have an "
			+ "automatic log uploading system that uploads your .log file after you're done playing. This file can contain "
			+ "information about your game path ( reflecting likely your username ), your operation system, your CPU/RAM/GPU combo,"
			+ "your IP address (we have that one already think about it) and whatever driver/crashes related stuff it may encounter during runtime."
			+ "\nObviously the only thing we'll ever use these files for is debuging purposes and you can chose wether you are ok with that or not."
			+ "\n\n"
			+ "Français:  \n"
			+ "Bienvenue sur l'alpha de Chunk Stories !\n"
			+ "Tout l'intérêt d'une version early access est de trouver et résoudre les bugs et crashs, et ceci requiert souvent "
			+ "de nous envoyer des informations sur votre ordinateur et sur comment le jeu fonctionne dessus.\n"
			+ "Nous avons un système d'envoi de logs automatique qui s'active une fois le jeu fermé. Ces fichiers contiennent des "
			+ "informations sur le répertoire d'installation du jeu ( reflétant votre username ), votre système d'exploitation, votre "
			+ "configuration matérielle, votre addresse IP ( on l'a déjà ) et quelquonque erreur/crash lié aux drivers que le jeu peut "
			+ "recontrer pendant son exécution.\n"
			+ "Evidement la seule utilisation que nous auront pour ces fichiers sera le débbugage et vous pouvez choisir de désactiver cette fonctionalité."
			;
	
	@Override
	public void drawToScreen(int positionStartX, int positionStartY, int width, int height)
	{
		ObjectRenderer.renderColoredRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, "000000", 0.5f);
		
		FontRenderer2.drawTextUsingSpecificFont(30, GameWindowOpenGL.windowHeight-64, 0, 64, "Chunk Stories indev log policy", BitmapFont.SMALLFONTS);
		
		int linesTaken = TrueTypeFont.arial12.getLinesHeight(message, (width-128) / 2 );
		float scaling = 2;
		if(linesTaken*32 > height)
			scaling  = 1f;
		
		TrueTypeFont.arial12.drawString(30, GameWindowOpenGL.windowHeight-128, message, scaling, width-128, scaling);
		
		//FontRenderer2.drawTextUsingSpecificFont(30, 100, 0, 32, message, BitmapFont.SMALLFONTS);
		//FontRenderer2.setLengthCutoff(false, width - 128);
		
		acceptButton.setPosition(GameWindowOpenGL.windowWidth/2 - 256, GameWindowOpenGL.windowHeight / 4 - 32);
		acceptButton.draw();

		if (acceptButton.clicked())
		{
			mainScene.changeOverlay(this.parent);
			Client.clientConfig.setProp("log-policy", "send");
			Client.clientConfig.save();
		}
		
		denyButton.setPosition(GameWindowOpenGL.windowWidth/2 + 256, GameWindowOpenGL.windowHeight / 4 - 32);
		denyButton.draw();

		if (denyButton.clicked())
		{
			mainScene.changeOverlay(this.parent);
			Client.clientConfig.setProp("log-policy", "dont");
			Client.clientConfig.save();
		}
	}
	
	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}

}
