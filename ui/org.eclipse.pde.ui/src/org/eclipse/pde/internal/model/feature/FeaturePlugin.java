package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.PDEPlugin;

public class FeaturePlugin
	extends VersionableObject
	implements IFeaturePlugin {
	private String os;
	private String ws;
	private String nl;
	private IPluginBase pluginBase;

	private int downloadSize;
	private int installSize;
	private boolean fragment;

	public FeaturePlugin() {
	}

	public boolean isFragment() {
		return fragment;
	}

	public IPluginBase getPluginBase() {
		return pluginBase;
	}

	public void setFragment(boolean fragment) throws CoreException {
		ensureModelEditable();
		this.fragment = fragment;
	}

	protected void parse(Node node) {
		super.parse(node);
		os = getNodeAttribute(node, "os");
		ws = getNodeAttribute(node, "ws");
		nl = getNodeAttribute(node, "nl");
		String f = getNodeAttribute(node, "fragment");
		if (f != null && f.equalsIgnoreCase("true"))
			fragment = true;
		downloadSize = getIntegerAttribute(node, "download-size");
		installSize = getIntegerAttribute(node, "install-size");
		hookWithWorkspace();
	}

	private void hookWithWorkspace() {
		if (fragment) {
			IFragmentModel[] fragments =
				PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
			for (int i = 0; i < fragments.length; i++) {
				IFragment fragment = fragments[i].getFragment();
				if (fragment.getId().equals(id)) {
					if (version == null || fragment.getVersion().equals(version)) {
						pluginBase = fragment;
						break;
					}
				}
			}
		} else {
			pluginBase = PDEPlugin.getDefault().findPlugin(id, version, 0);
		}
	}

	public void loadFrom(IPluginBase plugin) {
		id = plugin.getId();
		label = plugin.getTranslatedName();
		version = plugin.getVersion();
		fragment = plugin instanceof IFragment;
		this.pluginBase = plugin;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin");
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		if (getId() != null) {
			writer.println();
			writer.print(indent2 + "id=\"" + getId() + "\"");
		}
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\"");
		}
		if (isFragment()) {
			writer.println();
			writer.print(indent2 + "fragment=\"true\"");
		}
		if (getOS() != null) {
			writer.println();
			writer.print(indent2 + "os=\"" + getOS() + "\"");
		}
		if (getWS() != null) {
			writer.println();
			writer.print(indent2 + "ws=\"" + getWS() + "\"");
		}
		if (getNL() != null) {
			writer.println();
			writer.print(indent2 + "nl=\"" + getNL() + "\"");
		}
		writer.println();
		writer.print(indent2 + "download-size=\"" + getDownloadSize() + "\"");
		writer.println();
		writer.print(indent2 + "install-size=\"" + getInstallSize() + "\"");
		writer.println(">");
		writer.println(indent + "</plugin>");
	}

	/**
	 * Gets the os.
	 * @return Returns a String
	 */
	public String getOS() {
		return os;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.os;
		this.os = os;
		firePropertyChanged(P_OS, oldValue, os);
	}

	/**
	 * Gets the ws.
	 * @return Returns a String
	 */
	public String getWS() {
		return ws;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.ws;
		this.ws = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}

	/**
	 * Gets the nl.
	 * @return Returns a String
	 */
	public String getNL() {
		return nl;
	}

	/**
	 * Sets the nl.
	 * @param nl The nl to set
	 */
	public void setNL(String nl) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.nl;
		this.nl = nl;
		firePropertyChanged(P_NL, oldValue, nl);
	}

	/**
	 * Gets the downloadSize.
	 * @return Returns a int
	 */
	public int getDownloadSize() {
		return downloadSize;
	}

	/**
	 * Sets the downloadSize.
	 * @param downloadSize The downloadSize to set
	 */
	public void setDownloadSize(int downloadSize) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Integer(this.downloadSize);
		this.downloadSize = downloadSize;
		firePropertyChanged(P_DOWNLOAD_SIZE, oldValue, new Integer(downloadSize));
	}

	/**
	 * Gets the installSize.
	 * @return Returns a int
	 */
	public int getInstallSize() {
		return installSize;
	}

	/**
	 * Sets the installSize.
	 * @param installSize The installSize to set
	 */
	public void setInstallSize(int installSize) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Integer(this.installSize);
		this.installSize = installSize;
		firePropertyChanged(P_DOWNLOAD_SIZE, oldValue, new Integer(installSize));
	}

	public String getLabel() {
		if (pluginBase != null) {
			return pluginBase.getTranslatedName();
		}
		String name = super.getLabel();
		if (name==null) name = getId();
		return name;
	}
	
	public String toString() {
		return getLabel();
	}
}