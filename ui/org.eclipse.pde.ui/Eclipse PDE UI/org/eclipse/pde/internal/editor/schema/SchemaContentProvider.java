package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.jface.viewers.*;

public class SchemaContentProvider implements ITreeContentProvider {

public SchemaContentProvider() {
	super();
}
public void dispose() {}
public Object[] getChildren(Object element) {
	return new Object[0];
}
public Object[] getElements(Object element) {
	return new Object[0];
}
public Object getParent(Object element) {
	return null;
}
public boolean hasChildren(Object element) {
	return false;
}
public void inputChanged(org.eclipse.jface.viewers.Viewer viewer, java.lang.Object oldInput, java.lang.Object newInput) {}
public boolean isDeleted(java.lang.Object element) {
	return false;
}
}
