package xyz.chunkstories.world.chunk;

import xyz.chunkstories.api.voxel.components.VoxelComponent;
import xyz.chunkstories.api.world.cell.Cell;
import xyz.chunkstories.api.world.cell.FutureCell;
import xyz.chunkstories.api.world.chunk.Chunk;
import xyz.chunkstories.api.world.chunk.FreshChunkCell;
import xyz.chunkstories.voxel.components.CellComponentsHolder;

class FreshFutureCell extends FutureCell implements FreshChunkCell {

    private ChunkImplementation chunk;

    public FreshFutureCell(ChunkImplementation chunk, Cell ogContext) {
        super(ogContext);
        this.chunk = chunk;
    }

    @Override
    public Chunk getChunk() {
        return chunk;
    }

    @Override
    public CellComponentsHolder getComponents() {
        return chunk.getComponentsAt(getX(), getY(), getZ());
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
