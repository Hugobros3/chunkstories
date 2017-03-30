package io.xol.chunkstories.core.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.ai.ZombieAI;
import io.xol.chunkstories.renderer.WorldRenderer.RenderingPass;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.model.ModelLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityZombie extends EntityHumanoid
{
	ZombieAI zombieAi;
	//public final Stage stage;
	final StageComponent stageComponent;
	
	public enum Stage {
		INFECTION(0.045, 5, 1800, 10f, 40f),
		TAKEOVER(0.060, 10, 1200, 15f, 80f),
		WHOLESOME(0.075, 15, 800, 20f, 160f),
		;
		
		private Stage(double speed, double aggroDistance, int attackCooldown, float attackDamage, float hp)
		{
			this.speed = speed;
			this.aggroRadius = aggroDistance;
			this.attackCooldown = attackCooldown;
			this.attackDamage = attackDamage;
			this.hp = hp;
		}

		public final double speed;
		public final double aggroRadius;
		public final int attackCooldown;
		public final float attackDamage;
		public final float hp;
	}
	
	static class StageComponent extends EntityComponent {

		Stage stage;
		
		public StageComponent(Entity entity)
		{
			super(entity);
		}
		
		public String getSerializedComponentName() {
			return "stage";
		}

		@Override
		protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
		{
			dos.writeByte(stage.ordinal());
		}

		@Override
		protected void pull(StreamSource from, DataInputStream dis) throws IOException
		{
			byte ok = dis.readByte();
			int i = (int)ok;
			
			stage = Stage.values()[i];
		}

		public void setStage(Stage stage2)
		{
			this.stage = stage2;
			this.pushComponentEveryone();
		}
		
	}

	static Set<Class<? extends Entity>> zombieTargets = new HashSet<Class<? extends Entity>>();
	
	static {
		zombieTargets.add(EntityPlayer.class);
	}
	
	public EntityZombie(World world, double x, double y, double z)
	{
		this(world, x, y, z, Stage.values()[(int) Math.floor(Math.random() * Stage.values().length)]);
	}
	
	public EntityZombie(World world, double x, double y, double z, Stage stage)
	{
		super(world, x, y, z);
		zombieAi = new ZombieAI(this, zombieTargets);
		
		this.stageComponent = new StageComponent(this);
		this.stageComponent.setStage(stage);
		//this.stage = stage;
		this.setHealth(stage.hp);
	}

	@Override
	public float getStartHealth()
	{
		return 50;
	}
	
	@Override
	public void tick()
	{
		//AI works on master
		if (world instanceof WorldMaster)
			zombieAi.tick();
		
		//Ticks the entity
		super.tick();
		
		//Anti-glitch
		if(Double.isNaN(this.getEntityRotationComponent().getHorizontalRotation()))
		{
			System.out.println("nan !" + this);
			this.getEntityRotationComponent().setRotation(0.0, 0.0);
		}
	}

	class EntityZombieRenderer extends EntityHumanoidRenderer<EntityZombie> {
		
		@Override
		public void setupRender(RenderingInterface renderingContext)
		{
			super.setupRender(renderingContext);
		}
		
		@Override
		public int renderEntities(RenderingInterface renderingContext, RenderingIterator<EntityZombie> renderableEntitiesIterator)
		{
			setupRender(renderingContext);
			
			int e = 0;

			for (EntityZombie entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				Location location = entity.getPredictedLocation();

				if (renderingContext.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.SHADOW && location.distanceTo(renderingContext.getCamera().getCameraPosition()) > 15f)
					continue;

				entity.cachedSkeleton.lodUpdate(renderingContext);

				Matrix4f matrix = new Matrix4f();
				matrix.translate(location.castToSinglePrecision());
				renderingContext.setObjectMatrix(matrix);
				
				//Player textures
				Texture2D playerTexture = TexturesHandler.getTexture("./models/zombie_s"+(entity.stage().ordinal() + 1)+".png");
				playerTexture.setLinearFiltering(false);
				
				renderingContext.bindAlbedoTexture(playerTexture);
				
				ModelLibrary.getRenderableMesh("./models/human.obj").render(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
				//animationsData.add(new AnimatableData(location.castToSinglePrecision(), entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, bl, sl));
			}
			
			//Render items in hands
			for (EntityHumanoid entity : renderableEntitiesIterator)
			{

				if (renderingContext.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.SHADOW && entity.getLocation().distanceTo(renderingContext.getCamera().getCameraPosition()) > 15f)
					continue;

				ItemPile selectedItemPile = null;

				if (entity instanceof EntityWithSelectedItem)
					selectedItemPile = ((EntityWithSelectedItem) entity).getSelectedItemComponent().getSelectedItem();

				renderingContext.currentShader().setUniform3f("objectPosition", new Vector3fm(0));

				if (selectedItemPile != null)
				{
					Matrix4f itemMatrix = new Matrix4f();
					itemMatrix.translate(entity.getPredictedLocation().castToSinglePrecision());

					Matrix4f.mul(itemMatrix, entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000), itemMatrix);

					selectedItemPile.getItem().getType().getRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, entity.getLocation(), itemMatrix);
				}

				e++;
			}
			
			return e;
		}
	}
	
	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return new EntityZombieRenderer();
	}
	
	public Stage stage()
	{
		return stageComponent.stage;
	}

	@Override
	public float damage(DamageCause cause, HitBox osef, float damage)
	{
		if(!isDead())
			world.getSoundManager().playSoundEffect("sounds/sfx/entities/zombie/hurt.ogg", this.getLocation(), (float)Math.random() * 0.4f + 0.8f, 1.5f + Math.min(0.5f, damage / 15.0f));
		
		if(cause instanceof EntityLiving) {

			EntityLiving entity = (EntityLiving)cause;
			
			this.zombieAi.setAiTask(zombieAi.new AiTaskAttackEntity(entity, 15f, 20f, zombieAi.currentTask(), stage().attackCooldown, stage().attackDamage));
		}
		
		return super.damage(cause, osef, damage);
	}

	public void attack(EntityLiving target, float maxDistance)
	{
		this.zombieAi.setAiTask(zombieAi.new AiTaskAttackEntity(target, 15f, maxDistance, zombieAi.currentTask(), stage().attackCooldown, stage().attackDamage));
	}
}
