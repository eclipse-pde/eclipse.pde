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

public class Fragment extends PluginBase implements IFragment {
	private String pluginId;
	private String pluginVersion;

public Fragment() {
}
public String getPluginId() {
	return pluginId;
}
public String getPluginVersion() {
	return pluginVersion;
}
void load(PluginModel pm) {
	PluginFragmentModel pfm = (PluginFragmentModel)pm;
	this.pluginId = pfm.getPluginId();
	this.pluginVersion = pfm.getPluginVersion();
	super.load(pm);
}
void load(Node node) {
	this.pluginId = getNodeAttribute(node, "plugin-id");
	this.pluginVersion = getNodeAttribute(node, "plugin-version");
	super.load(node);
}
public void reset() {
	pluginId = null;
	pluginVersion = null;
	super.reset();
}
public void setPluginId(String newPluginId) throws CoreException {
	ensureModelEditable();
	pluginId = newPluginId;
	firePropertyChanged(P_PLUGIN_ID);
}
public void setPluginVersion(String newPluginVersion) throws CoreException {
	ensureModelEditable();
	pluginVersion = newPluginVersion;
	firePropertyChanged(P_PLUGIN_VERSION);
}
public void write(String indent, PrintWriter writer) {
	writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	writer.println("<!-- File written by PDE 1.0 -->");
	writer.print("<fragment");
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
	if (getPluginId() != null) {
		writer.println();
		writer.print("   plugin-id=\"" + getPluginId() + "\"");
	}
	if (getPluginVersion() != null) {
		writer.println();
		writer.print("   plugin-version=\"" + getPluginVersion() + "\"");
	}
	writer.println(">");
	writer.println();
	writer.println("<!-- Runtime -->");
	writer.println();
	// add runtime
	Object [] children = getLibraries();
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
	writer.println("</fragment>");
}
}
