package io.xol.chunkstories.core.item;

import io.xol.engine.math.lalgb.Matrix4f;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;
import io.xol.engine.model.ModelLibrary;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Ak47ViewModelRenderer extends DefaultItemRenderer
{
	
	public Ak47ViewModelRenderer(Item item)
	{
		super(item);
	}

	@Override
	public void renderItemInInventory(RenderingContext context, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		super.renderItemInInventory(context, pile, screenPositionX, screenPositionY, scaling);
	}

	@Override
	public void renderItemInWorld(RenderingContext renderingContext, ItemPile pile, WorldInterface world, Matrix4f handTransformation)
	{
		renderingContext.sendTransformationMatrix(handTransformation);
		renderingContext.setDiffuseTexture(TexturesHandler.getTextureID("res/models/ak47.hq.png"));
		renderingContext.setNormalTexture(TexturesHandler.getTextureID("res/textures/normalnormal.png"));
		ModelLibrary.getMesh("./res/models/ak47.hq.obj").render(renderingContext);
	}

}
