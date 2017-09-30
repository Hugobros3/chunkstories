package io.xol.chunkstories.core.entity.medical;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

@SuppressWarnings("unused")
/** Wip class to implement some sort of medical component */
public class AdvancedMedicalComponent extends EntityComponent
{
	//Flashbang timer
	private int flashedForTicks = 0;
	
	//Breakable bones
	private boolean boneLeftLegStatus = false;
	private boolean boneRightLegStatus = false;
	private boolean boneLeftArmStatus = false;
	private boolean boneRightArmStatus = false;

	//Amount of blood/health lost per second
	private float bleeding = 0f;
	
	private float bodyTemperature = 273.15f + 37f;
	public final static float hypothermiaTemperature = 273.15f + 35.0f;
	
	public final static float bodyTemperatureLowerSafeBound = 273.15f + 35.5f;
	public final static float bodyTemperatureUpperSafeBound = 273.15f + 38.5f;
	
	public final static float heatDeathTemperature = 273.15f + 41f;
	
	private float coldIllnessLevel = 0f;
	
	/*private List<Illness> illnesses;
	
	abstract class Illness {
		
		public abstract String getName();
		
		public abstract void tick();
		
		public boolean equals(Object o)
		{	
			return (o instanceof Illness) && ((Illness)o).getName().equals(this);
		}
	}*/
	
	private EntityLiving entityLiving;

	public AdvancedMedicalComponent(EntityLiving entity, float health)
	{
		super(entity);
		this.entityLiving = entity;
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

}
