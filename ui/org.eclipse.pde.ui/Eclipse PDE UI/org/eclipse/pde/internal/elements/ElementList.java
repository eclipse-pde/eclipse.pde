package org.eclipse.pde.internal.elements;

import java.util.*;
import org.eclipse.swt.graphics.*;


public class ElementList extends NamedElement {
	private Vector children = new Vector();

public ElementList(String name) {
	super(name);
}
public ElementList(String name, Image icon) {
	super(name, icon);
}
public ElementList(String name, Image icon, IPDEElement parent) {
	super(name, icon, parent);
}
public void add(IPDEElement child) {
	children.addElement(child);
}
public Object[] getChildren() {
	if (children.size()==0) return new Object[0];
	Object[] result = new Object[children.size()];
	children.copyInto(result);
	return result;
}
public void remove(IPDEElement child) {
	children.remove(child);
}
public int size() {
	return children.size();
}
public String toString() {
	return children.toString();
}
}
