package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.forcefield.ForceField;
import com.actelion.research.chem.forcefield.ForceFieldChangeListener;
import com.actelion.research.util.DoubleFormat;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import mmff.ForceFieldMMFF94;
import org.openmolecules.mesh.MoleculeSurfaceMesh;

import java.util.ArrayList;

public class V3DMinimizationHandler implements ForceFieldChangeListener {
	private V3DMolecule[] mFXMol;
	private ForceField mForceField;
	private V3DScene mScene3D;
	private V3DSceneEditor mEditor;
	private volatile Thread mMinimizationThread;
	
	public V3DMinimizationHandler(V3DScene scene3D, V3DSceneEditor editor)  {
		mScene3D = scene3D;
		mEditor = editor;

		ArrayList<V3DMolecule> fxmolList = new ArrayList<V3DMolecule>();
		for (Node node : scene3D.getWorld().getChildren()) {
			if (node instanceof V3DMolecule && node.isVisible()) {
				if (node.isVisible()) {
					V3DMolecule fxmol = (V3DMolecule)node;
					fxmol.addImplicitHydrogens();
					fxmolList.add(fxmol);
				}
			}
		}

		mFXMol = fxmolList.toArray(new V3DMolecule[0]);
	}

	public V3DMinimizationHandler(V3DScene scene3D, V3DMolecule fxmol, V3DSceneEditor editor)  {
		mScene3D = scene3D;
		mEditor = editor;
		mFXMol = new V3DMolecule[1];
		mFXMol[0] = fxmol;
		mFXMol[0].addImplicitHydrogens();
	}

	public void minimize() {
		if (mFXMol.length == 0)
			return;

		StereoMolecule molScenery = new StereoMolecule();

		int atom = 0;
		for(V3DMolecule fxmol : mFXMol) {
			// remove any surfaces
			for (int type=0; type<MoleculeSurfaceMesh.SURFACE_TYPE.length; type++)
				fxmol.setSurfaceMode(type ,V3DMolecule.SURFACE_NONE);

			Conformer conf = fxmol.getConformer();
			StereoMolecule mol = conf.getMolecule();
			molScenery.addMolecule(mol);

			for(int a=0;a<mol.getAllAtoms();a++) {
				Point3D globalCoords = fxmol.localToParent(conf.getX(a), conf.getY(a), conf.getZ(a));
				molScenery.setAtomX(atom, globalCoords.getX());
				molScenery.setAtomY(atom, globalCoords.getY());
				molScenery.setAtomZ(atom, globalCoords.getZ());
				atom++;
			}
		}

		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);
		mForceField = new ForceFieldMMFF94(molScenery, ForceFieldMMFF94.MMFF94SPLUS);
		mForceField.addListener(this);

		mMinimizationThread = new Thread(() -> mForceField.minimise());
		mMinimizationThread.start();
	}

	public void stopMinimization() {
		mMinimizationThread = null;
		mForceField.interrupt();
	}

	@Override
	public void stateChanged() {
		final double[] pos = mForceField.getCurrentPositions();
		final double energy = mForceField.getTotalEnergy();
		Platform.runLater(() -> {
			if (mMinimizationThread != null) {
				mScene3D.updateCoordinates(mFXMol, pos);
				if (mEditor != null)
					mEditor.createOutput("energy: " + Double.toString(energy) + " kcal/mol");
				else
					System.out.println("MMFF-Energy: "+ DoubleFormat.toString(energy) + " kcal/mol");
			}
		});
	}
}
