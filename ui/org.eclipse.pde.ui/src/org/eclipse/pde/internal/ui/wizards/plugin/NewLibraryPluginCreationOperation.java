/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 248852, bug 247553
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.converter.PluginConverter;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.search.dependencies.AddNewBinaryDependenciesOperation;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class NewLibraryPluginCreationOperation extends NewProjectCreationOperation {

	private LibraryPluginFieldData fData;

	public NewLibraryPluginCreationOperation(LibraryPluginFieldData data, IProjectProvider provider, IPluginContentWizard contentWizard) {
		super(data, provider, contentWizard);
		fData = data;
	}

	private void addJar(File jarFile, IProject project, IProgressMonitor monitor) throws CoreException {
		String jarName = jarFile.getName();
		IFile file = project.getFile(jarName);
		monitor.subTask(NLS.bind(PDEUIMessages.NewProjectCreationOperation_copyingJar, jarName));
		InputStream in = null;
		try {
			in = new FileInputStream(jarFile);
			file.create(in, true, monitor);
		} catch (FileNotFoundException fnfe) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
				}
			}
		}
	}

	private void adjustExportRoot(IProject project, IBundle bundle) throws CoreException {
		IResource[] resources = project.members(false);
		for (int j = 0; j < resources.length; j++) {
			if (resources[j] instanceof IFile) {
				if (".project".equals(resources[j].getName()) //$NON-NLS-1$
						|| ".classpath".equals(resources[j] //$NON-NLS-1$
								.getName()) || ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR.equals(resources[j].getName()) || ICoreConstants.BUILD_FILENAME_DESCRIPTOR.equals(resources[j].getName())) {
					continue;
				}
				// resource at the root, export root
				return;
			}
		}
		removeExportRoot(bundle);
	}

	protected void adjustManifests(IProgressMonitor monitor, IProject project, IPluginBase base) throws CoreException {
		super.adjustManifests(monitor, project, base);
		int units = fData.doFindDependencies() ? 4 : 2;
		units += fData.isUpdateReferences() ? 1 : 0;
		monitor.beginTask(new String(), units);
		IBundle bundle = (base instanceof BundlePluginBase) ? ((BundlePluginBase) base).getBundle() : null;
		if (bundle != null) {
			adjustExportRoot(project, bundle);
			monitor.worked(1);
			addExportedPackages(project, bundle);
			monitor.worked(1);
			if (fData.doFindDependencies()) {
				addDependencies(project, base.getModel(), new SubProgressMonitor(monitor, 2));
			}
			if (fData.isUpdateReferences()) {
				updateReferences(monitor, project);
				monitor.worked(1);
			}
		}
		monitor.done();
	}

	protected void updateReferences(IProgressMonitor monitor, IProject project) throws JavaModelException {
		IJavaProject currentProject = JavaCore.create(project);
		IPluginModelBase[] pluginstoUpdate = fData.getPluginsToUpdate();
		for (int i = 0; i < pluginstoUpdate.length; ++i) {
			IProject proj = pluginstoUpdate[i].getUnderlyingResource().getProject();
			if (currentProject.getProject().equals(proj))
				continue;
			IJavaProject javaProject = JavaCore.create(proj);
			IClasspathEntry[] cp = javaProject.getRawClasspath();
			IClasspathEntry[] updated = getUpdatedClasspath(cp, currentProject);
			if (updated != null) {
				javaProject.setRawClasspath(updated, monitor);
			}
			try {
				updateRequiredPlugins(javaProject, monitor, pluginstoUpdate[i]);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}

	private static void updateRequiredPlugins(IJavaProject javaProject, IProgressMonitor monitor, IPluginModelBase model) throws CoreException {
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		List classpath = new ArrayList();
		List requiredProjects = new ArrayList();
		for (int i = 0; i < entries.length; i++) {
			if (isPluginProjectEntry(entries[i])) {
				requiredProjects.add(entries[i]);
			} else {
				classpath.add(entries[i]);
			}
		}
		if (requiredProjects.size() <= 0)
			return;
		IFile file = PDEProject.getManifest(javaProject.getProject());
		try {
			// TODO format manifest
			Manifest manifest = new Manifest(file.getContents());
			String value = manifest.getMainAttributes().getValue(Constants.REQUIRE_BUNDLE);
			StringBuffer sb = value != null ? new StringBuffer(value) : new StringBuffer();
			if (sb.length() > 0)
				sb.append(","); //$NON-NLS-1$
			for (int i = 0; i < requiredProjects.size(); i++) {
				IClasspathEntry entry = (IClasspathEntry) requiredProjects.get(i);
				if (i > 0)
					sb.append(","); //$NON-NLS-1$
				sb.append(entry.getPath().segment(0));
				if (entry.isExported())
					sb.append(";visibility:=reexport"); // TODO is there a //$NON-NLS-1$
				// constant?
			}
			manifest.getMainAttributes().putValue(Constants.REQUIRE_BUNDLE, sb.toString());
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			manifest.write(content);
			file.setContents(new ByteArrayInputStream(content.toByteArray()), true, false, monitor);
			// now update .classpath
			javaProject.setRawClasspath((IClasspathEntry[]) classpath.toArray(new IClasspathEntry[classpath.size()]), monitor);
//			ClasspathComputer.setClasspath(javaProject.getProject(), model);
		} catch (IOException e) {
		} catch (CoreException e) {
		}
	}

	private static boolean isPluginProjectEntry(IClasspathEntry entry) {
		if (IClasspathEntry.CPE_PROJECT != entry.getEntryKind())
			return false;
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject other = workspaceRoot.getProject(entry.getPath().segment(0));
		if (!PDE.hasPluginNature(other))
			return false;
		if (PDEProject.getFragmentXml(other).exists())
			return false;
		try {
			InputStream is = PDEProject.getManifest(other).getContents();
			try {
				Manifest mf = new Manifest(is);
				if (mf.getMainAttributes().getValue(Constants.FRAGMENT_HOST) != null)
					return false;
			} finally {
				is.close();
			}
		} catch (IOException e) {
			// assume "not a fragment"
		} catch (CoreException e) {
			// assume "not a fragment"
		}
		return true;
	}

	/**
	 * @return updated classpath or null if there were no changes
	 */
	private IClasspathEntry[] getUpdatedClasspath(IClasspathEntry[] cp, IJavaProject currentProject) {
		boolean exposed = false;
		int refIndex = -1;
		List result = new ArrayList();
		Set manifests = new HashSet();
		for (int i = 0; i < fData.getLibraryPaths().length; ++i) {
			try {
				manifests.add(new JarFile(fData.getLibraryPaths()[i]).getManifest());
			} catch (IOException e) {
				PDEPlugin.log(e);
			}
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (int i = 0; i < cp.length; ++i) {
			IClasspathEntry cpe = cp[i];
			switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY :
					String path = null;
					IPath location = root.getFile(cpe.getPath()).getLocation();
					if (location != null) {
						path = location.toString();
					}
					//try maybe path is absolute
					if (path == null) {
						path = cpe.getPath().toString();
					}
					try {
						JarFile jarFile = new JarFile(path);
						if (manifests.contains(jarFile.getManifest())) {
							if (refIndex < 0) {
								// allocate slot
								refIndex = result.size();
								result.add(null);
							}
							exposed |= cpe.isExported();
						} else {
							result.add(cpe);
						}
					} catch (IOException e) {
						PDEPlugin.log(e);
					}
					break;
				default :
					result.add(cpe);
					break;
			}
		}
		if (refIndex >= 0) {
			result.set(refIndex, JavaCore.newProjectEntry(currentProject.getPath(), exposed));
			return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
		}
		return null;
	}

	protected void createContents(IProgressMonitor monitor, IProject project) throws CoreException, JavaModelException, InvocationTargetException, InterruptedException {
		// copy jars
		String[] paths = fData.getLibraryPaths();
		for (int i = paths.length - 1; i >= 0; i--) {
			File jarFile = new File(paths[i]);
			if (fData.isUnzipLibraries()) {
				importJar(jarFile, project, monitor);
			} else {
				addJar(jarFile, project, monitor);
			}
			monitor.worked(1);
		}

		// delete manifest.mf imported from libraries
		IFile importedManifest = PDEProject.getManifest(project);
		if (importedManifest.exists()) {
			importedManifest.delete(true, false, monitor);
			if (!fData.hasBundleStructure()) {
				IFolder meta_inf = project.getFolder("META-INF"); //$NON-NLS-1$
				if (meta_inf.members().length == 0) {
					meta_inf.delete(true, false, monitor);
				}
			}
		}
	}

	protected void fillBinIncludes(IProject project, IBuildEntry binEntry) throws CoreException {
		if (fData.hasBundleStructure())
			binEntry.addToken(ICoreConstants.MANIFEST_FOLDER_NAME);
		else
			binEntry.addToken(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);

		if (fData.isUnzipLibraries()) {
			IResource[] resources = project.members(false);
			for (int j = 0; j < resources.length; j++) {
				String resourceName = resources[j].getName();
				if (resources[j] instanceof IFolder) {
					if (!".settings".equals(resourceName) && !binEntry.contains(resourceName + "/")) //$NON-NLS-1$ //$NON-NLS-2$
						binEntry.addToken(resourceName + "/"); //$NON-NLS-1$
				} else {
					if (".project".equals(resourceName) //$NON-NLS-1$
							|| ".classpath".equals(resourceName) //$NON-NLS-1$
							|| ICoreConstants.BUILD_FILENAME_DESCRIPTOR.equals(resourceName)) {
						continue;
					}
					if (!binEntry.contains(resourceName))
						binEntry.addToken(resourceName);
				}
			}
		} else {
			String[] libraryPaths = fData.getLibraryPaths();
			for (int j = 0; j < libraryPaths.length; j++) {
				File jarFile = new File(libraryPaths[j]);
				String name = jarFile.getName();
				if (!binEntry.contains(name))
					binEntry.addToken(name);
			}
		}
	}

	protected IClasspathEntry[] getInternalClassPathEntries(IJavaProject project, IFieldData data) {
		String[] libraryPaths;
		if (fData.isUnzipLibraries()) {
			libraryPaths = new String[] {""}; //$NON-NLS-1$
		} else {
			libraryPaths = fData.getLibraryPaths();
		}
		IClasspathEntry[] entries = new IClasspathEntry[libraryPaths.length];
		for (int j = 0; j < libraryPaths.length; j++) {
			File jarFile = new File(libraryPaths[j]);
			String jarName = jarFile.getName();
			IPath path = project.getProject().getFullPath().append(jarName);
			entries[j] = JavaCore.newLibraryEntry(path, null, null, true);
		}
		return entries;
	}

	protected int getNumberOfWorkUnits() {
		int numUnits = super.getNumberOfWorkUnits();
		numUnits += fData.getLibraryPaths().length;
		return numUnits;
	}

	private void importJar(File jar, IResource destination, IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		ZipFile input = null;
		try {
			try {
				input = new ZipFile(jar);
				ZipFileStructureProvider provider = new ZipFileStructureProvider(input);
				ImportOperation op = new ImportOperation(destination.getFullPath(), provider.getRoot(), provider, new IOverwriteQuery() {
					public String queryOverwrite(String pathString) {
						return IOverwriteQuery.ALL;
					}
				});
				op.run(monitor);
			} finally {
				if (input != null)
					input.close();
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.OK, NLS.bind(PDEUIMessages.NewProjectCreationOperation_errorImportingJar, jar), e));
		}
	}

	private void removeExportRoot(IBundle bundle) {
		String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
		if (value == null)
			value = "."; //$NON-NLS-1$
		try {
			ManifestElement[] elems = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
			StringBuffer buff = new StringBuffer(value.length());
			for (int i = 0; i < elems.length; i++) {
				if (!elems[i].getValue().equals(".")) //$NON-NLS-1$
					buff.append(elems[i].getValue());
			}
			bundle.setHeader(Constants.BUNDLE_CLASSPATH, buff.toString());
		} catch (BundleException e) {
		}
	}

	protected void setPluginLibraries(WorkspacePluginModelBase model) throws CoreException {
		IPluginBase pluginBase = model.getPluginBase();
		if (fData.isUnzipLibraries()) {
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName("."); //$NON-NLS-1$
			library.setExported(true);
			pluginBase.add(library);
		} else {
			String[] paths = fData.getLibraryPaths();
			for (int i = 0; i < paths.length; i++) {
				File jarFile = new File(paths[i]);
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				library.setName(jarFile.getName());
				library.setExported(true);
				pluginBase.add(library);
			}
		}
	}

	protected void createSourceOutputBuildEntries(WorkspaceBuildModel model, IBuildModelFactory factory) throws CoreException {
		if (fData.isUnzipLibraries()) {
			// SOURCE.<LIBRARY_NAME>
			IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX + "."); //$NON-NLS-1$
			entry.addToken("."); //$NON-NLS-1$
			model.getBuild().add(entry);

			// OUTPUT.<LIBRARY_NAME>
			entry = factory.createEntry(IBuildEntry.OUTPUT_PREFIX + "."); //$NON-NLS-1$
			entry.addToken("."); //$NON-NLS-1$
			model.getBuild().add(entry);
		}
	}

	private void addExportedPackages(IProject project, IBundle bundle) {
		String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
		if (value == null)
			value = "."; //$NON-NLS-1$
		try {
			ManifestElement[] elems = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
			HashMap map = new HashMap();
			for (int i = 0; i < elems.length; i++) {
				ArrayList filter = new ArrayList();
				filter.add("*"); //$NON-NLS-1$
				map.put(elems[i].getValue(), filter);
			}
			Set packages = PluginConverter.getDefault().getExports(project, map);
			String pkgValue = getCommaValuesFromPackagesSet(packages, fData.getVersion());
			bundle.setHeader(Constants.EXPORT_PACKAGE, pkgValue);
		} catch (BundleException e) {
		}
	}

	private void addDependencies(IProject project, ISharedPluginModel model, IProgressMonitor monitor) {
		if (!(model instanceof IBundlePluginModelBase)) {
			monitor.done();
			return;
		}
		final boolean unzip = fData.isUnzipLibraries();
		try {
			new AddNewBinaryDependenciesOperation(project, (IBundlePluginModelBase) model) {
				// Need to override this function to include every bundle in the
				// target platform as a possible dependency
				protected String[] findSecondaryBundles(IBundle bundle, Set ignorePkgs) {
					IPluginModelBase[] bases = PluginRegistry.getActiveModels();
					String[] ids = new String[bases.length];
					for (int i = 0; i < bases.length; i++) {
						BundleDescription desc = bases[i].getBundleDescription();
						if (desc == null)
							ids[i] = bases[i].getPluginBase().getId();
						else
							ids[i] = desc.getSymbolicName();
					}
					return ids;
				}

				// Need to override this function because when jar is unzipped,
				// build.properties does not contain entry for '.'.
				// Therefore, the super.addProjectPackages will not find the
				// project packages(it includes only things in bin.includes)
				protected void addProjectPackages(IBundle bundle, Set ignorePkgs) {
					if (!unzip)
						super.addProjectPackages(bundle, ignorePkgs);
					Stack stack = new Stack();
					stack.push(fProject);
					try {
						while (!stack.isEmpty()) {
							IContainer folder = (IContainer) stack.pop();
							IResource[] children = folder.members();
							for (int i = 0; i < children.length; i++) {
								if (children[i] instanceof IContainer)
									stack.push(children[i]);
								else if ("class".equals(((IFile) children[i]).getFileExtension())) { //$NON-NLS-1$
									String path = folder.getProjectRelativePath().toString();
									ignorePkgs.add(path.replace('/', '.'));
								}
							}
						}
					} catch (CoreException e) {
					}

				}
			}.run(monitor);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}

}
