/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.schema;

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
