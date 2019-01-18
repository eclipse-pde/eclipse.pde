/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeatureURL;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Feature extends VersionableObject implements IFeature {
	private static final long serialVersionUID = 1L;
	final static String INDENT = "   "; //$NON-NLS-1$
	private String fProviderName;
	private IFeatureURL fUrl;
	private final IFeatureInfo[] fInfos = new IFeatureInfo[3];
	private final Vector<IFeatureData> fData = new Vector<>();
	private final Vector<IFeatureChild> fChildren = new Vector<>();
	private final Vector<IFeaturePlugin> fPlugins = new Vector<>();
	private Vector<IFeatureImport> fImports = new Vector<>();
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
	private String fCopyright;
	private String fLicenseFeatureID;
	private String fLicenseFeatureVersion;

	@Override
	public void addPlugins(IFeaturePlugin[] newPlugins) throws CoreException {
		ensureModelEditable();
		for (IFeaturePlugin plugin : newPlugins) {
			fPlugins.add(plugin);
			((FeaturePlugin) plugin).setInTheModel(true);
		}
		fireStructureChanged(newPlugins, IModelChangedEvent.INSERT);
	}

	@Override
	public void addData(IFeatureData[] newData) throws CoreException {
		ensureModelEditable();
		for (IFeatureData data : newData) {
			fData.add(data);
			((FeatureData) data).setInTheModel(true);
		}
		fireStructureChanged(newData, IModelChangedEvent.INSERT);
	}

	@Override
	public void addIncludedFeatures(IFeatureChild[] features) throws CoreException {
		ensureModelEditable();
		for (IFeatureChild child : features) {
			fChildren.add(child);
			((FeatureChild) child).setInTheModel(true);
		}
		fireStructureChanged(features, IModelChangedEvent.INSERT);
	}

	@Override
	public void addImports(IFeatureImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (IFeatureImport iimport : iimports) {
			fImports.add(iimport);
			((FeatureImport) iimport).setInTheModel(true);
		}
		fireStructureChanged(iimports, IModelChangedEvent.INSERT);
	}

	@Override
	public IFeaturePlugin[] getPlugins() {
		IFeaturePlugin[] result = new IFeaturePlugin[fPlugins.size()];
		fPlugins.copyInto(result);
		return result;
	}

	@Override
	public IFeatureData[] getData() {
		IFeatureData[] result = new IFeatureData[fData.size()];
		fData.copyInto(result);
		return result;
	}

	@Override
	public IFeatureChild[] getIncludedFeatures() {
		IFeatureChild[] result = new IFeatureChild[fChildren.size()];
		fChildren.copyInto(result);
		return result;
	}

	@Override
	public IFeatureImport[] getImports() {
		IFeatureImport[] result = new IFeatureImport[fImports.size()];
		fImports.copyInto(result);
		return result;
	}

	@Override
	public String getProviderName() {
		return fProviderName;
	}

	@Override
	public void setLicenseFeatureID(String featureID) {
		fLicenseFeatureID = featureID;
	}

	@Override
	public String getLicenseFeatureID() {
		if (fLicenseFeatureID == null) {
			fLicenseFeatureID = ""; //$NON-NLS-1$
		}
		return fLicenseFeatureID;
	}

	@Override
	public void setLicenseFeatureVersion(String version) {
		fLicenseFeatureVersion = version;
	}

	@Override
	public String getLicenseFeatureVersion() {
		if (fLicenseFeatureVersion == null) {
			fLicenseFeatureVersion = ""; //$NON-NLS-1$
		}
		return fLicenseFeatureVersion;
	}

	@Override
	public String getPlugin() {
		return fPlugin;
	}

	@Override
	public IPluginModelBase getReferencedModel(IFeaturePlugin reference) {
		IPluginModelBase model = PluginRegistry.findModel(reference.getId());
		return (model != null && model.isEnabled()) ? model : null;
	}

	@Override
	public IFeatureURL getURL() {
		return fUrl;
	}

	@Override
	public IFeatureInstallHandler getInstallHandler() {
		return fHandler;
	}

	@Override
	public IFeatureInfo getFeatureInfo(int infoType) {
		if (infoType < 0 || infoType > (fInfos.length - 1)) {
			return null;
		}
		return fInfos[infoType];
	}

	@Override
	public boolean isPrimary() {
		return fPrimary;
	}

	@Override
	public boolean isExclusive() {
		return fExclusive;
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		fProviderName = getNodeAttribute(node, "provider-name"); //$NON-NLS-1$
		fLicenseFeatureID = getNodeAttribute(node, "license-feature"); //$NON-NLS-1$
		fLicenseFeatureVersion = getNodeAttribute(node, "license-feature-version"); //$NON-NLS-1$
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
				String tag = child.getNodeName().toLowerCase(Locale.ENGLISH);
				if (tag.equals("description")) { //$NON-NLS-1$
					IFeatureInfo info = getModel().getFactory().createInfo(IFeature.INFO_DESCRIPTION);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child);
					fInfos[IFeature.INFO_DESCRIPTION] = info;
				} else if (tag.equals("license")) { //$NON-NLS-1$
					IFeatureInfo info = getModel().getFactory().createInfo(IFeature.INFO_LICENSE);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child);
					fInfos[IFeature.INFO_LICENSE] = info;
				} else if (tag.equals("copyright")) { //$NON-NLS-1$
					IFeatureInfo info = getModel().getFactory().createInfo(IFeature.INFO_COPYRIGHT);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child);
					fInfos[IFeature.INFO_COPYRIGHT] = info;
				} else if (tag.equals("url")) { //$NON-NLS-1$
					if (fUrl == null) {
						fUrl = getModel().getFactory().createURL();
						((FeatureURL) fUrl).setInTheModel(true);
						((FeatureURL) fUrl).parse(child);
					}
				} else if (tag.equals("requires")) { //$NON-NLS-1$
					parseRequires(child);
				} else if (tag.equals("install-handler")) { //$NON-NLS-1$
					IFeatureInstallHandler handler = getModel().getFactory().createInstallHandler();
					((FeatureInstallHandler) handler).parse(child);
					((FeatureInstallHandler) handler).setInTheModel(true);
					this.fHandler = handler;
				} else if (tag.equals("plugin")) { //$NON-NLS-1$
					IFeaturePlugin plugin = getModel().getFactory().createPlugin();
					((FeaturePlugin) plugin).parse(child);
					((FeaturePlugin) plugin).setInTheModel(true);
					fPlugins.add(plugin);
				} else if (tag.equals("data")) { //$NON-NLS-1$
					IFeatureData newData = getModel().getFactory().createData();
					((FeatureData) newData).parse(child);
					((FeatureData) newData).setInTheModel(true);
					fData.add(newData);
				} else if (tag.equals("includes")) { //$NON-NLS-1$
					IFeatureChild newChild = getModel().getFactory().createChild();
					((FeatureChild) newChild).parse(child);
					((FeatureChild) newChild).setInTheModel(true);
					this.fChildren.add(newChild);
				}
			}
		}
		fValid = hasRequiredAttributes();
	}

	private void parseRequires(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equalsIgnoreCase("import")) { //$NON-NLS-1$
					IFeatureImport iimport = getModel().getFactory().createImport();
					((FeatureImport) iimport).parse(child);
					((FeatureImport) iimport).setInTheModel(true);
					fImports.add(iimport);
				}
			}
		}
	}

	@Override
	public void computeImports() throws CoreException {
		// some existing imports may valid and can be preserved
		Vector<IFeatureImport> preservedImports = new Vector<>(fImports.size());
		// new imports
		ArrayList<IFeatureImport> newImports = new ArrayList<>();
		IPluginModelBase model = null;
		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin fp = fPlugins.get(i);
			ModelEntry entry = PluginRegistry.findEntry(fp.getId());
			if (entry == null) {
				continue;
			}
			IPluginModelBase[] models = entry.getActiveModels();
			for (IPluginModelBase m : models) {
				if (fp.getVersion().equals(m.getPluginBase().getVersion()) || fp.getVersion().equals(ICoreConstants.DEFAULT_VERSION)) {
					model = m;
				}
			}
			if (model != null) {
				addPluginImports(preservedImports, newImports, model.getPluginBase());
				if (model.isFragmentModel()) {
					BundleDescription desc = model.getBundleDescription();
					if (desc == null) {
						continue;
					}
					HostSpecification hostSpec = desc.getHost();
					String id = hostSpec.getName();
					String version = null;
					int match = IMatchRules.NONE;
					VersionRange versionRange = hostSpec.getVersionRange();
					if (!(versionRange == null || VersionRange.emptyRange.equals(versionRange))) {
						version = versionRange.getMinimum() != null ? versionRange.getMinimum().toString() : null;
						match = PluginBase.getMatchRule(versionRange);
					}
					addNewDependency(id, version, match, preservedImports, newImports);
				}
			}
		}
		// preserve imports of features
		for (int i = 0; i < fImports.size(); i++) {
			IFeatureImport iimport = fImports.get(i);
			if (iimport.getType() == IFeatureImport.FEATURE) {
				preservedImports.add(iimport);
			}
		}
		// removed = old - preserved
		@SuppressWarnings("unchecked")
		Vector<IFeatureImport> removedImports = ((Vector<IFeatureImport>) fImports.clone());
		removedImports.removeAll(preservedImports);
		// perform remove
		fImports = preservedImports;
		if (!removedImports.isEmpty()) {
			fireStructureChanged(removedImports.toArray(new IFeatureImport[removedImports.size()]), IModelChangedEvent.REMOVE);
		}
		// perform add
		if (!newImports.isEmpty()) {
			fImports.addAll(newImports);
			fireStructureChanged(newImports.toArray(new IFeatureImport[newImports.size()]), IModelChangedEvent.INSERT);
		}
	}

	/**
	 * Creates IFeatureImports based on IPluginImports. Ensures no duplicates in
	 * preservedImports + newImports
	 *
	 * @param preservedImports
	 *            out for valid existing imports
	 * @param newImports
	 *            out for new imports
	 * @param plugin
	 * @throws CoreException
	 */
	private void addPluginImports(List<IFeatureImport> preservedImports, List<IFeatureImport> newImports, IPluginBase plugin) throws CoreException {
		IPluginImport[] pluginImports = plugin.getImports();
		for (IPluginImport pluginImport : pluginImports) {
			if (pluginImport.isOptional()) {
				continue;
			}
			String id = pluginImport.getId();
			String version = pluginImport.getVersion();
			int match = pluginImport.getMatch();
			addNewDependency(id, version, match, preservedImports, newImports);
		}
	}

	private void addNewDependency(String id, String version, int match, List<IFeatureImport> preservedImports, List<IFeatureImport> newImports) throws CoreException {
		if (findFeaturePlugin(id, version, match) != null) {
			// don't add imports to local plug-ins
			return;
		}
		if (findImport(preservedImports, id, version, match) != null) {
			// already seen
			return;
		}
		if (findImport(newImports, id, version, match) != null) {
			// already seen
			return;
		}
		IFeatureImport iimport = findImport(fImports, id, version, match);
		if (iimport != null) {
			// import still valid
			preservedImports.add(iimport);
			return;
		}
		// a new one is needed
		iimport = getModel().getFactory().createImport();
		iimport.setId(id);
		iimport.setVersion(version);
		iimport.setMatch(match);
		((FeatureImport) iimport).setInTheModel(true);
		newImports.add(iimport);
	}

	/**
	 * Finds a given import in the list
	 * @param imports list of imports
	 * @param id
	 * @param version
	 * @param match
	 * @return IFeatureImport or null
	 */
	private IFeatureImport findImport(List<IFeatureImport> imports, String id, String version, int match) {
		for (int i = 0; i < imports.size(); i++) {
			IFeatureImport iimport = imports.get(i);
			if (iimport.getId().equals(id)) {
				if (version == null) {
					return iimport;
				}
				if (version.equals(iimport.getVersion()) && match == iimport.getMatch()) {
					return iimport;
				}
			}
		}
		return null;
	}

	private IFeaturePlugin findFeaturePlugin(String id, String version, int match) {

		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin fp = fPlugins.get(i);
			String pid = fp.getId();
			String pversion = fp.getVersion();
			if (VersionUtil.compare(pid, pversion, id, version, match)) {
				return fp;
			}
		}
		return null;
	}

	@Override
	public void removePlugins(IFeaturePlugin[] removed) throws CoreException {
		ensureModelEditable();
		for (IFeaturePlugin element : removed) {
			fPlugins.remove(element);
			((FeaturePlugin) element).setInTheModel(false);
		}
		fireStructureChanged(removed, IModelChangedEvent.REMOVE);
	}

	@Override
	public void removeData(IFeatureData[] removed) throws CoreException {
		ensureModelEditable();
		for (IFeatureData element : removed) {
			fData.remove(element);
			((FeatureData) element).setInTheModel(false);
		}
		fireStructureChanged(removed, IModelChangedEvent.REMOVE);
	}

	@Override
	public void removeIncludedFeatures(IFeatureChild[] features) throws CoreException {
		ensureModelEditable();
		for (IFeatureChild feature : features) {
			fChildren.remove(feature);
			((FeatureChild) feature).setInTheModel(false);
		}
		fireStructureChanged(features, IModelChangedEvent.REMOVE);
	}

	@Override
	public void removeImports(IFeatureImport[] iimports) throws CoreException {
		ensureModelEditable();
		for (IFeatureImport iimport : iimports) {
			fImports.remove(iimport);
			((FeatureImport) iimport).setInTheModel(false);
		}
		fireStructureChanged(iimports, IModelChangedEvent.REMOVE);
	}

	@Override
	public String getOS() {
		return fOs;
	}

	@Override
	public String getWS() {
		return fWs;
	}

	@Override
	public String getNL() {
		return fNl;
	}

	@Override
	public String getArch() {
		return fArch;
	}

	@Override
	public String getColocationAffinity() {
		return fColocationAffinity;
	}

	@Override
	public String getApplication() {
		return fApplication;
	}

	@Override
	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fOs;
		this.fOs = os;
		firePropertyChanged(P_OS, oldValue, os);
	}

	@Override
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fWs;
		this.fWs = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}

	@Override
	public void setNL(String nl) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fNl;
		this.fNl = nl;
		firePropertyChanged(P_NL, oldValue, nl);
	}

	@Override
	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fArch;
		this.fArch = arch;
		firePropertyChanged(P_ARCH, oldValue, arch);
	}

	@Override
	public void setPrimary(boolean newValue) throws CoreException {
		if (this.fPrimary == newValue) {
			return;
		}
		ensureModelEditable();
		Boolean oldValue = this.fPrimary ? Boolean.TRUE : Boolean.FALSE;
		this.fPrimary = newValue;
		firePropertyChanged(P_PRIMARY, oldValue, newValue ? Boolean.TRUE : Boolean.FALSE);
	}

	@Override
	public void setExclusive(boolean newValue) throws CoreException {
		if (this.fExclusive == newValue) {
			return;
		}
		ensureModelEditable();
		Boolean oldValue = this.fExclusive ? Boolean.TRUE : Boolean.FALSE;
		this.fExclusive = newValue;
		firePropertyChanged(P_EXCLUSIVE, oldValue, newValue ? Boolean.TRUE : Boolean.FALSE);
	}

	@Override
	public void setColocationAffinity(String newValue) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fColocationAffinity;
		this.fColocationAffinity = newValue;
		firePropertyChanged(P_COLLOCATION_AFFINITY, oldValue, newValue);
	}

	@Override
	public void setApplication(String newValue) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fApplication;
		this.fApplication = newValue;
		firePropertyChanged(P_APPLICATION, oldValue, newValue);
	}

	@Override
	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fProviderName;
		this.fProviderName = providerName;
		firePropertyChanged(P_PROVIDER, oldValue, providerName);
	}

	@Override
	public void setPlugin(String plugin) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fPlugin;
		this.fPlugin = plugin;
		firePropertyChanged(P_PLUGIN, oldValue, plugin);
	}

	@Override
	public void setURL(IFeatureURL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fUrl;
		if (this.fUrl != null) {
			((FeatureURL) this.fUrl).setInTheModel(false);
		}
		this.fUrl = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	@Override
	public void setInstallHandler(IFeatureInstallHandler handler) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fHandler;
		if (this.fHandler != null) {
			((FeatureInstallHandler) this.fHandler).setInTheModel(false);
		}
		this.fHandler = handler;
		firePropertyChanged(P_INSTALL_HANDLER, oldValue, handler);
	}

	@Override
	public void setFeatureInfo(IFeatureInfo info, int index) throws CoreException {
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
	@Override
	public void setImageName(String imageName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fImageName;
		this.fImageName = imageName;
		firePropertyChanged(P_IMAGE, oldValue, imageName);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
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
			setPrimary(newValue != null ? ((Boolean) newValue).booleanValue() : false);
		} else if (name.equals(P_EXCLUSIVE)) {
			setExclusive(newValue != null ? ((Boolean) newValue).booleanValue() : false);
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
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

	@Override
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

	@Override
	public boolean isValid() {
		return fValid;
	}

	private boolean hasRequiredAttributes() {
		// Verify that all the required attributes are
		// defined.
		if (id == null) {
			return false;
		}
		if (version == null) {
			return false;
		}

		for (int i = 0; i < fChildren.size(); i++) {
			IFeatureChild child = fChildren.elementAt(i);
			if (child.getId() == null || child.getVersion() == null) {
				return false;
			}
		}
		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin plugin = fPlugins.elementAt(i);
			if (plugin.getId() == null || plugin.getVersion() == null) {
				return false;
			}

		}
		for (int i = 0; i < fData.size(); i++) {
			IFeatureData entry = fData.elementAt(i);
			if (entry.getId() == null) {
				return false;
			}
		}
		for (int i = 0; i < fImports.size(); i++) {
			IFeatureImport iimport = fImports.elementAt(i);
			if (iimport.getId() == null) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		if (fCopyright != null) {
			writer.println("<!--" + fCopyright + "-->"); //$NON-NLS-1$ //$NON-NLS-2$
		}
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
		if (fImageName != null) {
			writeIfDefined(indenta, writer, "image", getWritableString(fImageName)); //$NON-NLS-1$
		}
		if (isPrimary()) {
			writer.println();
			writer.print(indenta + "primary=\"true\""); //$NON-NLS-1$
		}
		if (getLicenseFeatureID().length() > 0) {
			writer.println();
			writer.print(indenta + "license-feature=\"" + getLicenseFeatureID() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getLicenseFeatureVersion().length() > 0) {
			writer.println();
			writer.print(indenta + "license-feature-version=\"" + getLicenseFeatureVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (isExclusive()) {
			writer.println();
			writer.print(indenta + "exclusive=\"true\""); //$NON-NLS-1$
		}
		writeIfDefined(indenta, writer, "colocation-affinity", fColocationAffinity); //$NON-NLS-1$
		writeIfDefined(indenta, writer, "application", fApplication); //$NON-NLS-1$

		writer.println(">"); //$NON-NLS-1$
		if (fHandler != null) {
			fHandler.write(indent2, writer);
		}

		for (int i = 0; i < 3; i++) {
			IFeatureInfo info = fInfos[i];
			if (info != null && !info.isEmpty()) {
				info.write(indent2, writer);
			}
		}

		if (fUrl != null) {
			fUrl.write(indent2, writer);
		}
		for (int i = 0; i < fChildren.size(); i++) {
			IFeatureChild child = fChildren.elementAt(i);
			writer.println();
			child.write(indent2, writer);
		}
		if (!fImports.isEmpty()) {
			writer.println();
			writer.println(indent2 + "<requires>"); //$NON-NLS-1$
			for (int i = 0; i < fImports.size(); i++) {
				IFeatureImport iimport = fImports.get(i);
				iimport.write(indenta, writer);
			}
			writer.println(indent2 + "</requires>"); //$NON-NLS-1$
		}
		for (int i = 0; i < fPlugins.size(); i++) {
			IFeaturePlugin plugin = fPlugins.elementAt(i);
			writer.println();
			plugin.write(indent2, writer);
		}
		for (int i = 0; i < fData.size(); i++) {
			IFeatureData entry = fData.elementAt(i);
			writer.println();
			entry.write(indent2, writer);
		}
		writer.println();
		writer.println(indent + "</feature>"); //$NON-NLS-1$
	}

	private void writeIfDefined(String indent, PrintWriter writer, String attName, String attValue) {
		if (attValue == null || attValue.trim().length() == 0) {
			return;
		}
		writer.println();
		writer.print(indent + attName + "=\"" + attValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Gets the imageName.
	 *
	 * @return Returns a String
	 */
	@Override
	public String getImageName() {
		return fImageName;
	}

	public String getCopyright() {
		return fCopyright;
	}

	public void setCopyright(String copyright) {
		fCopyright = copyright;
	}

	@Override
	public void swap(IFeatureChild feature1, IFeatureChild feature2) {
		int index1 = fChildren.indexOf(feature1);
		int index2 = fChildren.indexOf(feature2);
		if (index1 == -1 || index2 == -1) {
			return;
		}

		fChildren.set(index2, feature1);
		fChildren.set(index1, feature2);

		fireStructureChanged(feature1, IModelChangedEvent.CHANGE);
	}

	@Override
	public String toString() {
		return getId() + " (" + getVersion() + ")"; //$NON-NLS-1$//$NON-NLS-2$
	}

}
