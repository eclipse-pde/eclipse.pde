/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * This class manages the ability of external plug-ins
 * in the model manager to take part in the Java search.
 * It manages a proxy Java projects and for each 
 * external plug-in added to Java search, it adds its
 * Java libraries as external JARs to the proxy project.
 * This makes the libraries visible to the Java model,
 * and they can take part in various Java searches.
 */
public class SearchablePluginsManager implements IFileAdapterFactory {
	private IJavaProject proxyProject;
	private PluginModelManager manager;
	private static final String PROXY_FILE_NAME = ".searchable";
	private static final String PROXY_PROJECT_NAME =
		"External Plug-in Libraries";
	private static final String KEY = "searchablePlugins";

	private Listener elementListener;

	class Listener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent e) {
			if (e.getType() == ElementChangedEvent.POST_CHANGE) {
				handleDelta(e.getDelta());
			}
		}
	}

	private String getProxyProjectName() {
		return PROXY_PROJECT_NAME;
	}

	public SearchablePluginsManager(PluginModelManager manager) {
		this.manager = manager;
		elementListener = new Listener();
	}

	public void initialize() {
		initializeProxyProject();
		if (proxyProject == null)
			return;
		IProject project = proxyProject.getProject();
		IFile proxyFile = project.getFile(PROXY_FILE_NAME);
		initializeStates(proxyFile);
		JavaCore.addElementChangedListener(elementListener);
	}

	public void shutdown() {
		JavaCore.removeElementChangedListener(elementListener);
	}

	private void initializeProxyProject() {
		IProject project =
			PDECore.getWorkspace().getRoot().getProject(getProxyProjectName());
		if (project == null)
			return;
		proxyProject = JavaCore.create(project);
	}

	private void initializeStates(IFile proxyFile) {
		if (proxyFile.exists() == false)
			return;
		Properties properties = new Properties();
		try {
			InputStream stream = proxyFile.getContents(true);
			properties.load(stream);
			stream.close();
			String value = properties.getProperty(KEY);
			if (value == null)
				return;
			ArrayList ids = new ArrayList();
			StringTokenizer stok = new StringTokenizer(value, ",");
			for (; stok.hasMoreTokens();) {
				ids.add(stok.nextToken());
			}
			initializeStates(ids);
		} catch (IOException e) {
		} catch (CoreException e) {
		}
	}
	private void initializeStates(ArrayList ids) {
		for (int i = 0; i < ids.size(); i++) {
			String id = (String) ids.get(i);
			ModelEntry entry = manager.findEntry(id, null, 0);
			if (entry != null)
				entry.setInJavaSearch(true);
		}
	}

	public void persistStates(boolean useContainers, IProgressMonitor monitor)
		throws CoreException {
		ModelEntry[] entries = manager.getEntries();
		StringBuffer buffer = new StringBuffer();

		monitor.beginTask("Saving...", 3);

		int counter = 0;

		for (int i = 0; i < entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()) {
				if (counter++ > 0)
					buffer.append(",");
				buffer.append(entry.getId());
			}
		}
		createProxyProject(monitor);
		if (proxyProject == null)
			return;
		monitor.worked(1);
		IFile file = proxyProject.getProject().getFile(PROXY_FILE_NAME);
		persistStates(
			file,
			buffer.toString(),
			new SubProgressMonitor(monitor, 1));
		monitor.worked(1);
		computeClasspath(
			entries,
			useContainers,
			new SubProgressMonitor(monitor, 1));
		monitor.worked(1);
	}

	private void computeClasspath(
		ModelEntry[] entries,
		boolean useContainers,
		IProgressMonitor monitor)
		throws CoreException {
		Vector result = new Vector();

		for (int i = 0; i < entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch() == false)
				continue;
			if (entry.getWorkspaceModel() != null)
				continue;
			IPluginModelBase model = entries[i].getExternalModel();
			if (model == null)
				continue;
			Vector modelResult = new Vector();
			ClasspathUtilCore.addLibraries(
				model,
				false,
				!useContainers,
				modelResult);
			addUniqueEntries(result, modelResult);
		}
		IClasspathEntry[] classpathEntries =
			(IClasspathEntry[]) result.toArray(
				new IClasspathEntry[result.size()]);
		try {
			proxyProject.setRawClasspath(classpathEntries, monitor);
		} catch (JavaModelException e) {
			throwCoreException(e);
		}
	}
	
	private void addUniqueEntries(Vector result, Vector localResult) {
		Vector resultCopy = (Vector)result.clone();
		for (int i=0; i<localResult.size(); i++) {
			IClasspathEntry localEntry = (IClasspathEntry)localResult.get(i);
			boolean duplicate=false;
			for (int j=0; j<resultCopy.size(); j++) {
				IClasspathEntry entry = (IClasspathEntry)resultCopy.get(j);
				if (entry.getEntryKind()==localEntry.getEntryKind() &&
				entry.getContentKind()==localEntry.getContentKind() &&
				entry.getPath().equals(localEntry.getPath())) {
					duplicate=true;
					break;
				}
			}
			if (!duplicate) result.add(localEntry);
		}
	}

	private void persistStates(
		IFile file,
		String value,
		IProgressMonitor monitor)
		throws CoreException {
		Properties properties = new Properties();
		properties.setProperty(KEY, value);
		try {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			properties.store(outStream, "");
			outStream.flush();
			outStream.close();
			ByteArrayInputStream inStream =
				new ByteArrayInputStream(outStream.toByteArray());
			if (file.exists())
				file.setContents(inStream, true, false, monitor);
			else
				file.create(inStream, true, monitor);
			inStream.close();
		} catch (IOException e) {
			throwCoreException(e);
		}
	}

	private void throwCoreException(Throwable e) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		throw new CoreException(status);
	}

	private void createProxyProject(IProgressMonitor monitor) {
		IProject project =
			PDECore.getWorkspace().getRoot().getProject(getProxyProjectName());
		if (project.exists())
			return;

		try {
			project.create(monitor);
			project.open(monitor);

			CoreUtility.addNatureToProject(
				project,
				JavaCore.NATURE_ID,
				monitor);
			proxyProject = JavaCore.create(project);
			proxyProject.setOutputLocation(project.getFullPath(), monitor);
		} catch (CoreException e) {
		}
	}

	private boolean handleDelta(IJavaElementDelta delta) {
		Object element = delta.getElement();
		if (element instanceof IJavaModel) {
			IJavaElementDelta[] projectDeltas = delta.getAffectedChildren();
			for (int i = 0; i < projectDeltas.length; i++) {
				if (handleDelta(projectDeltas[i]))
					break;
			}
			return true;
		}
		if (delta.getElement() instanceof IJavaProject) {
			IJavaProject project = (IJavaProject) delta.getElement();

			if (project.equals(proxyProject)
				&& delta.getKind() == IJavaElementDelta.REMOVED) {
				manager.searchablePluginsRemoved();
				proxyProject = null;
				return true;
			}
		}
		return false;
	}

	public Object createAdapterChild(FileAdapter parent, File file) {
		if (file.isDirectory() == false) {
			String name = file.getName().toLowerCase();
			if (name.endsWith(".jar")) {
				IPackageFragmentRoot root =
					findPackageFragmentRoot(file.getAbsolutePath());
				if (root != null)
					return root;
			}
		}
		return new FileAdapter(parent, file, this);
	}

	private IPackageFragmentRoot findPackageFragmentRoot(String absolutePath) {
		if (proxyProject == null)
			return null;
		IPath jarPath = new Path(absolutePath);
		try {
			IPackageFragmentRoot[] roots =
				proxyProject.getAllPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				IPath path = root.getPath();
				if (path.equals(jarPath))
					return root;

			}
		} catch (JavaModelException e) {
		}
		return null;
	}
}