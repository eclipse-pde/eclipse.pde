/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 265931
 *     Rapicorp Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IAboutInfo;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.ICSSInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationProperty;
import org.eclipse.pde.internal.core.iproduct.IIntroInfo;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.ILicenseInfo;
import org.eclipse.pde.internal.core.iproduct.IPluginConfiguration;
import org.eclipse.pde.internal.core.iproduct.IPreferencesInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductObject;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.iproduct.IRepositoryInfo;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Product extends ProductObject implements IProduct {

	private static final long serialVersionUID = 1L;
	private String fId;
	private String fProductId;
	private String fName;
	private String fApplication;
	private String fVersion;
	private IAboutInfo fAboutInfo;

	private final TreeMap<String, IProductObject> fPlugins = new TreeMap<>();
	private final TreeMap<String, IProductObject> fPluginConfigurations = new TreeMap<>();
	private final TreeMap<String, IProductObject> fConfigurationProperties = new TreeMap<>();
	private final List<IProductFeature> fFeatures = new ArrayList<>();
	private IConfigurationFileInfo fConfigIniInfo;
	private IJREInfo fJVMInfo;
	private boolean fUseFeatures;
	private boolean fIncludeLaunchers;
	private IWindowImages fWindowImages;
	private ISplashInfo fSplashInfo;
	private ILauncherInfo fLauncherInfo;
	private IArgumentsInfo fLauncherArgs;
	private IIntroInfo fIntroInfo;
	private ILicenseInfo fLicenseInfo;
	private final List<IProductObject> fRepositories = new ArrayList<>();
	private IPreferencesInfo fPreferencesInfo;
	private ICSSInfo fCSSInfo;

	public Product(IProductModel model) {
		super(model);
		fIncludeLaunchers = true;
	}

	@Override
	public String getId() {
		return fId;
	}

	@Override
	public String getProductId() {
		return fProductId;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getVersion() {
		return fVersion;
	}

	@Override
	public String getApplication() {
		return fApplication;
	}

	@Override
	public String getDefiningPluginId() {
		if (fProductId == null) {
			return null;
		}
		int dot = fProductId.lastIndexOf('.');
		return (dot != -1) ? fProductId.substring(0, dot) : null;
	}

	@Override
	public void setId(String id) {
		String old = fId;
		fId = id;
		if (isEditable()) {
			firePropertyChanged(P_UID, old, fId);
		}
	}

	@Override
	public void setProductId(String id) {
		String old = fProductId;
		fProductId = id;
		if (isEditable()) {
			firePropertyChanged(P_ID, old, fProductId);
		}
	}

	@Override
	public void setVersion(String version) {
		String old = fVersion;
		fVersion = version;
		if (isEditable()) {
			firePropertyChanged(P_VERSION, old, fVersion);
		}
	}

	@Override
	public void setName(String name) {
		String old = fName;
		fName = name;
		if (isEditable()) {
			firePropertyChanged(P_NAME, old, fName);
		}
	}

	@Override
	public void setAboutInfo(IAboutInfo info) {
		fAboutInfo = info;
	}

	@Override
	public void setApplication(String application) {
		String old = fApplication;
		fApplication = application;
		if (isEditable()) {
			firePropertyChanged(P_APPLICATION, old, fApplication);
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<product"); //$NON-NLS-1$
		if (fName != null && fName.length() > 0) {
			writer.print(" " + P_NAME + "=\"" + getWritableString(fName) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (fId != null && fId.length() > 0) {
			writer.print(" " + P_UID + "=\"" + fId + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (fProductId != null && fProductId.length() > 0) {
			writer.print(" " + P_ID + "=\"" + fProductId + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (fApplication != null && fApplication.length() > 0) {
			writer.print(" " + P_APPLICATION + "=\"" + fApplication + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (fVersion != null && fVersion.length() > 0) {
			writer.print(" " + P_VERSION + "=\"" + fVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		writer.print(" " + P_USEFEATURES + "=\"" + Boolean.toString(fUseFeatures) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.print(" " + P_INCLUDE_LAUNCHERS + "=\"" + Boolean.toString(fIncludeLaunchers) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println(">"); //$NON-NLS-1$

		if (fAboutInfo != null) {
			writer.println();
			fAboutInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fConfigIniInfo != null) {
			writer.println();
			fConfigIniInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fLauncherArgs != null) {
			writer.println();
			fLauncherArgs.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fWindowImages != null) {
			writer.println();
			fWindowImages.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fSplashInfo != null) {
			writer.println();
			fSplashInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fLauncherInfo != null) {
			writer.println();
			fLauncherInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fIntroInfo != null) {
			writer.println();
			fIntroInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fJVMInfo != null) {
			writer.println();
			fJVMInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fLicenseInfo != null) {
			writer.println();
			fLicenseInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		writer.println();
		writer.println(indent + "   <plugins>"); //$NON-NLS-1$
		Iterator<IProductObject> iter = fPlugins.values().iterator();
		while (iter.hasNext()) {
			IProductPlugin plugin = (IProductPlugin) iter.next();
			plugin.write(indent + "      ", writer); //$NON-NLS-1$
		}
		writer.println(indent + "   </plugins>"); //$NON-NLS-1$

		if (!fFeatures.isEmpty()) {
			writer.println();
			writer.println(indent + "   <features>"); //$NON-NLS-1$
			Iterator<IProductFeature> fIter = fFeatures.iterator();
			while (fIter.hasNext()) {
				IProductFeature feature = fIter.next();
				feature.write(indent + "      ", writer); //$NON-NLS-1$
			}
			writer.println(indent + "   </features>"); //$NON-NLS-1$
		}

		writer.println();

		if (!fConfigurationProperties.isEmpty() || !fPluginConfigurations.isEmpty()) {
			writer.println(indent + "   <configurations>"); //$NON-NLS-1$
			iter = fPluginConfigurations.values().iterator();
			while (iter.hasNext()) {
				IPluginConfiguration configuration = (IPluginConfiguration) iter.next();
				configuration.write(indent + "      ", writer); //$NON-NLS-1$
			}
			iter = fConfigurationProperties.values().iterator();
			while (iter.hasNext()) {
				IConfigurationProperty property = (IConfigurationProperty) iter.next();
				property.write(indent + "      ", writer); //$NON-NLS-1$
			}
			writer.println(indent + "   </configurations>"); //$NON-NLS-1$
		}

		if (!fRepositories.isEmpty()) {
			writer.println();
			writer.println(indent + "   <repositories>"); //$NON-NLS-1$
			Iterator<IProductObject> iterator = fRepositories.iterator();
			while (iterator.hasNext()) {
				IRepositoryInfo repo = (IRepositoryInfo) iterator.next();
				repo.write(indent + "      ", writer); //$NON-NLS-1$
			}
			writer.println(indent + "   </repositories>"); //$NON-NLS-1$
		}

		if (fPreferencesInfo != null) {
			writer.println();
			fPreferencesInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		if (fCSSInfo != null) {
			writer.println();
			fCSSInfo.write(indent + "   ", writer); //$NON-NLS-1$
		}

		writer.println();
		writer.println("</product>"); //$NON-NLS-1$
	}

	@Override
	public IAboutInfo getAboutInfo() {
		return fAboutInfo;
	}

	@Override
	public void reset() {
		fApplication = null;
		fId = null;
		fProductId = null;
		fName = null;
		fUseFeatures = false;
		fIncludeLaunchers = true;
		fAboutInfo = null;
		fPlugins.clear();
		fPluginConfigurations.clear();
		fConfigurationProperties.clear();
		fFeatures.clear();
		fConfigIniInfo = null;
		fWindowImages = null;
		fSplashInfo = null;
		fLauncherInfo = null;
		fLauncherArgs = null;
		fIntroInfo = null;
		fJVMInfo = null;
		fLicenseInfo = null;
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("product")) { //$NON-NLS-1$
			Element element = (Element) node;
			fApplication = element.getAttribute(P_APPLICATION);
			fProductId = element.getAttribute(P_ID);
			fId = element.getAttribute(P_UID);
			fName = element.getAttribute(P_NAME);
			fVersion = element.getAttribute(P_VERSION);
			fUseFeatures = "true".equals(element.getAttribute(P_USEFEATURES)); //$NON-NLS-1$
			String launchers = element.getAttribute(P_INCLUDE_LAUNCHERS);
			fIncludeLaunchers = ("true".equals(launchers) || launchers.length() == 0); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			IProductModelFactory factory = getModel().getFactory();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("aboutInfo")) { //$NON-NLS-1$
						fAboutInfo = factory.createAboutInfo();
						fAboutInfo.parse(child);
					} else if (name.equals("plugins")) { //$NON-NLS-1$
						parsePlugins(child.getChildNodes());
					} else if (name.equals("features")) { //$NON-NLS-1$
						parseFeatures(child.getChildNodes());
					} else if (name.equals("configurations")) { //$NON-NLS-1$
						parseConfigations(child.getChildNodes());
					} else if (name.equals("configIni")) { //$NON-NLS-1$
						fConfigIniInfo = factory.createConfigFileInfo();
						fConfigIniInfo.parse(child);
					} else if (name.equals("windowImages")) { //$NON-NLS-1$
						fWindowImages = factory.createWindowImages();
						fWindowImages.parse(child);
					} else if (name.equals("splash")) { //$NON-NLS-1$
						fSplashInfo = factory.createSplashInfo();
						fSplashInfo.parse(child);
					} else if (name.equals("launcher")) { //$NON-NLS-1$
						fLauncherInfo = factory.createLauncherInfo();
						fLauncherInfo.parse(child);
					} else if (name.equals("launcherArgs")) { //$NON-NLS-1$
						fLauncherArgs = factory.createLauncherArguments();
						fLauncherArgs.parse(child);
					} else if (name.equals("intro")) { //$NON-NLS-1$
						fIntroInfo = factory.createIntroInfo();
						fIntroInfo.parse(child);
					} else if (name.equals("vm")) { //$NON-NLS-1$
						fJVMInfo = factory.createJVMInfo();
						fJVMInfo.parse(child);
					} else if (name.equals("license")) { //$NON-NLS-1$
						fLicenseInfo = factory.createLicenseInfo();
						fLicenseInfo.parse(child);
					} else if (name.equals("repositories")) { //$NON-NLS-1$
						parseRepositories(child.getChildNodes());
					} else if (name.equals("preferencesInfo")) { //$NON-NLS-1$
						fPreferencesInfo = factory.createPreferencesInfo();
						fPreferencesInfo.parse(child);
					} else if (name.equals("cssInfo")) { //$NON-NLS-1$
						fCSSInfo = factory.createCSSInfo();
						fCSSInfo.parse(child);
					}
				}
			}
		}
	}

	private void parsePlugins(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("plugin")) { //$NON-NLS-1$
					IProductPlugin plugin = getModel().getFactory().createPlugin();
					plugin.parse(child);
					fPlugins.put(plugin.getId(), plugin);
				}
			}
		}
	}

	private void parseConfigations(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("plugin")) { //$NON-NLS-1$
					IPluginConfiguration configuration = getModel().getFactory().createPluginConfiguration();
					configuration.parse(child);
					fPluginConfigurations.put(configuration.getId(), configuration);
				}
				if (child.getNodeName().equals("property")) { //$NON-NLS-1$
					IConfigurationProperty property = getModel().getFactory().createConfigurationProperty();
					property.parse(child);
					fConfigurationProperties.put(property.getName(), property);
				}
			}
		}
	}

	private void parseFeatures(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("feature")) { //$NON-NLS-1$
					IProductFeature feature = getModel().getFactory().createFeature();
					feature.parse(child);
					fFeatures.add(feature);
				}
			}
		}
	}

	private void parseRepositories(NodeList children) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("repository")) { //$NON-NLS-1$
					IRepositoryInfo repo = getModel().getFactory().createRepositoryInfo();
					repo.parse(child);
					fRepositories.add(repo);
				}
			}
		}
	}

	@Override
	public void addPlugins(IProductPlugin[] plugins) {
		boolean modified = false;
		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i] == null) {
				continue;
			}
			String id = plugins[i].getId();
			if (id == null || fPlugins.containsKey(id)) {
				plugins[i] = null;
				continue;
			}

			plugins[i].setModel(getModel());
			fPlugins.put(id, plugins[i]);
			modified = true;
		}
		if (modified && isEditable()) {
			fireStructureChanged(plugins, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public void addPluginConfigurations(IPluginConfiguration[] configuration) {
		boolean modified = false;
		for (int i = 0; i < configuration.length; i++) {
			if (configuration[i] == null) {
				continue;
			}
			String id = configuration[i].getId();
			if (id == null || fPluginConfigurations.containsKey(id)) {
				configuration[i] = null;
				continue;
			}

			configuration[i].setModel(getModel());
			fPluginConfigurations.put(id, configuration[i]);
			modified = true;
		}
		if (modified && isEditable()) {
			fireStructureChanged(configuration, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public void addConfigurationProperties(IConfigurationProperty[] properties) {
		boolean modified = false;
		for (IConfigurationProperty property : properties) {
			if (property == null) {
				continue;
			}
			String name = property.getName();
			if (name == null || fConfigurationProperties.containsKey(name)) {
				continue;
			}

			property.setModel(getModel());
			fConfigurationProperties.put(name, property);
			modified = true;
		}
		if (modified && isEditable()) {
			fireStructureChanged(properties, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public void removePlugins(IProductPlugin[] plugins) {
		boolean modified = false;
		LinkedList<Object> removedConfigurations = new LinkedList<>();
		for (IProductPlugin plugin : plugins) {
			String id = plugin.getId();
			if (fPlugins.remove(id) != null) {
				modified = true;
				Object configuration = fPluginConfigurations.remove(id);
				if (configuration != null) {
					removedConfigurations.add(configuration);
				}
			}
		}
		if (isEditable()) {
			if (modified) {
				fireStructureChanged(plugins, IModelChangedEvent.REMOVE);
			}
			if (!removedConfigurations.isEmpty()) {
				fireStructureChanged(removedConfigurations.toArray(new IProductObject[removedConfigurations.size()]), IModelChangedEvent.REMOVE);
			}
		}
	}

	@Override
	public void removePluginConfigurations(IPluginConfiguration[] configurations) {
		boolean modified = false;
		for (IPluginConfiguration configuration : configurations) {
			if (fPluginConfigurations.remove(configuration.getId()) != null) {
				modified = true;
			}
		}
		if (isEditable() && modified) {
			fireStructureChanged(configurations, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public void removeConfigurationProperties(IConfigurationProperty[] properties) {
		boolean modified = false;
		for (IConfigurationProperty property : properties) {
			if (fConfigurationProperties.remove(property.getName()) != null) {
				modified = true;
			}
		}
		if (isEditable() && modified) {
			fireStructureChanged(properties, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public IProductPlugin[] getPlugins() {
		return fPlugins.values().toArray(new IProductPlugin[fPlugins.size()]);
	}

	@Override
	public IPluginConfiguration[] getPluginConfigurations() {
		return fPluginConfigurations.values().toArray(new IPluginConfiguration[fPluginConfigurations.size()]);
	}

	@Override
	public IConfigurationProperty[] getConfigurationProperties() {
		return fConfigurationProperties.values().toArray(new IConfigurationProperty[fConfigurationProperties.size()]);
	}

	@Override
	public IRepositoryInfo[] getRepositories() {
		return fRepositories.toArray(new IRepositoryInfo[fRepositories.size()]);
	}

	@Override
	public void addRepositories(IRepositoryInfo[] repos) {
		boolean modified = false;
		for (IRepositoryInfo repo : repos) {
			modified = modified || fRepositories.add(repo);
		}
		if (modified && isEditable()) {
			fireStructureChanged(repos, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public void removeRepositories(IRepositoryInfo[] repos) {
		boolean modified = false;
		for (IRepositoryInfo repo : repos) {
			modified = fRepositories.remove(repo) || modified;
		}
		if (modified && isEditable()) {
			fireStructureChanged(repos, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public IPreferencesInfo getPreferencesInfo() {
		return fPreferencesInfo;
	}

	@Override
	public void setPreferencesInfo(IPreferencesInfo info) {
		fPreferencesInfo = info;
	}

	@Override
	public ICSSInfo getCSSInfo() {
		return fCSSInfo;
	}

	@Override
	public void setCSSInfo(ICSSInfo info) {
		fCSSInfo = info;
	}

	@Override
	public IConfigurationFileInfo getConfigurationFileInfo() {
		return fConfigIniInfo;
	}

	@Override
	public void setConfigurationFileInfo(IConfigurationFileInfo info) {
		fConfigIniInfo = info;
	}

	@Override
	public boolean useFeatures() {
		return fUseFeatures;
	}

	@Override
	public void setUseFeatures(boolean use) {
		boolean old = fUseFeatures;
		fUseFeatures = use;
		if (isEditable()) {
			firePropertyChanged(P_USEFEATURES, Boolean.toString(old), Boolean.toString(fUseFeatures));
		}
	}

	@Override
	public boolean containsPlugin(String id) {
		return fPlugins.containsKey(id);
	}

	@Override
	public boolean containsFeature(String id) {
		IProductFeature[] features = getFeatures();
		for (IProductFeature feature : features) {
			if (feature.getId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IWindowImages getWindowImages() {
		return fWindowImages;
	}

	@Override
	public void setWindowImages(IWindowImages images) {
		fWindowImages = images;
	}

	@Override
	public ISplashInfo getSplashInfo() {
		return fSplashInfo;
	}

	@Override
	public void setSplashInfo(ISplashInfo info) {
		fSplashInfo = info;
	}

	@Override
	public ILauncherInfo getLauncherInfo() {
		return fLauncherInfo;
	}

	@Override
	public void setLauncherInfo(ILauncherInfo info) {
		fLauncherInfo = info;
	}

	@Override
	public void addFeatures(IProductFeature[] features) {
		boolean modified = false;
		Set<String> knownIds = new HashSet<>(fFeatures.size());
		fFeatures.forEach(feat -> knownIds.add(feat.getId()));

		for (int i = 0; i < features.length; i++) {
			if (features[i] == null) {
				continue;
			}
			String id = features[i].getId();
			if (knownIds.contains(id)) {
				features[i] = null;
				continue;
			}

			features[i].setModel(getModel());
			fFeatures.add(features[i]);
			knownIds.add(id);
			modified = true;
		}

		if (modified && isEditable()) {
			fireStructureChanged(features, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public void removeFeatures(IProductFeature[] features) {
		boolean modified = false;
		for (IProductFeature feature : features) {
			if (feature.getId() != null) {
				fFeatures.remove(feature);
				modified = true;
			}
		}
		if (modified && isEditable()) {
			fireStructureChanged(features, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public IProductFeature[] getFeatures() {
		return fFeatures.toArray(new IProductFeature[fFeatures.size()]);
	}

	@Override
	public IArgumentsInfo getLauncherArguments() {
		return fLauncherArgs;
	}

	@Override
	public void setLauncherArguments(IArgumentsInfo info) {
		fLauncherArgs = info;
	}

	@Override
	public IIntroInfo getIntroInfo() {
		return fIntroInfo;
	}

	@Override
	public void setIntroInfo(IIntroInfo introInfo) {
		fIntroInfo = introInfo;
	}

	@Override
	public IJREInfo getJREInfo() {
		return fJVMInfo;
	}

	@Override
	public void setJREInfo(IJREInfo info) {
		fJVMInfo = info;
	}

	@Override
	public ILicenseInfo getLicenseInfo() {
		return fLicenseInfo;
	}

	@Override
	public void setLicenseInfo(ILicenseInfo info) {
		fLicenseInfo = info;
	}

	@Override
	public void swap(IProductFeature feature1, IProductFeature feature2) {
		int index1 = fFeatures.indexOf(feature1);
		int index2 = fFeatures.indexOf(feature2);
		if (index1 == -1 || index2 == -1) {
			return;
		}

		fFeatures.set(index2, feature1);
		fFeatures.set(index1, feature2);

		fireStructureChanged(feature1, IModelChangedEvent.CHANGE);
	}

	@Override
	public IPluginConfiguration findPluginConfiguration(String id) {
		return (IPluginConfiguration) fPluginConfigurations.get(id);
	}

	@Override
	public boolean includeLaunchers() {
		return fIncludeLaunchers;
	}

	@Override
	public void setIncludeLaunchers(boolean include) {
		boolean old = fIncludeLaunchers;
		fIncludeLaunchers = include;
		if (isEditable()) {
			firePropertyChanged(P_INCLUDE_LAUNCHERS, Boolean.toString(old), Boolean.toString(fIncludeLaunchers));
		}
	}
}
