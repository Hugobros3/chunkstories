package xyz.chunkstories.util.math;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HeightmapMesherTest {

    @Test
    public void testNextSurface() {
        ArrayList<Integer> mapLoad = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            mapLoad.add(i);
        }
        int[] map = mapLoad.stream().mapToInt(i -> i).toArray();
        int[] id_list = mapLoad.stream().mapToInt(i -> i).toArray();

        final HeightmapMesher heightMap = new HeightmapMesher(map, id_list, 1, 3, 1, 1, 500);

        HeightmapMesher.Surface surface = heightMap.nextSurface();
        assertEquals(0, surface.getX());
        assertEquals(0, surface.getY());
        assertEquals(1, surface.getW());
        assertEquals(1, surface.getH());
        assertEquals(502, surface.getLevel());
        assertEquals(502, surface.getId());

        HeightmapMesher.Surface surface2 = heightMap.nextSurface();
        assertEquals(0, surface2.getX());
        assertEquals(1, surface2.getY());
        assertEquals(1, surface2.getW());
        assertEquals(1, surface2.getH());
        assertEquals(503, surface2.getLevel());
        assertEquals(503, surface2.getId());

        HeightmapMesher.Surface surface3 = heightMap.nextSurface();
        assertEquals(0, surface3.getX());
        assertEquals(2, surface3.getY());
        assertEquals(1, surface3.getW());
        assertEquals(1, surface3.getH());
        assertEquals(504, surface3.getLevel());
        assertEquals(504, surface3.getId());
    }

    @Test
    public void testNextSurface_nullReturn() {
        final int[] map = {1, 2, 3, 4, 5};
        final int[] id_list = {1, 2, 3, 4, 5};
        final HeightmapMesher heightMap = new HeightmapMesher(map, id_list, 0, 1, 1, 1, 2);
        heightMap.n = 3;

        assertNull(heightMap.nextSurface());
    }
}
