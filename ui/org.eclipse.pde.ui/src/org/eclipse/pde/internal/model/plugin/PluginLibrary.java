package org.eclipse.pde.internal.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.LibraryModel;
import java.util.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.plugin.*;

public class PluginLibrary extends PluginObject implements IPluginLibrary {
	private String[] contentFilters;
	private boolean exported=false;
	private String type;

public PluginLibrary() {
}
public String[] getContentFilters() {
	return contentFilters;
}
public boolean isExported() {
	return exported;
}
public boolean isFullyExported() {
	return exported && (contentFilters==null || contentFilters.length == 0);
}

public String getType() {
	return type;
}

void load(LibraryModel libraryModel) {
	this.name = libraryModel.getName();
	this.contentFilters = libraryModel.getExports();
	this.exported = libraryModel.isExported();
}
void load(Node node, Hashtable lineTable) {
	this.name = getNodeAttribute(node, "name");
	NodeList children = node.getChildNodes();
	Vector exports = new Vector();
	boolean all=false;
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE
			&& child.getNodeName().toLowerCase().equals("export")) {
		   String ename = getNodeAttribute(child, "name");
		   if (ename!=null) {
			   ename = ename.trim();
			   if (ename.equals("*")) {
				  all = true;
			   }
			   else {
				  exports.add(ename);
			   }
		   }
		}
	}
	if (exports.size()>0) {
		contentFilters = new String [exports.size()];
		exports.copyInto(contentFilters);
	}
	exported = all || exports.size()>0;
	addComments(node);
	bindSourceLocation(node, lineTable);
}
public void setContentFilters(String[] filters) throws CoreException {
	ensureModelEditable();
	ArrayList oldValue = createArrayList(contentFilters);
	contentFilters = filters;
	firePropertyChanged(P_CONTENT_FILTERS, oldValue, createArrayList(filters));
}

public void setExported(boolean value) throws CoreException {
	ensureModelEditable();
	Boolean oldValue = new Boolean(this.exported);
	this.exported = value;
	firePropertyChanged(P_EXPORTED, oldValue, new Boolean(value));
}

public void setType(String type) throws CoreException {
	ensureModelEditable();
	String oldValue = this.type;
	this.type = type;
	firePropertyChanged(P_TYPE, oldValue, type);
}

public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
	if (name.equals(P_CONTENT_FILTERS)) {
		ArrayList list = (ArrayList)newValue;
		if (list!=null)
			setContentFilters((String[])list.toArray(new String[list.size()]));
		else
			setContentFilters(null);
		return;
	}
	if (name.equals(P_EXPORTED)) {
		setExported(((Boolean)newValue).booleanValue());
		return;
	}
	if (name.equals(P_TYPE)) {
		setType(newValue!=null ? newValue.toString():null);
		return;
	}
	super.restoreProperty(name, oldValue, newValue);
}

private ArrayList createArrayList(String [] array) {
	if (array==null) return null;
	ArrayList list = new ArrayList();
	for (int i=0; i<array.length; i++) {
		list.add(array[i]);
	}
	return list;
}

public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.print(indent);
	writer.print("<library name=\"" + getName() + "\"");
	if (type!=null)
	   writer.print(" type=\""+type+"\"");
	if (isExported() == false) {
		writer.println("/>");
	} else {
		writer.println(">");
		String indent2 = indent + "   ";
		if (isFullyExported()) {
			writer.println(indent2+"<export name=\"*\"/>");
		}
		else {
			for (int i=0; i<contentFilters.length; i++) {
				writer.println(indent2+"<export name=\""+contentFilters[i]+"\"/>");
			}
		}
		writer.println(indent + "</library>");
	}
}
}
