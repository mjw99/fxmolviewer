package org.openmolecules.fx.viewer3d;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.forcefield.ForceField;
import com.actelion.research.chem.forcefield.ForceFieldChangeListener;
import com.actelion.research.util.ArrayUtils;
import com.actelion.research.util.DoubleFormat;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import mmff.ForceFieldMMFF94;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;

import java.util.ArrayList;

public class V3DMinimizer implements ForceFieldChangeListener {
	private static V3DMinimizer sInstance;
	private V3DMolecule[] mFXMol;
	private V3DMoleculeUpdater[] mFXMolUpdater;
	private ForceField mForceField;
	private V3DScene mScene3D;
	private V3DSceneEditor mEditor;
	private volatile Thread mMinimizationThread;

	/**
	 *
	 * @param scene3D
	 * @param editor
	 * @param fxmol if null, then all visible molecules are minimized
	 */
	public static void minimize(V3DScene scene3D, V3DSceneEditor editor, V3DMolecule fxmol) {
		stopMinimization();

		sInstance = new V3DMinimizer(scene3D, editor, fxmol);
		sInstance.minimize();
	}

	private V3DMinimizer(V3DScene scene3D, V3DSceneEditor editor, V3DMolecule fxmol)  {
		mScene3D = scene3D;
		mEditor = editor;

		if (fxmol != null) {
			mFXMol = new V3DMolecule[1];
			mFXMol[0] = fxmol;
			mFXMol[0].addImplicitHydrogens();
		}
		else {
			ArrayList<V3DMolecule> fxmolList = new ArrayList<V3DMolecule>();
			for (Node node : scene3D.getWorld().getChildren()) {
				if (node instanceof V3DMolecule && node.isVisible()) {
					if (node.isVisible()) {
						fxmol = (V3DMolecule)node;
						fxmol.addImplicitHydrogens();
						fxmolList.add(fxmol);
					}
				}
			}

			mFXMol = fxmolList.toArray(new V3DMolecule[0]);
		}
	}

	private void minimize() {
		if (mFXMol.length == 0)
			return;

		mFXMolUpdater = new V3DMoleculeUpdater[mFXMol.length];
		int totalAtomCount = 0;
		for (int i=0; i<mFXMol.length; i++) {
			mFXMolUpdater[i] = new V3DMoleculeUpdater(mFXMol[i]);
			totalAtomCount += mFXMol[i].getMolecule().getAllAtoms();
		}

		StereoMolecule molScenery = new StereoMolecule();

		int atom = 0;
		int fixedAtomCount = 0;
		int[] fixedAtom = new int[totalAtomCount];
		ArrayList<Integer> rigidAtoms = new ArrayList<>();
		for(V3DMolecule fxmol : mFXMol) {
			// remove any surfaces
			for (int type = 0; type<MoleculeSurfaceAlgorithm.SURFACE_TYPE.length; type++)
				fxmol.setSurfaceMode(type ,V3DMolecule.SURFACE_NONE);

			StereoMolecule mol = fxmol.getMolecule();
			molScenery.addMolecule(mol);

			for(int a=0;a<mol.getAllAtoms();a++) {
				Point3D globalCoords = fxmol.localToParent(mol.getAtomX(a), mol.getAtomY(a), mol.getAtomZ(a));
				molScenery.setAtomX(atom, globalCoords.getX());
				molScenery.setAtomY(atom, globalCoords.getY());
				molScenery.setAtomZ(atom, globalCoords.getZ());
				if (mol.isMarkedAtom(a))
					fixedAtom[fixedAtomCount++] = atom;
				if (mol.getAtomicNo(a) == 0)
					molScenery.setAtomicNo(atom, 6);
				atom++;
			}
		}

		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);
		mForceField = new ForceFieldMMFF94(molScenery, ForceFieldMMFF94.MMFF94SPLUS);
		mForceField.addListener(this);

		if (fixedAtomCount != 0) {
			ArrayUtils.resize(fixedAtom, fixedAtomCount);
			for (int i = 0; i < rigidAtoms.size(); i++)
				fixedAtom[i] = rigidAtoms.get(i);
			mForceField.setFixedAtoms(fixedAtom);
		}

		mMinimizationThread = new Thread(() -> mForceField.minimise());
		mMinimizationThread.start();
	}

	public static void stopMinimization() {
		if (sInstance != null && sInstance.mMinimizationThread != null) {
			sInstance.mMinimizationThread = null;
			sInstance.mForceField.interrupt();
			sInstance = null;
		}
	}

	@Override
	public void stateChanged() {
		if (mMinimizationThread != null) {
			final double[] pos = mForceField.getCurrentPositions();
			final double energy = mForceField.getTotalEnergy();
			Platform.runLater(() -> {
				if (mMinimizationThread != null) {
					int posIndex = 0;
					for (int i=0; i<mFXMol.length; i++) {
						StereoMolecule mol = mFXMol[i].getMolecule();
						for (int atom=0; atom<mol.getAllAtoms(); atom++) {
							Point3D p = mFXMol[i].parentToLocal(pos[posIndex], pos[posIndex+1], pos[posIndex+2]);
							mol.setAtomX(atom, p.getX());
							mol.setAtomY(atom, p.getY());
							mol.setAtomZ(atom, p.getZ());
							posIndex += 3;
						}
						mFXMolUpdater[i].update();
					}

					if (mEditor != null)
						mEditor.createOutput("energy: " + Double.toString(energy) + " kcal/mol");
					else
						System.out.println("MMFF-Energy: "+ DoubleFormat.toString(energy) + " kcal/mol");
				}
			});
		}
	}
}