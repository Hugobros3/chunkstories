//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.mesh;

import assimp.*;
import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.hppc.FloatArrayList;
import glm_.vec3.Vec3;
import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.exceptions.content.MeshLoadException;
import xyz.chunkstories.api.graphics.*;
import xyz.chunkstories.api.graphics.representation.Model;
import xyz.chunkstories.util.FoldersUtils;
import kotlin.ranges.IntRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.Map.Entry;

public class AssimpMeshLoader {

    private static final Logger logger = LoggerFactory.getLogger("content.meshes.assimp-kotlin");

    final MeshStore store;

    Importer im = new Importer();

    public AssimpMeshLoader(MeshStore meshStore) {
        store = meshStore;

        assimp.SettingsKt.setASSIMP_LOAD_TEXTURES(false);
        im.setIoHandler(new AssetIOSystem(store.parent()));
    }

    class VertexBoneWeights {
        float[] weights = new float[4];
        int[] bones = new int[4];
        int slot = 0;
        float totalWeight = 0.0f;
    }

    public Model load(Asset mainAsset) throws MeshLoadException {
        if (mainAsset == null)
            throw new MeshLoadException(mainAsset);

        AiScene scene = im.readFile(mainAsset.getName(), im.getIoHandler(), 0);

        if (scene == null) {
            logger.error("Could not load meshes from asset: " + mainAsset);
            throw new MeshLoadException(mainAsset);
        }

        scene.getMeshes();
        if (scene.getMeshes().size() == 0) {
            logger.error("Loaded mesh did not contain any mesh data.");
            return null;
        }

        List<Mesh> meshes = new LinkedList<>();

        FloatArrayList vertices = new FloatArrayList();
        FloatArrayList normals = new FloatArrayList();
        FloatArrayList texcoords = new FloatArrayList();

        Map<String, Integer> boneNamesToIds = new HashMap<>();
        ByteArrayList boneIds = new ByteArrayList();
        ByteArrayList boneWeights = new ByteArrayList();

        String assetFolder = mainAsset.getName().substring(0, mainAsset.getName().lastIndexOf('/') + 1);

        int[] order = {0, 1, 2};

        // For each submesh ...
        for (AiMesh aiMesh : scene.getMeshes()) {
            int firstVertex = vertices.size() / 3;

            AiMaterial material = scene.getMaterials().get(aiMesh.getMaterialIndex());
            HashMap<String, String> materialTextures = new HashMap<>();

            for (AiMaterial.Texture tex : material.getTextures()) {
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
            }

            boolean hasAnimationData = aiMesh.getHasBones();
            Map<Integer, VertexBoneWeights> boneWeightsForeachVertex = null;

            if (hasAnimationData) {
                boneWeightsForeachVertex = new HashMap<>();

                // Create objects to receive the animation data for all the vertices of this submesh
                for (int i = 0; i < aiMesh.getNumVertices(); i++) {
                    boneWeightsForeachVertex.put(firstVertex + i, new VertexBoneWeights());
                }

                // For each bone used in the submesh
                for (AiBone aiBone : aiMesh.getBones()) {
                    String boneName = aiBone.getName().substring(aiBone.getName().lastIndexOf('_') + 1);

                    // Maps a bone id to a bone name (maybe useful later!)
                    // TODO check if Assimp's bone ordering is the same as BVH, and if we need this
                    int boneId = boneNamesToIds.getOrDefault(boneName, -1);
                    if (boneId == -1) {
                        boneId = boneNamesToIds.size();
                        boneNamesToIds.put(boneName, boneId);
                    }

                    // For each weight this bone is applying
                    for (AiVertexWeight aiWeight : aiBone.getWeights()) {
                        int vertexId = firstVertex + aiWeight.getVertexId();
                        VertexBoneWeights vertexBoneWeights = boneWeightsForeachVertex.get(vertexId);

                        // Write the weight and bone information to the next available slot in that vertex
                        vertexBoneWeights.bones[vertexBoneWeights.slot] = boneId;
                        vertexBoneWeights.weights[vertexBoneWeights.slot] = aiWeight.getWeight();
                        vertexBoneWeights.slot++;

                        vertexBoneWeights.totalWeight += aiWeight.getWeight();

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
            for (List<Integer> aiFace : aiMesh.getFaces()) {
                if (aiFace.size() == 3) {
                    for (int i : order) { // swap vertices order
                        Vec3 vertex = aiMesh.getVertices().get(aiFace.get(i));
                        Vec3 normal = aiMesh.getNormals().get(aiFace.get(i));
                        float[] texcoord = aiMesh.getTextureCoords().get(0).get(aiFace.get(i));

                        if (mainAsset.getName().endsWith("dae")) {
                            // swap Y and Z axises
                            vertices.add(vertex.x, vertex.z, -vertex.y);
                            normals.add(normal.x, normal.z, -normal.y);
                        } else {
                            vertices.add(vertex.x, vertex.y, vertex.z);
                            normals.add(normal.x, normal.y, normal.z);
                        }

                        texcoords.add(texcoord[0], 1.0f - texcoord[1]);

                        if (hasAnimationData) {
                            VertexBoneWeights boned = boneWeightsForeachVertex.get(firstVertex + aiFace.get(i));
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
                    logger.warn("Should triangulate! (face=" + aiFace.size() + ")");
            }

            String materialName = aiMesh.getName();
            MeshMaterial meshMaterial = new MeshMaterial(materialName, materialTextures, "opaque");

            int verticesCount = vertices.size() / 3;

            if(verticesCount == 0)
                continue;

            List<MeshAttributeSet> attributes = new LinkedList<>();
            attributes.add(new MeshAttributeSet("vertexIn", 3, VertexFormat.FLOAT, toByteBuffer(vertices)));
            attributes.add(new MeshAttributeSet("normalIn", 3, VertexFormat.FLOAT, toByteBuffer(normals)));
            attributes.add(new MeshAttributeSet("texCoordIn", 2, VertexFormat.FLOAT, toByteBuffer(texcoords)));
            if(hasAnimationData) {
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

            if(!hasAnimationData)
                boneNamesToIds = null;

            meshes.add(new Mesh(verticesCount, attributes, meshMaterial, boneNamesToIds));
        }

        return new Model(meshes);
        /*int verticesCount = vertices.size();
        return new Mesh(verticesCount, attributes, meshMaterials);*/
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
