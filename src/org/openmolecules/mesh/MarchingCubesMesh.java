/*
 * Copyright 2017 Thomas Sander, Therwilerstrasse 41, CH-4153 Reinach, Switzerland
 *
 * This file is part of openmolecules.org's 3D-Molecule-Viewer.
 *
 * 3D-Molecule-Viewer is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * 3D-Molecule-Viewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with 3D-Molecule-Viewer.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package org.openmolecules.mesh;

import com.actelion.research.chem.conf.VDWRadii;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.TreeMap;

public class MarchingCubesMesh implements VDWRadii {
	public static final float DEFAULT_PROBE_SIZE = 1.4f;	// angstrom
	protected static final float ISOLEVEL_VALUE = 5.0f;		// not 0 to save the array initialization to something else
	private static final float RADIUS_SURPLUS = 1.0f;		// voxel edge lengths to consider beyond sphere radii

	// Convention of edge and vertex indexes: 
	//	       4 ____________________ 5
	//         /|         4         /|
	//        / |                  / |
	//       /7 |                 /5 |
	//      /   |8               /   |9
	//    7/____|_______________/6   |
	//     |    |     6         |    |
	//     |   0|_______________|____|1
	//     |    /         0     |    /
	//   11|   /              10|   /
	//     |  /3                |  /1
	//     | /                  | /
	//     |/___________________|/
	//    3           2          2

	// Coordinate system
	//
	//       y |
	//         |
	//         |
	//         |
	//         |_____________
	//        /             x
	//       /
	//    z /

	public static final int[] EDGE_TABLE = {
		0x000, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
		0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
		0x190, 0x099, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
		0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
		0x230, 0x339, 0x033, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
		0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
		0x3a0, 0x2a9, 0x1a3, 0x0aa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
		0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
		0x460, 0x569, 0x663, 0x76a, 0x066, 0x16f, 0x265, 0x36c,
		0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
		0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0x0ff, 0x3f5, 0x2fc,
		0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
		0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x055, 0x15c,
		0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
		0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0x0cc,
		0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
		0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
		0x0cc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
		0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
		0x15c, 0x055, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
		0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
		0x2fc, 0x3f5, 0x0ff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
		0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
		0x36c, 0x265, 0x16f, 0x066, 0x76a, 0x663, 0x569, 0x460,
		0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
		0x4ac, 0x5a5, 0x6af, 0x7a6, 0x0aa, 0x1a3, 0x2a9, 0x3a0,
		0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
		0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x033, 0x339, 0x230,
		0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
		0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x099, 0x190,
		0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
		0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x000 };

	public static final int[][] FACE_TABLE =
		{{},
		{0, 8, 3},
		{0, 1, 9},
		{1, 8, 3, 9, 8, 1},
		{1, 2, 10},
		{0, 8, 3, 1, 2, 10},
		{9, 2, 10, 0, 2, 9},
		{2, 8, 3, 2, 10, 8, 10, 9, 8},
		{3, 11, 2},
		{0, 11, 2, 8, 11, 0},
		{1, 9, 0, 2, 3, 11},
		{1, 11, 2, 1, 9, 11, 9, 8, 11},
		{3, 10, 1, 11, 10, 3},
		{0, 10, 1, 0, 8, 10, 8, 11, 10},
		{3, 9, 0, 3, 11, 9, 11, 10, 9},
		{9, 8, 10, 10, 8, 11},
		{4, 7, 8},
		{4, 3, 0, 7, 3, 4},
		{0, 1, 9, 8, 4, 7},
		{4, 1, 9, 4, 7, 1, 7, 3, 1},
		{1, 2, 10, 8, 4, 7},
		{3, 4, 7, 3, 0, 4, 1, 2, 10},
		{9, 2, 10, 9, 0, 2, 8, 4, 7},
		{2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4},
		{8, 4, 7, 3, 11, 2},
		{11, 4, 7, 11, 2, 4, 2, 0, 4},
		{9, 0, 1, 8, 4, 7, 2, 3, 11},
		{4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1},
		{3, 10, 1, 3, 11, 10, 7, 8, 4},
		{1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4},
		{4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3},
		{4, 7, 11, 4, 11, 9, 9, 11, 10},
		{9, 5, 4},
		{9, 5, 4, 0, 8, 3},
		{0, 5, 4, 1, 5, 0},
		{8, 5, 4, 8, 3, 5, 3, 1, 5},
		{1, 2, 10, 9, 5, 4},
		{3, 0, 8, 1, 2, 10, 4, 9, 5},
		{5, 2, 10, 5, 4, 2, 4, 0, 2},
		{2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8},
		{9, 5, 4, 2, 3, 11},
		{0, 11, 2, 0, 8, 11, 4, 9, 5},
		{0, 5, 4, 0, 1, 5, 2, 3, 11},
		{2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5},
		{10, 3, 11, 10, 1, 3, 9, 5, 4},
		{4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10},
		{5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3},
		{5, 4, 8, 5, 8, 10, 10, 8, 11},
		{9, 7, 8, 5, 7, 9},
		{9, 3, 0, 9, 5, 3, 5, 7, 3},
		{0, 7, 8, 0, 1, 7, 1, 5, 7},
		{1, 5, 3, 3, 5, 7},
		{9, 7, 8, 9, 5, 7, 10, 1, 2},
		{10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3},
		{8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2},
		{2, 10, 5, 2, 5, 3, 3, 5, 7},
		{7, 9, 5, 7, 8, 9, 3, 11, 2},
		{9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11},
		{2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7},
		{11, 2, 1, 11, 1, 7, 7, 1, 5},
		{9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11},
		{5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0},
		{11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0},
		{11, 10, 5, 7, 11, 5},
		{10, 6, 5},
		{0, 8, 3, 5, 10, 6},
		{9, 0, 1, 5, 10, 6},
		{1, 8, 3, 1, 9, 8, 5, 10, 6},
		{1, 6, 5, 2, 6, 1},
		{1, 6, 5, 1, 2, 6, 3, 0, 8},
		{9, 6, 5, 9, 0, 6, 0, 2, 6},
		{5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8},
		{2, 3, 11, 10, 6, 5},
		{11, 0, 8, 11, 2, 0, 10, 6, 5},
		{0, 1, 9, 2, 3, 11, 5, 10, 6},
		{5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11},
		{6, 3, 11, 6, 5, 3, 5, 1, 3},
		{0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6},
		{3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9},
		{6, 5, 9, 6, 9, 11, 11, 9, 8},
		{5, 10, 6, 4, 7, 8},
		{4, 3, 0, 4, 7, 3, 6, 5, 10},
		{1, 9, 0, 5, 10, 6, 8, 4, 7},
		{10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4},
		{6, 1, 2, 6, 5, 1, 4, 7, 8},
		{1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7},
		{8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6},
		{7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9},
		{3, 11, 2, 7, 8, 4, 10, 6, 5},
		{5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11},
		{0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6},
		{9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6},
		{8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6},
		{5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11},
		{0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7},
		{6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9},
		{10, 4, 9, 6, 4, 10},
		{4, 10, 6, 4, 9, 10, 0, 8, 3},
		{10, 0, 1, 10, 6, 0, 6, 4, 0},
		{8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10},
		{1, 4, 9, 1, 2, 4, 2, 6, 4},
		{3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4},
		{0, 2, 4, 4, 2, 6},
		{8, 3, 2, 8, 2, 4, 4, 2, 6},
		{10, 4, 9, 10, 6, 4, 11, 2, 3},
		{0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6},
		{3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10},
		{6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1},
		{9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3},
		{8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1},
		{3, 11, 6, 3, 6, 0, 0, 6, 4},
		{6, 4, 8, 11, 6, 8},
		{7, 10, 6, 7, 8, 10, 8, 9, 10},
		{0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10},
		{10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0},
		{10, 6, 7, 10, 7, 1, 1, 7, 3},
		{1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7},
		{2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9},
		{7, 8, 0, 7, 0, 6, 6, 0, 2},
		{7, 3, 2, 6, 7, 2},
		{2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7},
		{2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7},
		{1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11},
		{11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1},
		{8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6},
		{0, 9, 1, 11, 6, 7},
		{7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0},
		{7, 11, 6},
		{7, 6, 11},
		{3, 0, 8, 11, 7, 6},
		{0, 1, 9, 11, 7, 6},
		{8, 1, 9, 8, 3, 1, 11, 7, 6},
		{10, 1, 2, 6, 11, 7},
		{1, 2, 10, 3, 0, 8, 6, 11, 7},
		{2, 9, 0, 2, 10, 9, 6, 11, 7},
		{6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8},
		{7, 2, 3, 6, 2, 7},
		{7, 0, 8, 7, 6, 0, 6, 2, 0},
		{2, 7, 6, 2, 3, 7, 0, 1, 9},
		{1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6},
		{10, 7, 6, 10, 1, 7, 1, 3, 7},
		{10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8},
		{0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7},
		{7, 6, 10, 7, 10, 8, 8, 10, 9},
		{6, 8, 4, 11, 8, 6},
		{3, 6, 11, 3, 0, 6, 0, 4, 6},
		{8, 6, 11, 8, 4, 6, 9, 0, 1},
		{9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6},
		{6, 8, 4, 6, 11, 8, 2, 10, 1},
		{1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6},
		{4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9},
		{10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3},
		{8, 2, 3, 8, 4, 2, 4, 6, 2},
		{0, 4, 2, 4, 6, 2},
		{1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8},
		{1, 9, 4, 1, 4, 2, 2, 4, 6},
		{8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1},
		{10, 1, 0, 10, 0, 6, 6, 0, 4},
		{4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3},
		{10, 9, 4, 6, 10, 4},
		{4, 9, 5, 7, 6, 11},
		{0, 8, 3, 4, 9, 5, 11, 7, 6},
		{5, 0, 1, 5, 4, 0, 7, 6, 11},
		{11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5},
		{9, 5, 4, 10, 1, 2, 7, 6, 11},
		{6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5},
		{7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2},
		{3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6},
		{7, 2, 3, 7, 6, 2, 5, 4, 9},
		{9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7},
		{3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0},
		{6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8},
		{9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7},
		{1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4},
		{4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10},
		{7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10},
		{6, 9, 5, 6, 11, 9, 11, 8, 9},
		{3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5},
		{0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11},
		{6, 11, 3, 6, 3, 5, 5, 3, 1},
		{1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6},
		{0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10},
		{11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5},
		{6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3},
		{5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2},
		{9, 5, 6, 9, 6, 0, 0, 6, 2},
		{1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8},
		{1, 5, 6, 2, 1, 6},
		{1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6},
		{10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0},
		{0, 3, 8, 5, 6, 10},
		{10, 5, 6},
		{11, 5, 10, 7, 5, 11},
		{11, 5, 10, 11, 7, 5, 8, 3, 0},
		{5, 11, 7, 5, 10, 11, 1, 9, 0},
		{10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1},
		{11, 1, 2, 11, 7, 1, 7, 5, 1},
		{0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11},
		{9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7},
		{7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2},
		{2, 5, 10, 2, 3, 5, 3, 7, 5},
		{8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5},
		{9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2},
		{9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2},
		{1, 3, 5, 3, 7, 5},
		{0, 8, 7, 0, 7, 1, 1, 7, 5},
		{9, 0, 3, 9, 3, 5, 5, 3, 7},
		{9, 8, 7, 5, 9, 7},
		{5, 8, 4, 5, 10, 8, 10, 11, 8},
		{5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0},
		{0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5},
		{10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4},
		{2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8},
		{0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11},
		{0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5},
		{9, 4, 5, 2, 11, 3},
		{2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4},
		{5, 10, 2, 5, 2, 4, 4, 2, 0},
		{3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9},
		{5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2},
		{8, 4, 5, 8, 5, 3, 3, 5, 1},
		{0, 4, 5, 1, 0, 5},
		{8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5},
		{9, 4, 5},
		{4, 11, 7, 4, 9, 11, 9, 10, 11},
		{0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11},
		{1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11},
		{3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4},
		{4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2},
		{9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3},
		{11, 7, 4, 11, 4, 2, 2, 4, 0},
		{11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4},
		{2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9},
		{9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7},
		{3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10},
		{1, 10, 2, 8, 7, 4},
		{4, 9, 1, 4, 1, 7, 7, 1, 3},
		{4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1},
		{4, 0, 3, 7, 4, 3},
		{4, 8, 7},
		{9, 10, 8, 10, 11, 8},
		{3, 0, 9, 3, 9, 11, 11, 9, 10},
		{0, 1, 10, 0, 10, 8, 8, 10, 11},
		{3, 1, 10, 11, 3, 10},
		{1, 2, 11, 1, 11, 9, 9, 11, 8},
		{3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9},
		{0, 2, 11, 8, 0, 11},
		{3, 2, 11},
		{2, 3, 8, 2, 8, 10, 10, 8, 9},
		{9, 10, 2, 0, 9, 2},
		{2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8},
		{1, 10, 2},
		{1, 3, 8, 9, 1, 8},
		{0, 9, 1},
		{0, 3, 8},
		{}};

	protected float mOffsetX,mOffsetY,mOffsetZ,mVoxelSize;	// all in angstrom
	protected MeshBuilder mMeshBuilder;

	public MarchingCubesMesh(MeshBuilder meshBuilder) {
		this(meshBuilder, 1.0f);
		}

	public MarchingCubesMesh(MeshBuilder meshBuilder, float voxelSize) {
		mMeshBuilder = meshBuilder;
		mVoxelSize = voxelSize;
		}

	/**
	 * Defines first the voxel position in space (default is 0f,0f,0f).
	 * @param x
	 * @param y
	 * @param z
	 */
	public void setOffset(float x, float y, float z) {
		mOffsetX = x;
		mOffsetY = y;
		mOffsetZ = z;
		}

	public void create(String filename, int sizeX, int sizeY, int sizeZ, float isoLayer) {
		float[] grid = new float[sizeX * sizeY * sizeZ];

		try {
			FileInputStream reader = new FileInputStream(filename);

			byte[] buf = new byte[sizeX*sizeY];
			for (int i = 0; i < sizeZ; i++) {
				int bytes = reader.read(buf);
				if (bytes != buf.length)
					break;
				for (int j=0; j<buf.length; j++)
					grid[i*buf.length+j] = (float)buf[j];
				}

			reader.close();
			}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return;
			}

		mOffsetX = -mVoxelSize*sizeX/2f;
		mOffsetY = -mVoxelSize*sizeY/2f;
		mOffsetZ = -mVoxelSize*sizeZ/2f;

		polygonise(grid, sizeX, sizeY, sizeZ, isoLayer);
		}

	protected void polygonise(float[] grid, int sx, int sy, int sz, float isoLayer) {
		TreeMap<Integer,Integer> edgeVertexMap = new TreeMap<Integer,Integer>();
		int[] vertexIndex = new int[12];
		int gridIndex = sy*sz;
		for (int ix=1; ix<sx; ix++) {
			gridIndex += sz;
			for (int iy=1; iy<sy; iy++) {
				gridIndex++;
				for (int iz=1; iz<sz; iz++) {
					// Determine the index into the edge table which
					// tells us which vertices are inside of the surface
					int cubeIndex = 0;
					if (grid[gridIndex-sy*sz -sz -1] <  isoLayer) cubeIndex |= 1;
					if (grid[gridIndex       -sz -1] <  isoLayer) cubeIndex |= 2;
					if (grid[gridIndex       -sz   ] <= isoLayer) cubeIndex |= 4;
					if (grid[gridIndex-sy*sz -sz   ] <  isoLayer) cubeIndex |= 8;
					if (grid[gridIndex-sy*sz     -1] <  isoLayer) cubeIndex |= 16;
					if (grid[gridIndex           -1] <= isoLayer) cubeIndex |= 32;
					if (grid[gridIndex             ] <= isoLayer) cubeIndex |= 64;
					if (grid[gridIndex-sy*sz       ] <= isoLayer) cubeIndex |= 128;

					// Cube is entirely in/out of the surface
					if (EDGE_TABLE[cubeIndex] != 0) {

						// Find the vertices where the surface intersects the cube
						if ((EDGE_TABLE[cubeIndex] & 1) != 0)
							vertexIndex[0] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy-1, iz-1, sx, sy, sz, 0, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 2) != 0)
							vertexIndex[1] = getEdgeVertexIndex(edgeVertexMap, ix,   iy-1, iz-1, sx, sy, sz, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 4) != 0)
							vertexIndex[2] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy-1, iz,   sx, sy, sz, 0, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 8) != 0)
							vertexIndex[3] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy-1, iz-1, sx, sy, sz, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 16) != 0)
							vertexIndex[4] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy,   iz-1, sx, sy, sz, 0, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 32) != 0)
							vertexIndex[5] = getEdgeVertexIndex(edgeVertexMap, ix,   iy,   iz-1, sx, sy, sz, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 64) != 0)
							vertexIndex[6] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy,   iz,   sx, sy, sz, 0, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 128) != 0)
							vertexIndex[7] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy,   iz-1, sx, sy, sz, 2, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 256) != 0)
							vertexIndex[8] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy-1, iz-1, sx, sy, sz, 1, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 512) != 0)
							vertexIndex[9] = getEdgeVertexIndex(edgeVertexMap, ix,   iy-1, iz-1, sx, sy, sz, 1, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 1024) != 0)
							vertexIndex[10] = getEdgeVertexIndex(edgeVertexMap, ix,  iy-1, iz,   sx, sy, sz, 1, grid, isoLayer);
						if ((EDGE_TABLE[cubeIndex] & 2048) != 0)
							vertexIndex[11] = getEdgeVertexIndex(edgeVertexMap, ix-1, iy-1, iz,  sx, sy, sz, 1, grid, isoLayer);

						// Create the triangle
						for (int j = 0; j < FACE_TABLE[cubeIndex].length; j += 3) {
							// TODO If the edgeposition is 0.0 or 1.0 then we may have equally positioned vertexes and
							// to avoid problems with surface rendering, we need to skip creating such triangles.
							mMeshBuilder.addTriangle(vertexIndex[FACE_TABLE[cubeIndex][j]],
									vertexIndex[FACE_TABLE[cubeIndex][j + 1]],
									vertexIndex[FACE_TABLE[cubeIndex][j + 2]]);
							}
						}

					gridIndex++;
					}
				}
			}
		}

	private int getEdgeVertexIndex(TreeMap<Integer,Integer> edgeVertexMap,
								   int ix, int iy, int iz, int sx, int sy, int sz,
								   int edgeDir, float[] grid, float isoLayer) {
		int gridIndex = ix*sy*sz + iy*sz + iz;
		int key = 3*gridIndex+edgeDir;
		Integer indexHolder = edgeVertexMap.get(key);
		if (indexHolder != null)
			return indexHolder.intValue();

		float val1 = grid[gridIndex];
		float val2 = (edgeDir == 0) ? grid[gridIndex+sy*sz]
				   : (edgeDir == 1) ? grid[gridIndex+sz] : grid[gridIndex+1];

		float pos = (val2 == val1) ? 0.5f : (isoLayer - val1) / (val2 - val1);
		float x = (edgeDir == 0) ? ix + pos : ix;
		float y = (edgeDir == 1) ? iy + pos : iy;
		float z = (edgeDir == 2) ? iz + pos : iz;
		int index = mMeshBuilder.addPoint(mOffsetX+mVoxelSize*x, mOffsetY+mVoxelSize*y, mOffsetZ+mVoxelSize*z);

		edgeVertexMap.put(key, index);
		return index;
		}
	}
