//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.converter;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.converter.ConverterWorkers.ConverterWorkerThread;
import io.xol.chunkstories.util.concurrency.CompoundFence;
import io.xol.chunkstories.world.WorldTool;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;

public class TaskBuildHeightmap extends Task {

    int regionX, regionZ;
    WorldTool csWorld;

    public TaskBuildHeightmap(int regionX, int regionZ, WorldTool csWorld) {
        super();
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.csWorld = csWorld;
    }

    @Override
    protected boolean task(TaskExecutor taskExecutor) {

        ConverterWorkerThread thread = (ConverterWorkerThread) taskExecutor;

        // We wait on a bunch of stuff to load everytime
        CompoundFence compoundFence = new CompoundFence();

        HeightmapImplementation heightmap = csWorld.getRegionsSummariesHolder().acquireHeightmap(thread, regionX, regionZ);
        Heightmap.State heightmapState = heightmap.getState();
        if (heightmapState instanceof Heightmap.State.Loading)
            compoundFence.add(((Heightmap.State.Loading) heightmapState).getFence());

        int heightInChunks = OfflineWorldConverter.mcWorldHeight / 32;
        ChunkHolder[] holders = new ChunkHolder[8 * 8 * heightInChunks];
        // acquires the chunks we want to make the summaries of.
        for (int innerCX = 0; innerCX < 8; innerCX++)
            for (int innerCZ = 0; innerCZ < 8; innerCZ++)
                for (int chunkY = 0; chunkY < heightInChunks; chunkY++) {
                    ChunkHolder holder = csWorld.acquireChunkHolder(thread, regionX * 8 + innerCX, chunkY, regionZ * 8 + innerCZ);
                    holders[(innerCX * 8 + chunkY) * heightInChunks + innerCZ] = holder;
                    ChunkHolder.State holderState = holder.getState();
                    if (holderState instanceof ChunkHolder.State.Loading)
                        compoundFence.add(((ChunkHolder.State.Loading) holderState).getFence());

                    if (thread.aquiredChunkHolders.add(holder))
                        thread.chunksAcquired++;
                }

        // Wait until all of that crap loads
        compoundFence.traverse();

        // Descend from top
        for (int i = 0; i < 256; i++)
            for (int j = 0; j < 256; j++) {
                for (int h = OfflineWorldConverter.mcWorldHeight - 1; h >= 0; h--) {
                    int cx = i / 32;
                    int cy = h / 32;
                    int cz = j / 32;
                    CellData data = holders[(cx * 8 + cy) * heightInChunks + cz].getChunk().peek(i % 32, h % 32, j % 32);
                    if (!data.getVoxel().isAir()) {
                        Voxel vox = data.getVoxel();
                        if (vox.isSolid() || vox.getName().equals("water")) {
                            heightmap.setTopCell(data);
                            break;
                        }
                    }
                }
            }


        // We don't need the summary anymore
        heightmap.unregisterUser(thread);
        Heightmap.State heightmapState2 = heightmap.getState();
        
        if(heightmapState2 instanceof Heightmap.State.Saving)
            ((Heightmap.State.Saving) heightmapState2).getFence().traverse();

        return true;
    }

}
