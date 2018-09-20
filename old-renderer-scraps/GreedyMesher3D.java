//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.scrap;

import java.util.ArrayList;
import java.util.List;

//No copyright notice, it's a direct transcribe of 0fps's JS greedy meshing example

public class GreedyMesher3D {
	public static List<int[][]> greedy(int[] volume, int[] dims) {
		List<int[][]> quads = new ArrayList<int[][]>();
		// Sweep over 3-axes
		for (int d = 0; d < 3; ++d) {
			int i, j, k, l;
			int w, h;

			int u = (d + 1) % 3;
			int v = (d + 2) % 3;

			int[] x = { 0, 0, 0 };
			int[] q = { 0, 0, 0 };

			boolean[] mask = new boolean[dims[u] * dims[v]];
			q[d] = 1;
			for (x[d] = -1; x[d] < dims[d];) {
				// Compute mask
				int n = 0;
				for (x[v] = 0; x[v] < dims[v]; ++x[v])
					for (x[u] = 0; x[u] < dims[u]; ++x[u]) {
						mask[n++] = ((0 <= x[d]) ? f(volume, dims, x[0], x[1], x[2]) : false) != ((x[d] < dims[d] - 1)
								? f(volume, dims, x[0] + q[0], x[1] + q[1], x[2] + q[2])
								: false);
					}
				// Increment x[d]
				++x[d];
				// Generate mesh for mask using lexicographic ordering
				n = 0;
				for (j = 0; j < dims[v]; ++j)
					for (i = 0; i < dims[u]; ++i) {
						if (mask[n]) {
							// Compute width
							for (w = 1; mask[n + w] && i + w < dims[u]; ++w) {
								// Nothing, the loop does it
							}
							// Compute height ( awkward )
							boolean done = false;
							for (h = 1; j + h < dims[v]; ++h) {
								for (k = 0; k < w; ++k) {
									if (!mask[n + k + h * dims[u]]) {
										done = true;
										break;
									}
								}
								if (done)
									break;
							}
							// Add quad
							x[u] = i;
							x[v] = j;
							int du[] = { 0, 0, 0 };
							int dv[] = { 0, 0, 0 };
							du[u] = w;
							dv[v] = h;
							quads.add(new int[][] { new int[] { x[0], x[1], x[2] },
									new int[] { x[0] + du[0], x[1] + du[1], x[2] + du[2] },
									new int[] { x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2] },
									new int[] { x[0] + dv[0], x[1] + dv[1], x[2] + dv[2] } });
							// Zero-out mask
							for (l = 0; l < h; ++l)
								for (k = 0; k < w; ++k)
									mask[n + k + l * dims[u]] = false;
							// Incrmeent counters and continue
							i += w;
							n += w;
						} else {
							++i;
							++n;
						}
					}

			}
		}
		return quads;
	}

	static boolean f(int[] volume, int[] dims, int i, int j, int k) {
		return volume[i + dims[0] * (j + dims[1] * k)] != 0;
	}
}
