package io.xol.chunkstories.core.item.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.ClientContent.TexturesLibrary;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.item.renderer.ItemRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class FlatIconItemRenderer extends DefaultItemRenderer
{
	private final TexturesLibrary textures;
	
	public FlatIconItemRenderer(Item item, ItemRenderer fallbackRenderer, ItemType itemType)
	{
		super(item);
		this.textures = ((ClientContent)item.getType().store().parent()).textures();
	}
	
	@Override
	public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		handTransformation.rotate((float) (Math.PI / 4f), new Vector3f(0.0f, 0.0f, 1.0f));
		handTransformation.rotate((float) (Math.PI / 2f), new Vector3f(0.0f, 1.0f, 0.0f));
		handTransformation.translate(new Vector3f(-0.05f, -0.05f, 0.05f));
		
		int max = pile.getItem().getType().getSlotsWidth() - 1;
		handTransformation.scale(new Vector3f(0.25f + 0.20f * max));
		
		renderingInterface.setObjectMatrix(handTransformation);

		textures.getTexture(pile.getTextureName()).setLinearFiltering(false);
		Texture2D texture = textures.getTexture(pile.getTextureName());
		if(texture == null)
			texture = textures.getTexture("items/icons/notex.png");
		
		texture.setLinearFiltering(false);
		renderingInterface.bindAlbedoTexture(texture);
		
		draw3DPlane(renderingInterface);
	}
}
