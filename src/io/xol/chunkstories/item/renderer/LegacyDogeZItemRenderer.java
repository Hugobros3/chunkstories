package io.xol.chunkstories.item.renderer;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;


//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LegacyDogeZItemRenderer extends DefaultItemRenderer
{
	public LegacyDogeZItemRenderer(Item item)
	{
		super(item);
	}
	
	@Override
	public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		handTransformation.rotate((float) (Math.PI / 4f), new Vector3fm(0.0, 0.0, 1.0));
		handTransformation.rotate((float) (Math.PI / 2f), new Vector3fm(0.0, 1.0, 0.0));
		handTransformation.translate(new Vector3fm(-0.05, -0.05, 0.05));
		
		int max = pile.getItem().getType().getSlotsWidth() - 1;
		
		handTransformation.scale(new Vector3fm(0.25 + 0.20 * max));
		renderingInterface.setObjectMatrix(handTransformation);

		TexturesHandler.getTexture(pile.getTextureName()).setLinearFiltering(false);
		Texture2D texture = TexturesHandler.getTexture(pile.getTextureName());
		if(texture == null)
			texture = TexturesHandler.getTexture("res/items/icons/notex.png");
		
		//texture = TexturesHandler.getTexture("res/textures/notex.png");
		texture.setLinearFiltering(false);
		renderingInterface.bindAlbedoTexture(texture);
		
		draw3DPlane(renderingInterface);
	}
}
