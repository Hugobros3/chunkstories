package io.xol.chunkstories.core.item.renderers;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;
import io.xol.engine.model.ModelLibrary;

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
	public void renderItemInWorld(RenderingContext renderingContext, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		renderingContext.sendTransformationMatrix(handTransformation);
		renderingContext.sendBoneTransformationMatrix(null);
		//renderingContext.getCurrentShader().setUniformFloat3("objectPosition", 0, 0, 0);
		renderingContext.bindAlbedoTexture(TexturesHandler.getTexture("./models/ak47.hq.png"));
		renderingContext.bindNormalTexture(TexturesHandler.getTexture("./textures/normalnormal.png"));
		ModelLibrary.getRenderableMesh("./models/ak47.hq.obj").render(renderingContext);
	}

}
