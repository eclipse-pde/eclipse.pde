package org.eclipse.pde.internal.core.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public class Feature extends VersionableObject implements IFeature {
	final static String INDENT = "   ";
	private String providerName;
	private IFeatureURL url;
	private IFeatureInfo[] infos = new IFeatureInfo[3];
	private Vector data = new Vector();
	private Vector children = new Vector();
	private Vector plugins = new Vector();
	private Vector imports = new Vector();
	private String os;
	private String ws;
	private String nl;
	private String arch;
	private String imageName;
	private IFeatureInstallHandler handler;
	private boolean primary;
	private boolean exclusive;
	private String colocationAffinity;
	private String application;
	private String plugin;
	
	public void addPlugins(IFeaturePlugin[] newPlugins) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newPlugins.length; i++) {
			plugins.add(newPlugins[i]);
			((FeaturePlugin) newPlugins[i]).setInTheModel(true);
		}
		fireStructureChanged(newPlugins, IModelChangedEvent.INSERT);
	}

	public void addData(IFeatureData[] newData) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newData.length; i++) {
			data.add(newData[i]);
			((FeatureData) newData[i]).setInTheModel(true);
		}
		fireStructureChanged(newData, IModelChangedEvent.INSERT);
	}

	public void addIncludedFeatures(IFeatureChild[] features)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < features.length; i++) {
			children.add(features[i]);
			((FeatureChild) features[i]).setInTheModel(true);
		}
		fireStructureChanged(features, IModelChangedEvent.INSERT);
	}

	public void addImports(IFeatureImport [] iimports) throws CoreException {
		ensureModelEditable();
		for (int i=0; i<iimports.length; i++) {
			imports.add(iimports[i]);
			((FeatureImport)iimports[i]).setInTheModel(true);
		}
		fireStructureChanged(iimports, IModelChangedEvent.INSERT);
	}

	public IFeaturePlugin[] getPlugins() {
		IFeaturePlugin[] result = new IFeaturePlugin[plugins.size()];
		plugins.copyInto(result);
		return result;
	}

	public IFeatureData[] getData() {
		IFeatureData[] result = new IFeatureData[data.size()];
		data.copyInto(result);
		return result;
	}

	public IFeatureChild[] getIncludedFeatures() {
		IFeatureChild[] result = new IFeatureChild[children.size()];
		children.copyInto(result);
		return result;
	}
	public IFeatureImport[] getImports() {
		IFeatureImport[] result = new IFeatureImport[imports.size()];
		imports.copyInto(result);
		return result;
	}
	public String getProviderName() {
		return providerName;
	}
	
	public String getPlugin() {
		return plugin;
	}

	public IPluginModelBase getReferencedModel(IFeaturePlugin reference) {
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
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
	public IFeatureInstallHandler getInstallHandler() {
		return handler;
	}

	public IFeatureInfo getFeatureInfo(int index) {
		return infos[index];
	}

	public boolean isPrimary() {
		return primary;
	}
	
	public boolean isExclusive() {
		return exclusive;
	}	

	protected void parse(Node node) {
		super.parse(node);
		providerName = getNodeAttribute(node, "provider-name");
		plugin = getNodeAttribute(node, "plugin");
		os = getNodeAttribute(node, "os");
		ws = getNodeAttribute(node, "ws");
		nl = getNodeAttribute(node, "nl");
		arch = getNodeAttribute(node, "arch");
		imageName = getNodeAttribute(node, "image");
		colocationAffinity = getNodeAttribute(node, "colocation-affinity");
		application = getNodeAttribute(node, "application");
		primary = getBooleanAttribute(node, "primary");
		exclusive = getBooleanAttribute(node, "exclusive");
		NodeList children = node.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase();
				if (tag.equals("description")) {
					IFeatureInfo info =
						getModel().getFactory().createInfo(IFeature.INFO_DESCRIPTION);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child);
					infos[IFeature.INFO_DESCRIPTION] = info;
				} else if (tag.equals("license")) {
					IFeatureInfo info = getModel().getFactory().createInfo(IFeature.INFO_LICENSE);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child);
					infos[IFeature.INFO_LICENSE] = info;
				} else if (tag.equals("copyright")) {
					IFeatureInfo info = getModel().getFactory().createInfo(IFeature.INFO_COPYRIGHT);
					((FeatureInfo) info).setInTheModel(true);
					((FeatureInfo) info).parse(child);
					infos[IFeature.INFO_COPYRIGHT] = info;
				} else if (tag.equals("url")) {
					if (url == null) {
						url = getModel().getFactory().createURL();
						((FeatureURL) url).setInTheModel(true);
						((FeatureURL) url).parse(child);
					}
				} else if (tag.equals("requires")) {
					parseRequires(child);
				} else if (tag.equals("install-handler")) {
					IFeatureInstallHandler handler = getModel().getFactory().createInstallHandler();
					((FeatureInstallHandler) handler).parse(child);
					((FeatureInstallHandler) handler).setInTheModel(true);
					this.handler = handler;
				} else if (tag.equals("plugin")) {
					IFeaturePlugin plugin = getModel().getFactory().createPlugin();
					((FeaturePlugin) plugin).parse(child);
					((FeaturePlugin) plugin).setInTheModel(true);
					plugins.add(plugin);
				} else if (tag.equals("data")) {
					IFeatureData newData = getModel().getFactory().createData();
					((FeatureData) newData).parse(child);
					((FeatureData) newData).setInTheModel(true);
					data.add(newData);
				} else if (tag.equals("includes")) {
					IFeatureChild newChild = getModel().getFactory().createChild();
					((FeatureChild) newChild).parse(child);
					((FeatureChild) newChild).setInTheModel(true);
					this.children.add(newChild);
				}
			}
		}
	}
	private void parseRequires(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equalsIgnoreCase("import")) {
					IFeatureImport iimport = getModel().getFactory().createImport();
					((FeatureImport) iimport).parse(child);
					((FeatureImport) iimport).setInTheModel(true);
					imports.add(iimport);
				}
			}
		}
	}

	public void computeImports() throws CoreException {
		IFeatureImport[] removed = null;
		if (imports.size() > 0)
			removed =
				(IFeatureImport[]) imports.toArray(new IFeatureImport[imports.size()]);
		imports.clear();
		if (removed != null)
			fireStructureChanged(removed, IModelChangedEvent.REMOVE);
		// Create full import list
		for (int i = 0; i < plugins.size(); i++) {
			IFeaturePlugin fp = (IFeaturePlugin) plugins.get(i);
			IPluginBase plugin =
				PDECore.getDefault().findPlugin(fp.getId(), fp.getVersion(), 0);
			if (plugin != null) {
				addPluginImports(plugin);
			}
		}
		// Find plug-ins that satisfy requirements within this feature.
		// Whatever remains will be feature external requirements.
		Vector inputImports = (Vector) imports.clone();
		for (int i = 0; i < inputImports.size(); i++) {
			IFeatureImport iimport = (IFeatureImport) inputImports.get(i);
			IFeaturePlugin local =
				findFeaturePlugin(iimport.getId(), iimport.getVersion(), iimport.getMatch());
			if (local != null)
				imports.remove(iimport);
		}
		if (imports.size() > 0) {
			IFeatureImport[] added =
				(IFeatureImport[]) imports.toArray(new IFeatureImport[imports.size()]);
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
			imports.add(iimport);
			IPlugin p =
				PDECore.getDefault().findPlugin(
					pluginImport.getId(),
					pluginImport.getVersion(),
					pluginImport.getMatch());
			((FeatureImport) iimport).setPlugin(p);
			/*
			if (p != null)
				addPluginImports(p);
			*/
		}
	}

	private IFeatureImport findImport(String id, String version, int match) {
		for (int i = 0; i < imports.size(); i++) {
			IFeatureImport iimport = (IFeatureImport) imports.get(i);
			if (iimport.getId().equals(id)) {
				if (version == null)
					return iimport;
				if (version.equals(iimport.getVersion()) && match == iimport.getMatch())
					return iimport;
			}
		}
		return null;
	}

	private IFeaturePlugin findFeaturePlugin(
		String id,
		String version,
		int match) {

		for (int i = 0; i < plugins.size(); i++) {
			IFeaturePlugin fp = (IFeaturePlugin) plugins.get(i);
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
			plugins.remove(removed[i]);
			((FeaturePlugin) removed[i]).setInTheModel(false);
		}
		fireStructureChanged(removed, IModelChangedEvent.REMOVE);
	}

	public void removeData(IFeatureData[] removed) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < removed.length; i++) {
			data.remove(removed[i]);
			((FeatureData) removed[i]).setInTheModel(false);
		}
		fireStructureChanged(removed, IModelChangedEvent.REMOVE);
	}

	public void removeIncludedFeatures(IFeatureChild[] features)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < features.length; i++) {
			children.remove(features[i]);
			((FeatureChild) features[i]).setInTheModel(false);
		}
		fireStructureChanged(features, IModelChangedEvent.REMOVE);
	}
	public void removeImports(IFeatureImport [] iimports) throws CoreException {
		ensureModelEditable();
		for (int i=0; i<iimports.length; i++) {
			imports.remove(iimports[i]);
			((FeatureImport)iimports[i]).setInTheModel(false);
		}
		fireStructureChanged(iimports, IModelChangedEvent.REMOVE);
	}

	public String getOS() {
		return os;
	}

	public String getWS() {
		return ws;
	}

	public String getNL() {
		return nl;
	}

	public String getArch() {
		return arch;
	}
	
	public String getColocationAffinity() {
		return colocationAffinity;
	}
	
	public String getApplication() {
		return application;
	}
	
	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.os;
		this.os = os;
		firePropertyChanged(P_OS, oldValue, os);
	}
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.ws;
		this.ws = ws;
		firePropertyChanged(P_WS, oldValue, ws);
	}
	public void setNL(String nl) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.nl;
		this.nl = nl;
		firePropertyChanged(P_NL, oldValue, nl);
	}
	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.arch;
		this.arch = arch;
		firePropertyChanged(P_ARCH, oldValue, arch);
	}

	public void setPrimary(boolean newValue) throws CoreException {
		if (this.primary == newValue)
			return;
		ensureModelEditable();
		Boolean oldValue = this.primary ? Boolean.TRUE : Boolean.FALSE;
		this.primary = newValue;
		firePropertyChanged(
			P_PRIMARY,
			oldValue,
			newValue ? Boolean.TRUE : Boolean.FALSE);
	}
	
	public void setExclusive(boolean newValue) throws CoreException {
		if (this.exclusive == newValue)
			return;
		ensureModelEditable();
		Boolean oldValue = this.exclusive ? Boolean.TRUE : Boolean.FALSE;
		this.exclusive = newValue;
		firePropertyChanged(
			P_EXCLUSIVE,
			oldValue,
			newValue ? Boolean.TRUE : Boolean.FALSE);
	}	
		
	public void setColocationAffinity(String newValue) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.colocationAffinity;
		this.colocationAffinity = newValue;
		firePropertyChanged(P_COLLOCATION_AFFINITY, oldValue, newValue);
	}
	
	public void setApplication(String newValue) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.application;
		this.application = newValue;
		firePropertyChanged(P_APPLICATION, oldValue, newValue);
	}

	public void setProviderName(String providerName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.providerName;
		this.providerName = providerName;
		firePropertyChanged(P_PROVIDER, oldValue, providerName);
	}
	
	public void setPlugin(String plugin) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.plugin;
		this.plugin = plugin;
		firePropertyChanged(P_PLUGIN, oldValue, plugin);
	}
	public void setURL(IFeatureURL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		if (this.url != null) {
			((FeatureURL) this.url).setInTheModel(false);
		}
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}
	public void setInstallHandler(IFeatureInstallHandler handler)
		throws CoreException {
		ensureModelEditable();
		Object oldValue = this.handler;
		if (this.handler != null) {
			((FeatureInstallHandler) this.handler).setInTheModel(false);
		}
		this.handler = handler;
		firePropertyChanged(P_INSTALL_HANDLER, oldValue, handler);
	}

	public void setFeatureInfo(IFeatureInfo info, int index) throws CoreException {
		ensureModelEditable();
		Object oldValue = infos[index];
		if (oldValue != null) {
			((FeatureInfo) oldValue).setInTheModel(true);
		}
		infos[index] = info;
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
	 * @param imageName The imageName to set
	 */
	public void setImageName(String imageName) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.imageName;
		this.imageName = imageName;
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
			setColocationAffinity((String)newValue);
		} else if (name.equals(P_APPLICATION)) {
			setApplication((String)newValue);
		} else if (name.equals(P_PRIMARY)) {
			setPrimary(newValue != null ? ((Boolean) newValue).booleanValue() : false);
		} else if (name.equals(P_EXCLUSIVE)) {
			setExclusive(newValue != null ? ((Boolean) newValue).booleanValue() : false);
		} else if (name.equals(P_PROVIDER)) {
			setProviderName((String) newValue);
		} else if (name.equals(P_PLUGIN)) {
			setPlugin((String)newValue);
		} else if (name.equals(P_URL)) {
			setURL((IFeatureURL) newValue);
		} else if (name.equals(P_INSTALL_HANDLER)) {
			setInstallHandler((IFeatureInstallHandler) newValue);
		} else if (name.equals(P_DESCRIPTION)) {
			setFeatureInfo((IFeatureInfo)newValue, INFO_DESCRIPTION);
		} else if (name.equals(P_LICENSE)) {
			setFeatureInfo((IFeatureInfo)newValue, INFO_LICENSE);
		} else if (name.equals(P_COPYRIGHT)) {
			setFeatureInfo((IFeatureInfo)newValue, INFO_COPYRIGHT);
		} else if (name.equals(P_IMAGE)) {
			setImageName((String) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void reset() {
		super.reset();
		data.clear();
		plugins.clear();
		imports.clear();
		children.clear();
		url = null;
		providerName = null;
		plugin = null;
		os = null;
		ws = null;
		nl = null;
		arch = null;
		infos[0] = null;
		infos[1] = null;
		infos[2] = null;
		primary = false;
		exclusive = false;
		colocationAffinity = null;
		application = null;
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
		writeIfDefined(
			indenta,
			writer,
			"plugin",
			getPlugin());
		writeIfDefined(indenta, writer, "os", os);
		writeIfDefined(indenta, writer, "ws", ws);
		writeIfDefined(indenta, writer, "nl", nl);
		writeIfDefined(indenta, writer, "arch", arch);
		writeIfDefined(indenta, writer, "image", imageName);
		if (isPrimary()) {
			writer.println();
			writer.print(indenta + "primary=\"true\"");
		}
		if (isExclusive()) {
			writer.println();
			writer.print(indenta + "exclusive=\"true\"");
		}
		writeIfDefined(indenta, writer, "colocation-affinity", colocationAffinity);
		writeIfDefined(indenta, writer, "application", application);

		writer.println(">");
		if (handler != null) {
			writer.println();
			handler.write(indent2, writer);
		}

		for (int i = 0; i < 3; i++) {
			IFeatureInfo info = infos[i];
			if (info != null && !info.isEmpty())
				info.write(indent2, writer);
		}

		if (url != null) {
			writer.println();
			url.write(indent2, writer);
		}
		for (int i = 0; i < children.size(); i++) {
			IFeatureChild child = (IFeatureChild) children.elementAt(i);
			writer.println();
			child.write(indent2, writer);
		}
		if (imports.size() > 0) {
			writer.println();
			writer.println(indent2 + "<requires>");
			for (int i = 0; i < imports.size(); i++) {
				IFeatureImport iimport = (IFeatureImport) imports.get(i);
				iimport.write(indenta, writer);
			}
			writer.println(indent2 + "</requires>");
		}
		for (int i = 0; i < plugins.size(); i++) {
			IFeaturePlugin plugin = (IFeaturePlugin) plugins.elementAt(i);
			writer.println();
			plugin.write(indent2, writer);
		}
		for (int i = 0; i < data.size(); i++) {
			IFeatureData entry = (IFeatureData) data.elementAt(i);
			writer.println();
			entry.write(indent2, writer);
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
	/**
	 * Gets the imageName.
	 * @return Returns a String
	 */
	public String getImageName() {
		return imageName;
	}
}