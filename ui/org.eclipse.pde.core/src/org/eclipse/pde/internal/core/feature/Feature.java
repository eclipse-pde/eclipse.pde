/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public class Feature extends VersionableObject implements IFeature {
	private static final long serialVersionUID = 1L;
	final static String INDENT = "   "; //$NON-NLS-1$
	private String fProviderName;
	private IFeatureURL fUrl;
	private IFeatureInfo[] fInfos = new IFeatureInfo[3];
	private Vector fData = new Vector();
	private Vector fChildren = new Vector();
	private Vector fPlugins = new Vector();
	private Vector fImports = new Vector();
	private String fOs;
	private String fWs;
	private String fNl;
	private String fArch;
	private String fImageName;
	private IFeatureInstallHandler fHandler;
	private boolean fPrimary;
	private boolean fExclusive;
	private String fColocationAffinity;
	private String fApplication;
	private String fPlugin;
	private boolean fValid;

	public void addPlugins(IFeaturePlugin[] newPlugins) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newPlugins.length; i++) {
			fPlugins.add(newPlugins[i]);
			((FeaturePlugin) newPlugins[i]).setInTheModel(true);
		}
		fireStructureChanged(newPlugins, IModelChangedEvent.INSERT);
	}

	public void addData(IFeatureData[] newData) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newData.length; i++) {
			fData.add(newData[i]);
			((FeatureData) newData[i]).setInTheModel(true);
		}
		fireStructureChanged(newData, IModelChangedEvent.INSERT);
	}

	public void addIncludedFeatures(IFeatureChild[] features)
			throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < features.length; i++) {
			fChildren.add(features[i]);
			((FeatureChild) features[i]).setInTheModel(true);
		}
		fireStructureChanged(features, IModelChangedEvent.INSERT);
	}

	public void addImports(IFeatureImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < iimports.length; i++) {
			fImports.add(iimports[i]);
			((FeatureImport) iimports[i]).setInTheModel(true);
		}
		fireStructureChanged(iimports, IModelChangedEvent.INSERT);
	}

	public IFeaturePlugin[] getPlugins() {
		IFeaturePlugin[] result = new IFeaturePlugin[fPlugins.size()];
		fPlugins.copyInto(result);
		return result;
	}

	public IFeatureData[] getData() {
		IFeatureData[] result = new IFeatureData[fData.size()];
		fData.copyInto(result);
		return result;
	}

	public IFeatureChild[] getIncludedFeatures() {
		IFeatureChild[] result = new IFeatureChild[fChildren.size()];
		fChildren.copyInto(result);
		return result;
	}
	public IFeatureImport[] getImports() {
		IFeatureImport[] result = new IFeatureImport[fImports.size()];
		fImports.copyInto(result);
		return result;
	}
	public String getProviderName() {
		return fProviderName;
	}

	public String getPlugin() {
		return fPlugin;
	}

	public IPluginModelBase getReferencedModel(IFeaturePlugin reference) {
		WorkspaceModelManager mng = PDECore.getDefault()
				.getWorkspaceModelManager();
		IPluginModelBase[] models = null;
		if (reference.isFragment())
			models = mng.getFragmentModels();
		else
			models = mng.getPluginModels();

		for (int i = 0; i < models.length; i++) {
			IPluginBase base = models[i].getPluginBase();
			if (base.getId().equals(reference.getId()))
				return models[i];
		}
		return null;
	}
	public IFeatureURL getURL() {
		return fUrl;
	}
	public IFeatureInstallHandler getInstallHandler() {
		return fHandler;
	}

	public IFeatureInfo getFeatureInfo(int index) {
		return fInfos[index];
	}

	public boolean isPrimary() {
		return fPrimary;
	}

	public boolean isExclusive() {
		return fExclusive;
	}

	protected void parse(Node node, Hashtable lineTable) {
		super.parse(node, lineTable);
		fProviderName = getNodeAttribute(node, "provider-name"); //$NON-NLS-1$
		fPlugin = getNodeAttribute(node, "plugin"); //$NON-NLS-1$
		fOs = getNodeAttribute(node, "os"); //$NON-NLS-1$
		fWs = getNodeAttribute(node, "ws"); //$NON-NLS-1$
		fNl = getNodeAttribute(node, "nl"); //$NON-NLS-1$
		fArch = getNodeAttribute(node, "arch"); //$NON-NLS-1$
		fImageName = getNodeAttribute(node, "image"); //$NON-NLS-1$
		fColocationAffinity = getNodeAttribute(node, "colocation-affinity"); //$NON-NLS-1$
		fApplication = getNodeAttribute(node, "application"); //$NON-NLS-1$
		fPrimary = getBooleanAttribute(node, "primary"); //$NON-NLS-1$
		fExclusive = getBooleanAttribute(node, "exclusive"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		fValid = true;

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase();
				if (tag.equals("description")) { //$NON-NLS-1$
					IFeatureInfo info = getModel().getFactory().createInfo(
							IFeature.INFO_DESCRIPTION);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child, lineTable);
					fInfos[IFeature.INFO_DESCRIPTION] = info;
				} else if (tag.equals("license")) { //$NON-NLS-1$
					IFeatureInfo info = getModel().getFactory().createInfo(
							IFeature.INFO_LICENSE);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child, lineTable);
					fInfos[IFeature.INFO_LICENSE] = info;
				} else if (tag.equals("copyright")) { //$NON-NLS-1$
					IFeatureInfo info = getModel().getFactory().createInfo(
							IFeature.INFO_COPYRIGHT);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child, lineTable);
					fInfos[IFeature.INFO_COPYRIGHT] = info;
				} else if (tag.equals("url")) { //$NON-NLS-1$
					if (fUrl == null) {
						fUrl = getModel().getFactory().createURL();
						((FeatureURL) fUrl).setInTheModel(true);
						((FeatureURL) fUrl).parse(child, lineTable);
					}
				} else if (tag.equals("requires")) { //$NON-NLS-1$
					parseRequires(child, lineTable);
				} else if (tag.equals("install-handler")) { //$NON-NLS-1$
					IFeatureInstallHandler handler = getModel().getFactory()
							.createInstallHandler();
					((FeatureInstallHandler) handler).parse(child, lineTable);
					((FeatureInstallHandler) handler).setInTheModel(true);
					this.fHandler = handler;
				} else if (tag.equals("plugin")) { //$NON-NLS-1$
					IFeaturePlugin plugin = getModel().getFactory()
							.createPlugin();
					((FeaturePlugin) plugin).parse(child, lineTable);
					((FeaturePlugin) plugin).setInTheModel(true);
					fPlugins.add(plugin);
				} else if (tag.equals("data")) { //$NON-NLS-1$
					IFeatureData newData = getModel().getFactory().createData();
					((FeatureData) newData).parse(child, lineTable);
					((FeatureData) newData).setInTheModel(true);
					fData.add(newData);
				} else if (tag.equals("includes")) { //$NON-NLS-1$
					IFeatureChild newChild = getModel().getFactory()
							.createChild();
					((FeatureChild) newChild).parse(child, lineTable);
					((FeatureChild) newChild).setInTheModel(true);
					this.fChildren.add(newChild);
				}
			}
		}
		fValid = hasRequiredAttributes();
	}
	private void parseRequires(Node node, Hashtable lineTable) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equalsIgnoreCase("import")) { //$NON-NLS-1$
					IFeatureImport iimport = getModel().getFactory()
							.createImport();
					((FeatureImport) iimport).parse(child, lineTable);
					((FeatureImport) iimport).setInTheModel(true);
					fImports.add(iimport);
				}
			}
		}
	}

	public void computeImports() throws CoreException {
		IFeatureImport[] removed = null;

		if (fImports.size() > 0) {
			ArrayList list = new ArrayList();
			for (int i = 0; i < fImports.size(); i++) {
				IFeatureImport iimport = (IFeatureImport) fImports.get(i);
				if (iimport.getType() != IFeatureImport.FEATURE)
					list.add(iimport);
			}
			if (list.size() > 0) {
				fImports.removeAll(list);
				removed = (IFeatureImport[]) list
						.toArray(new IFeatureImport[list.size()]);
			}
		}
		fImports.clear();
		if (removed != null)
			fireStructureChanged(removed, IModelChangedEvent.REMOVE);
		// Create full import list
		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin fp = (IFeaturePlugin) fPlugins.get(i);
			IPluginBase plugin = PDECore.getDefault().findPlugin(fp.getId(),
					fp.getVersion(), 0);
			if (plugin != null) {
				addPluginImports(plugin);
			}
		}
		// Find plug-ins that satisfy requirements within this feature.
		// Whatever remains will be feature external requirements.
		Vector inputImports = (Vector) fImports.clone();
		for (int i = 0; i < inputImports.size(); i++) {
			IFeatureImport iimport = (IFeatureImport) inputImports.get(i);
			IFeaturePlugin local = findFeaturePlugin(iimport.getId(), iimport
					.getVersion(), iimport.getMatch());
			if (local != null)
				fImports.remove(iimport);
		}
		if (fImports.size() > 0) {
			IFeatureImport[] added = (IFeatureImport[]) fImports
					.toArray(new IFeatureImport[fImports.size()]);
			fireStructureChanged(added, IModelChangedEvent.INSERT);
		}
	}

	private void addPluginImports(IPluginBase plugin) throws CoreException {
		IPluginImport[] pluginImports = plugin.getImports();
		for (int i = 0; i < pluginImports.length; i++) {
			IPluginImport pluginImport = pluginImports[i];
			String id = pluginImport.getId();
			String version = pluginImport.getVersion();
			int match = pluginImport.getMatch();
			// Don't add duplicates
			if (findImport(id, version, match) != null)
				continue;
			IFeatureImport iimport = getModel().getFactory().createImport();
			iimport.setId(id);
			iimport.setVersion(version);
			iimport.setMatch(match);
			((FeatureImport) iimport).setInTheModel(true);
			fImports.add(iimport);
			IPlugin p = PDECore.getDefault().findPlugin(pluginImport.getId(),
					pluginImport.getVersion(), pluginImport.getMatch());
			((FeatureImport) iimport).setPlugin(p);
			/*
			 * if (p != null) addPluginImports(p);
			 */
		}
	}

	private IFeatureImport findImport(String id, String version, int match) {
		for (int i = 0; i < fImports.size(); i++) {
			IFeatureImport iimport = (IFeatureImport) fImports.get(i);
			if (iimport.getId().equals(id)) {
				if (version == null)
					return iimport;
				if (version.equals(iimport.getVersion())
						&& match == iimport.getMatch())
					return iimport;
			}
		}
		return null;
	}

	private IFeaturePlugin findFeaturePlugin(String id, String version,
			int match) {

		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin fp = (IFeaturePlugin) fPlugins.get(i);
			String pid = fp.getId();
			String pversion = fp.getVersion();
			if (PDECore.compare(id, version, pid, pversion, match))
				return fp;
		}
		return null;
	}

	public void removePlugins(IFeaturePlugin[] removed) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < removed.length; i++) {
			fPlugins.remove(removed[i]);
			((FeaturePlugin) removed[i]).setInTheModel(false);
		}
		fireStructureChanged(removed, IModelChangedEvent.REMOVE);
	}

	public void removeData(IFeatureData[] removed) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < removed.length; i++) {
			fData.remove(removed[i]);
			((FeatureData) removed[i]).setInTheModel(false);
		}
		fireStructureChanged(removed, IModelChangedEvent.REMOVE);
	}

	public void removeIncludedFeatures(IFeatureChild[] features)
			throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < features.length; i++) {
			fChildren.remove(features[i]);
			((FeatureChild) features[i]).setInTheModel(false);
		}
		fireStructureChanged(features, IModelChangedEvent.REMOVE);
	}
	public void removeImports(IFeatureImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < iimports.length; i++) {
			fImports.remove(iimports[i]);
			((FeatureImport) iimports[i]).setInTheModel(false);
		}
		fireStructureChanged(iimports, IModelChangedEvent.REMOVE);
	}

	public String getOS() {
		return fOs;
	}

	public String getWS() {
		return fWs;
	}

	public String getNL() {
		return fNl;
	}

	public String getArch() {
		return fArch;
	}

	public String getColocationAffinity() {
		return fColocationAffinity;
	}

	public String getApplication() {
		return fApplication;
	}

	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fOs;
		this.fOs = os;
		firePropertyChanged(P_OS, oldValue, os);
	}
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fWs;
		this.fWs = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}
	public void setNL(String nl) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fNl;
		this.fNl = nl;
		firePropertyChanged(P_NL, oldValue, nl);
	}
	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fArch;
		this.fArch = arch;
		firePropertyChanged(P_ARCH, oldValue, arch);
	}

	public void setPrimary(boolean newValue) throws CoreException {
		if (this.fPrimary == newValue)
			return;
		ensureModelEditable();
		Boolean oldValue = this.fPrimary ? Boolean.TRUE : Boolean.FALSE;
		this.fPrimary = newValue;
		firePropertyChanged(P_PRIMARY, oldValue, newValue
				? Boolean.TRUE
				: Boolean.FALSE);
	}

	public void setExclusive(boolean newValue) throws CoreException {
		if (this.fExclusive == newValue)
			return;
		ensureModelEditable();
		Boolean oldValue = this.fExclusive ? Boolean.TRUE : Boolean.FALSE;
		this.fExclusive = newValue;
		firePropertyChanged(P_EXCLUSIVE, oldValue, newValue
				? Boolean.TRUE
				: Boolean.FALSE);
	}

	public void setColocationAffinity(String newValue) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fColocationAffinity;
		this.fColocationAffinity = newValue;
		firePropertyChanged(P_COLLOCATION_AFFINITY, oldValue, newValue);
	}

	public void setApplication(String newValue) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fApplication;
		this.fApplication = newValue;
		firePropertyChanged(P_APPLICATION, oldValue, newValue);
	}

	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fProviderName;
		this.fProviderName = providerName;
		firePropertyChanged(P_PROVIDER, oldValue, providerName);
	}

	public void setPlugin(String plugin) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fPlugin;
		this.fPlugin = plugin;
		firePropertyChanged(P_PLUGIN, oldValue, plugin);
	}
	public void setURL(IFeatureURL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fUrl;
		if (this.fUrl != null) {
			((FeatureURL) this.fUrl).setInTheModel(false);
		}
		this.fUrl = url;
		firePropertyChanged(P_URL, oldValue, url);
	}
	public void setInstallHandler(IFeatureInstallHandler handler)
			throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fHandler;
		if (this.fHandler != null) {
			((FeatureInstallHandler) this.fHandler).setInTheModel(false);
		}
		this.fHandler = handler;
		firePropertyChanged(P_INSTALL_HANDLER, oldValue, handler);
	}

	public void setFeatureInfo(IFeatureInfo info, int index)
			throws CoreException {
		ensureModelEditable();
		Object oldValue = fInfos[index];
		if (oldValue != null) {
			((FeatureInfo) oldValue).setInTheModel(true);
		}
		fInfos[index] = info;
		String property;
		switch (index) {
			case INFO_DESCRIPTION :
				property = P_DESCRIPTION;
				break;
			case INFO_LICENSE :
				property = P_LICENSE;
				break;
			case INFO_COPYRIGHT :
				property = P_COPYRIGHT;
				break;
			default :
				return;
		}
		firePropertyChanged(property, oldValue, info);
	}

	/**
	 * Sets the imageName.
	 * 
	 * @param imageName
	 *            The imageName to set
	 */
	public void setImageName(String imageName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fImageName;
		this.fImageName = imageName;
		firePropertyChanged(P_IMAGE, oldValue, imageName);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
			throws CoreException {
		if (name.equals(P_OS)) {
			setOS((String) newValue);
		} else if (name.equals(P_WS)) {
			setWS((String) newValue);
		} else if (name.equals(P_NL)) {
			setNL((String) newValue);
		} else if (name.equals(P_ARCH)) {
			setArch((String) newValue);
		} else if (name.equals(P_COLLOCATION_AFFINITY)) {
			setColocationAffinity((String) newValue);
		} else if (name.equals(P_APPLICATION)) {
			setApplication((String) newValue);
		} else if (name.equals(P_PRIMARY)) {
			setPrimary(newValue != null
					? ((Boolean) newValue).booleanValue()
					: false);
		} else if (name.equals(P_EXCLUSIVE)) {
			setExclusive(newValue != null
					? ((Boolean) newValue).booleanValue()
					: false);
		} else if (name.equals(P_PROVIDER)) {
			setProviderName((String) newValue);
		} else if (name.equals(P_PLUGIN)) {
			setPlugin((String) newValue);
		} else if (name.equals(P_URL)) {
			setURL((IFeatureURL) newValue);
		} else if (name.equals(P_INSTALL_HANDLER)) {
			setInstallHandler((IFeatureInstallHandler) newValue);
		} else if (name.equals(P_DESCRIPTION)) {
			setFeatureInfo((IFeatureInfo) newValue, INFO_DESCRIPTION);
		} else if (name.equals(P_LICENSE)) {
			setFeatureInfo((IFeatureInfo) newValue, INFO_LICENSE);
		} else if (name.equals(P_COPYRIGHT)) {
			setFeatureInfo((IFeatureInfo) newValue, INFO_COPYRIGHT);
		} else if (name.equals(P_IMAGE)) {
			setImageName((String) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void reset() {
		super.reset();
		fData.clear();
		fPlugins.clear();
		fImports.clear();
		fChildren.clear();
		fUrl = null;
		fProviderName = null;
		fPlugin = null;
		fOs = null;
		fWs = null;
		fNl = null;
		fArch = null;
		fInfos[0] = null;
		fInfos[1] = null;
		fInfos[2] = null;
		fPrimary = false;
		fExclusive = false;
		fColocationAffinity = null;
		fApplication = null;
		fValid = false;
	}

	public boolean isValid() {
		return fValid;
	}

	private boolean hasRequiredAttributes() {
		// Verify that all the required attributes are
		// defined.
		if (id == null)
			return false;
		if (version == null)
			return false;

		for (int i = 0; i < fChildren.size(); i++) {
			IFeatureChild child = (IFeatureChild) fChildren.elementAt(i);
			if (child.getId() == null || child.getVersion() == null)
				return false;
		}
		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin plugin = (IFeaturePlugin) fPlugins.elementAt(i);
			if (plugin.getId() == null || plugin.getVersion() == null)
				return false;

		}
		for (int i = 0; i < fData.size(); i++) {
			IFeatureData entry = (IFeatureData) fData.elementAt(i);
			if (entry.getId() == null)
				return false;
		}
		for (int i = 0; i < fImports.size(); i++) {
			IFeatureImport iimport = (IFeatureImport) fImports.elementAt(i);
			if (iimport.getId() == null)
				return false;
		}
		return true;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<feature"); //$NON-NLS-1$
		String indent2 = indent + INDENT;
		String indenta = indent + INDENT + INDENT;
		writeIfDefined(indenta, writer, "id", getId()); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "label", getWritableString(getLabel())); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "version", getVersion()); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "provider-name", //$NON-NLS-1$
				getWritableString(fProviderName));
		writeIfDefined(indenta, writer, "plugin", //$NON-NLS-1$
				getPlugin());
		writeIfDefined(indenta, writer, "os", fOs); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "ws", fWs); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "nl", fNl); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "arch", fArch); //$NON-NLS-1$
		if (fImageName != null)
			writeIfDefined(indenta, writer,
					"image", getWritableString(fImageName)); //$NON-NLS-1$
		if (isPrimary()) {
			writer.println();
			writer.print(indenta + "primary=\"true\""); //$NON-NLS-1$
		}
		if (isExclusive()) {
			writer.println();
			writer.print(indenta + "exclusive=\"true\""); //$NON-NLS-1$
		}
		writeIfDefined(indenta, writer,
				"colocation-affinity", fColocationAffinity); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "application", fApplication); //$NON-NLS-1$

		writer.println(">"); //$NON-NLS-1$
		if (fHandler != null) {
			fHandler.write(indent2, writer);
		}

		for (int i = 0; i < 3; i++) {
			IFeatureInfo info = fInfos[i];
			if (info != null && !info.isEmpty())
				info.write(indent2, writer);
		}

		if (fUrl != null) {
			fUrl.write(indent2, writer);
		}
		for (int i = 0; i < fChildren.size(); i++) {
			IFeatureChild child = (IFeatureChild) fChildren.elementAt(i);
			writer.println();
			child.write(indent2, writer);
		}
		if (fImports.size() > 0) {
			writer.println();
			writer.println(indent2 + "<requires>"); //$NON-NLS-1$
			for (int i = 0; i < fImports.size(); i++) {
				IFeatureImport iimport = (IFeatureImport) fImports.get(i);
				iimport.write(indenta, writer);
			}
			writer.println(indent2 + "</requires>"); //$NON-NLS-1$
		}
		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin plugin = (IFeaturePlugin) fPlugins.elementAt(i);
			writer.println();
			plugin.write(indent2, writer);
		}
		for (int i = 0; i < fData.size(); i++) {
			IFeatureData entry = (IFeatureData) fData.elementAt(i);
			writer.println();
			entry.write(indent2, writer);
		}
		writer.println();
		writer.println(indent + "</feature>"); //$NON-NLS-1$
	}
	private void writeIfDefined(String indent, PrintWriter writer,
			String attName, String attValue) {
		if (attValue == null || attValue.trim().length() == 0)
			return;
		writer.println();
		writer.print(indent + attName + "=\"" + attValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}
	/**
	 * Gets the imageName.
	 * 
	 * @return Returns a String
	 */
	public String getImageName() {
		return fImageName;
	}
}
