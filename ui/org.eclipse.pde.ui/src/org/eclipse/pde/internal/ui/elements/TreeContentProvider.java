package org.eclipse.pde.internal.ui.elements;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.jface.viewers.*;

public class TreeContentProvider extends ListContentProvider implements ITreeContentProvider {

public TreeContentProvider() {
	super();
}
public void dispose() {}
public Object[] getChildren(Object element) {
	if (element instanceof IPDEElement) {
		return ((IPDEElement)element).getChildren();
	}
	return null;
}
public Object[] getElements(Object element) {
	if (element instanceof IPDEElement) {
		return ((IPDEElement)element).getChildren();
	}
	return null;
}
public Object getParent(Object element) {
	if (element instanceof IPDEElement) {
		return ((IPDEElement)element).getParent();
	}
	return null;
}
public boolean hasChildren(java.lang.Object element) {
	if (element instanceof IPDEElement) {
		Object [] children = ((IPDEElement)element).getChildren();
		return children != null && children.length > 0;
	}
	return false;
}
public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
}
public boolean isDeleted(Object element) {
	return false;
}
}
