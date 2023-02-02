/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.api.tools.internal.builder.ApiAnalysisBuilder.ApiAnalysisJob;
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
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.DependencyManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This manager is used to maintain (persist, restore, access, update) API
 * baselines. This manager is lazy, in that caches are built and maintained when
 * requests are made for information, nothing is pre-loaded when the manager is
 * initialized.
 *
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ApiBaselineManager implements IApiBaselineManager, ISaveParticipant {

	/**
	 * Constant for the default API baseline. Value is:
	 * <code>default_api_profile</code>
	 */
	private static final String DEFAULT_BASELINE = "default_api_profile"; //$NON-NLS-1$

	/**
	 * Constant representing the id of the workspace {@link IApiBaseline}. Value
	 * is: <code>workspace</code>
	 */
	public static final String WORKSPACE_API_BASELINE_ID = "workspace"; //$NON-NLS-1$

	/**
	 * Constant representing the file extension for a baseline file. Value is:
	 * <code>.profile</code>
	 */
	private static final String BASELINE_FILE_EXTENSION = ".profile"; //$NON-NLS-1$

	/**
	 * The main cache for the manager. The form of the cache is:
	 *
	 * <pre>
	 * Map<String(baselineid), {@link IApiBaseline}>
	 * </pre>
	 */
	private volatile ConcurrentHashMap<String, IApiBaseline> baselinecache;

	/**
	 * Cache of baseline names to the location with their infos in it
	 */
	private volatile Map<String, String> handlecache;

	private volatile Set<String> hasinfos;

	/**
	 * The current default {@link IApiBaseline}
	 */
	private String defaultbaseline = null;

	/**
	 * The current workspace baseline
	 */
	private volatile IApiBaseline workspacebaseline;

	/**
	 * The default save location for persisting the cache from this manager.
	 */
	private IPath savelocation = null;

	/**
	 * If the cache of baselines needs to be saved or not.
	 */
	private volatile boolean fNeedsSaving;

	/**
	 * The singleton instance
	 */
	private static ApiBaselineManager fInstance = null;

	/**
	 * Constructor
	 */
	private ApiBaselineManager(boolean framework) {
		if (framework) {
			ApiPlugin.getDefault().addSaveParticipant(this);
			savelocation = ApiPlugin.getDefault().getStateLocation().append(".api_profiles").addTrailingSeparator(); //$NON-NLS-1$
		}
		hasinfos = Collections.emptySet();
	}

	/**
	 * Returns the singleton instance of the manager
	 *
	 * @return the singleton instance of the manager
	 */
	public static synchronized ApiBaselineManager getManager() {
		if (fInstance == null) {
			fInstance = new ApiBaselineManager(ApiPlugin.isRunningInFramework());
		}
		return fInstance;
	}

	@Override
	public IApiBaseline getApiBaseline(String name) {
		if (name == null) {
			return null;
		}
		initializeStateCache();
		return baselinecache.get(name);
	}

	@Override
	public IApiBaseline[] getApiBaselines() {
		initializeStateCache();
		return baselinecache.values().toArray(new IApiBaseline[0]);
	}

	@Override
	public void addApiBaseline(IApiBaseline newbaseline) {
		if (newbaseline != null) {
			initializeStateCache();
			baselinecache.put(newbaseline.getName(), newbaseline);
			if (((ApiBaseline) newbaseline).peekInfos()) {
				hasinfos.add(newbaseline.getName());
			}
			fNeedsSaving = true;
		}
	}

	@Override
	public boolean removeApiBaseline(String name) {
		if (name == null) {
			return false;
		}
		initializeStateCache();
		IApiBaseline baseline = baselinecache.remove(name);
		if (baseline == null) {
			return false;
		}
		synchronized (this) {
			baseline.dispose();
			boolean success = true;
			if (savelocation == null) {
				return success;
			}
			// remove from filesystem
			File file = savelocation.append(name + BASELINE_FILE_EXTENSION).toFile();
			if (file.exists()) {
				try {
					success &= Files.deleteIfExists(file.toPath());
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			}
			fNeedsSaving = true;

			// flush the model cache
			ApiModelCache.getCache().removeElementInfo(baseline);
			return success;
		}
	}

	/**
	 * Loads the infos for the given baseline from persisted storage (the
	 * *.profile file)
	 *
	 * @param baseline the given baseline
	 * @throws CoreException if an exception occurs while loading baseline infos
	 */
	public void loadBaselineInfos(ApiBaseline baseline) throws CoreException {
		initializeStateCache();
		if (isBaselineLoaded(baseline)) {
			return;
		}
		String filename = handlecache.get(baseline.getName());
		if (filename != null) {
			File file = new File(filename);
			if (file.exists()) {
				try (FileInputStream inputStream = new FileInputStream(file)) {
					baseline.restoreFrom(inputStream);
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
				hasinfos.add(baseline.getName());
			}
		}
	}

	public boolean isBaselineLoaded(IApiBaseline baseline) {
		return hasinfos.contains(baseline.getName());
	}

	/**
	 * Initializes the baseline cache lazily. Only performs work if the current
	 * cache has not been created yet
	 *
	 * @throws FactoryConfigurationError
	 * @throws ParserConfigurationException
	 */
	private void initializeStateCache() {
		if (baselinecache != null) {
			return;
		}
		if (!ApiPlugin.isRunningInFramework()) {
			synchronized (this) {
				if (baselinecache == null) {
					handlecache = new ConcurrentHashMap<>(8);
					hasinfos = ConcurrentHashMap.newKeySet(8);
					baselinecache = new ConcurrentHashMap<>(8);
				}
			}
			return;
		}

		long time = System.currentTimeMillis();
		synchronized (this) {
			if (baselinecache == null) {
				handlecache = new ConcurrentHashMap<>(8);
				hasinfos = ConcurrentHashMap.newKeySet(8);
				ConcurrentHashMap<String, IApiBaseline> bcache = new ConcurrentHashMap<>(8);
				File[] baselines = savelocation.toFile().listFiles((FileFilter) pathname -> pathname.getName().endsWith(BASELINE_FILE_EXTENSION));
				if (baselines != null) {
					IApiBaseline newbaseline = null;
					for (File baseline : baselines) {
						if (baseline.exists()) {
							newbaseline = new ApiBaseline(new Path(baseline.getName()).removeFileExtension().toString());
							handlecache.put(newbaseline.getName(), baseline.getAbsolutePath());
							bcache.put(newbaseline.getName(), newbaseline);
						}
					}
				}
				String def = getDefaultProfilePref();
				if (def != null && bcache.get(def) != null) {
					defaultbaseline = def;
				} else {
					defaultbaseline = null;
				}
				baselinecache = bcache;
				if (ApiPlugin.DEBUG_BASELINE_MANAGER) {
					System.out.println("Time to initialize state cache: " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	/**
	 * @return the default API baseline saved in the preferences, or
	 *         <code>null</code> if there isn't one
	 */
	private String getDefaultProfilePref() {
		IPreferencesService service = Platform.getPreferencesService();
		return service.getString(ApiPlugin.PLUGIN_ID, DEFAULT_BASELINE, null, new IScopeContext[] { InstanceScope.INSTANCE });
	}

	/**
	 * Persists all of the cached elements to individual xml files named with
	 * the id of the API baseline
	 *
	 * @throws IOException
	 */
	private void persistStateCache() throws CoreException, IOException {
		if (savelocation == null) {
			return;
		}
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		if (defaultbaseline != null) {
			node.put(DEFAULT_BASELINE, defaultbaseline);
		} else {
			node.remove(DEFAULT_BASELINE);
		}
		if (baselinecache != null && !hasinfos.isEmpty()) {
			File dir = new File(savelocation.toOSString());
			Files.createDirectories(dir.toPath());
			IApiBaseline baseline = null;
			for (Entry<String, IApiBaseline> entry : baselinecache.entrySet()) {
				String id = entry.getKey();
				baseline = entry.getValue();
				if (!isBaselineLoaded(baseline)) {
					continue;
				}
				File file = savelocation.append(id + BASELINE_FILE_EXTENSION).toFile();
				if (!file.exists()) {
					try {
						Files.createFile(file.toPath());
					} catch (IOException ioe) {
						ApiPlugin.log(new IOException("Unable to save API baseline with id: '" + id + "'", ioe)); //$NON-NLS-1$ //$NON-NLS-2$
						continue;
					}
				}
				try (FileOutputStream fout = new FileOutputStream(file)) {
					writeBaselineDescription(baseline, fout);
					// need to save the api baseline state in order to be able
					// to reload it later
					handlecache.put(baseline.getName(), file.getAbsolutePath());
					fout.flush();
				}
			}
		}
	}

	/**
	 * Writes out the current state of the {@link IApiBaseline} as XML to the
	 * given output stream
	 *
	 * @param stream
	 * @throws CoreException
	 */
	private void writeBaselineDescription(IApiBaseline baseline, OutputStream stream) throws CoreException {
		String xml = getProfileXML(baseline);
		try {
			stream.write(xml.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			abort("Error writing pofile descrition", e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns an XML description of the given baseline.
	 *
	 * @param baseline the given API baseline
	 * @return XML string representation of the given baseline
	 * @throws CoreException if an exception occurs while retrieving the xml
	 *             string representation
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
		for (IApiComponent component : components) {
			Set<IApiComponent> allComponentSet = new HashSet<>();
			// if the baseline has multiple versions, persist all versions
			Set<IApiComponent> multipleComponents = baseline.getAllApiComponents(component.getSymbolicName());
			if (multipleComponents.isEmpty()) {
				// no multiple version - add the current component
				allComponentSet.add(component);
			} else {
				allComponentSet.addAll(multipleComponents);
			}
			for (Iterator<IApiComponent> iterator = allComponentSet.iterator(); iterator.hasNext();) {
				IApiComponent iApiComponent = iterator.next();
				if (!iApiComponent.isSystemComponent()) {
					celement = document.createElement(IApiXmlConstants.ELEMENT_APICOMPONENT);
					celement.setAttribute(IApiXmlConstants.ATTR_ID, iApiComponent.getSymbolicName());
					celement.setAttribute(IApiXmlConstants.ATTR_VERSION, iApiComponent.getVersion());
					celement.setAttribute(IApiXmlConstants.ATTR_LOCATION, new Path(iApiComponent.getLocation()).toPortableString());
					root.appendChild(celement);
				}
			}
			// clear the temporary hashset
			allComponentSet.clear();
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
		throw new CoreException(Status.error(message, e));
	}

	/**
	 * Restore a baseline from the given input stream (persisted baseline).
	 *
	 * @param baseline the given baseline to restore
	 * @param stream   the given input stream
	 * @throws CoreException if unable to restore the baseline
	 * @return restored baseline components or null if restore didn't work
	 */
	public IApiComponent[] readBaselineComponents(ApiBaseline baseline, InputStream stream) throws CoreException {
		long start = System.currentTimeMillis();
		DocumentBuilder parser = null;
		IApiComponent[] restored = null;
		try {
			parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
		} catch (ParserConfigurationException | FactoryConfigurationError e) {
			abort("Error restoring API baseline", e); //$NON-NLS-1$
		}
		try {
			Document document = parser.parse(stream);
			Element root = document.getDocumentElement();
			if (root.getNodeName().equals(IApiXmlConstants.ELEMENT_APIPROFILE)) {
				String baselineLocation = root.getAttribute(IApiXmlConstants.ATTR_LOCATION);
				if (baselineLocation != null && !baselineLocation.equals(Util.EMPTY_STRING)) {
					baseline.setLocation(Path.fromPortableString(baselineLocation).toOSString());
				}
				// un-pooled components
				NodeList children = root.getElementsByTagName(IApiXmlConstants.ELEMENT_APICOMPONENT);
				List<IApiComponent> components = new ArrayList<>();
				for (int j = 0; j < children.getLength(); j++) {
					Element componentNode = (Element) children.item(j);
					// this also contains components in pools, so don't process
					// them
					if (componentNode.getParentNode().equals(root)) {
						String location = componentNode.getAttribute(IApiXmlConstants.ATTR_LOCATION);
						IApiComponent component = ApiModelFactory.newApiComponent(baseline, Path.fromPortableString(location).toOSString());
						if (component != null) {
							components.add(component);
						}
					}
				}
				// pooled components - only for xml file with version <= 1
				// since version 2, pools have been removed
				children = root.getElementsByTagName(IApiXmlConstants.ELEMENT_POOL);
				IApiComponent component = null;
				for (int j = 0; j < children.getLength(); j++) {
					String location = ((Element) children.item(j)).getAttribute(IApiXmlConstants.ATTR_LOCATION);
					IPath poolPath = Path.fromPortableString(location);
					NodeList componentNodes = root.getElementsByTagName(IApiXmlConstants.ELEMENT_APICOMPONENT);
					for (int i = 0; i < componentNodes.getLength(); i++) {
						Element compElement = (Element) componentNodes.item(i);
						String id = compElement.getAttribute(IApiXmlConstants.ATTR_ID);
						String ver = compElement.getAttribute(IApiXmlConstants.ATTR_VERSION);
						StringBuilder name = new StringBuilder();
						name.append(id);
						name.append('_');
						name.append(ver);
						File file = poolPath.append(name.toString()).toFile();
						if (!file.exists()) {
							name.append(".jar"); //$NON-NLS-1$
							file = poolPath.append(name.toString()).toFile();
						}
						component = ApiModelFactory.newApiComponent(baseline, file.getAbsolutePath());
						if (component != null) {
							components.add(component);
						}
					}
				}
				restored = components.toArray(new IApiComponent[components.size()]);
			}
		} catch (IOException | SAXException e) {
			abort("Error restoring API baseline", e); //$NON-NLS-1$
		}
		if (ApiPlugin.DEBUG_BASELINE_MANAGER) {
			System.out.println("Time to restore a persisted baseline : " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return restored;
	}

	@Override
	public void saving(ISaveContext context) throws CoreException {
		if (!fNeedsSaving) {
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
	 * Cleans out all but the default baseline from the in-memory cache of
	 * baselines
	 */
	private void cleanStateCache() {
		if (baselinecache != null) {
			IApiBaseline baseline = null;
			for (Entry<String, IApiBaseline> entry : baselinecache.entrySet()) {
				baseline = entry.getValue();
				if (!baseline.getName().equals(defaultbaseline)) {
					baseline.dispose();
					hasinfos.remove(baseline.getName());
					// iter.remove();
				}
			}
		}
	}

	/**
	 * Returns if the given name is an existing baseline name
	 *
	 * @param name
	 * @return true if the given name is an existing baseline name, false
	 *         otherwise
	 */
	public boolean isExistingProfileName(String name) {
		if (baselinecache == null || name == null) {
			return false;
		}
		return baselinecache.containsKey(name);
	}

	/**
	 * Cleans up the manager
	 */
	public void stop() {
		try {
			Job.getJobManager().cancel(ApiAnalysisJob.class);
			if (baselinecache != null) {
				// we should first dispose all existing baselines
				for (IApiBaseline iApiBaseline : baselinecache.values()) {
					iApiBaseline.dispose();
				}
				baselinecache.clear();
			}
			synchronized (this) {
				if (workspacebaseline != null) {
					workspacebaseline.dispose();
				}
			}
			if (handlecache != null) {
				handlecache.clear();
			}
			if (!hasinfos.isEmpty()) {
				hasinfos.clear();
			}
			StubApiComponent.disposeAllCaches();
		} finally {
			if (ApiPlugin.isRunningInFramework()) {
				ApiPlugin.getDefault().removeSaveParticipant(this);
			}
		}
	}

	@Override
	public void doneSaving(ISaveContext context) {
		//
	}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {
		//
	}

	@Override
	public void rollback(ISaveContext context) {
		//
	}

	@Override
	public IApiBaseline getDefaultApiBaseline() {
		initializeStateCache();
		String defbaseline = defaultbaseline;
		if (defbaseline == null) {
			return null;
		}
		return baselinecache.get(defbaseline);
	}

	@Override
	public void setDefaultApiBaseline(String name) {
		fNeedsSaving = true;
		defaultbaseline = name;
	}

	@Override
	public IApiBaseline getWorkspaceBaseline() {
		if (!ApiPlugin.isRunningInFramework()) {
			return null;
		}
		if (this.workspacebaseline == null) {
			try {
				synchronized (this) {
					if (this.workspacebaseline == null) {
						this.workspacebaseline = createWorkspaceBaseline();
					}
				}
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
		return this.workspacebaseline;
	}

	/**
	 * Disposes the workspace baseline such that a new one will be created on
	 * the next request.
	 */
	public void disposeWorkspaceBaseline() {
		if (workspacebaseline == null) {
			return;
		}
		Job.getJobManager().cancel(ApiAnalysisJob.class);
		IApiBaseline oldBaseline = null;
		synchronized (this) {
			if (workspacebaseline != null) {
				if (ApiPlugin.DEBUG_BASELINE_MANAGER) {
					System.out.println("disposing workspace baseline"); //$NON-NLS-1$
				}
				oldBaseline = workspacebaseline;
				StubApiComponent.disposeAllCaches();
				workspacebaseline = null;
			}
		}
		if (oldBaseline != null) {
			oldBaseline.dispose();
		}
	}

	/**
	 * Creates a workspace {@link IApiBaseline}
	 *
	 * @return a new workspace {@link IApiBaseline} or <code>null</code>
	 */
	private IApiBaseline createWorkspaceBaseline() throws CoreException {
		long time = System.currentTimeMillis();
		IApiBaseline baseline = null;
		try {
			baseline = new WorkspaceBaseline();
			// populate it with only projects that are API aware
			List<IPluginModelBase> models = Arrays.asList(PluginRegistry.getWorkspaceModels());
			Set<BundleDescription> bundles = DependencyManager.getSelfAndDependencies(models);
			List<IApiComponent> componentsList = new ArrayList<>(bundles.size());
			for (BundleDescription bundle : bundles) {
				String id = bundle.getSymbolicName();
				ModelEntry modelEntry = PluginRegistry.findEntry(id);
				IPluginModelBase[] workspaceModels = modelEntry.getWorkspaceModels();
				IApiComponent apiComponent = null;
				if (workspaceModels.length == 0) {
					// external model - reexported case
					IPluginModelBase externalModel = PluginRegistry.findModel(id);
					if (externalModel != null) {
						try {
							apiComponent = ApiModelFactory.newApiComponent(baseline, externalModel);
							if (apiComponent != null) {
								componentsList.add(apiComponent);
							}
						} catch (CoreException e) {
							ApiPlugin.log(e);
						}
					}
					continue;
				}
				for (IPluginModelBase iPluginModelBase : workspaceModels) {
					try {
						apiComponent = ApiModelFactory.newApiComponent(baseline, iPluginModelBase);
						if (apiComponent != null) {
							componentsList.add(apiComponent);
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
				}
			}
			baseline.addApiComponents(componentsList.toArray(new IApiComponent[componentsList.size()]));
		} finally {
			if (ApiPlugin.DEBUG_BASELINE_MANAGER) {
				System.out.println("Time to create a workspace baseline : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return baseline;
	}

	@Override
	public IApiComponent getWorkspaceComponent(String symbolicName) {
		IApiBaseline baseline = getWorkspaceBaseline();
		if (baseline != null) {
			return baseline.getApiComponent(symbolicName);
		}
		return null;
	}
}
