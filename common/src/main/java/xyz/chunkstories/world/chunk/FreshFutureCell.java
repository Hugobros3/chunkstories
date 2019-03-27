package xyz.chunkstories.world.chunk;

import xyz.chunkstories.api.voxel.components.VoxelComponent;
import xyz.chunkstories.api.world.cell.CellData;
import xyz.chunkstories.api.world.cell.FutureCell;
import xyz.chunkstories.api.world.chunk.Chunk;
import xyz.chunkstories.api.world.chunk.FreshChunkCell;
import xyz.chunkstories.voxel.components.CellComponentsHolder;

class FreshFutureCell extends FutureCell implements FreshChunkCell {

    private CubicChunk cubicChunk;

    public FreshFutureCell(CubicChunk cubicChunk, CellData ogContext) {
        super(ogContext);
        this.cubicChunk = cubicChunk;
    }

    @Override
    public Chunk getChunk() {
        return cubicChunk;
    }

    @Override
    public CellComponentsHolder getComponents() {
        return cubicChunk.getComponentsAt(getX(), getY(), getZ());
    }

    @Override
    public void refreshRepresentation() {
        //nope
    }

    @Override
    public void registerComponent(String name, VoxelComponent component) {
        getComponents().put(name, component);
    }

}
