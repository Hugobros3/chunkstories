package io.xol.chunkstories.core.item.renderers;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.renderer.VoxelContextOlder;
import io.xol.chunkstories.renderer.WorldRenderer.RenderingPass;
import io.xol.chunkstories.renderer.chunks.RenderByteBuffer;
import io.xol.chunkstories.voxel.models.VoxelModelLoaded;
import io.xol.chunkstories.world.chunk.DummyChunk;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelItemRenderer extends ItemRenderer
{
	Matrix4f transformation = new Matrix4f();
	Map<Integer, VerticesObject> voxelItemsModelBuffer = new HashMap<Integer, VerticesObject>();
	private ChunkRenderContext bakingContext;

	public VoxelItemRenderer(ItemRenderer fallbackRenderer)
	{
		super(fallbackRenderer);
		
		bakingContext = new ChunkRenderContext() {

			private VoxelLighter lighter = new VoxelLighter() {

				@Override
				public byte getSunlightLevelForCorner(Corners corner)
				{
					// TODO Auto-generated method stub
					return 15;
				}

				@Override
				public byte getBlocklightLevelForCorner(Corners corner)
				{
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public byte getAoLevelForCorner(Corners corner)
				{
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public byte getSunlightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					// TODO Auto-generated method stub
					return 15;
				}

				@Override
				public byte getBlocklightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public byte getAoLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					// TODO Auto-generated method stub
					return 0;
				}
				
			};

			@Override
			public boolean isTopChunkLoaded()
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isBottomChunkLoaded()
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isLeftChunkLoaded()
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isRightChunkLoaded()
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isFrontChunkLoaded()
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isBackChunkLoaded()
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public int getRenderedVoxelPositionInChunkX()
			{
				return 0;
			}

			@Override
			public int getRenderedVoxelPositionInChunkY()
			{
				return 0;
			}

			@Override
			public int getRenderedVoxelPositionInChunkZ()
			{
				return 0;
			}

			@Override
			public VoxelLighter getCurrentVoxelLighter()
			{
				// TODO Auto-generated method stub
				return lighter ;
			}
			
		};
	}

	@Override
	public void renderItemInInventory(RenderingInterface renderingContext, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		//voxelItemsModelBuffer.clear();
		
		if (((ItemVoxel) pile.getItem()).getVoxel() instanceof VoxelCustomIcon)
		{
			fallbackRenderer.renderItemInInventory(renderingContext, pile, screenPositionX, screenPositionY, scaling);
			return;
		}

		int slotSize = 24 * scaling;
		ShaderInterface program = renderingContext.useShader("inventory_blockmodel");
		
		renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);

		program.setUniform2f("screenSize", renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());
		program.setUniform2f("dekal", screenPositionX + pile.getItem().getType().getSlotsWidth() * slotSize / 2, screenPositionY + pile.getItem().getType().getSlotsHeight() * slotSize / 2);
		program.setUniform1f("scaling", slotSize / 1.65f);
		transformation.setIdentity();
		transformation.scale(new Vector3fm(-1f, 1f, 1f));
		transformation.rotate(toRad(-22.5f), new Vector3fm(1.0f, 0.0f, 0.0f));
		transformation.rotate(toRad(45f), new Vector3fm(0.0f, 1.0f, 0.0f));
		transformation.translate(new Vector3fm(-0.5f, -0.5f, -0.5f));
		
		program.setUniformMatrix4f("transformation", transformation);
		Voxel voxel = ((ItemVoxel) pile.getItem()).getVoxel();
		if (voxel == null)
		{
			int width = slotSize * pile.getItem().getType().getSlotsWidth();
			int height = slotSize * pile.getItem().getType().getSlotsHeight();
			renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(screenPositionX, screenPositionY, width, height, 0, 1, 1, 0, TexturesHandler.getTexture("./items/icons/notex.png"), true, true, null);
			return;
		}
		Texture2D texture = Client.getInstance().getContent().voxels().textures().getDiffuseAtlasTexture();
		texture.setLinearFiltering(false);
		renderingContext.bindAlbedoTexture(texture);
		
		Texture2D normalTexture = Client.getInstance().getContent().voxels().textures().getNormalAtlasTexture();
		normalTexture.setLinearFiltering(false);
		renderingContext.bindNormalTexture(normalTexture);
		
		Texture2D materialTexture = Client.getInstance().getContent().voxels().textures().getMaterialAtlasTexture();
		materialTexture.setLinearFiltering(false);
		renderingContext.bindMaterialTexture(materialTexture);

		VoxelContext bri = new VoxelContextOlder(VoxelFormat.format(voxel.getId(), ((ItemVoxel) pile.getItem()).getVoxelMeta(), 15, voxel.getLightLevel(0)), 0, 0, 0);
		VoxelRenderer model = voxel.getVoxelRenderer(bri);
		if (model == null)
		{
			model = voxel.store().models().getVoxelModelByName("default");
		}
		renderVoxel(renderingContext, voxel, model, bri);
	}
	
	private class EditedTexCoordsRenderByteBuffer extends RenderByteBuffer {

		public EditedTexCoordsRenderByteBuffer(ByteBuffer byteBuffer)
		{
			super(byteBuffer);
		}
		
		@Override
		public void addTexCoordInt(int i0, int i1)
		{
			byteBuffer.putFloat(i0 / 32768f);
			byteBuffer.putFloat(i1 / 32768f);
		}
		
		@Override
		public void addColors(byte a, byte b, byte c)
		{
		}
		
		@Override
		public void addColors(float f0, float f1, float f2)
		{
		}
		
		@Override
		public void addColorsSpecial(float f0, float f1, float f2, int extended)
		{
		}
	}

	private void renderVoxel(RenderingInterface renderingContext, Voxel voxel, VoxelRenderer voxelRenderer, VoxelContext bri)
	{
		VoxelModelLoaded model = null;
		if (voxelRenderer instanceof VoxelModelLoaded)
			model = (VoxelModelLoaded) voxelRenderer;
		else
			return;

		if (!voxelItemsModelBuffer.containsKey(bri.getMetaData() + 16 * voxel.getId()))
		{
			//Wow calm down satan with your huge-ass models
			ByteBuffer buffer = BufferUtils.createByteBuffer(16384);
			RenderByteBuffer rbbuf = new EditedTexCoordsRenderByteBuffer(buffer);
			model.renderInto(rbbuf, bakingContext, bri, new DummyChunk(), 0, 0, 0);
			
			buffer.flip();
			
			VerticesObject mesh = new VerticesObject();
			mesh.uploadData(buffer);
			
			voxelItemsModelBuffer.put(bri.getMetaData() + 16 * voxel.getId(), mesh);
		}
		
		
		if (voxelItemsModelBuffer.containsKey(bri.getMetaData() + 16 * voxel.getId()))
		{
			VerticesObject mesh = voxelItemsModelBuffer.get(bri.getMetaData() + 16 * voxel.getId());

			renderingContext.bindAttribute("vertexIn", mesh.asAttributeSource(VertexFormat.FLOAT, 3, 24, 0));
			renderingContext.bindAttribute("texCoordIn", mesh.asAttributeSource(VertexFormat.FLOAT, 3, 24, 12));
			renderingContext.bindAttribute("normalIn", mesh.asAttributeSource(VertexFormat.U1010102, 4, 24, 20));
			
			//renderingContext.setVertexAttributePointerLocation("vertexIn", 3, GL_FLOAT, false, 24, 0 + 0, mesh);
			//renderingContext.setVertexAttributePointerLocation("texCoordIn", 2, GL_FLOAT, false, 24, 0 + 12, mesh);
			//renderingContext.setVertexAttributePointerLocation("normalIn", 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 24, 0 + 20, mesh);
			
			renderingContext.draw(Primitive.TRIANGLE, 0, (int) (mesh.getVramUsage() / 24));
			
			//mesh.drawElementsTriangles((int) (mesh.getVramUsage() / 24));
		}
	}

	private float toRad(float f)
	{
		return (float) (f / 180 * Math.PI);
	}

	@Override
	public void renderItemInWorld(RenderingInterface context, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		float s = 0.45f;
		handTransformation.scale(new Vector3fm(s, s, s));
		handTransformation.translate(new Vector3fm(-0.25f, -0.5f, -0.5f));
		context.setObjectMatrix(handTransformation);
		Voxel voxel = ((ItemVoxel) pile.getItem()).getVoxel();
		if (voxel == null)
		{
			return;
		}

		//Add a light only on the opaque pass
		if (((ItemVoxel) pile.getItem()).getVoxel().getLightLevel(0x00) > 0 && context.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.NORMAL_OPAQUE)
		{
			Vector4fm lightposition = new Vector4fm(0.0, 0.0, 0.0, 1.0);
			Matrix4f.transform(handTransformation, lightposition, lightposition);
			
			Light heldBlockLight = new Light(new Vector3fm(0.5f, 0.45f, 0.4f).scale(2.0f), new Vector3fm(lightposition.getX(), lightposition.getY(), lightposition.getZ()), 15f);
			context.getLightsRenderer().queueLight(heldBlockLight);	
			
			//If we hold a light source, prepare the shader accordingly
			context.currentShader().setUniform2f("worldLightIn", ((ItemVoxel) pile.getItem()).getVoxel().getLightLevel(0x00), world.getSunlightLevelLocation(location));
		}
		
		Texture2D texture = Client.getInstance().getContent().voxels().textures().getDiffuseAtlasTexture();
		texture.setLinearFiltering(false);
		context.bindAlbedoTexture(texture);
		
		Texture2D normalTexture = Client.getInstance().getContent().voxels().textures().getNormalAtlasTexture();
		normalTexture.setLinearFiltering(false);
		context.bindNormalTexture(normalTexture);
		
		Texture2D materialTexture = Client.getInstance().getContent().voxels().textures().getMaterialAtlasTexture();
		materialTexture.setLinearFiltering(false);
		context.bindMaterialTexture(materialTexture);

		VoxelContext bri = new VoxelContextOlder(VoxelFormat.format(voxel.getId(), ((ItemVoxel) pile.getItem()).getVoxelMeta(), 15, voxel.getLightLevel(0)), 0, 0, 0);
		
		//bri.voxelType = VoxelsStore.get().getVoxelById(bri.data);
		VoxelRenderer model = voxel.getVoxelRenderer(bri);
		if (model == null)
		{
			model = voxel.store().models().getVoxelModelByName("default");
		}
		renderVoxel(context, voxel, model, bri);
	}

}
