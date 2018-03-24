//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memUTF8;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.content.AssetToByteBufferHelper;

public class AssimpMesh {
	protected final Asset mainAsset;
	protected final MeshLibrary store;

	public AssimpMesh(Asset mainAsset, MeshLibrary store) throws MeshLoadException {
		this.mainAsset = mainAsset;
		this.store = store;
		
		int flags = Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_Triangulate | Assimp.aiProcess_FixInfacingNormals | Assimp.aiProcess_LimitBoneWeights;
		AIScene aiScene;
		// No LWJGL Java bindings to do this cleanly :'(
		//aiScene = Assimp.aiImportFileEx(mainAsset.getName(), flags, new AIFileIO() );

		//File cachedFile = AssetAsFileHelper.cacheAsset(mainAsset, store.parent().getContext());
		//aiScene = Assimp.aiImportFile(cachedFile.getAbsolutePath(), flags);
		
		// oof http://forum.lwjgl.org/index.php?topic=6704.0
		try (MemoryStack stack = MemoryStack.stackPush()) {
			aiScene = Assimp.aiImportFileEx(mainAsset.getName(), flags, 
					 AIFileIO.callocStack(stack)
		                .OpenProc((pFileIO, fileName, openMode) -> {
		                	String assetName = memUTF8(fileName);
		                	Asset asset = store.parent().getAsset(assetName);
		                	if(asset == null)
		                		throw new RuntimeException("Assimp was looking for an asset we don't have");
		                	
		                    ByteBuffer data = AssetToByteBufferHelper.loadIntoByteBuffer(asset);

		                    return AIFile.callocStack(stack)
		                        .ReadProc((pFile, pBuffer, size, count) -> {
		                            long remaining = data.remaining();
		                            long requested = size * count;

		                            long elements = Long.compareUnsigned(requested, remaining) <= 0
		                                ? count
		                                : Long.divideUnsigned(remaining, size);

		                            memCopy(memAddress(data), pBuffer, (int)(size * elements));
		                            data.position(data.position() + (int)((size * elements) & 0xFFFFFFFF));

		                            return elements;
		                        })
		                        .TellProc(pFile -> Integer.toUnsignedLong(data.position()))
		                        .FileSizeProc(pFile -> Integer.toUnsignedLong(data.capacity()))
		                        .SeekProc((pFile, offset, origin) -> {
		                            long position;
		                            switch (origin) {
		                                case Assimp.aiOrigin_SET:
		                                    position = offset;
		                                    break;
		                                case Assimp.aiOrigin_CUR:
		                                    position = data.position() + offset;
		                                    break;
		                                case Assimp.aiOrigin_END:
		                                    position = data.capacity() - offset;
		                                    break;
		                                default:
		                                    throw new IllegalArgumentException();
		                            }

		                            try {
		                                data.position((int)(position & 0xFFFFFFFF));
		                            } catch (IllegalArgumentException e) {
		                                return -1;
		                            }
		                            return 0;
		                        })
		                        .WriteProc((pFile, pBuffer, memB, count) -> { throw new UnsupportedOperationException(); })
		                        .FlushProc(pFile -> { throw new UnsupportedOperationException(); })
		                        .UserData(-1L)
		                        .address();
		                })
		                .CloseProc((pFileIO, pFile) -> {})
		                .UserData(-1L)
		        );
		}
		
		if(aiScene == null) {
			System.out.println(Assimp.aiGetErrorString());
			throw new MeshLoadException(mainAsset);
		}
		
		
		int meshesN = aiScene.mNumMeshes();
		for(int mesh = 0; mesh < meshesN; mesh++) {
			AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(mesh));
			System.out.println("Found mesh: "+aiMesh.mName().dataString());
			System.out.println(aiMesh.mNumVertices());
			System.out.println(aiMesh.mNumFaces());
			
			AIFace.Buffer buf = aiMesh.mFaces();
			for(int j = 0; j < aiMesh.mNumFaces(); j++) {
				IntBuffer indices = buf.get(j).mIndices();
				for(int k = 0; k < buf.get(j).mNumIndices(); k++) {
					System.out.println(indices.get(k));
				}
				
				System.out.println("-");
			}
			
			System.out.println("bones: "+aiMesh.mNumBones());
		}
		//aiMesh.
	}

	
}
