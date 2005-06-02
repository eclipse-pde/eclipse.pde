/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;

/**
 * This class manages the ability of external plug-ins in the model manager to
 * take part in the Java search. It manages a proxy Java projects and for each
 * external plug-in added to Java search, it adds its Java libraries as external
 * JARs to the proxy project. This makes the libraries visible to the Java
 * model, and they can take part in various Java searches.
 */
public class SearchablePluginsManager implements IFileAdapterFactory {
	private IJavaProject proxyProject;
	private PluginModelManager manager;
	private static final String PROXY_FILE_NAME = ".searchable"; //$NON-NLS-1$
	private static final String PROXY_PROJECT_NAME = "External Plug-in Libraries"; //$NON-NLS-1$
	private static final String KEY = "searchablePlugins"; //$NON-NLS-1$

	private Listener elementListener;
	private ExternalJavaSearchClasspathContainer classpathContainer;

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
		IProject project = PDECore.getWorkspace().getRoot().getProject(
				getProxyProjectName());
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
			StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
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
			ModelEntry entry = manager.findEntry(id);
			if (entry != null)
				entry.setInJavaSearch(true);
		}
	}

	public void persistStates(IProgressMonitor monitor) throws CoreException {
		ModelEntry[] entries = manager.getEntries();
		StringBuffer buffer = new StringBuffer();

		monitor.beginTask(PDECoreMessages.SearchablePluginsManager_saving, 3); //$NON-NLS-1$

		int counter = 0;

		for (int i = 0; i < entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()) {
				if (counter++ > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append(entry.getId());
			}
		}
		createProxyProject(monitor);
		if (proxyProject == null)
			return;
		monitor.worked(1);
		IFile file = proxyProject.getProject().getFile(PROXY_FILE_NAME);
		persistStates(file, buffer.toString(), new SubProgressMonitor(monitor,
				1));
		updateClasspathContainer();
	}
	
	public void updateClasspathContainer() {
		if (proxyProject==null) return;
		try {
			updateClasspathContainer(proxyProject);
		}
		catch (CoreException e) {
			PDECore.logException(e);
		}
	}

	public void updateClasspathContainer(IJavaProject project)
			throws CoreException {
		IJavaProject[] javaProjects = new IJavaProject[]{project};
		IClasspathContainer[] containers = new IClasspathContainer[]{getClasspathContainer()};
		IPath path = new Path(PDECore.JAVA_SEARCH_CONTAINER_ID);
		try {
			getClasspathContainer().reset();
			JavaCore
					.setClasspathContainer(path, javaProjects, containers, null);
		} catch (OperationCanceledException e) {
			getClasspathContainer().reset();
			throw e;
		}
	}

	public ExternalJavaSearchClasspathContainer getClasspathContainer() {
		/*
		if (classpathContainer == null)
		*/
		classpathContainer = new ExternalJavaSearchClasspathContainer(this);
		return classpathContainer;
	}

	public IClasspathEntry[] computeContainerClasspathEntries()
			throws CoreException {
		ArrayList result = new ArrayList();

		ModelEntry[] entries = manager.getEntries();
		for (int i = 0; i < entries.length; i++) {
			ModelEntry entry = entries[i];
			ArrayList entryResult = new ArrayList();			
			if (entry.getWorkspaceModel() != null) {
				// We used to skip workspace models before.
				// If we add them as references,
				// scoped searches will work according
				// to bug 52667
				IProject eproject = entry.getWorkspaceModel().getUnderlyingResource().getProject();
				if (eproject.hasNature(JavaCore.NATURE_ID)) {
					IClasspathEntry pentry =
						JavaCore.newProjectEntry(eproject.getFullPath());
					entryResult.add(pentry);
				}
			}
			else {
				if (entry.isInJavaSearch() == false)
					continue;				
				IPluginModelBase model = entry.getExternalModel();
				if (model == null)
					continue;
				ClasspathUtilCore.addLibraries(model, entryResult);
			}
			addUniqueEntries(result, entryResult);			
		}
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result
				.size()]);
	}

	private void computeClasspath(ModelEntry[] entries, IJavaProject project,
			IProgressMonitor monitor) throws CoreException {
		ArrayList list = new ArrayList();

		//The project has the dynamic classpath container for
		// searchable entries + JRE
		list.add(JavaCore.newContainerEntry(new Path(
				PDECore.JAVA_SEARCH_CONTAINER_ID)));
		list.add(ClasspathUtilCore.createJREEntry());
		try {
			project.setRawClasspath((IClasspathEntry[]) list
					.toArray(new IClasspathEntry[list.size()]), monitor);
		} catch (JavaModelException e) {
			throwCoreException(e);
		}
	}

	private void addUniqueEntries(ArrayList result, ArrayList localResult) {
		ArrayList resultCopy = (ArrayList) result.clone();
		for (int i = 0; i < localResult.size(); i++) {
			IClasspathEntry localEntry = (IClasspathEntry) localResult.get(i);
			boolean duplicate = false;
			for (int j = 0; j < resultCopy.size(); j++) {
				IClasspathEntry entry = (IClasspathEntry) resultCopy.get(j);
				if (entry.getEntryKind() == localEntry.getEntryKind()
						&& entry.getContentKind() == localEntry
								.getContentKind()
						&& entry.getPath().equals(localEntry.getPath())) {
					duplicate = true;
					break;
				}
			}
			if (!duplicate)
				result.add(localEntry);
		}
	}

	private void persistStates(IFile file, String value,
			IProgressMonitor monitor) throws CoreException {
		Properties properties = new Properties();
		properties.setProperty(KEY, value);
		try {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			properties.store(outStream, ""); //$NON-NLS-1$
			outStream.flush();
			outStream.close();
			ByteArrayInputStream inStream = new ByteArrayInputStream(outStream
					.toByteArray());
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
		IStatus status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID,
				IStatus.OK, e.getMessage(), e);
		throw new CoreException(status);
	}

	private void createProxyProject(IProgressMonitor monitor) {
		IProject project = PDECore.getWorkspace().getRoot().getProject(
				getProxyProjectName());
		if (project.exists())
			return;
		monitor.beginTask("", 5); //$NON-NLS-1$

		try {
			project.create(new SubProgressMonitor(monitor, 1));
			project.open(new SubProgressMonitor(monitor, 1));

			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID,
					new SubProgressMonitor(monitor, 1));
			proxyProject = JavaCore.create(project);
			proxyProject.setOutputLocation(project.getFullPath(),
					new SubProgressMonitor(monitor, 1));
			computeClasspath(manager.getEntries(), proxyProject,
					new SubProgressMonitor(monitor, 1));
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
			String name = file.getName().toLowerCase(Locale.ENGLISH);
			if (name.endsWith(".jar")) { //$NON-NLS-1$
				IPackageFragmentRoot root = findPackageFragmentRoot(file
						.getAbsolutePath());
				if (root != null)
					return root;
			}
		}
		return new FileAdapter(parent, file, this);
	}

	private IPackageFragmentRoot findPackageFragmentRoot(String absolutePath) {
		IPath jarPath = new Path(absolutePath);
		// Find in proxy project jars
		if (proxyProject != null) {
			try {
				IPackageFragmentRoot[] roots = proxyProject
						.getAllPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					IPackageFragmentRoot root = roots[i];
					IPath path = root.getPath();
					if (path.equals(jarPath))
						return root;

				}
			} catch (JavaModelException e) {
			}
		}
		// Find in other plugin (and fragments) projects dependencies
		IPluginModelBase[] pluginModels = PDECore.getDefault()
				.getModelManager().getWorkspaceModels();
		for (int i = 0; i < pluginModels.length; i++) {
			IProject project = pluginModels[i].getUnderlyingResource()
					.getProject();
			IJavaProject javaProject = JavaCore.create(project);
			try {
				IPackageFragmentRoot[] roots = javaProject
						.getAllPackageFragmentRoots();
				for (int j = 0; j < roots.length; j++) {
					IPackageFragmentRoot root = roots[j];
					IPath path = root.getPath();
					if (path.equals(jarPath))
						return root;
				}
			} catch (JavaModelException e) {
			}
		}

		return null;
	}
	
	public IJavaProject getProxyProject() {
		return proxyProject;
	}
}
