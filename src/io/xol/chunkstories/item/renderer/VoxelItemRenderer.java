package io.xol.chunkstories.item.renderer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;

import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.core.ItemVoxel;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.chunkstories.world.World;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.gui.GuiDrawer;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.engine.textures.Texture;
import io.xol.engine.textures.TexturesHandler;

public class VoxelItemRenderer implements ItemRenderer
{
	ItemVoxel itemVoxel;
	Matrix4f transformation = new Matrix4f();
	
	public VoxelItemRenderer(ItemVoxel itemVoxel)
	{
		this.itemVoxel = itemVoxel;
	}

	@Override
	public void renderItemInInventory(RenderingContext context, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		int slotSize = 24 * scaling;
		ShaderProgram program = ShadersLibrary.getShaderProgram("inventory_blockmodel");
		context.setCurrentShader(program);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		
		program.setUniformFloat2("screenSize", XolioWindow.frameW, XolioWindow.frameH);
		program.setUniformFloat2("dekal", screenPositionX + pile.getItem().getSlotsWidth() * slotSize / 2 , screenPositionY + pile.getItem().getSlotsHeight() * slotSize / 2);
		program.setUniformFloat("scaling", slotSize / 1.65f);
		transformation.setIdentity();
		transformation.rotate(toRad(-22.5f), new Vector3f(1.0f, 0.0f, 0.0f));
		transformation.rotate(toRad(45f), new Vector3f(0.0f, 1.0f, 0.0f));
		transformation.translate(new Vector3f(-0.5f, -0.5f, -0.5f));
		//transformation.rotate(45f, new Vector3f(0.0f, 1.0f, 1.0f));
		//transformation.m02 = -0.5f;
		transformation.scale(new Vector3f(1f, 1f, 1f));
		program.setUniformMatrix4f("transformation", transformation);
		Voxel voxel = itemVoxel.getVoxel(pile);
		if(voxel == null)
		{
			int width = slotSize * pile.item.getSlotsWidth();
			int height = slotSize * pile.item.getSlotsHeight();
			GuiDrawer.drawBoxWindowsSpaceWithSize(screenPositionX, screenPositionY, width, height, 0, 1, 1, 0, TexturesHandler.getTexture("res/items/icons/notex.png").getID(), true, true, null);
			return;
		}
		BlockRenderInfo bri = new BlockRenderInfo(0);
		bri.data = VoxelFormat.format(0, itemVoxel.getVoxelMeta(pile), 15, voxel.getLightLevel(0));
		
		VoxelTexture voxTexture = voxel.getVoxelTexture(bri.data, 0, bri);
		program.setUniformFloat2("texBase", ((float)voxTexture.atlasS)/32768, ((float)voxTexture.atlasT)/32768);
		program.setUniformFloat2("texScaling", ((float)voxTexture.atlasOffset)/32768, ((float)voxTexture.atlasOffset)/32768);
		
		Texture texture = TexturesHandler.getTexture("./res/textures/tiles_merged_diffuse.png");
		texture.setLinearFiltering(false);
		context.setDiffuseTexture(texture.getID());
		VoxelModel model = voxel.getVoxelModel(bri);
		if(model == null || !voxel.isVoxelUsingCustomModel())
		{
			model = VoxelModels.getVoxelModel("default");
		}
		assert model != null;
		context.renderDirect(model.vertices, model.texCoords, null, model.normals);
	}

	private float toRad(float f)
	{
		return (float) (f / 180 * Math.PI);
	}

	@Override
	public void renderItemInWorld(RenderingContext context, ItemPile pile, World world, Matrix4f handTransformation)
	{
		
	}

}
