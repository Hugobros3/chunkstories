package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.events.EntityDeathEvent;
import io.xol.chunkstories.core.events.PlayerDeathEvent;
import io.xol.chunkstories.server.Server;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Handles the most important point of being a EntityLiving: being able to die.
 */
public class EntityComponentHealth extends EntityComponent
{
	private EntityLiving entityLiving;

	private float value;

	public EntityComponentHealth(EntityLiving entity, float health)
	{
		super(entity);
		this.entityLiving = entity;
		this.value = health;
	}

	public float getHealth()
	{
		return value;
	}

	public void setHealth(float health)
	{
		boolean wasntDead = value > 0.0;
		this.value = health;

		if (health <= 0.0 && wasntDead)
			handleDeath();

		if (entity.getWorld() instanceof WorldMaster)
		{
			if (health > 0.0)
				this.pushComponentController();
			else
				this.pushComponentEveryone();
		}
	}

	public void damage(float dmg)
	{
		boolean wasntDead = value > 0.0;
		this.value -= dmg;

		if (value <= 0.0 && wasntDead)
			handleDeath();

		if (entity.getWorld() instanceof WorldMaster)
		{
			if (value > 0.0)
				this.pushComponentController();
			else
				this.pushComponentEveryone();
		}
	}

	private void handleDeath()
	{
		EntityDeathEvent entityDeathEvent = new EntityDeathEvent(entityLiving);
		entity.getWorld().getGameLogic().getPluginsManager().fireEvent(entityDeathEvent);

		//Handles cases of controlled player death
		if (entity instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) entity).getControllerComponent().getController();
			if (controller != null)
			{
				controller.setControlledEntity(null);

				//Serverside stuff
				if (controller instanceof Player)
				{
					Player player = (Player) controller;

					PlayerDeathEvent event = new PlayerDeathEvent(player);
					entity.getWorld().getGameLogic().getPluginsManager().fireEvent(event);

					//When a player dies, delete his save as well
					File playerSavefile = new File("./players/" + player.getName().toLowerCase() + ".csf");
					if (playerSavefile.exists())
					{
						//Player save file is deleted upon death
						playerSavefile.delete();
					}

					if (event.getDeathMessage() != null)
						player.getServer().broadcastMessage(event.getDeathMessage());
				}
				else
				{
					//Weird, undefined cases ( controller wasn't a player, maybe some weird mod logic here
				}
			}
		}
	}

	@Override
	public void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeFloat(value);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		value = dis.readFloat();
	}

}
