package org.eclipse.pde.internal.model;

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
	private Vector imports=new Vector();

public Plugin() {
}
public void add(IPluginImport iimport) throws CoreException {
	ensureModelEditable();
	imports.addElement(iimport);
	fireStructureChanged(iimport, IModelChangedEvent.INSERT);
}
public String getClassName() {
	return className;
}
public IPluginImport[] getImports() {
	IPluginImport [] result = new IPluginImport [ imports.size() ];
	imports.copyInto(result);
	return result;
}
public IPlugin getPlugin() {
	return this;
}
void load(PluginModel pm) {
	PluginDescriptorModel pd = (PluginDescriptorModel)pm;
	this.className = pd.getPluginClass();

	// add imports
	loadImports(pd.getRequires());
	super.load(pm);
}
void load(Node node) {
	this.className = getNodeAttribute(node, "class");
	super.load(node);
}
void loadImports(PluginPrerequisiteModel[] importModels) {
	if (importModels == null)
		return;

	for (int i = 0; i < importModels.length; i++) {
		PluginPrerequisiteModel importModel = importModels[i];
		PluginImport importElement = new PluginImport();
		importElement.setModel(getModel());
		importElement.setParent(this);
		imports.add(importElement);
		importElement.load(importModel);
	}
}
void loadImports(Node node) {
	NodeList children = node.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE
			&& child.getNodeName().toLowerCase().equals("import")) {
			PluginImport importElement = new PluginImport();
			importElement.setModel(getModel());
			importElement.setParent(this);
			imports.add(importElement);
			importElement.load(child);
		}
	}
}
protected void processChild(Node child) {
	String name = child.getNodeName().toLowerCase();
	if (name.equals("requires"))
		loadImports(child);
	else
		super.processChild(child);
}
public void remove(IPluginImport iimport) throws CoreException {
	ensureModelEditable();
	imports.removeElement(iimport);
	fireStructureChanged(iimport, ModelChangedEvent.REMOVE);
}
public void reset() {
	imports = new Vector();
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
	writer.println("<!-- File written by PDE 1.0 -->");
	writer.print("<plugin");
	if (getId() != null) {
		writer.println();
		writer.print("   id=\"" + getId() + "\"");
	}
	if (getName() != null) {
		writer.println();
		writer.print("   name=\"" + getName() + "\"");
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
	// add requires
	Object [] children = getImports();
	if (children.length>0) {
		writer.println("<!-- Required plugins -->");
		writer.println();
		writeChildren("requires", children, writer);
		writer.println();
	}
	writer.println("<!-- Runtime -->");
	writer.println();
	// add runtime
	children = getLibraries();
	writeChildren("runtime", children, writer);
	// add extension points
	writer.println();

	writer.println("<!-- Extension points -->");
	writer.println();
	children = getExtensionPoints();
	for (int i=0; i<children.length; i++) {
		((IPluginExtensionPoint) children[i]).write("", writer);
	}
	writer.println();

	// add extensions
	children = getExtensions();
	writer.println("<!-- Extensions -->");
	for (int i=0; i<children.length; i++) {
		((IPluginExtension) children[i]).write("", writer);
	}
	writer.println("</plugin>");
}
}
