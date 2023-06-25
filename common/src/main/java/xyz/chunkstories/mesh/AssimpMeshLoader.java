//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.mesh;

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.hppc.FloatArrayList;

import kotlin.Pair;
import org.lwjgl.assimp.*;

import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.exceptions.content.MeshLoadException;
import xyz.chunkstories.api.graphics.*;
import xyz.chunkstories.api.graphics.representation.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chunkstories.util.FoldersUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static org.lwjgl.system.MemoryUtil.*;

public class AssimpMeshLoader {

    private static final Logger logger = LoggerFactory.getLogger("content.meshes.assimp-kotlin");

    final MeshStore store;

    AIFileIO ioHandler = AIFileIO.malloc();

    public AssimpMeshLoader(MeshStore meshStore) {
        store = meshStore;

        // ioHandler.set()

        AIFileOpenProcI fileOpenProc = new AIFileOpenProc() {
            public long invoke(long pFileIO, long fileName, long openMode) {
                AIFile aiFile = AIFile.create();
                final ByteBuffer data;

                String name = memUTF8(fileName);
                //System.err.println("Opening file... " + name);

                try {
                    data = ByteBuffer.wrap(store.parent().getAsset(name).read().readAllBytes());
                } catch (IOException e) {
                    throw new RuntimeException();
                }

                AIFileReadProcI fileReadProc = new AIFileReadProc() {
                    public long invoke(long pFile, long pBuffer, long size, long count) {
                        //System.err.println("read file... " + name);
                        long max = Math.min(data.remaining(), size * count);
                        //System.err.println("copying " + max + " bytes out of " + data.remaining());
                        //System.err.println("pBuffer:" + pBuffer);
                        //System.err.println("data.position():" + data.position());
                        //System.err.println("memAddress(data):" + memAddress(data));
                        ByteBuffer dst = memByteBuffer(pBuffer, (int) max);
                        data.limit(data.position() + (int) max);
                        //System.err.println("memAddress(dst):" + memAddress(dst));
                        dst.put(data);
                        // memCopy(memAddress(data) + data.position(), pBuffer, max);
                        //System.err.println("memcpy good... " + name);
                        return max;
                    }
                };
                AIFileSeekI fileSeekProc = new AIFileSeek() {
                    public int invoke(long pFile, long offset, int origin) {
                        //System.err.println("seek file... " + name);
                        if (origin == Assimp.aiOrigin_CUR) {
                            data.position(data.position() + (int) offset);
                        } else if (origin == Assimp.aiOrigin_SET) {
                            data.position((int) offset);
                        } else if (origin == Assimp.aiOrigin_END) {
                            data.position(data.limit() + (int) offset);
                        }
                        return 0;
                    }
                };
                AIFileTellProcI fileTellProc = new AIFileTellProc() {
                    public long invoke(long pFile) {
                        //System.err.println("tell file... " + name);
                        return data.limit();
                    }
                };
                aiFile.ReadProc(fileReadProc);
                aiFile.SeekProc(fileSeekProc);
                aiFile.FileSizeProc(fileTellProc);
                return aiFile.address();
            }
        };
        AIFileCloseProcI fileCloseProc = new AIFileCloseProc() {
            public void invoke(long pFileIO, long pFile) {
                //System.err.println("close file... ");
                /* Nothing to do */
            }
        };
        ioHandler.set(fileOpenProc, fileCloseProc, 0);

        //assimp.SettingsKt.setASSIMP_LOAD_TEXTURES(false);
        //im.setIoHandler(new AssetIOSystem(store.parent()));
    }

    class VertexBoneWeights {
        float[] weights = new float[4];
        int[] bones = new int[4];
        int slot = 0;
        float totalWeight = 0.0f;
    }

    ReentrantLock lock = new ReentrantLock();

    public Model load(Asset mainAsset) throws MeshLoadException {
        if (mainAsset == null)
            throw new MeshLoadException(mainAsset);

        try {
            lock.lock();

            AIScene scene = Assimp.aiImportFileEx(mainAsset.getName(), 0, ioHandler);

            if (scene == null) {
                logger.error("Could not load meshes from asset: " + mainAsset);
                throw new MeshLoadException(mainAsset);
            }

            if (scene.mNumMeshes() == 0) {
                logger.error("Loaded mesh did not contain any mesh data.");
                return null;
            }

            List<Mesh> meshes = new ArrayList<>();

            FloatArrayList vertices = new FloatArrayList();
            FloatArrayList normals = new FloatArrayList();
            FloatArrayList texcoords = new FloatArrayList();

            Map<String, Integer> boneNamesToIds = new HashMap<>();
            ByteArrayList boneIds = new ByteArrayList();
            ByteArrayList boneWeights = new ByteArrayList();

            String assetFolder = mainAsset.getName().substring(0, mainAsset.getName().lastIndexOf('/') + 1);

            int[] order = { 0, 1, 2 };

            // For each submesh ...
            for (int i = 0; i < scene.mNumMeshes(); i++) {
                AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(i));
                int firstVertex = vertices.size() / 3;

                AIMaterial material = AIMaterial.create(scene.mMaterials().get(aiMesh.mMaterialIndex()));
                HashMap<String, String> materialTextures = new HashMap<>();

                /*for (int j = 0; j < material.mNumProperties(); j++) {
                    AIMaterialProperty property = AIMaterialProperty.create(material.mProperties().get(i));
                    switch (property.mKey().dataString()) {
                        case Assimp.
                    }
                }*/

                /*int texturesCount = Assimp.aiGetMaterialTextureCount(material, Assimp.aiTextureType_DIFFUSE);
                for (int j = 0; j < texturesCount; j++) {
                    Assimp.aiGetMaterialTexture(material, )
                }

                    switch (tex.getType()) {
                    case ambient:
                        break;
                    case diffuse:
                        materialTextures.put("albedoTexture", FoldersUtils.combineNames(assetFolder, tex.getFile()));
                        break;
                    case displacement:
                        break;
                    case emissive:
                        break;
                    case height:
                        break;
                    case lightmap:
                        break;
                    case none:
                        break;
                    case normals:
                        materialTextures.put("normalTexture", FoldersUtils.combineNames(assetFolder, tex.getFile()));
                        break;
                    case opacity:
                        materialTextures.put("aoTexture", FoldersUtils.combineNames(assetFolder, tex.getFile()));
                        break;
                    case reflection:
                        break;
                    case shininess:
                        materialTextures.put("roughnessTexture", FoldersUtils.combineNames(assetFolder, tex.getFile()));
                        break;
                    case specular:
                        materialTextures.put("metallicTexture", FoldersUtils.combineNames(assetFolder, tex.getFile()));
                        break;
                    case unknown:
                        break;
                    default:
                        break;

                    }
                }*/

                boolean hasAnimationData = aiMesh.mNumBones() > 0;
                Map<Integer, VertexBoneWeights> boneWeightsForeachVertex = null;

                if (hasAnimationData) {
                    boneWeightsForeachVertex = new HashMap<>();

                    // Create objects to receive the animation data for all the vertices of this submesh
                    for (int j = 0; j < aiMesh.mNumVertices(); j++) {
                        boneWeightsForeachVertex.put(firstVertex + j, new VertexBoneWeights());
                    }

                    // For each bone used in the submesh
                    for (int j = 0; j < aiMesh.mNumBones(); j++) {
                        AIBone aiBone = AIBone.create(aiMesh.mBones().get(j));
                        String boneName = aiBone.mName().dataString();
                        boneName = boneName.substring(boneName.lastIndexOf('_') + 1);

                        // Maps a bone id to a bone name (maybe useful later!)
                        // TODO check if Assimp's bone ordering is the same as BVH, and if we need this
                        int boneId = boneNamesToIds.getOrDefault(boneName, -1);
                        if (boneId == -1) {
                            boneId = boneNamesToIds.size();
                            boneNamesToIds.put(boneName, boneId);
                        }

                        // For each weight this bone is applying
                        for (int k = 0; k < aiBone.mNumWeights(); k++) {
                            AIVertexWeight aiWeight = aiBone.mWeights().get(k);
                            int vertexId = firstVertex + aiWeight.mVertexId();
                            VertexBoneWeights vertexBoneWeights = boneWeightsForeachVertex.get(vertexId);

                            // Write the weight and bone information to the next available slot in that vertex
                            vertexBoneWeights.bones[vertexBoneWeights.slot] = boneId;
                            vertexBoneWeights.weights[vertexBoneWeights.slot] = aiWeight.mWeight();
                            vertexBoneWeights.slot++;

                            vertexBoneWeights.totalWeight += aiWeight.mWeight();

                            if (vertexBoneWeights.totalWeight > 1.0f) {
                                logger.warn("Total weight > 1 for vertex #" + vertexId);
                            }
                            if (vertexBoneWeights.slot >= 4) {
                                logger.error("More than 4 bones weighted against vertex #" + vertexId);
                                return null;
                            }
                        }
                    }
                }

                // Now onto the main course, we need the actual mesh data
                for (int j = 0; j < aiMesh.mNumFaces(); j++) {
                    AIFace aiFace = aiMesh.mFaces().get(j);
                    if (aiFace.mNumIndices() == 3) {
                        AIVector3D.Buffer buf = new AIVector3D.Buffer(aiMesh.mTextureCoords().get(0), aiMesh.mNumVertices());
                        for (int o : order) { // swap vertices order
                            int vertexID = aiFace.mIndices().get(o);
                            AIVector3D vertex = aiMesh.mVertices().get(vertexID);
                            AIVector3D normal = aiMesh.mNormals().get(vertexID);
                            AIVector3D texcoord = buf.get(vertexID);

                            if (mainAsset.getName().endsWith("dae")) {
                                // swap Y and Z axises
                                //vertices.add(vertex.x, vertex.z, -vertex.y);
                                //normals.add(normal.x, normal.z, -normal.y);

                                vertices.add(vertex.y(), vertex.z(), vertex.x());
                                normals.add(normal.y(), normal.z(), normal.x());

                                //vertices.add(vertex.getY(), vertex.getZ(), vertex.getX());
                                //normals.add(normal.getY(), normal.getZ(), normal.getX());
                            } else {
                                vertices.add(vertex.x(), vertex.y(), vertex.z());
                                normals.add(normal.x(), normal.y(), normal.z());

                                //vertices.add(vertex.getX(), vertex.getY(), vertex.getZ());
                                //normals.add(normal.getX(), normal.getY(), normal.getZ());
                            }

                            texcoords.add(texcoord.x(), 1.0f - texcoord.y());

                            if (hasAnimationData) {
                                VertexBoneWeights boned = boneWeightsForeachVertex.get(firstVertex + vertexID);
                                boneIds.add((byte) boned.bones[0]);
                                boneIds.add((byte) boned.bones[1]);
                                boneIds.add((byte) boned.bones[2]);
                                boneIds.add((byte) boned.bones[3]);

                                boneWeights.add((byte) (boned.weights[0] * 255));
                                boneWeights.add((byte) (boned.weights[1] * 255));
                                boneWeights.add((byte) (boned.weights[2] * 255));
                                boneWeights.add((byte) (boned.weights[3] * 255));
                            }
                        }
                    } else
                        logger.warn("Should triangulate! (face=" + aiFace.mIndices() + ")");
                }

                AIString aiMaterialName = AIString.create();
                Assimp.aiGetMaterialString(material, Assimp.AI_MATKEY_NAME, 0, 0, aiMaterialName);
                String materialName = aiMaterialName.dataString();
                MeshMaterial meshMaterial = new MeshMaterial(materialName, materialTextures, "opaque");

                int verticesCount = vertices.size() / 3;

                if (verticesCount == 0)
                    continue;

                List<MeshAttributeSet> attributes = new LinkedList<>();
                attributes.add(new MeshAttributeSet("vertexIn", 3, VertexFormat.FLOAT, toByteBuffer(vertices)));
                attributes.add(new MeshAttributeSet("normalIn", 3, VertexFormat.FLOAT, toByteBuffer(normals)));
                attributes.add(new MeshAttributeSet("texCoordIn", 2, VertexFormat.FLOAT, toByteBuffer(texcoords)));
                if (hasAnimationData) {
                    attributes.add(new MeshAttributeSet("boneIdIn", 4, VertexFormat.BYTE, toByteBuffer(boneIds)));
                    attributes.add(new MeshAttributeSet("boneWeightIn", 4, VertexFormat.NORMALIZED_UBYTE, toByteBuffer(boneWeights)));
                }

                vertices.clear();
                normals.clear();
                texcoords.clear();

                boneIds.clear();
                boneWeights.clear();

            /*String[] boneNamesArray = null;
            if(hasAnimationData) {
                // TODO unused, left in because might be needed, see earlier in the file
                boneNamesArray = new String[boneNamesToIds.size()];
                for (Entry<String, Integer> e : boneNamesToIds.entrySet()) {
                    boneNamesArray[e.getValue()] = e.getKey();
                }
            }*/

                if (!hasAnimationData)
                    boneNamesToIds = null;

                meshes.add(new Mesh(verticesCount, attributes, meshMaterial, boneNamesToIds));
            }

            return new Model(meshes);
        } finally {
            lock.unlock();
        }
        //throw new RuntimeException("TODO");
    }

    private ByteBuffer toByteBuffer(FloatArrayList array) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.size() * 4).order(ByteOrder.nativeOrder());
        for (int i = 0; i < array.size(); i++) {
            byteBuffer.putFloat(array.get(i));
        }
        byteBuffer.flip();
        return byteBuffer;
    }

    private ByteBuffer toByteBuffer(ByteArrayList array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(array.size()).order(ByteOrder.nativeOrder());
        for (int i = 0; i < array.size(); i++) {
            bb.put(i, array.get(i));
        }
        bb.position(0);
        bb.limit(bb.capacity());
        return bb;
    }
}
