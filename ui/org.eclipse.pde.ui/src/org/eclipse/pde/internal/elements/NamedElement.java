package org.eclipse.pde.internal.elements;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;

public class NamedElement extends DefaultElement {
	protected Image image;
	private String name;
	private IPDEElement parent;

public NamedElement(String name) {
	this(name, null, null);
}
public NamedElement(String name, Image icon) {
	this(name, icon, null);
}
public NamedElement(String name, Image image, IPDEElement parent) {
	this.name = name;
	this.image = image;
	this.parent = parent;
}
public Image getImage() {
	return image;
}
public String getLabel() {
	return name;
}
public Object getParent() {
	return parent;
}
public String toString() {
	return getLabel();
}
}
