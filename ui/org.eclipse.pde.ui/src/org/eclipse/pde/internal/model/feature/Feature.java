package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.io.*;
import org.w3c.dom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.feature.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;

public class Feature extends VersionableObject implements IFeature {
	final static String INDENT = "   ";
	private String providerName;
	private IFeatureURL url;
	private IFeatureInfo description, copyright, license;
	private Vector plugins = new Vector();

	public void addPlugin(IFeaturePlugin plugin) throws CoreException {
		ensureModelEditable();
		plugins.add(plugin);
		fireStructureChanged(plugin, IModelChangedEvent.INSERT);
	}

	public IFeaturePlugin[] getPlugins() {
		IFeaturePlugin[] result = new IFeaturePlugin[plugins.size()];
		plugins.copyInto(result);
		return result;
	}
	public String getProviderName() {
		return providerName;
	}

	public IPluginModelBase getReferencedModel(IFeaturePlugin reference) {
		WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] models = null;
		if (reference.isFragment())
			models = mng.getWorkspaceFragmentModels();
		else
			models = mng.getWorkspacePluginModels();

		for (int i = 0; i < models.length; i++) {
			IPluginBase base = models[i].getPluginBase();
			if (base.getId().equals(reference.getId()))
				return models[i];
		}
		return null;
	}
	public IFeatureURL getURL() {
		return url;
	}

	protected void parse(Node node) {
		super.parse(node);
		providerName = getNodeAttribute(node, "provider-name");
		NodeList children = node.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase();
				if (tag.equals("description")) {
					IFeatureInfo info = getModel().getFactory().createInfo(tag);
					description = info;
				}
				else if (tag.equals("license")) {
					IFeatureInfo info = getModel().getFactory().createInfo(tag);
					license = info;
				}
				else if (tag.equals("copyright")) {
					IFeatureInfo info = getModel().getFactory().createInfo(tag);
					copyright = info;
				} else if (tag.equals("url")) {
					if (url == null) {
						url = getModel().getFactory().createURL();
						((FeatureURL) url).parse(child);
					}
				} else if (tag.equals("plugin")) {
					IFeaturePlugin plugin = getModel().getFactory().createPlugin();
					((FeaturePlugin) plugin).parse(child);
					plugins.add(plugin);
				}
			}
		}
	}

	public void removePlugin(IFeaturePlugin plugin) throws CoreException {
		ensureModelEditable();
		plugins.remove(plugin);
		fireStructureChanged(plugin, IModelChangedEvent.REMOVE);
	}
	public void reset() {
		super.reset();
		plugins.clear();
		url = null;
		providerName = null;
	}

	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.providerName;
		this.providerName = providerName;
		firePropertyChanged(P_PROVIDER, oldValue, providerName);
	}
	public void setURL(IFeatureURL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	public IFeatureInfo getDescription() {
		return description;
	}

	public IFeatureInfo getCopyright() {
		return copyright;
	}

	public IFeatureInfo getLicense() {
		return license;
	}

	public void setDescription(IFeatureInfo desc) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = desc;
		firePropertyChanged(P_DESCRIPTION, oldValue, desc);
	}

	public void setLicense(IFeatureInfo license) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.license;
		this.license = license;
		firePropertyChanged(P_LICENSE, oldValue, license);
	}

	public void setCopyright(IFeatureInfo copyright) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.copyright;
		this.copyright = copyright;
		firePropertyChanged(P_COPYRIGHT, oldValue, copyright);
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<feature");
		String indent2 = indent + INDENT;
		String indenta = indent + INDENT + INDENT;
		writeIfDefined(indenta, writer, "id", getId());
		writeIfDefined(indenta, writer, "label", getWritableString(getLabel()));
		writeIfDefined(indenta, writer, "version", getVersion());
		writeIfDefined(
			indenta,
			writer,
			"provider-name",
			getWritableString(providerName));
		writer.println(">");
		if (description!=null) {
			description.write(indent2, writer);
		}
		if (license!=null) {
			license.write(indent2, writer);
		}
		if (copyright!=null) {
			copyright.write(indent2, writer);
		}
		if (url != null) {
			writer.println();
			url.write(indent2, writer);
		}
		for (int i = 0; i < plugins.size(); i++) {
			IFeaturePlugin plugin = (IFeaturePlugin) plugins.elementAt(i);
			writer.println();
			plugin.write(indent2, writer);
		}
		writer.println();
		writer.println(indent + "</feature>");
	}
	private void writeIfDefined(
		String indent,
		PrintWriter writer,
		String attName,
		String attValue) {
		if (attValue == null)
			return;
		writer.println();
		writer.print(indent + attName + "=\"" + attValue + "\"");
	}
}