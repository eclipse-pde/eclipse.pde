package org.eclipse.pde.internal.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.io.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.component.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;

public class Component extends ComponentObject implements IComponent {
	final static String INDENT = "   ";
	private String description;
	private String id;
	private String providerName;
	private String version;
	private IComponentURL url;
	private Vector plugins=new Vector();
	private Vector fragments=new Vector();

public void addFragment(IComponentFragment fragment) throws CoreException {
	ensureModelEditable();
	fragments.add(fragment);
	fireStructureChanged(fragment, IModelChangedEvent.INSERT);
}
public void addPlugin(IComponentPlugin plugin) throws CoreException {
	ensureModelEditable();
	plugins.add(plugin);
	fireStructureChanged(plugin, IModelChangedEvent.INSERT);
}
public String getDescription() {
	return description;
}
public IComponentFragment[] getFragments() {
	IComponentFragment [] result = new IComponentFragment[fragments.size()];
	fragments.copyInto(result);
	return result;
}
public String getId() {
	return id;
}
public IComponentPlugin[] getPlugins() {
	IComponentPlugin [] result = new IComponentPlugin[plugins.size()];
	plugins.copyInto(result);
	return result;
}
public String getProviderName() {
	return providerName;
}
public IPluginModelBase getReferencedModel(IComponentReference reference) {
	WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
	IPluginModelBase[] models = null;
	if (reference instanceof IComponentFragment)
		models = mng.getWorkspaceFragmentModels();
	else
		models = mng.getWorkspacePluginModels();

	for (int i=0; i<models.length; i++) {
		IPluginBase base = models[i].getPluginBase();
		if (base.getId().equals(reference.getId())) return models[i];
	}
	return null;        
}
public IComponentURL getURL() {
	return url;
}
public String getVersion() {
	return version;
}
void parse(Node node) {
	id = getNodeAttribute(node, "id");
	label = getNodeAttribute(node, "label");
	providerName = getNodeAttribute(node, "provider-name");
	version = getNodeAttribute(node, "version");
	NodeList children = node.getChildNodes();

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			String tag = child.getNodeName().toLowerCase();
			if (tag.equals("description")) {
				if (description == null) {
					String value = child.getFirstChild().getNodeValue();
					if (value != null)
						description = value.trim();
				}
			} else
				if (tag.equals("url")) {
					if (url == null) {
						url = getModel().getFactory().createURL();
						((ComponentURL) url).parse(child);
					}
				} else
					if (tag.equals("plugin")) {
						IComponentPlugin plugin = getModel().getFactory().createPlugin();
						((ComponentPlugin) plugin).parse(child);
						plugins.add(plugin);
					} else
						if (tag.equals("fragment")) {
							IComponentFragment fragment = getModel().getFactory().createFragment();
							((ComponentFragment) fragment).parse(child);
							fragments.add(fragment);
						}
		}
	}
}
public void removeFragment(IComponentFragment fragment) throws CoreException {
	ensureModelEditable();
	fragments.remove(fragment);
	fireStructureChanged(fragment, IModelChangedEvent.REMOVE);
}
public void removePlugin(IComponentPlugin plugin) throws CoreException {
	ensureModelEditable();
	plugins.remove(plugin);
	fireStructureChanged(plugin, IModelChangedEvent.REMOVE);
}
public void reset() {
	description = null;
	id = null;
	label = null;
	plugins.clear();
	fragments.clear();
	url = null;
	providerName = null;
	version = null;
}
public void setDescription(String description) throws CoreException {
	ensureModelEditable();
	this.description = description;
	firePropertyChanged(P_DESCRIPTION);
}
public void setId(String id) throws CoreException {
	ensureModelEditable();
	this.id = id;
	firePropertyChanged(P_ID);
}
public void setProviderName(String providerName) throws CoreException {
	ensureModelEditable();
	this.providerName = providerName;
	firePropertyChanged(P_PROVIDER);
}
public void setURL(IComponentURL url) throws CoreException {
	ensureModelEditable();
	this.url = url;
	firePropertyChanged(P_URL);
}
public void setVersion(String version) throws CoreException {
	ensureModelEditable();
	this.version = version;
	firePropertyChanged(P_VERSION);
}
public void write(String indent, PrintWriter writer) {
	writer.print(indent+"<component");
	String indent2 = indent + INDENT;
	String indenta = indent + INDENT+INDENT;
	writeIfDefined(indenta, writer, "id", id);
	writeIfDefined(indenta, writer, "label", getWritableString(label));
	writeIfDefined(indenta, writer, "version", version);
	writeIfDefined(indenta, writer, "provider-name", getWritableString(providerName));
	writer.println(">");
	if (description!=null) {
		String indent3 = indent2+INDENT;
		description = getWritableString(description.trim());
		writer.println();
		writer.println(indent2+"<description>");
		writer.println(indent3+description);
		writer.println(indent2+"</description>");
	}
	if (url!=null) {
		writer.println();
		url.write(indent2, writer);
	}
	for (int i=0; i<plugins.size(); i++) {
		IComponentPlugin plugin = (IComponentPlugin)plugins.elementAt(i);
		writer.println();
		plugin.write(indent2, writer);
	}
	for (int i=0; i<fragments.size(); i++) {
		IComponentFragment fragment = (IComponentFragment)fragments.elementAt(i);
		writer.println();
		fragment.write(indent2, writer);
	}
	writer.println();
	writer.println(indent+"</component>");
}
private void writeIfDefined(String indent, PrintWriter writer, String attName, String attValue) {
	if (attValue==null) return;
	writer.println();
	writer.print(indent+attName+"=\""+attValue+"\"");
}
}
