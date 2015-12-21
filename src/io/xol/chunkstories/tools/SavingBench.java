package io.xol.chunkstories.tools;

/*
 import java.io.File;
 import java.io.FileOutputStream;

 import net.jpountz.lz4.LZ4Compressor;
 import net.jpountz.lz4.LZ4Factory;

 import io.xol.chunkstories.voxel.VoxelTypes;
 import io.xol.chunkstories.world.Chunk;
 import io.xol.chunkstories.world.World;
 import io.xol.chunkstories.world.generator.PerlinWorldAccessor;
 */
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SavingBench
{
	/*
	 * public static void main(String[] args) { try {
	 * VoxelTypes.initVoxelTypes();
	 * 
	 * File compressed = new File("compressed.xv"); FileOutputStream
	 * compressedOut = new FileOutputStream(compressed); LZ4Factory factory =
	 * LZ4Factory.fastestJavaInstance(); LZ4Compressor compressor =
	 * factory.fastCompressor(); compressor.maxCompressedLength(4*32*32*256);
	 * 
	 * String seed = "cacaboudin"; World.WorldSize worldSize =
	 * World.WorldSize.TINY;
	 * System.out.println("Generating "+worldSize.name+" worldmap for seed : "
	 * +seed);
	 * 
	 * PerlinWorldAccessor pwa = new PerlinWorldAccessor(seed,worldSize);
	 * 
	 * int toProcessChunks = worldSize.sizeInChunks*worldSize.sizeInChunks; int
	 * processedChunks = 0; int percentage = 0;
	 * 
	 * long shortestChunkGen = System.currentTimeMillis(); long longestChunkGen
	 * = 0; long totalChunkGen = 0; long lastStartChunkGen = 0; long
	 * totalTimeCompressing = 0; long lastTimeCompressing = 0; long
	 * totalTimeWriting = 0; long lastTimeWriting = 0;
	 * 
	 * totalChunkGen = System.currentTimeMillis(); for(int cx = 0; cx <
	 * worldSize.sizeInChunks; cx++) { for(int cz = 0; cz <
	 * worldSize.sizeInChunks; cz++) { lastStartChunkGen =
	 * System.currentTimeMillis(); byte[] data = new byte[4*32*32*256]; Chunk
	 * chunk = pwa.loadChunk(cx, cz); //Saving for(int c = 0; c < 256; c++)
	 * for(int a = 0; a < 32; a++) for(int b = 0; b < 32; b++) {
	 * data[((a*32+b)*32+c)*4] = (byte) ((chunk.data[a][c][b] >> 24) & 0xFF);
	 * data[((a*32+b)*32+c)*4+1] = (byte) ((chunk.data[a][c][b] >> 16) & 0xFF);
	 * data[((a*32+b)*32+c)*4+2] = (byte) ((chunk.data[a][c][b] >> 8) & 0xFF);
	 * data[((a*32+b)*32+c)*4+3] = (byte) ((chunk.data[a][c][b] >> 0) & 0xFF);
	 * 
	 * } //That was ... a 4x256x32x32 bytes block ... a 1MiB block ! perfect
	 * size ! lastTimeCompressing = System.currentTimeMillis(); byte[]
	 * compressedData = compressor.compress(data); totalTimeCompressing +=
	 * (System.currentTimeMillis()-lastTimeCompressing);
	 * 
	 * lastTimeWriting = System.currentTimeMillis();
	 * compressedOut.write(compressedData); totalTimeWriting +=
	 * (System.currentTimeMillis()-lastTimeWriting); //compressedOut.flush();
	 * 
	 * //Chunk timing long currentTimeTook =
	 * System.currentTimeMillis()-lastStartChunkGen; if(currentTimeTook <
	 * shortestChunkGen) shortestChunkGen = currentTimeTook; if(currentTimeTook
	 * > longestChunkGen) longestChunkGen = currentTimeTook;
	 * 
	 * processedChunks++; int newPercentage =
	 * processedChunks*100/toProcessChunks; if(newPercentage > percentage) {
	 * percentage = newPercentage; if(percentage % 10 == 0)
	 * System.out.println("Working ... "
	 * +processedChunks+" out of "+toProcessChunks+" ("+percentage+"%)"); } } }
	 * totalChunkGen = System.currentTimeMillis()-totalChunkGen;
	 * compressedOut.close(); System.out.println("Done !");
	 * System.out.println("Total time (s) : "+totalChunkGen/1000f);
	 * System.out.println
	 * ("Total time spent compressing (s) : "+totalTimeCompressing/1000f);
	 * System
	 * .out.println("Total time spent writing (s) : "+totalTimeWriting/1000f);
	 * System
	 * .out.println("Average chunk time (ms) : "+totalChunkGen/processedChunks);
	 * System.out.println("Min chunk time (ms) :"+shortestChunkGen);
	 * System.out.println("Max chunk time (ms) :"+longestChunkGen); }
	 * catch(Exception e) { e.printStackTrace(); }
	 */
	/*
	 * try { VoxelTypes.initVoxelTypes();
	 * 
	 * File uncompressed = new File("uncompressed.xv"); File compressed = new
	 * File("compressed.xv"); FileOutputStream uncompressedOut = new
	 * FileOutputStream(uncompressed); LZMA2Options options = new
	 * LZMA2Options(); options.setPreset(0); XZOutputStream compressedOut = new
	 * XZOutputStream(new FileOutputStream(compressed), options);
	 * 
	 * String seed = "cacaboudin"; World.WorldSize worldSize =
	 * World.WorldSize.TINY;
	 * System.out.println("Generating "+worldSize.name+" worldmap for seed : "
	 * +seed);
	 * 
	 * PerlinWorldAccessor pwa = new PerlinWorldAccessor(seed,worldSize);
	 * 
	 * int toProcessChunks = worldSize.sizeInChunks*worldSize.sizeInChunks; int
	 * processedChunks = 0; int percentage = 0;
	 * 
	 * long shortestChunkGen = System.currentTimeMillis(); long longestChunkGen
	 * = 0; long totalChunkGen = 0; long lastStartChunkGen = 0;
	 * 
	 * totalChunkGen = System.currentTimeMillis(); for(int cx = 0; cx <
	 * worldSize.sizeInChunks; cx++) { for(int cz = 0; cz <
	 * worldSize.sizeInChunks; cz++) { lastStartChunkGen =
	 * System.currentTimeMillis(); Chunk chunk = pwa.loadChunk(cx, cz); //Saving
	 * for(int a = 0; a < 32; a++) for(int b = 0; b < 32; b++) for(int c = 0; c
	 * < 256; c++) { // compressedOut.write((chunk.data[a][c][b] >> 24) & 0xFF);
	 * compressedOut.write((chunk.data[a][c][b] >> 16) & 0xFF);
	 * compressedOut.write((chunk.data[a][c][b] >> 8) & 0xFF);
	 * compressedOut.write(chunk.data[a][c][b] & 0xFF); } //That was ... a
	 * 4x256x32x32 bytes block ... a 1MiB block ! perfect size !
	 * compressedOut.flush(); compressedOut.endBlock();
	 * 
	 * //Chunk timing long currentTimeTook =
	 * System.currentTimeMillis()-lastStartChunkGen; if(currentTimeTook <
	 * shortestChunkGen) shortestChunkGen = currentTimeTook; if(currentTimeTook
	 * > longestChunkGen) longestChunkGen = currentTimeTook;
	 * 
	 * processedChunks++; int newPercentage =
	 * processedChunks*100/toProcessChunks; if(newPercentage > percentage) {
	 * percentage = newPercentage; if(percentage % 10 == 0)
	 * System.out.println("Working ... "
	 * +processedChunks+" out of "+toProcessChunks+" ("+percentage+"%)"); } } }
	 * totalChunkGen = System.currentTimeMillis()-totalChunkGen;
	 * uncompressedOut.close(); compressedOut.close();
	 * System.out.println("Done !");
	 * System.out.println("Total time (s) : "+totalChunkGen/1000f);
	 * System.out.println
	 * ("Average chunk time (ms) : "+totalChunkGen/processedChunks);
	 * System.out.println("Min chunk time (ms) :"+shortestChunkGen);
	 * System.out.println("Max chunk time (ms) :"+longestChunkGen); }
	 * catch(Exception e) { e.printStackTrace(); }
	 */

	/*
	 * VoxelTypes.initVoxelTypes(); PerlinWorldAccessor pwa = new
	 * PerlinWorldAccessor("lahaine",WorldSize.TINY); Chunk chunk =
	 * pwa.loadChunk(0, 5);
	 * 
	 * File uncompressed = new File("uncompressed.xv"); File compressed = new
	 * File("compressed.xv");
	 * 
	 * ByteArrayOutputStream dummy = new ByteArrayOutputStream();
	 * 
	 * try {
	 * 
	 * System.out.println("Wrinting uncompressed version"); long start =
	 * System.currentTimeMillis();
	 * 
	 * FileOutputStream uncompressedOut = new FileOutputStream(uncompressed);
	 * 
	 * for(int a = 0; a < 32; a++) for(int b = 0; b < 32; b++) for(int c = 0; c
	 * < 256; c++) { uncompressedOut.write((chunk.data[a][c][b] >> 24) & 0xFF);
	 * uncompressedOut.write((chunk.data[a][c][b] >> 16) & 0xFF);
	 * uncompressedOut.write((chunk.data[a][c][b] >> 8) & 0xFF);
	 * uncompressedOut.write(chunk.data[a][c][b] & 0xFF); }
	 * uncompressedOut.close();
	 * System.out.println("Done, took "+(System.currentTimeMillis
	 * ()-start)+"ms");
	 * 
	 * start = System.currentTimeMillis();
	 * System.out.println("Wrinting compressed version"); LZMA2Options options =
	 * new LZMA2Options(); options.setPreset(0); XZOutputStream compressedOut =
	 * new XZOutputStream(new FileOutputStream(compressed), options);
	 * 
	 * for(int a = 0; a < 32; a++) for(int b = 0; b < 32; b++) for(int c = 0; c
	 * < 256; c++) { compressedOut.write((chunk.data[a][c][b] >> 24) & 0xFF);
	 * compressedOut.write((chunk.data[a][c][b] >> 16) & 0xFF);
	 * compressedOut.write((chunk.data[a][c][b] >> 8) & 0xFF);
	 * compressedOut.write(chunk.data[a][c][b] & 0xFF); } compressedOut.close();
	 * System
	 * .out.println("Done, took "+(System.currentTimeMillis()-start)+"ms");
	 * 
	 * } catch( Exception e) { e.printStackTrace(); } }
	 */
}
