package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IResource;
import java.io.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.PlatformObject;

public class Plugin extends PluginBase implements IPlugin {
	private String className;


public Plugin() {
}

public String getClassName() {
	return className;
}

public IPlugin getPlugin() {
	return this;
}
void load(PluginModel pm) {
	PluginDescriptorModel pd = (PluginDescriptorModel)pm;
	this.className = pd.getPluginClass();
	super.load(pm);
}

void load(Node node, Hashtable lineTable) {
	this.className = getNodeAttribute(node, "class");
	super.load(node, lineTable);
}



public void reset() {
	className = null;
	super.reset();
}
public void setClassName(String newClassName) throws CoreException {
	ensureModelEditable();
	className = newClassName;
	firePropertyChanged(P_CLASS_NAME);
}
public void write(String indent, PrintWriter writer) {
	writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	//writer.println("<!-- File written by PDE 1.0 -->");
	writer.print("<plugin");
	if (getId() != null) {
		writer.println();
		writer.print("   id=\"" + getId() + "\"");
	}
	if (getName() != null) {
		writer.println();
		writer.print("   name=\"" + getWritableString(getName()) + "\"");
	}
	if (getVersion() != null) {
		writer.println();
		writer.print("   version=\"" + getVersion() + "\"");
	}
	if (getProviderName() != null) {
		writer.println();
		writer.print("   provider-name=\"" + getProviderName() + "\"");
	}
	if (getClassName() != null) {
		writer.println();
		writer.print("   class=\"" + getClassName() + "\"");
	}
	writer.println(">");
	writer.println();

	// add runtime
	Object [] children = getLibraries();
	writeChildren("runtime", children, writer);
	
	// add requires
	children = getImports();
	if (children.length > 0) {
		writeComments(writer, requiresComments);
		writeChildren("requires", children, writer);
		writer.println();
	}

	children = getExtensionPoints();
	if (children.length>0) writer.println();
	for (int i=0; i<children.length; i++) {
		((IPluginExtensionPoint) children[i]).write("", writer);
	}

	// add extensions
	children = getExtensions();
	if (children.length>0) writer.println();
	for (int i=0; i<children.length; i++) {
		((IPluginExtension) children[i]).write("", writer);
	}
	writer.println();
	writer.println("</plugin>");
}
}
