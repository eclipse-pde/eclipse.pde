package org.eclipse.pde.internal.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.elements.*;



public class WizardCollectionElement extends ElementList {
	private WizardCollectionElement parent;
	private ElementList     wizards = new ElementList("wizards");
	private String                  id;

	// properties
	public static String            P_WIZARDS = "org.eclipse.pde.wizards";

public WizardCollectionElement(String id, String name, WizardCollectionElement parent) {
	super(name, null, parent);
	this.id = id;
}
public WizardCollectionElement findChildCollection(IPath searchPath) {
	String searchString = searchPath.segment(0);

	Object [] children = getChildren(); 
	for (int i=0; i<children.length; i++) {
		WizardCollectionElement currentCategory = (WizardCollectionElement)children[i];
		if (currentCategory.getLabel().equals(searchString)) {
			if (searchPath.segmentCount() == 1)
				return currentCategory;
				
			return currentCategory.findChildCollection(searchPath.removeFirstSegments(1));
		}
	}
	
	return null;
}
public WizardElement findWizard(String searchId) {
	Object [] children = getWizards().getChildren();

	for (int i=0; i<children.length; i++) {
		WizardElement currentWizard = (WizardElement)children[i];
		if (currentWizard.getID().equals(searchId))
			return currentWizard;
	}
	return null;
}
public String getId() {
	return id;
}
public IPath getPath() {
	if (parent == null)
		return new Path("");
		
	return parent.getPath().append(getLabel());
}
public ElementList getWizards() {
	return wizards;
}
public void setId(java.lang.String newId) {
	id = newId;
}
public void setWizards(ElementList value) {
	wizards = value;
}
}
