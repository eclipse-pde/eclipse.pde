/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.api.tools.internal.model.ApiBaseline;
import org.eclipse.pde.api.tools.internal.model.ApiModelCache;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.model.WorkspaceBaseline;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.DependencyManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This manager is used to maintain (persist, restore, access, update) API baselines.
 * This manager is lazy, in that caches are built and maintained when requests
 * are made for information, nothing is pre-loaded when the manager is initialized.
 * 
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ApiBaselineManager implements IApiBaselineManager, ISaveParticipant {
	
	/**
	 * Constant for the default API baseline.
	 * Value is: <code>default_api_profile</code>
	 */
	private static final String DEFAULT_BASELINE = "default_api_profile"; //$NON-NLS-1$
	
	/**
	 * Constant representing the id of the workspace {@link IApiBaseline}.
	 * Value is: <code>workspace</code>
	 */
	public static final String WORKSPACE_API_BASELINE_ID = "workspace"; //$NON-NLS-1$
	
	/**
	 * Constant representing the file extension for a baseline file.
	 * Value is: <code>.profile</code>
	 */
	private static final String BASELINE_FILE_EXTENSION = ".profile"; //$NON-NLS-1$
	
	/**
	 * The main cache for the manager.
	 * The form of the cache is: 
	 * <pre>
	 * HashMap<String(baselineid), {@link IApiBaseline}>
	 * </pre>
	 */
	private HashMap baselinecache = null;
	
	/**
	 * Cache of baseline names to the location with their infos in it
	 */
	private HashMap handlecache = null;
	
	private HashSet hasinfos = null;
	
	/**
	 * The current default {@link IApiBaseline}
	 */
	private String defaultbaseline = null;
	
	/**
	 * The current workspace baseline
	 */
	private IApiBaseline workspacebaseline = null;
	
	/**
	 * The default save location for persisting the cache from this manager.
	 */
	private IPath savelocation = null;
	
	/**
	 * If the cache of baselines needs to be saved or not.
	 */
	private boolean fNeedsSaving = false;
	
	/**
	 * The singleton instance
	 */
	private static ApiBaselineManager fInstance = null;
	
	/**
	 * Constructor
	 */
	private ApiBaselineManager(boolean framework) {
		if(framework) {
			ApiPlugin.getDefault().addSaveParticipant(this);
			savelocation = ApiPlugin.getDefault().getStateLocation().append(".api_profiles").addTrailingSeparator(); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns the singleton instance of the manager
	 * @return the singleton instance of the manager
	 */
	public static synchronized ApiBaselineManager getManager() {
		if(fInstance == null) {
			fInstance = new ApiBaselineManager(ApiPlugin.isRunningInFramework());
		}
		return fInstance;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiBaselineManager#getApiProfile(java.lang.String)
	 */
	public synchronized IApiBaseline getApiBaseline(String name) {
		initializeStateCache();
		return (ApiBaseline) baselinecache.get(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiBaselineManager#getApiProfiles()
	 */
	public synchronized IApiBaseline[] getApiBaselines() {
		initializeStateCache();
		return (IApiBaseline[]) baselinecache.values().toArray(new IApiBaseline[baselinecache.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiBaselineManager#addApiProfile(org.eclipse.pde.api.tools.model.component.IApiBaseline)
	 */
	public synchronized void addApiBaseline(IApiBaseline newbaseline) {
		if(newbaseline != null) {
			initializeStateCache();
			baselinecache.put(newbaseline.getName(), newbaseline);
			if(((ApiBaseline)newbaseline).peekInfos()) {
				hasinfos.add(newbaseline.getName());
			}
			fNeedsSaving = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiBaselineManager#removeApiBaseline(java.lang.String)
	 */
	public synchronized boolean removeApiBaseline(String name) {
		if(name != null) {
			initializeStateCache();
			IApiBaseline baseline = (IApiBaseline) baselinecache.remove(name);
			if(baseline != null) {
				baseline.dispose();
				boolean success = true;
				if(savelocation == null) {
					return success;
				}
				//remove from filesystem
				File file = savelocation.append(name+BASELINE_FILE_EXTENSION).toFile();
				if(file.exists()) {
					success &= file.delete();
				}
				fNeedsSaving = true;
				
				//flush the model cache
				ApiModelCache.getCache().removeElementInfo(baseline);
				return success;
			}
		}
		return false;
	}
	
	/**
	 * Loads the infos for the given baseline from persisted storage (the *.profile file)
	 * @param baseline the given baseline
	 * @throws CoreException if an exception occurs while loading baseline infos
	 */
	public void loadBaselineInfos(IApiBaseline baseline) throws CoreException {
		initializeStateCache();
		if(hasinfos.contains(baseline.getName())) {
			return;
		}
		String filename = (String) handlecache.get(baseline.getName());
		if(filename != null) {
			File file = new File(filename);
			if(file.exists()) {
				FileInputStream inputStream = null;
				try {
					inputStream = new FileInputStream(file);
					restoreBaseline(baseline, inputStream);
				} catch (IOException e) {
					ApiPlugin.log(e);
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch(IOException e) {
							// ignore
						}
					}
				}
				hasinfos.add(baseline.getName());
			}
		}
	}
	
	/**
	 * Initializes the baseline cache lazily. Only performs work
	 * if the current cache has not been created yet
	 * @throws FactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 */
	private synchronized void initializeStateCache() {
		long time = System.currentTimeMillis();
		if(baselinecache == null) {
			handlecache = new HashMap(8);
			hasinfos = new HashSet(8);
			baselinecache = new HashMap(8);
			if(!ApiPlugin.isRunningInFramework()) {
				return;
			}
			File[] baselines = savelocation.toFile().listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(BASELINE_FILE_EXTENSION);
				}
			});
			if(baselines != null) {
				IApiBaseline newbaseline = null;
				for(int i = 0; i < baselines.length; i++) {
					File baseline = baselines[i];
					if(baseline.exists()) {
						newbaseline = new ApiBaseline(new Path(baseline.getName()).removeFileExtension().toString());
						handlecache.put(newbaseline.getName(), baseline.getAbsolutePath());
						baselinecache.put(newbaseline.getName(), newbaseline);
					}
				}
			}
			String def = getDefaultProfilePref();
			IApiBaseline baseline = (IApiBaseline) baselinecache.get(def);
			defaultbaseline = (baseline != null ? def : null);
			if(ApiPlugin.DEBUG_BASELINE_MANAGER) {
				System.out.println("Time to initialize state cache: " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	/**
	 * @return the default API baseline saved in the preferences, or <code>null</code> if there isn't one
	 */
	private String getDefaultProfilePref() {
		IPreferencesService service = Platform.getPreferencesService();
		return service.getString(ApiPlugin.PLUGIN_ID, DEFAULT_BASELINE, null, new IScopeContext[] {InstanceScope.INSTANCE});
	}
	
	/**
	 * Persists all of the cached elements to individual xml files named 
	 * with the id of the API baseline
	 * @throws IOException 
	 */
	private void persistStateCache() throws CoreException, IOException {
		if(savelocation == null) {
			return;
		}
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		if(defaultbaseline != null) {
			node.put(DEFAULT_BASELINE, defaultbaseline);
		}
		else {
			node.remove(DEFAULT_BASELINE);
		}
		if(baselinecache != null && hasinfos != null) {
			File dir = new File(savelocation.toOSString());
			if(!dir.exists()) {
				dir.mkdirs();
			}
			String id = null;
			File file = null;
			FileOutputStream fout = null;
			IApiBaseline baseline = null;
			for(Iterator iter = baselinecache.keySet().iterator(); iter.hasNext();) {
				id = (String) iter.next();
				baseline = (IApiBaseline) baselinecache.get(id);
				if(!hasinfos.contains(baseline.getName())) {
					continue;
				}
				file = savelocation.append(id+BASELINE_FILE_EXTENSION).toFile();
				if(!file.exists()) {
					file.createNewFile();
				}
				try {
					fout = new FileOutputStream(file);
					writeBaselineDescription(baseline, fout);
					// need to save the api baseline state in order to be able to reload it later
					handlecache.put(baseline.getName(), file.getAbsolutePath());
					fout.flush();
				}
				finally {
					fout.close();
				}
			}
		}
	}	
	
	/**
	 * Writes out the current state of the {@link IApiBaseline} as XML
	 * to the given output stream
	 * @param stream
	 * @throws CoreException
	 */
	private void writeBaselineDescription(IApiBaseline baseline, OutputStream stream) throws CoreException {
		String xml = getProfileXML(baseline);
		try {
			stream.write(xml.getBytes(IApiCoreConstants.UTF_8));
		} catch (UnsupportedEncodingException e) {
			abort("Error writing pofile descrition", e); //$NON-NLS-1$
		} catch (IOException e) {
			abort("Error writing pofile descrition", e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns an XML description of the given baseline.
	 * 
	 * @param baseline the given API baseline
	 * @return XML string representation of the given baseline
	 * @throws CoreException if an exception occurs while retrieving the xml string representation
	 */
	private String getProfileXML(IApiBaseline baseline) throws CoreException {
		Document document = Util.newDocument();
		Element root = document.createElement(IApiXmlConstants.ELEMENT_APIPROFILE);
		document.appendChild(root);
		root.setAttribute(IApiXmlConstants.ATTR_NAME, baseline.getName());
		root.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_PROFILE_CURRENT_VERSION);
		String location = baseline.getLocation();
		if (location != null) {
			root.setAttribute(IApiXmlConstants.ATTR_LOCATION, location);
		}
		Element celement = null;
		IApiComponent[] components = baseline.getApiComponents();
		for(int i = 0, max = components.length; i < max; i++) {
			IApiComponent comp = components[i];
			if (!comp.isSystemComponent()) {
				celement = document.createElement(IApiXmlConstants.ELEMENT_APICOMPONENT);
				celement.setAttribute(IApiXmlConstants.ATTR_ID, comp.getSymbolicName());
				celement.setAttribute(IApiXmlConstants.ATTR_VERSION, comp.getVersion());
				celement.setAttribute(IApiXmlConstants.ATTR_LOCATION, new Path(comp.getLocation()).toPortableString());
				root.appendChild(celement);
			}
		}
		return Util.serializeDocument(document);
	}
	
	/**
	 * Throws a core exception with the given message and underlying exception,
	 * if any.
	 * 
	 * @param message error message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	private static void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, message, e));
	}	
	
	/**
	 * Restore a baseline from the given input stream (persisted baseline).
	 * 
	 * @param baseline the given baseline to restore
	 * @param stream the given input stream
	 * @throws CoreException if unable to restore the baseline
	 */
	private void restoreBaseline(IApiBaseline baseline, InputStream stream) throws CoreException {
		long start = System.currentTimeMillis();
		DocumentBuilder parser = null;
		try {
			parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
		} catch (ParserConfigurationException e) {
			abort("Error restoring API baseline", e); //$NON-NLS-1$
		} catch (FactoryConfigurationError e) {
			abort("Error restoring API baseline", e); //$NON-NLS-1$
		}
		try {
			Document document = parser.parse(stream);
			Element root = document.getDocumentElement();
			if(root.getNodeName().equals(IApiXmlConstants.ELEMENT_APIPROFILE)) {
				String baselineLocation = root.getAttribute(IApiXmlConstants.ATTR_LOCATION);
				if (baselineLocation != null && !baselineLocation.equals(Util.EMPTY_STRING)) {
					baseline.setLocation(Path.fromPortableString(baselineLocation).toOSString());
				}
				// un-pooled components
				NodeList children = root.getElementsByTagName(IApiXmlConstants.ELEMENT_APICOMPONENT);
				List components = new ArrayList();
				for(int j = 0; j < children.getLength(); j++) {
					Element componentNode = (Element) children.item(j);
					// this also contains components in pools, so don't process them
					if (componentNode.getParentNode().equals(root)) {
						String location = componentNode.getAttribute(IApiXmlConstants.ATTR_LOCATION);
						IApiComponent component = ApiModelFactory.newApiComponent(baseline, Path.fromPortableString(location).toOSString());
						if(component != null) {
							components.add(component);
						}
					}
				}
				// pooled components - only for xml file with version <= 1
				// since version 2, pools have been removed
				children = root.getElementsByTagName(IApiXmlConstants.ELEMENT_POOL);
				IApiComponent component = null;
				for(int j = 0; j < children.getLength(); j++) {
					String location = ((Element) children.item(j)).getAttribute(IApiXmlConstants.ATTR_LOCATION);
					IPath poolPath = Path.fromPortableString(location);
					NodeList componentNodes = root.getElementsByTagName(IApiXmlConstants.ELEMENT_APICOMPONENT);
					for (int i = 0; i < componentNodes.getLength(); i++) {
						Element compElement = (Element) componentNodes.item(i);
						String id = compElement.getAttribute(IApiXmlConstants.ATTR_ID);
						String ver = compElement.getAttribute(IApiXmlConstants.ATTR_VERSION);
						StringBuffer name = new StringBuffer();
						name.append(id);
						name.append('_');
						name.append(ver);
						File file = poolPath.append(name.toString()).toFile();
						if (!file.exists()) {
							name.append(".jar"); //$NON-NLS-1$
							file = poolPath.append(name.toString()).toFile();
						}
						component = ApiModelFactory.newApiComponent(baseline, file.getAbsolutePath());
						if(component != null) {
							components.add(component);
						}
					}
				}
				baseline.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			}
		} catch (IOException e) {
			abort("Error restoring API baseline", e); //$NON-NLS-1$
		} catch(SAXException e) {
			abort("Error restoring API baseline", e); //$NON-NLS-1$
		} finally {
			try {
				stream.close();
			} catch (IOException io) {
				ApiPlugin.log(io);
			}
		}
		if (baseline == null) {
			abort("Invalid baseline description", null); //$NON-NLS-1$
		}
		if(ApiPlugin.DEBUG_BASELINE_MANAGER) {
			System.out.println("Time to restore a persisted baseline : " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
		if(!fNeedsSaving) {
			return;
		}
		try {
			persistStateCache();
			cleanStateCache();
			fNeedsSaving = false;
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Cleans out all but the default baseline from the in-memory cache of baselines
	 */
	private void cleanStateCache() {
		if(baselinecache != null) {
			IApiBaseline baseline = null;
			for(Iterator iter = baselinecache.keySet().iterator(); iter.hasNext();) {
				baseline = (IApiBaseline) baselinecache.get(iter.next());
				if(!baseline.getName().equals(defaultbaseline)) {
					baseline.dispose();
					hasinfos.remove(baseline.getName());
					//iter.remove();
				}
			}
		}
	}
	
	/**
	 * Returns if the given name is an existing baseline name
	 * @param name
	 * @return true if the given name is an existing baseline name, false otherwise
	 */
	public boolean isExistingProfileName(String name) {
		if(baselinecache == null) {
			return false;
		}
		return baselinecache.keySet().contains(name);
	}
	
	/**
	 * Cleans up the manager
	 */
	public void stop() {
		try {
			if(baselinecache != null) {
				// we should first dispose all existing baselines
				for (Iterator iterator = this.baselinecache.values().iterator(); iterator.hasNext();) {
					IApiBaseline baseline = (IApiBaseline) iterator.next();
					baseline.dispose();
				}
				this.baselinecache.clear();
			}
			synchronized (this) {
				if(this.workspacebaseline != null) {
					this.workspacebaseline.dispose();
				}
			}
			if(this.handlecache != null) {
				this.handlecache.clear();
			}
			if(hasinfos != null) {
				hasinfos.clear();
			}
			StubApiComponent.disposeAllCaches();
		}
		finally {
			if(ApiPlugin.isRunningInFramework()) {
				ApiPlugin.getDefault().removeSaveParticipant(this);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	public void doneSaving(ISaveContext context) {}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) throws CoreException {}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager#getDefaultApiBaseline()
	 */
	public synchronized IApiBaseline getDefaultApiBaseline() {
		initializeStateCache();
		return (IApiBaseline) baselinecache.get(defaultbaseline);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager#setDefaultApiBaseline(java.lang.String)
	 */
	public void setDefaultApiBaseline(String name) {
		fNeedsSaving = true;
		defaultbaseline = name;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager#getWorkspaceBaseline()
	 */
	public synchronized IApiBaseline getWorkspaceBaseline() {
		if(ApiPlugin.isRunningInFramework()) {
			if(this.workspacebaseline == null) {
				try {
					this.workspacebaseline = createWorkspaceBaseline();
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			return this.workspacebaseline;
		}
		return null;
	}	

	/**
	 * Disposes the workspace baseline such that a new one will be created
	 * on the next request.
	 */
	synchronized void disposeWorkspaceBaseline() {
		if (workspacebaseline != null) {
			if(ApiPlugin.DEBUG_BASELINE_MANAGER) {
				System.out.println("disposing workspace baseline"); //$NON-NLS-1$
			}
			workspacebaseline.dispose();
			StubApiComponent.disposeAllCaches();
			workspacebaseline = null;
		}
	}

	/**
	 * Creates a workspace {@link IApiBaseline}
	 * @return a new workspace {@link IApiBaseline} or <code>null</code>
	 */
	private IApiBaseline createWorkspaceBaseline() throws CoreException {
		long time = System.currentTimeMillis();
		IApiBaseline baseline = null; 
		try {
			baseline = new WorkspaceBaseline();
			// populate it with only projects that are API aware
			Set ids = DependencyManager.getSelfandDependencies(PluginRegistry.getWorkspaceModels(), null);
			List componentsList = new ArrayList(ids.size());
			IApiComponent apiComponent = null;
			IPluginModelBase model = null;
			for (Iterator iter = ids.iterator(); iter.hasNext();) {
				model = PluginRegistry.findModel((String) iter.next());
				if(model == null) {
					continue;
				}
				try {
					apiComponent = ApiModelFactory.newApiComponent(baseline, model);
					if (apiComponent != null) {
						componentsList.add(apiComponent);
					}
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			baseline.addApiComponents((IApiComponent[]) componentsList.toArray(new IApiComponent[componentsList.size()]));
		} finally {
			if (ApiPlugin.DEBUG_BASELINE_MANAGER) {
				System.out.println("Time to create a workspace baseline : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return baseline;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager#getWorkspaceComponent(java.lang.String)
	 */
	public synchronized IApiComponent getWorkspaceComponent(String symbolicName) {
		IApiBaseline baseline = getWorkspaceBaseline();
		if (baseline != null) {
			return baseline.getApiComponent(symbolicName);
		}
		return null;
	}
}
