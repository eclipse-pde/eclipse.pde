package org.eclipse.pde.internal.ui.elements;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;

public class ListContentProvider extends DefaultContentProvider implements IStructuredContentProvider {

public ListContentProvider() {
	super();
}
public Object[] getElements(Object element) {
	if (element instanceof IPDEElement) {
		return ((IPDEElement)element).getChildren();
	}
	return null;
}
}
