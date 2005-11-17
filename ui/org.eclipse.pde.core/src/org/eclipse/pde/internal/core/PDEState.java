/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.bundle.BundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModelBase;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class PDEState extends MinimalState {
	
	class PluginInfo {
		String name;
		String providerName;
		String className;
		boolean hasExtensibleAPI;
		boolean legacy;
		String[] libraries;
		String project;
		String localization;
	}
	
	private URL[] fWorkspaceURLs;
	private URL[] fTargetURLs;
	private IProgressMonitor fMonitor;
	private Map fPluginInfos;
	private Map fExtensions;
	private TreeMap fTargetModels = new TreeMap();
	private ArrayList fWorkspaceModels = new ArrayList();
	private boolean fCombined;
	private long fTargetTimestamp;
	private boolean fResolve = true;
	private boolean fNewState;
	
	public PDEState(URL[] urls, boolean resolve, IProgressMonitor monitor) {
		this(new URL[0], urls, resolve, TargetPlatform.getTargetEnvironment(), monitor);
	}
	
	public PDEState(URL[] workspace, URL[] target, boolean resolve, Dictionary properties, IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		fResolve = resolve;
		fWorkspaceURLs = workspace;
		fTargetURLs = target;
		fMonitor = monitor;
		setTargetMode(target);
		if (fResolve) {
			readTargetState();
		} else {
			createNewTargetState();
			createExtensionDocument();
		}
		fState.setResolver(Platform.getPlatformAdmin().getResolver());
		fState.setPlatformProperties(properties);
		resolveState(false);
		
		if (fResolve)
			logResolutionErrors();
		
		createTargetModels();
		
		if (fResolve && workspace.length > 0 && !fNewState && !"true".equals(System.getProperty("pde.nocache"))) { //$NON-NLS-1$ //$NON-NLS-2$
			readWorkspaceState();
		}
		
		if (DEBUG)
			System.out.println("Time to create state: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void readTargetState() {
		fTargetTimestamp = computeTimestamp(fTargetURLs);
		File dir = new File(DIR, Long.toString(fTargetTimestamp) + ".target"); //$NON-NLS-1$
		if ((fState = readStateCache(dir)) == null
				|| (fPluginInfos = readPluginInfoCache(dir)) == null) {
			createNewTargetState();
			saveState(dir);
			savePluginInfo(dir);
			fNewState = true;
		} else {
			fId = fState.getBundles().length;
		}
		if ((fExtensions = readExtensionsCache(dir)) == null)
			saveExtensions(dir);
	}
	
	private void createNewTargetState() {
		fState = stateObjectFactory.createState();
		fPluginInfos = new HashMap();
		fMonitor.beginTask(PDECoreMessages.PDEState_readingPlugins, fTargetURLs.length);
		for (int i = 0; i < fTargetURLs.length; i++) {
			File file = new File(fTargetURLs[i].getFile());
			try {
				fMonitor.subTask(file.getName());
				addBundle(file, true, -1);
			} catch (PluginConversionException e) {
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR,
						PDECoreMessages.PDEState_invalidFormat + " " + file.getAbsolutePath(), //$NON-NLS-1$
						null)); 
			} finally {
				fMonitor.worked(1);
			}
		}		
	}
	
	private void createTargetModels() {
		BundleDescription[] bundleDescriptions = fState.getBundles();
		for (int i = 0; i < bundleDescriptions.length; i++) {
			boolean add = true;
			if (!bundleDescriptions[i].isResolved()) {
				ResolverError[] error = fState.getResolverErrors(bundleDescriptions[i]);
				for (int j = 0; j < error.length; j++) {
					if (error[j].getType() == ResolverError.SINGLETON_SELECTION) {
						add = false;
						break;
					}
				}
			}
			if (add) {
				BundleDescription desc = bundleDescriptions[i];
				fTargetModels.put(desc.getSymbolicName(), createExternalModel(desc));
				fExtensions.remove(Long.toString(desc.getBundleId()));
				fPluginInfos.remove(Long.toString(desc.getBundleId()));
			}
		}
	}
 	
	private void readWorkspaceState() {
		long workspace = computeTimestamp(fWorkspaceURLs);
		File dir = new File(DIR, Long.toString(workspace) + ".workspace"); //$NON-NLS-1$
		State localState = readStateCache(dir);
		Map localPluginInfos = readPluginInfoCache(dir);
		Map localExtensions = readExtensionsCache(dir);
		
		fCombined = localState != null && localPluginInfos != null && localExtensions != null;
		if (fCombined) {
			Iterator iter = localPluginInfos.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next().toString();
				fPluginInfos.put(key, localPluginInfos.get(key));
			}
			
			iter = localExtensions.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next().toString();
				fExtensions.put(key, localExtensions.get(key));
			}
			
			BundleDescription[] bundles = localState.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription desc = bundles[i];
				String id = desc.getSymbolicName();
				BundleDescription[] conflicts = fState.getBundles(id);
				
				for (int j = 0; j < conflicts.length; j++) {
					fState.removeBundle(conflicts[j]);
				}
				
				BundleDescription newbundle = stateObjectFactory.createBundleDescription(desc);
				IPluginModelBase model = createWorkspaceModel(newbundle);
				if (model != null && fState.addBundle(newbundle)) {
					fId = Math.max(fId, newbundle.getBundleId());
					fWorkspaceModels.add(model);
				}
				fExtensions.remove(Long.toString(newbundle.getBundleId()));
				fPluginInfos.remove(Long.toString(newbundle.getBundleId()));
			}
			fId = Math.max(fId, fState.getBundles().length);
			fState.resolve(true);
		}
	}
	
	public boolean isCombined() {
		return fCombined;
	}
	
	public BundleDescription addBundle(Dictionary manifest, File bundleLocation, boolean keepLibraries, long bundleId) {
		BundleDescription desc = super.addBundle(manifest, bundleLocation, keepLibraries, bundleId);
		if (desc != null && keepLibraries)
			createPluginInfo(desc, manifest);
		return desc;
	}

	private void createPluginInfo(BundleDescription desc, Dictionary manifest) {
		PluginInfo info = new PluginInfo();
		info.name = (String)manifest.get(Constants.BUNDLE_NAME);
		info.providerName = (String)manifest.get(Constants.BUNDLE_VENDOR);
		
		String className = (String)manifest.get("Plugin-Class"); //$NON-NLS-1$
		info.className	= className != null ? className : (String)manifest.get(Constants.BUNDLE_ACTIVATOR);	
		info.libraries = PDEStateHelper.getClasspath(manifest);
		info.hasExtensibleAPI = "true".equals(manifest.get(ICoreConstants.EXTENSIBLE_API)); //$NON-NLS-1$ 
		info.localization = (String)manifest.get(Constants.BUNDLE_LOCALIZATION);
		fPluginInfos.put(Long.toString(desc.getBundleId()), info);
	}
	
	private void createPluginInfo(Map map, Element element) {
		PluginInfo info = new PluginInfo();
		info.name = element.getAttribute("name"); //$NON-NLS-1$
		info.providerName = element.getAttribute("provider"); //$NON-NLS-1$
		info.className	= element.getAttribute("class"); //$NON-NLS-1$
		info.hasExtensibleAPI = "true".equals(element.getAttribute("hasExtensibleAPI")); //$NON-NLS-1$ //$NON-NLS-2$
		info.project = element.getAttribute("project"); //$NON-NLS-1$
		info.legacy = "true".equals(element.getAttribute("legacy")); //$NON-NLS-1$ //$NON-NLS-2$
		info.localization = element.getAttribute("localization"); //$NON-NLS-1$
		
		NodeList libs = element.getChildNodes(); 
		ArrayList list = new ArrayList(libs.getLength());
		for (int i = 0; i < libs.getLength(); i++) {
			if (libs.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element lib = (Element)libs.item(i);
				list.add(lib.getAttribute("name")); //$NON-NLS-1$
			}
		}
		info.libraries = (String[])list.toArray(new String[list.size()]);
		map.put(element.getAttribute("bundleID"), info); //$NON-NLS-1$
	}
	
	private Document createExtensionDocument(){
		fExtensions = new HashMap();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			doc = factory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			return null;
		}
		Element root = doc.createElement("extensions"); //$NON-NLS-1$

		BundleDescription[] bundles = fState.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription desc = bundles[i];
			Element element = doc.createElement("bundle"); //$NON-NLS-1$
			element.setAttribute("bundleID", Long.toString(desc.getBundleId())); //$NON-NLS-1$
			PDEStateHelper.parseExtensions(desc, element);
			if (element.hasChildNodes()) {
				root.appendChild(element);
				fExtensions.put(Long.toString(desc.getBundleId()), element);
			}
		}
		doc.appendChild(root);
		return doc;
	}
	
	private void savePluginInfo(File dir) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("map"); //$NON-NLS-1$
			
			Iterator iter = fPluginInfos.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next().toString();
				Element element = doc.createElement("bundle"); //$NON-NLS-1$
				element.setAttribute("bundleID", key); //$NON-NLS-1$
				PluginInfo info = (PluginInfo)fPluginInfos.get(key);
				if (info.className != null)
					element.setAttribute("class", info.className); //$NON-NLS-1$
				if (info.providerName != null)
					element.setAttribute("provider", info.providerName); //$NON-NLS-1$
				if (info.name != null)
					element.setAttribute("name", info.name); //$NON-NLS-1$
				if (info.hasExtensibleAPI)
					element.setAttribute("hasExtensibleAPI", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				if (info.localization != null)
					element.setAttribute("localization", info.localization); //$NON-NLS-1$
				if (info.libraries != null) {
					for (int i = 0; i < info.libraries.length; i++) {
						Element lib = doc.createElement("library"); //$NON-NLS-1$
						lib.setAttribute("name", info.libraries[i]); //$NON-NLS-1$
						element.appendChild(lib);
					}
				}
				root.appendChild(element);
			}
			doc.appendChild(root);
			XMLPrintHandler.writeFile(doc, new File(dir, ".pluginInfo")); //$NON-NLS-1$
		} catch (Exception e) {
			PDECore.log(e);
		} 
	}
	
	private void saveExtensions(File dir) {
		try {
			File file = new File(dir, ".extensions"); //$NON-NLS-1$
			XMLPrintHandler.writeFile(createExtensionDocument(), file);
		} catch (IOException e) {
		}
	}
	
	private Map readExtensionsCache(File dir) {
		long start = System.currentTimeMillis();
		File file = new File(dir, ".extensions"); //$NON-NLS-1$
		if (file.exists() && file.isFile()) {
			try {
				Map map = new HashMap();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList bundles = root.getChildNodes();
					for (int i = 0; i < bundles.getLength(); i++) {
						if (bundles.item(i).getNodeType() == Node.ELEMENT_NODE) {
							Element bundle = (Element)bundles.item(i); 
							String id = bundle.getAttribute("bundleID"); //$NON-NLS-1$
							map.put(id, bundle.getChildNodes());
						}
					}
				}
				if (DEBUG)
					System.out.println("Time to read extensions: " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
				return map;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		}
		return null;
	}

	private Map readPluginInfoCache(File dir) {
		File file = new File(dir, ".pluginInfo"); //$NON-NLS-1$
		if (file.exists() && file.isFile()) {
			try {
				Map map = new HashMap();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList list = root.getChildNodes();
					for (int i = 0; i < list.getLength(); i++) {
						if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
							createPluginInfo(map, (Element)list.item(i));
					}
				}
				return map;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		} 
		return null;
	}

	private State readStateCache(File dir) {
		if (dir.exists() && dir.isDirectory()) {
			try {
				return stateObjectFactory.readState(dir);	
			} catch (IllegalStateException e) {
				PDECore.log(e);
			} catch (FileNotFoundException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
			}
		} 
		return null;
	}
	
 	private long computeTimestamp(URL[] urls) {
		long timestamp = 0;
		for (int i = 0; i < urls.length; i++) {
			File file = new File(urls[i].getFile());
			if (file.exists()) {
				if (file.isFile()) {
					timestamp ^= file.lastModified();
				} else {
					File manifest = new File(file, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "plugin.xml"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "fragment.xml"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
				}
				timestamp ^= file.getAbsolutePath().hashCode();
			}
		}
		return timestamp;
	}
 	
 	private IPluginModelBase createWorkspaceModel(BundleDescription desc) {
 		String projectName = getProject(desc.getBundleId());
 		IProject project = PDECore.getWorkspace().getRoot().getProject(projectName);
 		if (!project.exists())
 			return null;
 		if (WorkspaceModelManager.hasBundleManifest(project)) {
 			BundlePluginModelBase model = null;
 			if (desc.getHost() == null)
 				model = new BundlePluginModel();
 			else
 				model = new BundleFragmentModel();
 			model.setEnabled(true);
 			WorkspaceBundleModel bundle = new WorkspaceBundleModel(project.getFile("META-INF/MANIFEST.MF")); //$NON-NLS-1$
 			bundle.load(desc, this);
  			model.setBundleDescription(desc);
 			model.setBundleModel(bundle);
 			
 			String filename = (desc.getHost() == null) ? "plugin.xml" : "fragment.xml"; //$NON-NLS-1$ //$NON-NLS-2$
 			IFile file = project.getFile(filename);
 			if (file.exists()) {
 				WorkspaceExtensionsModel extensions = new WorkspaceExtensionsModel(file);
 				extensions.load(desc, this);
 				extensions.setBundleModel(model);
 				model.setExtensionsModel(extensions);
 			}
 			return model;
 		}
 		
		WorkspacePluginModelBase model = null;
		if (desc.getHost() == null)
			model = new WorkspacePluginModel(project.getFile("plugin.xml"), true); //$NON-NLS-1$
		else
			model = new WorkspaceFragmentModel(project.getFile("fragment.xml"), true); //$NON-NLS-1$
		model.load(desc, this, false);
		model.setBundleDescription(desc);
		return model;
	}

	private IPluginModelBase createExternalModel(BundleDescription desc) {
 		ExternalPluginModelBase model = null;
 		if (desc.getHost() == null)
			model = new ExternalPluginModel();
		else
			model = new ExternalFragmentModel();
		model.load(desc, this, !fResolve);
		model.setBundleDescription(desc);
		return model;
 	}
 	
 	public IPluginModelBase[] getTargetModels() {
 		return (IPluginModelBase[])fTargetModels.values().toArray(new IPluginModelBase[fTargetModels.size()]);
 	}
 	
 	public IPluginModelBase[] getWorkspaceModels() {
 		return (IPluginModelBase[])fWorkspaceModels.toArray(new IPluginModelBase[fWorkspaceModels.size()]);		
 	}
 	
 	public IPluginModelBase[] getModels() {
 		IPluginModelBase[] workspace = getWorkspaceModels();
 		IPluginModelBase[] target = getTargetModels();
 		IPluginModelBase[] all = new IPluginModelBase[workspace.length + target.length];
 		if (workspace.length > 0)
 			System.arraycopy(workspace, 0, all, 0, workspace.length);
 		if (target.length > 0)
 			System.arraycopy(target, 0, all, workspace.length, target.length);
 		return all;
 	}
	
	public String getClassName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.className;
	}
	
	public boolean hasExtensibleAPI(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? false : info.hasExtensibleAPI;		
	}

	public boolean isLegacy(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? false : info.legacy;		
	}

	public String getPluginName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.name;
	}
	
	public String getProviderName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.providerName;
	}
	
	public String[] getLibraryNames(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? new String[0] : info.libraries;
	}
	
	public String getBundleLocalization(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.localization;		
	}
	
	public String getProject(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.project;		
	}
	
	public Node[] getExtensions(long bundleID) {
		return getChildren(bundleID, "extension"); //$NON-NLS-1$
	}
	
	public Node[] getExtensionPoints(long bundleID) {
		return getChildren(bundleID, "extension-point"); //$NON-NLS-1$
	}
	
	private Node[] getChildren(long bundleID, String tagName) {
		ArrayList list = new ArrayList();
		if (fExtensions != null) {
			Element bundle = (Element)fExtensions.get(Long.toString(bundleID));
			if (bundle != null) {
				NodeList children = bundle.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					if (tagName.equals(children.item(i).getNodeName())) {
						list.add(children.item(i));
					}
				}
			}
		}
		return (Node[])list.toArray(new Node[list.size()]);
	}

	public Node[] getAllExtensions(long bundleID) {
		ArrayList list = new ArrayList();
		if (fExtensions != null) {
			Element bundle = (Element)fExtensions.get(Long.toString(bundleID));
			if (bundle != null) {
				NodeList children = bundle.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					String name = children.item(i).getNodeName();
					if ("extension".equals(name) || "extension-point".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
						list.add(children.item(i));
					}
				}
			}
		}
		return (Node[])list.toArray(new Node[list.size()]);
	}

	public void shutdown() {
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
		long timestamp = 0;
		if (!"true".equals(System.getProperty("pde.nocache")) && shouldSaveState(models)) { //$NON-NLS-1$ //$NON-NLS-2$
			timestamp = computeTimestamp(models);
			File dir = new File(DIR, Long.toString(timestamp) + ".workspace"); //$NON-NLS-1$
			State state = stateObjectFactory.createState();
			for (int i = 0; i < models.length; i++) {
				state.addBundle(models[i].getBundleDescription());
			}
			saveState(state, dir);
			writePluginInfo(models, dir);
			writeExtensions(models, dir);
		}
		clearStaleStates(".target", fTargetTimestamp); //$NON-NLS-1$
		clearStaleStates(".workspace", timestamp); //$NON-NLS-1$
		clearStaleStates(".cache", 0); //$NON-NLS-1$
	}
	
	public void writePluginInfo(IPluginModelBase[] models, File destination) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
		
			Element root = doc.createElement("map"); //$NON-NLS-1$
			doc.appendChild(root);
			for (int i = 0; i < models.length; i++) {
				IPluginBase plugin = models[i].getPluginBase();
				BundleDescription desc = models[i].getBundleDescription();
				Element element = doc.createElement("bundle"); //$NON-NLS-1$
				element.setAttribute("bundleID", Long.toString(desc.getBundleId())); //$NON-NLS-1$
				element.setAttribute("project", models[i].getUnderlyingResource().getProject().getName()); //$NON-NLS-1$
				if (plugin instanceof IPlugin && ((IPlugin)plugin).getClassName() != null)
					element.setAttribute("class", ((IPlugin)plugin).getClassName()); //$NON-NLS-1$
				if (plugin.getProviderName() != null)
					element.setAttribute("provider", plugin.getProviderName()); //$NON-NLS-1$
				if (plugin.getName() != null)
					element.setAttribute("name", plugin.getName()); //$NON-NLS-1$
				if (plugin instanceof IPlugin && ClasspathUtilCore.hasExtensibleAPI((IPlugin)plugin))
					element.setAttribute("hasExtensibleAPI", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				if (plugin.getSchemaVersion() == null)
					element.setAttribute("legacy", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				if (models[i] instanceof IBundlePluginModelBase) {
					String localization = ((IBundlePluginModelBase)models[i]).getBundleLocalization();
					if (localization != null)
						element.setAttribute("localization", localization); //$NON-NLS-1$
				}
				IPluginLibrary[] libraries = plugin.getLibraries();
				for (int j = 0; j < libraries.length; j++) {
						Element lib = doc.createElement("library"); //$NON-NLS-1$
						lib.setAttribute("name", libraries[j].getName()); //$NON-NLS-1$
						if (!libraries[j].isExported())
							lib.setAttribute("exported", "false"); //$NON-NLS-1$ //$NON-NLS-2$
						element.appendChild(lib);
				}
				root.appendChild(element);
			}
			XMLPrintHandler.writeFile(doc, new File(destination, ".pluginInfo")); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		} catch (IOException e) {
		}
	}
	
	public void writeExtensions(IPluginModelBase[] models, File destination) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
		
			Element root = doc.createElement("extensions"); //$NON-NLS-1$
			doc.appendChild(root);
		
			for (int i = 0; i < models.length; i++) {
				IPluginBase plugin = models[i].getPluginBase();
				IPluginExtension[] extensions = plugin.getExtensions();
				IPluginExtensionPoint[] extPoints = plugin.getExtensionPoints();
				if (extensions.length == 0 && extPoints.length == 0)
					continue;
				Element element = doc.createElement("bundle"); //$NON-NLS-1$
				element.setAttribute("bundleID", Long.toString(models[i].getBundleDescription().getBundleId())); //$NON-NLS-1$
				for (int j = 0; j < extensions.length; j++) {
					element.appendChild(CoreUtility.writeExtension(doc, extensions[j]));
				}				
				for (int j = 0; j < extPoints.length; j++) {
					element.appendChild(CoreUtility.writeExtensionPoint(doc, extPoints[j]));
				}			
				root.appendChild(element);
			}
			XMLPrintHandler.writeFile(doc, new File(destination, ".extensions")); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		} catch (IOException e) {
		}	
	}
	
	private long computeTimestamp(IPluginModelBase[] models) {
		URL[] urls = new URL[models.length];
		for (int i = 0; i < models.length; i++) {
			try {
				IProject project = models[i].getUnderlyingResource().getProject();
				urls[i] = new File(project.getLocation().toString()).toURL();
			} catch (MalformedURLException e) {
			}
		}
		return computeTimestamp(urls);
	}
	
	private boolean shouldSaveState(IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null
					|| id.trim().length() == 0
					|| !models[i].isLoaded()
					||!models[i].isInSync() 
					|| models[i].getBundleDescription() == null)
				return false;
		}
		return models.length > 0;
	}
	
	private void clearStaleStates(String extension, long latest) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				if (child.isDirectory()) {
					String name = child.getName();
					if (name.endsWith(extension)
							&& name.length() > extension.length()
							&& !name.equals(Long.toString(latest) + extension)) { 
						CoreUtility.deleteContent(child);
					}
				}
			}
		}
	}	

}
