/*
 * FXMolViewer, showing and manipulating molecules and protein structures in 3D.
 * Copyright (C) 2019 Thomas Sander

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @author Thomas Sander
 */

package org.openmolecules.fx.sunflow;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.conf.Conformer;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.shape.ObservableFaceArray;
import javafx.stage.Window;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.surface.SurfaceTexture;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.mesh.MoleculeSurfaceMesh;
import org.openmolecules.render.SunflowMoleculeBuilder;
import org.sunflow.core.shader.ColorProvider;
import org.sunflow.math.Point3;

public class RayTraceOptions {
	private static final String DEFAULT_SCENE_NAME = "Untitled";
	private static final String MULTIMOL_SCENE_NAME = "Molecules";
	private static final float CAMERA_FOCUS = 0.1f; // crisp in front (0.0) or rear (1.0)

	public String size;
	public int mode, atomMaterial,bondMaterial;
	public int[] surfaceMaterial;
	public float brightness;
	public boolean shinyFloor,optimizeTranslation,optimizeRotation,depthBlurring;
	public Color backgroundColor,floorColor;
	private volatile String mSceneName;
	private volatile SunflowMoleculeBuilder mRenderer;
	private volatile int counter;

	/**
	 * Initializes the ray-tracer with background and floor color options,
	 * atom and bond materials, default camera distance and default field of view.
	 */
	public void rayTraceInit() {
		rayTraceInit(SunflowMoleculeBuilder.DEFAULT_CAMERA_DISTANCE,
					 SunflowMoleculeBuilder.DEFAULT_FIELD_OF_VIEW);
	}

	/**
	 * Initializes the ray-tracer with background and floor color options,
	 * atom and bond materials, camera distance and field of view.
	 * @param cameraDistance
	 * @param fieldOfView
	 */
	public void rayTraceInit(double cameraDistance, double fieldOfView) {
		int i = size.indexOf(" x ");
		int width = Integer.parseInt(size.substring(0, i));
		int height = Integer.parseInt(size.substring(i + 3));

		mSceneName = DEFAULT_SCENE_NAME;

		mRenderer = optimizeTranslation ? new SunflowMoleculeBuilder()
				: new SunflowMoleculeBuilder((float)cameraDistance, (float)fieldOfView);
		mRenderer.setAtomMaterial(atomMaterial);
		mRenderer.setBondMaterial(bondMaterial);
		if (backgroundColor == null)
			mRenderer.setBackgroundColor(-1, -1, -1);
		else
			mRenderer.setBackgroundColor((float)backgroundColor.getRed(),
					(float)backgroundColor.getGreen(),
					(float)backgroundColor.getBlue());
		if (floorColor == null)
			mRenderer.setFloorColor(-1, -1, -1);
		else
			mRenderer.setFloorColor((float)floorColor.getRed(),
					(float)floorColor.getGreen(),
					(float)floorColor.getBlue());
		mRenderer.setGlossyFloor(shinyFloor);
		mRenderer.setBrightness(brightness);
		mRenderer.initializeScene(width, height);
	}

	public void addMolecule(V3DMolecule fxmol) {
		Conformer conformer = new Conformer(fxmol.getConformer());

		if (mSceneName.equals(DEFAULT_SCENE_NAME) && conformer.getMolecule().getName() != null)
			mSceneName = conformer.getMolecule().getName();
		else
			mSceneName = MULTIMOL_SCENE_NAME;

		for (int atom = 0; atom < conformer.getSize(); atom++) {
			Coordinates c = conformer.getCoordinates(atom);
			Point3D sp = fxmol.localToScene(c.x, c.y, c.z);
			c.set(sp.getX(), sp.getZ(), -sp.getY());
		}

		double surplus = -1;
		for (int type=0; type<MoleculeSurfaceMesh.SURFACE_TYPE.length; type++)
			if (fxmol.getSurfaceMode(type) != V3DMolecule.SURFACE_NONE)
				surplus = Math.max(surplus, fxmol.getSurfaceMesh(type).getSurfaceSurplus());
		Color color = fxmol.getColor();
		mRenderer.setRenderMode(mode == -1 ? fxmol.getConstructionMode() : mode);
		mRenderer.setOverrideColor(color == null ? null : new java.awt.Color((float)color.getRed(), (float)color.getGreen(), (float)color.getBlue()));
		mRenderer.drawMolecule(conformer, optimizeRotation, optimizeTranslation, surplus);

		for (int type=0; type<MoleculeSurfaceMesh.SURFACE_TYPE.length; type++) {
			SurfaceMesh mesh = fxmol.getSurfaceMesh(type);
			if (mesh != null) {
				int pointSize = mesh.getPointElementSize();
				int pointCount = mesh.getPoints().size() / pointSize;
				ObservableFloatArray points = mesh.getPoints();
				float[] p = new float[3 * pointCount];
				for (int i = 0; i < pointCount; i++) {
					Point3D sp = fxmol.localToScene(points.get(i * pointSize), points.get(i * pointSize + 1), points.get(i * pointSize + 2));
					p[3 * i] = (float) sp.getX();
					p[3 * i + 1] = (float) sp.getZ();
					p[3 * i + 2] = (float) -sp.getY();
					if (optimizeRotation || optimizeTranslation)
						mRenderer.optimizeCoordinate(p, 3 * i);
				}

				int faceSize = mesh.getFaceElementSize();
				int faceCount = mesh.getFaces().size() / faceSize;
				int faceOffset = faceSize / 3;
				ObservableFaceArray faces = mesh.getFaces();
				int[] f = new int[3 * faceCount];
				for (int i = 0; i < faceCount; i++) {
					f[3 * i] = faces.get(i * faceSize);
					f[3 * i + 1] = faces.get(i * faceSize + faceOffset);
					f[3 * i + 2] = faces.get(i * faceSize + faceOffset * 2);
				}

				float[] n = createSurfaceNormals(f, p);

				Color c = fxmol.getColor();	// is null if molecule color is not explicitly set
				if (c == null || fxmol.getSurfaceColorMode(type) != SurfaceMesh.SURFACE_COLOR_INHERIT)
					c = fxmol.getSurfaceColor(type);

				java.awt.Color awtColor = (c == null) ? null
						: new java.awt.Color((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue());

				ColorProvider cp = (mesh.getTexture() == null) ? null
						: new MoleculeSurfaceColorProvider(conformer, mesh.getTexture());

				mRenderer.createSurfaceShader(surfaceMaterial[type] == -1 ? getOriginalSurfaceMaterial(fxmol, type)
						  : surfaceMaterial[type], awtColor, cp, (float)fxmol.getSurfaceTransparency(type), counter);
				mRenderer.drawMesh("mesh" + (counter++), p, f, n);
			}
		}
	}

	private class MoleculeSurfaceColorProvider implements ColorProvider {
		SurfaceTexture mTexture;

		public MoleculeSurfaceColorProvider(Conformer conformer, SurfaceTexture texture) {
			mTexture = texture;
			texture.initializeSurfaceColor(conformer);
		}

		@Override
		public org.sunflow.image.Color colorAtPoint(Point3 p) {
// for testing purposes create a x-location dependent color
//			float v = 0.5f + (float)Math.sin(p.x) / 2f;
//			return new org.sunflow.image.Color(0.5f, v, 0.5f);

			return mTexture.getSurfaceColor(p);
		}
	}

	private int getOriginalSurfaceMaterial(V3DMolecule fxmol, int surfaceType) {
		int surfaceMode = fxmol.getSurfaceMode(surfaceType);
		if (surfaceMode == V3DMolecule.SURFACE_WIRES)
			return SunflowMoleculeBuilder.SURFACE_WIRES;
		if (fxmol.getSurfaceTransparency(surfaceType) >= 0.1)
			return SunflowMoleculeBuilder.SURFACE_TRANSPARENT;
		else
			return SunflowMoleculeBuilder.SURFACE_SHINY;   // could also by SunflowMoleculeBuilder.SURFACE_FILLED
	}

	private float[] createSurfaceNormals(int[] tris, float[] verts) {
		float[] normals = new float[verts.length]; // filled with 0's
		for (int i=0; i<tris.length; i+=3) {
			int v0 = tris[i];
			int v1 = tris[i+1];
			int v2 = tris[i+2];
			float vx = verts[v1*3]   - verts[v0*3];
			float vy = verts[v1*3+1] - verts[v0*3+1];
			float vz = verts[v1*3+2] - verts[v0*3+2];
			float wx = verts[v2*3]   - verts[v0*3];
			float wy = verts[v2*3+1] - verts[v0*3+1];
			float wz = verts[v2*3+2] - verts[v0*3+2];
			float nx = vz*wy - vy*wz;
			float ny = vx*wz - vz*wx;
			float nz = vy*wx - vx*wy;
			float l = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
			nx /= l;
			ny /= l;
			nz /= l;

			normals[v0*3  ] += nx;
			normals[v0*3+1] += ny;
			normals[v0*3+2] += nz;
			normals[v1*3  ] += nx;
			normals[v1*3+1] += ny;
			normals[v1*3+2] += nz;
			normals[v2*3  ] += nx;
			normals[v2*3+1] += ny;
			normals[v2*3+2] += nz;
		}

		for (int i=0; i<normals.length; i+=3) {
			float l = (float)Math.sqrt(normals[i]*normals[i]
					+ normals[i+1]*normals[i+1]
					+ normals[i+2]*normals[i+2]);
			normals[i]   /= l;
			normals[i+1] /= l;
			normals[i+2] /= l;
		}

		return normals;
	}

	public void rayTraceStart(final Window owner) {
		new Thread(() -> {
				mRenderer.finalizeScene(depthBlurring ? CAMERA_FOCUS : -1f);
//				mRenderer.render("/home/thomas/sunflowTest.png");
				mRenderer.setDisplay(new RayTraceFrameDisplay(owner, mSceneName));
				mRenderer.render();
		}).start();
	}

	/**
	 * Convenience function to initialize ray-tracer, add one molecule, and
	 * launch the ray-tracing process in a frame display. This method uses
	 * the SunFlow defaults for camera distance and field of view.
	 * @param owner
	 * @param fxmol
	 *
	public void renderMolecule(Window owner, V3DMolecule fxmol) {
		renderMolecule(owner, fxmol,
					   SunflowMoleculeBuilder.DEFAULT_CAMERA_DISTANCE,
					   SunflowMoleculeBuilder.DEFAULT_FIELD_OF_VIEW);
		}*/

	/**
	 * Convenience function to initialize ray-tracer, add one molecule, and
	 * launch the ray-tracing process in a frame display.
	 * @param owner
	 * @param fxmol
	 * @param cameraDistance
	 * @param fieldOfView
	 *
	public void renderMolecule(Window owner, V3DMolecule fxmol, double cameraDistance, double fieldOfView) {
		Conformer conformer = new Conformer(fxmol.getConformer());
		new Thread(() -> {
				int i = size.indexOf(" x ");
				int width = Integer.parseInt(size.substring(0, i));
				int height = Integer.parseInt(size.substring(i + 3));

				for (int atom = 0; atom < conformer.getSize(); atom++) {
					Coordinate c = conformer.getCoordinate(atom);
					Point3D p = fxmol.localToScene(c.getX(), c.getY(), c.getZ());
					c.setX(p.getX());
					c.setY(p.getZ());
					c.setZ(-p.getY());
					}

				SunflowMoleculeBuilder mr = optimizeTranslation ? new SunflowMoleculeBuilder(width, height)
						: new SunflowMoleculeBuilder(width, height, (float)cameraDistance, (float)fieldOfView);
				mr.setRenderMode(mode);
				mr.setAtomMaterial(atomMaterial);
				mr.setBondMaterial(bondMaterial);
				if (backgroundColor == null)
					mr.setBackgroundColor(-1, -1, -1);
				else
					mr.setBackgroundColor((float)backgroundColor.getRed(),
										  (float)backgroundColor.getGreen(),
										  (float)backgroundColor.getBlue());
				if (floorColor == null)
					mr.setFloorColor(-1, -1, -1);
				else
					mr.setFloorColor((float)floorColor.getRed(),
									 (float)floorColor.getGreen(),
									 (float)floorColor.getBlue());
				mr.setGlossyFloor(shinyFloor);
				mr.drawMolecule(conformer, optimizeRotation, optimizeTranslation, false);
				mr.finalizeScene(depthBlurring ? CAMERA_FOCUS : -1f);
//				mr.render("/home/thomas/sunflowTest.png");
				mr.setDisplay(new RayTraceFrameDisplay(owner, fxmol.getConformer().getMolecule().getName()));
				mr.render();
		}).start();
	}*/
}