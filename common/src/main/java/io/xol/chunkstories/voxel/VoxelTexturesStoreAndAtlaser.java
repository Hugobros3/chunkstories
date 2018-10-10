//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import io.xol.chunkstories.api.client.Client;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.graphics.Texture2D;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.content.mods.AssetHierarchy;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.util.CasterIterator;

public class VoxelTexturesStoreAndAtlaser implements Content.Voxels.VoxelTextures {
    Map<String, VoxelTextureAtlased> texMap = new HashMap<String, VoxelTextureAtlased>();
    int uniquesIds = 0;
    // static Map<String, Vector4f> colors = new HashMap<String, Vector4f>();

    private int BLOCK_ATLAS_SIZE;
    private int BLOCK_ATLAS_FACTOR;

    private final GameContentStore content;
    private final VoxelsStore voxels;

    BufferedImage diffuseTextureImage = null;
    BufferedImage normalTextureImage = null;
    BufferedImage materialTextureImage = null;

    public static final Logger logger = LoggerFactory.getLogger("content.voxels.textures");

    public Logger logger() {
        return logger;
    }

    public VoxelTexturesStoreAndAtlaser(VoxelsStore voxels) {
        this.content = voxels.parent();
        this.voxels = voxels;
        //this.buildTextureAtlas();
    }

    public void buildTextureAtlas() {
        logger.debug("Rebuilding texture atlas");
        try {
            // Clear previous values
            texMap.clear();

            // Compute getAllVoxelComponents sizes first.
            int totalSurfacedNeeded = 0;
            // Get getAllVoxelComponents sizes :
            List<VoxelTextureAtlased> voxelTexturesSortedBySize = new ArrayList<VoxelTextureAtlased>();

            // First we want to iterate over every file to getVoxelComponent an idea of how many textures
            // (and of how many sizes) we are dealing
            for (AssetHierarchy entry : content.modsManager().getAllUniqueEntries()) {
                if (entry.getName().startsWith("voxels/textures/")) {
                    String name = entry.getName().replace("voxels/textures/", "");

                    Asset asset = entry.getTopInstance();
                    // For now only PNG is supported TODO: .hdr and more ?
                    if (asset.getName().endsWith(".png")) {
                        String textureName = name.replace(".png", "").replace("/", ".").replace("\\", ".");
                        if (textureName.endsWith("_normal") || textureName.endsWith("_roughness") || textureName.endsWith("_metalness")
                                || textureName.endsWith("_n") || textureName.endsWith("_r") || textureName.endsWith("_m")) {
                            // Don't create entries for complementary textures!
                            continue;
                        }

                        if (asset.getName().contains("/_"))
                            continue; // ignore directories starting with _

                        if (!texMap.containsKey(textureName)) {
                            VoxelTextureAtlased voxelTexture = new VoxelTextureAtlased(textureName, asset, uniquesIds);
                            uniquesIds++;

                            voxelTexture.imageFileDimensions = getImageSize(asset);

                            voxelTexturesSortedBySize.add(voxelTexture);
                            totalSurfacedNeeded += voxelTexture.imageFileDimensions * voxelTexture.imageFileDimensions;
                        }
                    }
                }
            }
            // Sort them by size
            Collections.sort(voxelTexturesSortedBySize, new Comparator<VoxelTextureAtlased>() {
                @Override
                public int compare(VoxelTextureAtlased a, VoxelTextureAtlased b) {
                    return Integer.compare(b.imageFileDimensions, a.imageFileDimensions);
                }
            });
            for (VoxelTextureAtlased voxelTexture : voxelTexturesSortedBySize) {
                // System.out.println(vt.imageFileDimensions);
                texMap.put(voxelTexture.getName(), voxelTexture);
            }

            // Estimates the required texture atlas size by surface
            int sizeRequired = 16;
            for (int i = 4; i < 14; i++) {
                int iSize = (int) Math.pow(2, i);
                if (iSize * iSize >= totalSurfacedNeeded) {
                    sizeRequired = iSize;
                    break;
                }
            }

            // ChunkStoriesLogger.getInstance().worldInfo("At least " + sizeRequired + " by " +
            // sizeRequired + " for TextureAtlas (surfacedNeeded : " + totalSurfacedNeeded +
            // ")");

            // Delete previous atlases
            File diffuseTextureFile = new File(GameDirectory.getGameFolderPath() + "/cache/tiles_merged_albedo.png");
            if (diffuseTextureFile.exists())
                diffuseTextureFile.delete();

            File normalTextureFile = new File(GameDirectory.getGameFolderPath() + "/cache/tiles_merged_normal.png");
            if (normalTextureFile.exists())
                normalTextureFile.delete();

            File materialTextureFile = new File(GameDirectory.getGameFolderPath() + "/cache/tiles_merged_material.png");
            if (materialTextureFile.exists())
                materialTextureFile.delete();

            // Build the new one
            boolean loadedOK = false;

            while (!loadedOK && sizeRequired <= 16384) // Security to prevend
            // HUGE-ASS textures
            {
                // We need this
                BLOCK_ATLAS_SIZE = sizeRequired;
                BLOCK_ATLAS_FACTOR = 32768 / BLOCK_ATLAS_SIZE;
                loadedOK = true;
                // Create boolean bitfield
                boolean[][] used = new boolean[sizeRequired / 16][sizeRequired / 16];

                diffuseTextureImage = null;
                normalTextureImage = null;
                materialTextureImage = null;

                if (content.getContext() instanceof Client) {
                    diffuseTextureImage = new BufferedImage(sizeRequired, sizeRequired, Transparency.TRANSLUCENT);
                    normalTextureImage = new BufferedImage(sizeRequired, sizeRequired, Transparency.TRANSLUCENT);
                    materialTextureImage = new BufferedImage(sizeRequired, sizeRequired, Transparency.TRANSLUCENT);

                    logger.debug("This is a client so we'll make the texture atlas");
                }

                BufferedImage tileAlbedoBuffer;
                BufferedImage tileNormalBuffer;

                BufferedImage tileRoughnessBuffer;
                BufferedImage tileMetalnessBuffer;

                for (VoxelTextureAtlased voxelTexture : voxelTexturesSortedBySize) {
                    // Find a free spot on the atlas
                    boolean foundSpot = false;
                    int spotX = 0, spotY = 0;
                    for (int a = 0; (a < sizeRequired / 16 && !foundSpot); a++)
                        for (int b = 0; (b < sizeRequired / 16 && !foundSpot); b++) {
                            if (used[a][b] == false && a + voxelTexture.imageFileDimensions / 16 <= sizeRequired / 16
                                    && b + voxelTexture.imageFileDimensions / 16 <= sizeRequired / 16) // Unused
                            {
                                boolean usedAlready = false;
                                // Not pretty loops that do clamped space checks
                                for (int i = 0; (i < voxelTexture.imageFileDimensions / 16 && a + i < sizeRequired / 16); i++)
                                    for (int j = 0; (j < voxelTexture.imageFileDimensions / 16 && b + j < sizeRequired / 16); j++)
                                        if (used[a + i][b + j] == true)
                                            usedAlready = true;

                                if (!usedAlready) {
                                    spotX = a * 16;
                                    spotY = b * 16;
                                    voxelTexture.setAtlasS(spotX * BLOCK_ATLAS_FACTOR);
                                    voxelTexture.setAtlasT(spotY * BLOCK_ATLAS_FACTOR);
                                    voxelTexture.setAtlasOffset(voxelTexture.imageFileDimensions * BLOCK_ATLAS_FACTOR);
                                    foundSpot = true;
                                    for (int i = 0; (i < voxelTexture.imageFileDimensions / 16 && a + i < sizeRequired / 16); i++)
                                        for (int j = 0; (j < voxelTexture.imageFileDimensions / 16 && b + j < sizeRequired / 16); j++)
                                            used[a + i][b + j] = true;
                                }
                            }
                        }
                    if (!foundSpot) {
                        this.logger().warn("Failed to find a space to place the texture in. Retrying with a larger atlas.");
                        loadedOK = false;
                        break;
                    }

                    String assetName = voxelTexture.getAsset().getName();

                    // Removes the file extension
                    String strippedAssetName = assetName.substring(0, assetName.length() - ".png".length());

                    try {
                        tileAlbedoBuffer = ImageIO.read(content.modsManager().getAsset(strippedAssetName + ".png").read());
                    } catch (NullPointerException e) {
                        System.out.println("voxels/textures/" + voxelTexture.getName() + ".png");
                        throw e;
                    }

                    float alphaTotal = 0;
                    int nonNullPixels = 0;
                    Vector3f color = new Vector3f();
                    for (int x = 0; x < voxelTexture.imageFileDimensions; x++) {
                        for (int y = 0; y < voxelTexture.imageFileDimensions; y++) {
                            int rgb = tileAlbedoBuffer.getRGB(x, y);

                            if (diffuseTextureImage != null)
                                diffuseTextureImage.setRGB(spotX + x, spotY + y, rgb);

                            float alpha = ((rgb & 0xFF000000) >>> 24) / 255f;
                            // System.out.println("a:"+alpha);
                            alphaTotal += alpha;
                            if (alpha > 0)
                                nonNullPixels++;
                            float red = ((rgb & 0xFF0000) >> 16) / 255f * alpha;
                            float green = ((rgb & 0x00FF00) >> 8) / 255f * alpha;
                            float blue = (rgb & 0x0000FF) / 255f * alpha;

                            color.add(new Vector3f(red, green, blue));
                            // Vector3f.add(color, new Vector3f(red, green, blue), color);
                        }
                    }

                    color.mul(1f / alphaTotal);
                    if (nonNullPixels > 0)
                        alphaTotal /= nonNullPixels;

                    voxelTexture.setColor(new Vector4f(color.x(), color.y(), color.z(), alphaTotal));

                    // Don't bother if it's not a Client context
                    if (diffuseTextureImage == null)
                        continue;

                    // Do also the normal maps !
                    Asset normalMap = content.modsManager().getAsset(strippedAssetName + "_normal.png");
                    if (normalMap == null)
                        normalMap = content.modsManager().getAsset(strippedAssetName + "_n.png");
                    if (normalMap == null)
                        normalMap = content.modsManager().getAsset("voxels/textures/notex_normal.png");

                    tileNormalBuffer = ImageIO.read(normalMap.read());
                    for (int x = 0; x < voxelTexture.imageFileDimensions; x++) {
                        for (int y = 0; y < voxelTexture.imageFileDimensions; y++) {
                            int rgb = tileNormalBuffer.getRGB(x % tileNormalBuffer.getWidth(), y % tileNormalBuffer.getHeight());
                            normalTextureImage.setRGB(spotX + x, spotY + y, rgb);
                        }
                    }

                    Asset roughnessMap = content.modsManager().getAsset(strippedAssetName + "_roughness.png");
                    if (roughnessMap == null)
                        roughnessMap = content.modsManager().getAsset(strippedAssetName + "_r.png");
                    if (roughnessMap == null)
                        roughnessMap = content.modsManager().getAsset("voxels/textures/notex_roughness.png");

                    tileRoughnessBuffer = ImageIO.read(roughnessMap.read());

                    Asset metalnessMap = content.modsManager().getAsset(strippedAssetName + "_metalness.png");
                    if (metalnessMap == null)
                        metalnessMap = content.modsManager().getAsset(strippedAssetName + "_m.png");
                    if (metalnessMap == null)
                        metalnessMap = content.modsManager().getAsset("voxels/textures/notex_metalness.png");

                    tileMetalnessBuffer = ImageIO.read(metalnessMap.read());

                    for (int x = 0; x < voxelTexture.imageFileDimensions; x++) {
                        for (int y = 0; y < voxelTexture.imageFileDimensions; y++) {
                            int roughnessInt = (tileRoughnessBuffer.getRGB(x % tileRoughnessBuffer.getWidth(), y % tileRoughnessBuffer.getHeight())
                                    & 0xFF0000) >> 16;
                            int metalnessInt = (tileMetalnessBuffer.getRGB(x % tileMetalnessBuffer.getWidth(), y % tileMetalnessBuffer.getHeight())
                                    & 0xFF0000) >> 16;

                            float roughness = roughnessInt / 255f;
                            float metalness = metalnessInt / 255f;

                            // int rgb = imageBuffer.getRGB(x % imageBuffer.getWidth(), y %
                            // imageBuffer.getHeight());
                            int rgb = roughnessInt << 16 | metalnessInt << 8 | 0xFF << 24;
                            materialTextureImage.setRGB(spotX + x, spotY + y, rgb);
                        }
                    }
                }
                if (loadedOK && diffuseTextureImage != null) {
                    // save it son
                    ImageIO.write(diffuseTextureImage, "PNG", diffuseTextureFile);
                    ImageIO.write(normalTextureImage, "PNG", normalTextureFile);
                    ImageIO.write(materialTextureImage, "PNG", materialTextureFile);

                    diffuseTexture = null;
                    normalTexture = null;
                    materialTexture = null;
                } else
                    // It's too small, initial estimation was wrong !
                    sizeRequired *= 2;
            }
            // Read textures metadata
            // TODO read getAllVoxelComponents overrides in priority
            readTexturesMeta(content.modsManager().getAsset("voxels/textures/meta.txt"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //this.texMap.entrySet().forEach(entry -> System.out.println("Available voxel texture: "+entry.getValue().getName() + " as "+entry.getKey()));
    }

    private void readTexturesMeta(Asset asset) {
        if (asset == null)
            return;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(asset.read()));

            String line = "";

            VoxelTextureAtlased vt = null;
            int ln = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    // It's a comment, ignore.
                } else {
                    if (line.startsWith("texture")) {
                        if (vt != null)
                            logger.warn("Parse error in file " + asset + ", line " + ln + ", unexpected 'texture' token.");
                        String splitted[] = line.split(" ");
                        String name = splitted[1];

                        vt = texMap.get(name);
                    } else if (line.startsWith("end")) {
                        if (vt != null) {
                            vt = null;
                        } else
                            logger.warn("Parse error in file " + asset + ", line " + ln + ", unexpected 'end' token.");
                    } else if (line.startsWith("\t")) {
                        if (vt != null) {
                            // System.out.println("Debug : loading voxel parameter : "+line);
                            String splitted[] = (line.replace(" ", "").replace("\t", "")).split(":");
                            String parameterName = splitted[0];
                            String parameterValue = splitted[1];
                            switch (parameterName) {
                                case "textureScale":
                                    vt.setTextureScale(Integer.parseInt(parameterValue));
                                    break;
                                default:
                                    logger.warn("Parse error in file " + asset + ", line " + ln + ", unknown parameter '" + parameterName + "'");
                                    break;
                            }
                        } else
                            logger.warn("Parse error in file " + asset + ", line " + ln + ", unexpected parameter.");
                    }
                }
                ln++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getImageSize(Asset asset) {
        try {
            ImageReader reader = ImageIO.getImageReadersBySuffix("png").next();
            ImageInputStream stream = ImageIO.createImageInputStream(asset.read());
            reader.setInput(stream);
            int size = reader.getWidth(reader.getMinIndex());
            return size;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public VoxelTexture getVoxelTexture(String textureName) {
        if (texMap.containsKey(textureName))
            return texMap.get(textureName);
        return texMap.get("notex");
    }

    public Iterator<VoxelTexture> all() {
        return new CasterIterator<>(texMap.values().iterator());
    }

    @Override
    public Voxels parent() {
        return voxels;
    }

    //TODO move that to implem
    private Texture2D getTextureFromBufferedImage(BufferedImage image) {
        throw new UnsupportedOperationException();

//        int[] data = new int[image.getWidth() * image.getHeight()];
//        image.getRGB(0, 0, image.getWidth(), image.getHeight(), data, 0, image.getWidth());
//
//        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * image.getWidth() * image.getHeight());// BufferUtils.createByteBuffer(4
//        // * image.getWidth() *
//        // image.getHeight());
//
//        for (int y = 0; y < image.getHeight(); y++) {
//            for (int x = 0; x < image.getWidth(); x++) {
//                int pixel = data[y * image.getWidth() + x];
//                buffer.put((byte) ((pixel >> 16) & 0xFF));
//                buffer.put((byte) ((pixel >> 8) & 0xFF));
//                buffer.put((byte) (pixel & 0xFF));
//                buffer.put((byte) ((pixel >> 24) & 0xFF));
//            }
//        }
//
//        buffer.flip();
//
//        Texture2D texture = ((ClientContent) parent().parent()).textures().newTexture2D(TextureFormat.RGBA_8BPP, image.getWidth(), image.getHeight());
//        texture.uploadTextureData(image.getWidth(), image.getHeight(), buffer);
//
//        return texture;
    }

    private Texture2D diffuseTexture = null;
    private Texture2D normalTexture = null;
    private Texture2D materialTexture = null;

    public Texture2D getDiffuseAtlasTexture() {
        Texture2D diffuseTexture = this.diffuseTexture;
        if (diffuseTexture == null) {
            diffuseTexture = getTextureFromBufferedImage(diffuseTextureImage);
            this.diffuseTexture = diffuseTexture;
        }
        return diffuseTexture;
    }

    public Texture2D getNormalAtlasTexture() {
        Texture2D normalTexture = this.normalTexture;
        if (normalTexture == null) {
            normalTexture = getTextureFromBufferedImage(normalTextureImage);
            this.normalTexture = normalTexture;
        }
        return normalTexture;
    }

    public Texture2D getMaterialAtlasTexture() {
        Texture2D materialTexture = this.materialTexture;
        if (materialTexture == null) {
            materialTexture = getTextureFromBufferedImage(materialTextureImage);
            this.materialTexture = materialTexture;
        }
        return materialTexture;
    }

    @NotNull
    @Override
    public VoxelTexture getDefaultVoxelTexture() {
        return getVoxelTexture("notex");
    }
}
