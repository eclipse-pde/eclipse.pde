package org.eclipse.pde.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;

public class WizardCollectionSorter extends ViewerSorter {
	private String baseCategory;

public WizardCollectionSorter(String baseCategory) {
	this.baseCategory = baseCategory;
}
public int compare(Viewer viewer, Object o1, Object o2) {
	String name2 = ((WizardCollectionElement) o2).getLabel();
	String name1 = ((WizardCollectionElement) o1).getLabel();
	if (name2.equals(name1))
		return 0;

	if (baseCategory != null) {
		// note that this must be checked for name2 before name1 because if they're
		// BOTH equal to baseCategory then we want to answer false by convention
		if (name2.equalsIgnoreCase(baseCategory))
			return -1;

		if (name1.equalsIgnoreCase(baseCategory))
			return 1;
	}

	return name2.compareTo(name1);
}
public boolean isSorterProperty(Object object,Object propertyId) {
	return true;
}
}
