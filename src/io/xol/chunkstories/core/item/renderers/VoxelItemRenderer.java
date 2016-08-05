package io.xol.chunkstories.core.item.renderers;

import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

import static org.lwjgl.opengl.GL11.*;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.renderer.lights.DefferedLight;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTextures;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;
import io.xol.chunkstories.voxel.models.VoxelRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelItemRenderer implements ItemRenderer
{
	DefaultItemRenderer defaultItemRenderer;

	ItemVoxel itemVoxel;
	Matrix4f transformation = new Matrix4f();
	Map<Integer, float[]> voxelItemsModelBuffer = new HashMap<Integer, float[]>();

	public VoxelItemRenderer(ItemVoxel itemVoxel)
	{
		this.itemVoxel = itemVoxel;
		this.defaultItemRenderer = new DefaultItemRenderer(itemVoxel);
	}

	@Override
	public void renderItemInInventory(RenderingContext renderingContext, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		if (((ItemVoxel) pile.getItem()).getVoxel() instanceof VoxelCustomIcon)
		{
			defaultItemRenderer.renderItemInInventory(renderingContext, pile, screenPositionX, screenPositionY, scaling);
			return;
		}

		int slotSize = 24 * scaling;
		ShaderProgram program = ShadersLibrary.getShaderProgram("inventory_blockmodel");
		renderingContext.setCurrentShader(program);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);

		program.setUniformFloat2("screenSize", GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		program.setUniformFloat2("dekal", screenPositionX + pile.getItem().getSlotsWidth() * slotSize / 2, screenPositionY + pile.getItem().getSlotsHeight() * slotSize / 2);
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
		if (voxel == null)
		{
			int width = slotSize * pile.item.getSlotsWidth();
			int height = slotSize * pile.item.getSlotsHeight();
			renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(screenPositionX, screenPositionY, width, height, 0, 1, 1, 0, TexturesHandler.getTexture("res/items/icons/notex.png").getId(), true, true, null);
			return;
		}
		Texture2D texture = TexturesHandler.getTexture("./res/textures/tiles_merged_diffuse.png");
		texture.setLinearFiltering(false);
		renderingContext.setDiffuseTexture(texture.getId());

		BlockRenderInfo bri = new BlockRenderInfo(0);
		bri.data = VoxelFormat.format(voxel.getId(), ((ItemVoxel) pile.getItem()).getVoxelMeta(), 15, voxel.getLightLevel(0));
		bri.voxelType = VoxelTypes.get(bri.data);
		VoxelRenderer model = voxel.getVoxelModel(bri);
		if (model == null || !voxel.isVoxelUsingCustomModel())
		{
			model = VoxelModels.getVoxelModel("default");
		}
		renderVoxel(renderingContext, voxel, model, bri);
	}

	private void renderVoxel(RenderingContext context, Voxel voxel, VoxelRenderer voxelRenderer, BlockRenderInfo bri)
	{
		VoxelModel model = null;
		if (voxelRenderer instanceof VoxelModel)
			model = (VoxelModel) voxelRenderer;
		else
			return;

		if (!voxelItemsModelBuffer.containsKey(bri.getMetaData() + 16 * voxel.getId()))
		{
			float[] transformTextures = new float[model.texCoords.length];

			VoxelTexture voxelTexture = voxel.getVoxelTexture(bri.data, VoxelSides.LEFT, bri);
			int modelTextureIndex = 0;
			String voxelName = VoxelTypes.get(bri.data).getName();
			if (!model.texturesNames[modelTextureIndex].equals("~"))
				voxelTexture = VoxelTextures.getVoxelTexture(model.texturesNames[modelTextureIndex].replace("~", voxelName));
			int useUntil = model.texturesOffsets[modelTextureIndex];
			int textureS = voxelTexture.atlasS;// +mod(sx,texture.textureScale)*offset;
			int textureT = voxelTexture.atlasT;// +mod(sz,texture.textureScale)*offset;

			for (int i = 0; i < transformTextures.length; i += 2)
			{
				int vertexIndice = i / 2;

				if (vertexIndice >= useUntil)
				{
					modelTextureIndex++;
					if (!model.texturesNames[modelTextureIndex].equals("~"))
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
			voxelItemsModelBuffer.put(bri.getMetaData() + 16 * voxel.getId(), transformTextures);
		}
		if (voxelItemsModelBuffer.containsKey(bri.getMetaData() + 16 * voxel.getId()))
		{
			context.enableVertexAttribute("vertexIn");
			context.enableVertexAttribute("texCoordIn");
			context.enableVertexAttribute("normalIn");
			context.getDirectRenderer().renderDirect(model.vertices, voxelItemsModelBuffer.get(bri.getMetaData() + 16 * voxel.getId()), null, model.normals);
		}
	}

	private float toRad(float f)
	{
		return (float) (f / 180 * Math.PI);
	}

	@Override
	public void renderItemInWorld(RenderingContext context, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		float s = 0.45f;
		handTransformation.scale(new Vector3f(s, s, s));
		handTransformation.translate(new Vector3f(-0.25f, -0.5f, -0.5f));
		context.sendTransformationMatrix(handTransformation);
		Voxel voxel = ((ItemVoxel) pile.getItem()).getVoxel();
		if (voxel == null)
		{
			return;
		}

		if (((ItemVoxel) pile.getItem()).getVoxel().getLightLevel(0x00) > 0)
		{
			Vector4f lightposition = new Vector4f(0.0, 0.0, 0.0, 1.0);
			Matrix4f.transform(handTransformation, lightposition, lightposition);
			
			Vector3d pos = location.clone();
			Light heldBlockLight = new DefferedLight(new Vector3f(0.5f, 0.45f, 0.4f).scale(2.0f), new Vector3f((float) pos.getX(), (float) pos.getY(), (float) pos.getZ()).add(new Vector3f(lightposition.x, lightposition.y, lightposition.z)), 15f);
			context.addLight(heldBlockLight);	
			
			//If we hold a light source, prepare the shader accordingly
			context.getCurrentShader().setUniformFloat2("worldLight", ((ItemVoxel) pile.getItem()).getVoxel().getLightLevel(0x00), world.getSunlightLevel(location));
			
		}
		
		Texture2D texture = TexturesHandler.getTexture("./res/textures/tiles_merged_diffuse.png");
		texture.setLinearFiltering(false);
		context.setDiffuseTexture(texture.getId());

		BlockRenderInfo bri = new BlockRenderInfo(0);
		bri.data = VoxelFormat.format(voxel.getId(), ((ItemVoxel) pile.getItem()).getVoxelMeta(), 15, voxel.getLightLevel(0));
		bri.voxelType = VoxelTypes.get(bri.data);
		VoxelRenderer model = voxel.getVoxelModel(bri);
		if (model == null || !voxel.isVoxelUsingCustomModel())
		{
			model = VoxelModels.getVoxelModel("default");
		}
		renderVoxel(context, voxel, model, bri);
	}

}
