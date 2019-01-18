/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.util.CoreUtility;

/**
 * This class manages the ability of external plug-ins in the model manager to
 * take part in the Java search. It manages a proxy Java projects and for each
 * external plug-in added to Java search, it adds its Java libraries as external
 * JARs to the proxy project. This makes the libraries visible to the Java
 * model, and they can take part in various Java searches.
 */
public class SearchablePluginsManager implements IFileAdapterFactory, IPluginModelListener {

	private static final String PROXY_FILE_NAME = ".searchable"; //$NON-NLS-1$
	public static final String PROXY_PROJECT_NAME = "External Plug-in Libraries"; //$NON-NLS-1$
	private static final String KEY = "searchablePlugins"; //$NON-NLS-1$

	private final Listener fElementListener;
	private Set<String> fPluginIdSet;
	private ArrayList<IPluginModelListener> fListeners;

	class Listener implements IElementChangedListener {
		@Override
		public void elementChanged(ElementChangedEvent e) {
			if (e.getType() == ElementChangedEvent.POST_CHANGE) {
				handleDelta(e.getDelta());
			}
		}

		private boolean handleDelta(IJavaElementDelta delta) {
			Object element = delta.getElement();
			if (element instanceof IJavaModel) {
				IJavaElementDelta[] projectDeltas = delta.getAffectedChildren();
				for (IJavaElementDelta projectDelta : projectDeltas) {
					if (handleDelta(projectDelta)) {
						break;
					}
				}
				return true;
			}
			if (element instanceof IJavaProject) {
				IJavaProject project = (IJavaProject) element;
				if (project.getElementName().equals(PROXY_PROJECT_NAME)) {
					if (delta.getKind() == IJavaElementDelta.REMOVED) {
						synchronized (fPluginIdSet) {
							fPluginIdSet.clear();
						}
					} else if (delta.getKind() == IJavaElementDelta.ADDED) {
						// We may be getting a queued delta from when the manager was initialized, ignore unless we don't already have data
						boolean emptyIds;
						synchronized (fPluginIdSet) {
							emptyIds = fPluginIdSet.isEmpty();
						}
						if (emptyIds) {
							// Something other than the manager created the project, check if it has a .searchable file to load from
							Set<String> ids = loadStates();
							synchronized (fPluginIdSet) {
								fPluginIdSet = ids;
							}
						}
					}
				}
				return true;
			}
			return false;
		}
	}

	public SearchablePluginsManager() {
		fPluginIdSet = loadStates();
		fElementListener = new Listener();
		JavaCore.addElementChangedListener(fElementListener);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
	}

	private static Set<String> loadStates() {
		Set<String> set = new TreeSet<>();
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		IProject project = root.getProject(PROXY_PROJECT_NAME);
		try {
			if (project.exists() && project.isOpen()) {
				IFile proxyFile = project.getFile(PROXY_FILE_NAME);
				if (proxyFile.exists()) {
					Properties properties = new Properties();
					try (InputStream stream = proxyFile.getContents(true)) {
						properties.load(stream);
					}
					String value = properties.getProperty(KEY);
					if (value != null) {
						StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
						while (stok.hasMoreTokens()) {
							set.add(stok.nextToken());
						}
					}
				}
			}
		} catch (IOException e) {
		} catch (CoreException e) {
		}
		return set;
	}

	public IJavaProject getProxyProject() {
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		IProject project = root.getProject(PROXY_PROJECT_NAME);
		try {
			if (project.exists() && project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
				return JavaCore.create(project);
			}

		} catch (CoreException e) {
		}
		return null;
	}

	public void shutdown() {
		// remove listener
		JavaCore.removeElementChangedListener(fElementListener);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		if (fListeners != null) {
			fListeners.clear();
		}
	}

	public IClasspathEntry[] computeContainerClasspathEntries() throws CoreException {
		ArrayList<IClasspathEntry> result = new ArrayList<>();

		IPluginModelBase[] wModels = PluginRegistry.getWorkspaceModels();
		for (IPluginModelBase model : wModels) {
			IProject project = model.getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				result.add(JavaCore.newProjectEntry(project.getFullPath()));
			}
		}
		List<String> plugins;
		synchronized (fPluginIdSet) {
			plugins = new ArrayList<>(fPluginIdSet);
		}
		for (String id : plugins) {
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				boolean addModel = true;
				wModels = entry.getWorkspaceModels();
				for (IPluginModelBase model : wModels) {
					IProject project = model.getUnderlyingResource().getProject();
					if (project.hasNature(JavaCore.NATURE_ID)) {
						addModel = false;
					}
				}
				if (!addModel) {
					continue;
				}
				IPluginModelBase[] models = entry.getExternalModels();
				for (IPluginModelBase model : models) {
					if (model.isEnabled()) {
						ClasspathUtilCore.addLibraries(model, result);
					}
				}
			}
		}

		if (result.size() > 1) {
			// sort
			Map<String, IClasspathEntry> map = new TreeMap<>();
			for (int i = 0; i < result.size(); i++) {
				IClasspathEntry entry = result.get(i);
				String key = entry.getPath().lastSegment().toString();
				while (map.containsKey(key)) {
					key += i;
				}
				map.put(key, entry);
			}
			return map.values().toArray(new IClasspathEntry[map.size()]);
		}
		return result.toArray(new IClasspathEntry[result.size()]);
	}

	@Override
	public Object createAdapterChild(FileAdapter parent, File file) {
		if (!file.isDirectory()) {
			if (file.isFile()) {
				IPackageFragmentRoot root = findPackageFragmentRoot(new Path(file.getAbsolutePath()));
				if (root != null) {
					return root;
				}
			}
		}
		return new FileAdapter(parent, file, this);
	}

	private IPackageFragmentRoot findPackageFragmentRoot(IPath jarPath) {
		IJavaProject jProject = getProxyProject();
		if (jProject != null) {
			try {
				IPackageFragmentRoot[] roots = jProject.getAllPackageFragmentRoots();
				for (IPackageFragmentRoot root : roots) {
					IPath path = root.getPath();
					if (path.equals(jarPath)) {
						return root;
					}

				}
			} catch (JavaModelException e) {
			}
		}

		// Find in other plug-in (and fragments) projects dependencies
		IPluginModelBase[] pluginModels = PluginRegistry.getWorkspaceModels();
		for (IPluginModelBase model : pluginModels) {
			IProject project = model.getUnderlyingResource().getProject();
			IJavaProject javaProject = JavaCore.create(project);
			try {
				IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();
				for (IPackageFragmentRoot root : roots) {
					IPath path = root.getPath();
					if (path.equals(jarPath)) {
						return root;
					}
				}
			} catch (JavaModelException e) {
			}
		}

		return null;
	}

	private void checkForProxyProject() {
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		try {
			IProject project = root.getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
			if (!project.exists()) {
				createProxyProject(new NullProgressMonitor());
			}
		} catch (CoreException e) {
		}
	}

	public void addToJavaSearch(IPluginModelBase[] models) {
		checkForProxyProject();
		PluginModelDelta delta = new PluginModelDelta();
		Set<String> toAdd = new HashSet<>();
		for (IPluginModelBase model : models) {
			String id = model.getPluginBase().getId();
			if (!isInJavaSearch(id) && !toAdd.contains(id)) {
				toAdd.add(id);
				ModelEntry entry = PluginRegistry.findEntry(id);
				if (entry != null) {
					delta.addEntry(entry, PluginModelDelta.CHANGED);
				}
			}
		}
		if (!toAdd.isEmpty()) {
			synchronized (fPluginIdSet) {
				fPluginIdSet.addAll(toAdd);
			}
			resetContainer();
			fireDelta(delta);
		}
	}

	public void removeFromJavaSearch(IPluginModelBase[] models) {
		PluginModelDelta delta = new PluginModelDelta();
		Set<String> toRemove = new HashSet<>();
		for (IPluginModelBase model : models) {
			String id = model.getPluginBase().getId();
			if (isInJavaSearch(id)) {
				toRemove.add(id);
				ModelEntry entry = PluginRegistry.findEntry(id);
				if (entry != null) {
					delta.addEntry(entry, PluginModelDelta.CHANGED);
				}
			}
		}
		if (!toRemove.isEmpty()) {
			synchronized (fPluginIdSet) {
				fPluginIdSet.removeAll(toRemove);
			}
			resetContainer();
			fireDelta(delta);
		}
	}

	public void removeAllFromJavaSearch() {
		Set<String> oldIds;
		synchronized (fPluginIdSet) {
			oldIds = fPluginIdSet;
			fPluginIdSet = new TreeSet<>();
		}
		if (!oldIds.isEmpty()) {
			PluginModelDelta delta = new PluginModelDelta();
			for (String id : oldIds) {
				ModelEntry entry = PluginRegistry.findEntry(id);
				if (entry != null) {
					delta.addEntry(entry, PluginModelDelta.CHANGED);
				}
			}
			resetContainer();
			fireDelta(delta);
		}
	}

	public boolean isInJavaSearch(String symbolicName) {
		synchronized (fPluginIdSet) {
			return fPluginIdSet.contains(symbolicName);
		}
	}

	private void resetContainer() {
		IJavaProject jProject = getProxyProject();
		try {
			if (jProject != null) {
				JavaCore.setClasspathContainer(PDECore.JAVA_SEARCH_CONTAINER_PATH, new IJavaProject[] {jProject}, new IClasspathContainer[] {new ExternalJavaSearchClasspathContainer()}, null);
				saveStates();
			}
		} catch (JavaModelException e) {
		}
	}

	@Override
	public void modelsChanged(PluginModelDelta delta) {
		ModelEntry[] entries = delta.getRemovedEntries();
		synchronized (fPluginIdSet) {
			for (ModelEntry entry : entries) {
				if (fPluginIdSet.contains(entry.getId())) {
					fPluginIdSet.remove(entry.getId());
				}
			}
		}
		resetContainer();
	}

	private void fireDelta(PluginModelDelta delta) {
		if (fListeners != null) {
			for (int i = 0; i < fListeners.size(); i++) {
				fListeners.get(i).modelsChanged(delta);
			}
		}
	}

	public void addPluginModelListener(IPluginModelListener listener) {
		if (fListeners == null) {
			fListeners = new ArrayList<>();
		}
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}

	public void removePluginModelListener(IPluginModelListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}

	private void saveStates() {
		// persist state
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		IProject project = root.getProject(PROXY_PROJECT_NAME);
		if (project.exists() && project.isOpen()) {
			// modify the .searchable file only if there is any change
			Set<String> loadedStates = loadStates();
			String propertyToSave;
			synchronized (fPluginIdSet) {
				if (loadedStates.equals(fPluginIdSet)) {
					return;
				}
				propertyToSave = fPluginIdSet.stream().collect(Collectors.joining(",")); //$NON-NLS-1$
			}
			IFile file = project.getFile(PROXY_FILE_NAME);
			Properties properties = new Properties();
			properties.setProperty(KEY, propertyToSave);
			try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
				properties.store(outStream, ""); //$NON-NLS-1$
				outStream.flush();
				outStream.close();
				try (ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray())) {
					if (file.exists()) {
						file.setContents(inStream, true, false, new NullProgressMonitor());
					} else {
						file.create(inStream, true, new NullProgressMonitor());
					}
				}
			} catch (IOException e) {
				PDECore.log(e);
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
	}

	public IProject createProxyProject(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		IProject project = root.getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
		if (project.exists()) {
			if (!project.isOpen()) {
				project.open(monitor);
			}
			return project;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(PDECoreMessages.SearchablePluginsManager_createProjectTaskName, SearchablePluginsManager.PROXY_PROJECT_NAME), 5);

		project.create(subMonitor.split(1));
		project.open(subMonitor.split(1));
		CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, subMonitor.split(1));
		IJavaProject jProject = JavaCore.create(project);
		jProject.setOutputLocation(project.getFullPath(), subMonitor.split(1));
		computeClasspath(jProject, subMonitor.split(1));
		return project;
	}

	private void computeClasspath(IJavaProject project, IProgressMonitor monitor) {
		IClasspathEntry[] classpath = new IClasspathEntry[2];
		classpath[0] = JavaCore.newContainerEntry(JavaRuntime.newDefaultJREContainerPath());
		classpath[1] = JavaCore.newContainerEntry(PDECore.JAVA_SEARCH_CONTAINER_PATH);
		try {
			project.setRawClasspath(classpath, monitor);
		} catch (JavaModelException e) {
		}
	}

}
