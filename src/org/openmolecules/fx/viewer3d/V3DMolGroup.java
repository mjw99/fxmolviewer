package org.openmolecules.fx.viewer3d;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;

public class V3DMolGroup extends RotatableGroup implements IV3DMoleculeGroup {
	
	protected ObservableList<V3DMolGroup> children;
	protected boolean isIncluded;
	protected List<ListChangeListener<V3DMolGroup>> listeners;

	
	public V3DMolGroup(String name) {
		super(name);
		children = FXCollections.observableArrayList();
		listeners = new ArrayList<ListChangeListener<V3DMolGroup>>();
	}

	
	public void addMolGroup(V3DMolGroup fxmol) {
		for(ListChangeListener<V3DMolGroup> listener : listeners)
			fxmol.addListener(listener);
		children.add(fxmol);
		getChildren().add(fxmol);
	}
	
	public void deleteMolecule(V3DMolGroup toDelete) {
		deleteMolecule(toDelete,this);
	}
	
	private void deleteMolecule(V3DMolGroup toDelete, V3DMolGroup root) {
		List<V3DMolGroup> children = root.children;
		if(children.size()==0)
			return;
		if(children.contains(toDelete)) {
			root.children.remove(toDelete);
			root.getChildren().remove(toDelete);
		}
		for(V3DMolGroup child : children)
			deleteMolecule(toDelete,child);
		
	}
	
	public V3DMolGroup getParent(V3DMolGroup group) {
		return getParent(group,this);
	}
	
	private V3DMolGroup getParent(V3DMolGroup group, V3DMolGroup root) {
		List<V3DMolGroup> children = root.children;
		if(children.size()==0)
			return null;
		else {
			if(children.contains(group))
				return root;
			else {
				for(V3DMolGroup child : children)
					return getParent(group,child);
			}
		}
		return null;
		
	}
	
	//all nodes of the subtree attached to this group
	public List<V3DMolGroup> getAllChildren() {
		List<V3DMolGroup> allChildren = new ArrayList<V3DMolGroup>();
		getAllChildren(this,allChildren);
		return allChildren;
	}
	
	private void getAllChildren(V3DMolGroup root, List<V3DMolGroup> allChildren) {
		allChildren.add(root);
		if(root.getMolGroups().size()==0)
			return;
		for(V3DMolGroup group :root.getMolGroups())
			getAllChildren(group,allChildren);
		
	}
	
	public List<V3DMolGroup> getMolGroups() {
		return this.children;
	}
	
	public void setIncluded(boolean included) {
		isIncluded = included;
	}
	
	public boolean isIncluded() {
		return isIncluded;
	}
	
	public void addListener(ListChangeListener<V3DMolGroup> listener) {
		listeners.add(listener);
		children.addListener(listener);
		children.forEach(e -> e.addListener(listener));
	}
	

	


	
	
}
