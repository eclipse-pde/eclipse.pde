package org.eclipse.pde.internal.elements;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import java.util.Iterator;
import java.util.ArrayList;

public abstract class DefaultElement implements IPDEElement {

public Object[] getChildren() {
	return null;
}
public Image getImage() {
	return null;
}
public String getLabel() {
	return "";
}
public Object getParent() {
	return null;
}
}
