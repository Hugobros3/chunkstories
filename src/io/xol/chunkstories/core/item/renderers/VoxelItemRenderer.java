package io.xol.chunkstories.core.item.renderers;

import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;

import static org.lwjgl.opengl.GL11.*;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;
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
	
	Map<Integer, float[]> transformedTexCoords = new HashMap<Integer, float[]>();
	
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
		transformation.scale(new Vector3f(-1f, 1f, 1f));
		transformation.rotate(toRad(-22.5f), new Vector3f(1.0f, 0.0f, 0.0f));
		transformation.rotate(toRad(45f), new Vector3f(0.0f, 1.0f, 0.0f));
		transformation.translate(new Vector3f(-0.5f, -0.5f, -0.5f));
		//transformation.rotate(45f, new Vector3f(0.0f, 1.0f, 1.0f));
		//transformation.m02 = -0.5f;
		program.setUniformMatrix4f("transformation", transformation);
		Voxel voxel = ((ItemVoxel) pile.getItem()).getVoxel();
		if(voxel == null)
		{
			int width = slotSize * pile.item.getSlotsWidth();
			int height = slotSize * pile.item.getSlotsHeight();
			GuiDrawer.drawBoxWindowsSpaceWithSize(screenPositionX, screenPositionY, width, height, 0, 1, 1, 0, TexturesHandler.getTexture("res/items/icons/notex.png").getID(), true, true, null);
			return;
		}
		Texture texture = TexturesHandler.getTexture("./res/textures/tiles_merged_diffuse.png");
		texture.setLinearFiltering(false);
		context.setDiffuseTexture(texture.getID());
		
		BlockRenderInfo bri = new BlockRenderInfo(0);
		bri.data = VoxelFormat.format(voxel.getId(), ((ItemVoxel) pile.getItem()).getVoxelMeta(), 15, voxel.getLightLevel(0));
		bri.voxelType = VoxelTypes.get(bri.data);
		VoxelModel model = voxel.getVoxelModel(bri);
		if(model == null || !voxel.isVoxelUsingCustomModel())
		{
			model = VoxelModels.getVoxelModel("default");
		}
		renderVoxel(context, voxel, model, bri);
	}
	
	private void renderVoxel(RenderingContext context, Voxel voxel, VoxelModel model, BlockRenderInfo bri)
	{
		if(!transformedTexCoords.containsKey(bri.getMetaData() + 16 * voxel.getId()))
		{
			float[] transformTextures = new float[model.texCoords.length];

			VoxelTexture voxelTexture = voxel.getVoxelTexture(bri.data, 0, bri);
			int modelTextureIndex = 0;
			String voxelName = VoxelTypes.get(bri.data).getName();
			if(!model.texturesNames[modelTextureIndex].equals("~"))
				voxelTexture = VoxelTextures.getVoxelTexture(model.texturesNames[modelTextureIndex].replace("~", voxelName));
			int useUntil = model.texturesOffsets[modelTextureIndex];
			int textureS = voxelTexture.atlasS;// +mod(sx,texture.textureScale)*offset;
			int textureT = voxelTexture.atlasT;// +mod(sz,texture.textureScale)*offset;
			
			for(int i = 0; i < transformTextures.length; i+= 2)
			{
				int vertexIndice = i / 2;
				
				if(vertexIndice >= useUntil)
				{
					modelTextureIndex++;
					if(!model.texturesNames[modelTextureIndex].equals("~"))
						voxelTexture = VoxelTextures.getVoxelTexture(model.texturesNames[modelTextureIndex].replace("~", voxelName));
					else
						voxelTexture = bri.getTexture();
					useUntil = model.texturesOffsets[modelTextureIndex];
					textureS = voxelTexture.atlasS;// +mod(sx,texture.textureScale)*offset;
					textureT = voxelTexture.atlasT;// +mod(sz,texture.textureScale)*offset;
				}
				
				transformTextures[i] = (textureS + model.texCoords[i] * voxelTexture.atlasOffset) / 32768f;
				transformTextures[i + 1] = (textureT + model.texCoords[i + 1] * voxelTexture.atlasOffset) / 32768f;
			}
			transformedTexCoords.put(bri.getMetaData() + 16 * voxel.getId(), transformTextures);
		}
		if(transformedTexCoords.containsKey(bri.getMetaData() + 16 * voxel.getId()))
		{
			context.enableVertexAttribute("vertexIn");
			context.enableVertexAttribute("texCoordIn");
			context.enableVertexAttribute("normalIn");
			context.renderDirect(model.vertices, transformedTexCoords.get(bri.getMetaData()  + 16 * voxel.getId()), null, model.normals);
		}
	}
	
	private float toRad(float f)
	{
		return (float) (f / 180 * Math.PI);
	}

	@Override
	public void renderItemInWorld(RenderingContext context, ItemPile pile, World world, Matrix4f handTransformation)
	{
		float s = 0.45f;
		handTransformation.scale(new Vector3f(s, s, s));
		handTransformation.translate(new Vector3f(-0.25f, -0.5f, -0.5f));
		context.sendTransformationMatrix(handTransformation);
		Voxel voxel = ((ItemVoxel) pile.getItem()).getVoxel();
		if(voxel == null)
		{
			return;
		}
		Texture texture = TexturesHandler.getTexture("./res/textures/tiles_merged_diffuse.png");
		texture.setLinearFiltering(false);
		context.setDiffuseTexture(texture.getID());
		
		BlockRenderInfo bri = new BlockRenderInfo(0);
		bri.data = VoxelFormat.format(voxel.getId(), ((ItemVoxel) pile.getItem()).getVoxelMeta(), 15, voxel.getLightLevel(0));
		bri.voxelType = VoxelTypes.get(bri.data);
		VoxelModel model = voxel.getVoxelModel(bri);
		if(model == null || !voxel.isVoxelUsingCustomModel())
		{
			model = VoxelModels.getVoxelModel("default");
		}
		renderVoxel(context, voxel, model, bri);
	}

}
