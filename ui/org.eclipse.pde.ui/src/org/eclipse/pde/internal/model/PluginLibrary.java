package org.eclipse.pde.internal.model;
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
import org.eclipse.pde.internal.base.model.plugin.*;

public class PluginLibrary extends PluginObject implements IPluginLibrary {
	private String[] contentFilters;
	private boolean exported=false;

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
void load(LibraryModel libraryModel) {
	this.name = libraryModel.getName();
	this.contentFilters = libraryModel.getExports();
	this.exported = libraryModel.isExported();
}
void load(Node node) {
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
}
public void setContentFilters(String[] filters) throws CoreException {
	ensureModelEditable();
	contentFilters = filters;
	firePropertyChanged(P_CONTENT_FILTERS);
}
public void setExported(boolean value) throws CoreException {
	ensureModelEditable();
	this.exported = value;
	firePropertyChanged(P_EXPORTED);
}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.print(indent);
	writer.print("<library name=\"" + getName() + "\"");
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
