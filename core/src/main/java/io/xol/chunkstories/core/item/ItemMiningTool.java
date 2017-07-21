package io.xol.chunkstories.core.item;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.events.voxel.VoxelModificationCause;
import io.xol.chunkstories.api.events.voxel.VoxelModificationEvent;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.item.renderer.ItemRenderer;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemMiningTool extends Item implements VoxelModificationCause {

	public final String toolType;
	public final float miningEfficiency;
	
	public final long animationCycleDuration;
	
	private MiningProgress progress;
	public static MiningProgress myProgress;
	
	public ItemMiningTool(ItemType type) {
		super(type);
		
		this.toolType = type.resolveProperty("toolType", "pickaxe");
		this.miningEfficiency = Float.parseFloat(type.resolveProperty("miningEfficiency", "0.5"));
		
		this.animationCycleDuration = Long.parseLong(type.resolveProperty("animationCycleDuration", "500"));
	}
	
	@Override
	public void tickInHand(Entity owner, ItemPile itemPile) {
		
		//if(!(owner.getWorld() instanceof WorldMaster))
		//	return;
		World world = owner.getWorld();
		if(owner instanceof EntityControllable) {
			EntityControllable owner2 = (EntityControllable)owner;
			Controller controller = owner2.getController();
			if(controller != null && controller instanceof Player) {
				InputsManager inputs = controller.getInputsManager();
				
				Location lookingAt = owner2.getBlockLookingAt(true);
				
				if(lookingAt != null && lookingAt.distance(owner.getLocation()) > 7f)
					lookingAt = null;
				
				if(inputs.getInputByName("mouse.left").isPressed()) {
					
					//Cancel mining if looking away or the block changed by itself
					if(lookingAt == null || (progress != null && (lookingAt.distance(progress.loc) > 0 || owner.getWorld().getVoxelData(progress.loc) != progress.startId))) {
						progress = null;
					}
					
					if(progress == null) {
						//Try starting mining something
						if(lookingAt != null)
							progress = new MiningProgress(lookingAt);
					} else {
						//Progress using efficiency / ticks per second
						progress.progress += ItemMiningTool.this.miningEfficiency / 60f / progress.materialHardnessForThisTool;

						if(progress.progress >= 1.0f) {
							if(owner.getWorld() instanceof WorldMaster) {
								//Check no one minds
								VoxelModificationEvent event = new VoxelModificationEvent(owner.getWorld().peek(progress.loc), 0, this);
								owner.getWorld().getGameContext().getPluginManager().fireEvent(event);
								
								//DO IT
								if(!event.isCancelled()) {
									Vector3d rnd = new Vector3d();
									for(int i = 0; i < 40; i++) {
										rnd.set(progress.loc);
										rnd.add(Math.random() * 0.98, Math.random() * 0.98, Math.random() * 0.98);
										world.getParticlesManager().spawnParticleAtPosition("voxel_frag", rnd);
										world.getSoundManager().playSoundEffect("sounds/gameplay/voxel_remove.ogg", progress.loc, 1.0f, 1.0f);
									}
									world.setVoxelData(progress.loc, 0, owner);
								}
							}
							
							progress = null;
						}
					}
				}
				else {
					progress = null;
				}
				
				Player player = (Player)controller;
				if(player.getContext() instanceof ClientInterface) {
					Player me = ((ClientInterface)player.getContext()).getPlayer();
					if(me.equals(player)) {
						myProgress = progress;
					}
				}
			}
		}
		
	}

	@Override
	public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer) {
		return new SwingToolRenderer(fallbackRenderer);
	}

	class SwingToolRenderer extends ItemRenderer {

		public SwingToolRenderer(ItemRenderer fallbackRenderer) {
			super(fallbackRenderer);
		}

		@Override
		public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world,
				Location location, Matrix4f transformation) {
			
			//Controlled by some player
			if(pile.getInventory() != null && pile.getInventory().getHolder() != null && pile.getInventory().getHolder() instanceof EntityControllable &&
					((EntityControllable)pile.getInventory().getHolder()).getController() != null) {
			
				Matrix4f rotated = new Matrix4f(transformation);

				Vector3f center = new Vector3f(0.0f, -0.5f, -100f);
				
				rotated.translate(-0.1f, 0.4f, -0.1f);
				MiningProgress progress = ((ItemMiningTool)pile.getItem()).progress;
				
				if(progress != null) {
					long elapsed = System.currentTimeMillis() - progress.started;
					float elapsedd = (float)elapsed;
					elapsedd /= (float)animationCycleDuration;
					
					if(elapsedd >= progress.timesSoundPlayed && elapsed > 50) {
						world.getSoundManager().playSoundEffect("sounds/gameplay/voxel_remove.ogg", progress.loc, 1.5f, 1.0f);
						progress.timesSoundPlayed++;
					}
					
					float swingCycle = (float) Math.sin(Math.PI * 2 * elapsedd + Math.PI);
					
					rotated.translate(center);
					rotated.rotate((float) (swingCycle), 0f, 0f, 1f);
					
					center.negate();
					rotated.translate(center);
				}
				
				//rotated.rotate((System.currentTimeMillis() % 100000) / 10000f, 0f, 0f, 1f);
				rotated.scale(2.0f);
				
				super.renderItemInWorld(renderingInterface, pile, world, location, rotated);
				
			}
			else
				super.renderItemInWorld(renderingInterface, pile, world, location, transformation);
		}
	}

	public class MiningProgress {
		
		public MiningProgress(Location loc) {
			this.loc = loc;
			this.startId = loc.getWorld().getVoxelData(loc);
			
			voxel = loc.getWorld().peek(loc).getVoxel();
			material = voxel.getMaterial();
			String hardnessString = null;
			
			//First order, check the voxel itself if it states a certain hardness for this tool type
			hardnessString = voxel.getType().resolveProperty("hardnessFor"+ItemMiningTool.this.toolType, null);
			
			//Then check if the voxel states a general hardness multiplier
			if(hardnessString == null)
				hardnessString = voxel.getType().resolveProperty("hardness", null);
			
			//if the voxel is devoid of information, we do the same on the material
			if(hardnessString == null)
				hardnessString = material.resolveProperty("materialHardnessFor"+ItemMiningTool.this.toolType, null);
			
			//Eventually we default to 1.0
			if(hardnessString == null)
				hardnessString = material.resolveProperty("materialHardness", "1.0");
			
			this.materialHardnessForThisTool = Float.parseFloat(hardnessString);
			
			this.progress = 0.0f;
			this.started = System.currentTimeMillis();
		}
		
		public final Voxel voxel;
		public final Material material;
		public final Location loc;
		public final int startId;
		public float progress;
		public final long started;
		
		public final float materialHardnessForThisTool;
		int timesSoundPlayed = 0;
	}
}
