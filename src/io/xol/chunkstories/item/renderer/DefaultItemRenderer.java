package io.xol.chunkstories.item.renderer;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DefaultItemRenderer extends ItemRenderer
{
	ItemType itemType;
	
	public DefaultItemRenderer(ItemType itemType)
	{
		super(null);
		this.itemType = itemType;
	}

	static VerticesObject defaultPlane = null;
	
	@Override
	public void renderItemInInventory(RenderingInterface renderingInterface, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		int slotSize = 24 * 2;
		TexturesHandler.getTexture(pile.getTextureName()).setLinearFiltering(false);
		Texture2D texture = TexturesHandler.getTexture(pile.getTextureName());
		if(texture == null)
			texture = TexturesHandler.getTexture("res/items/icons/notex.png");
		
		int width = slotSize * pile.getItem().getType().getSlotsWidth();
		int height = slotSize * pile.getItem().getType().getSlotsHeight();
		renderingInterface.getGuiRenderer().drawBoxWindowsSpaceWithSize(screenPositionX, screenPositionY, width, height, 0, 1, 1, 0, texture, true, true, null);
	}

	@Override
	public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		Matrix4f handTransformation2 = handTransformation.clone();
		
		handTransformation2.rotate((float) (Math.PI / 2f), new Vector3fm(0.0, 0.0, 1.0));
		handTransformation2.rotate((float) (Math.PI / 2f), new Vector3fm(0.0, 1.0, 0.0));
		handTransformation2.translate(new Vector3fm(-0.05, -0.15, 0.0));
		handTransformation2.scale(new Vector3fm(0.35));
		renderingInterface.setObjectMatrix(handTransformation2);

		TexturesHandler.getTexture(pile.getTextureName()).setLinearFiltering(false);
		Texture2D texture = TexturesHandler.getTexture(pile.getTextureName());
		if(texture == null)
			texture = TexturesHandler.getTexture("res/items/icons/notex.png");
		
		texture.setLinearFiltering(false);
		renderingInterface.bindAlbedoTexture(texture);
		
		draw3DPlane(renderingInterface);
	}
	
	protected void draw3DPlane(RenderingInterface renderingInterface)
	{
		//defaultPlane = null;
		if(defaultPlane == null)
		{
			defaultPlane = new VerticesObject();
			ByteBuffer buf = BufferUtils.createByteBuffer(2 * (4 * 3 * 6 + 4 * 2 * 6 + 4 * 3 * 6));
			//Vertex pos data
			buf.putFloat(0.0f);
			buf.putFloat(-1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(-1.0f);
			buf.putFloat(-1.0f);
			
			buf.putFloat(0.0f);
			buf.putFloat(1.0f);
			buf.putFloat(-1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(-1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(1.0f);
			buf.putFloat(-1.0f);
			//Flipped version

			buf.putFloat(0.0f);
			buf.putFloat(-1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(-1.0f);
			buf.putFloat(-1.0f);
			
			buf.putFloat(0.0f);
			buf.putFloat(1.0f);
			buf.putFloat(-1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(-1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(0.0f);
			buf.putFloat(1.0f);
			buf.putFloat(-1.0f);
			
			//Vertex texcoord data
			buf.putFloat(0.0f);
			buf.putFloat(0.0f);

			buf.putFloat(0.0f);
			buf.putFloat(1.0f);

			buf.putFloat(1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(1.0f);
			buf.putFloat(0.0f);
			
			buf.putFloat(0.0f);
			buf.putFloat(0.0f);

			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			//Flipped

			buf.putFloat(0.0f);
			buf.putFloat(0.0f);

			buf.putFloat(0.0f);
			buf.putFloat(1.0f);

			buf.putFloat(1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(1.0f);
			buf.putFloat(0.0f);
			
			buf.putFloat(0.0f);
			buf.putFloat(0.0f);

			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			
			//Normals
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			//And again

			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);

			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			buf.putFloat(1.0f);
			
			
			buf.flip();
			defaultPlane.uploadData(buf);
		}
		
		renderingInterface.bindAttribute("vertexIn", defaultPlane.asAttributeSource(VertexFormat.FLOAT, 3, 0, 0));
		renderingInterface.bindAttribute("texCoordIn", defaultPlane.asAttributeSource(VertexFormat.FLOAT, 2, 0, 2 * 4 * 3 * 6));
		renderingInterface.bindAttribute("normalIn", defaultPlane.asAttributeSource(VertexFormat.FLOAT, 3, 0, 2 *( 4 * 3 * 6 + 4 * 2 * 6)));
		
		renderingInterface.draw(Primitive.TRIANGLE, 0, 6);
	}

}
