/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Code 9 Corporation - ongoing enhancements
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 248852, bug 247553
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
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
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.Build;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
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

	private static final String SOURCE_PREFIX = "source."; //$NON-NLS-1$

	private LibraryPluginFieldData fData;

	public NewLibraryPluginCreationOperation(LibraryPluginFieldData data, IProjectProvider provider, IPluginContentWizard contentWizard) {
		super(data, provider, contentWizard);
		fData = data;
	}

	private void addJar(File jarFile, IProject project, IProgressMonitor monitor) throws CoreException {
		String jarName = jarFile.getName();
		IFile file = project.getFile(jarName);
		monitor.subTask(NLS.bind(PDEUIMessages.NewProjectCreationOperation_copyingJar, jarName));
		try (InputStream in = new FileInputStream(jarFile)) {
			file.create(in, true, monitor);
		} catch (IOException fnfe) {
		}
	}

	private void adjustExportRoot(IProject project, IBundle bundle) throws CoreException {
		IResource[] resources = project.members(false);
		for (IResource resource : resources) {
			if (resource instanceof IFile) {
				if (".project".equals(resource.getName()) //$NON-NLS-1$
						|| ".classpath".equals(resource //$NON-NLS-1$
								.getName()) || ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR.equals(resource.getName()) || ICoreConstants.BUILD_FILENAME_DESCRIPTOR.equals(resource.getName())) {
					continue;
				}
				// resource at the root, export root
				return;
			}
		}
		removeExportRoot(bundle);
	}

	@Override
	protected void adjustManifests(IProgressMonitor monitor, IProject project, IPluginBase base) throws CoreException {
		super.adjustManifests(monitor, project, base);
		int units = fData.doFindDependencies() ? 4 : 2;
		units += fData.isUpdateReferences() ? 1 : 0;
		SubMonitor subMonitor = SubMonitor.convert(monitor, units);
		IBundle bundle = (base instanceof BundlePluginBase) ? ((BundlePluginBase) base).getBundle() : null;
		if (bundle != null) {
			adjustExportRoot(project, bundle);
			subMonitor.worked(1);
			addExportedPackages(project, bundle);
			subMonitor.worked(1);
			if (fData.doFindDependencies()) {
				addDependencies(project, base.getModel(), subMonitor.split(2));
			}
			if (fData.isUpdateReferences()) {
				updateReferences(subMonitor.split(1), project);
			}
		}
	}

	protected void updateReferences(IProgressMonitor monitor, IProject project) throws JavaModelException {
		IJavaProject currentProject = JavaCore.create(project);
		IPluginModelBase[] pluginstoUpdate = fData.getPluginsToUpdate();
		SubMonitor subMonitor = SubMonitor.convert(monitor, pluginstoUpdate.length);
		for (IPluginModelBase element : pluginstoUpdate) {
			SubMonitor iterationMonitor = subMonitor.split(1).setWorkRemaining(2);
			IProject proj = element.getUnderlyingResource().getProject();
			if (currentProject.getProject().equals(proj))
				continue;
			IJavaProject javaProject = JavaCore.create(proj);
			IClasspathEntry[] cp = javaProject.getRawClasspath();
			IClasspathEntry[] updated = getUpdatedClasspath(cp, currentProject);
			if (updated != null) {
				javaProject.setRawClasspath(updated, iterationMonitor.split(1));
			}
			iterationMonitor.setWorkRemaining(1);
			try {
				updateRequiredPlugins(javaProject, iterationMonitor.split(1), element);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}

	private static void updateRequiredPlugins(IJavaProject javaProject, IProgressMonitor monitor, IPluginModelBase model) throws CoreException {
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		List<IClasspathEntry> classpath = new ArrayList<>();
		List<IClasspathEntry> requiredProjects = new ArrayList<>();
		for (IClasspathEntry entry : entries) {
			if (isPluginProjectEntry(entry)) {
				requiredProjects.add(entry);
			} else {
				classpath.add(entry);
			}
		}
		if (requiredProjects.isEmpty())
			return;
		IFile file = PDEProject.getManifest(javaProject.getProject());
		try {
			// TODO format manifest
			Manifest manifest = new Manifest(file.getContents());
			String value = manifest.getMainAttributes().getValue(Constants.REQUIRE_BUNDLE);
			StringBuilder sb = value != null ? new StringBuilder(value) : new StringBuilder();
			if (sb.length() > 0)
				sb.append(","); //$NON-NLS-1$
			for (int i = 0; i < requiredProjects.size(); i++) {
				IClasspathEntry entry = requiredProjects.get(i);
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
			SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
			file.setContents(new ByteArrayInputStream(content.toByteArray()), true, false, subMonitor.split(1));
			// now update .classpath
			javaProject.setRawClasspath(classpath.toArray(new IClasspathEntry[classpath.size()]),
					subMonitor.split(1));
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

		try (InputStream is = PDEProject.getManifest(other).getContents()) {
			Manifest mf = new Manifest(is);
			if (mf.getMainAttributes().getValue(Constants.FRAGMENT_HOST) != null)
				return false;
		} catch (IOException | CoreException e) {
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
		List<IClasspathEntry> result = new ArrayList<>();
		Set<Manifest> manifests = new HashSet<>();
		for (int i = 0; i < fData.getLibraryPaths().length; ++i) {
			try (JarFile jarFile = new JarFile(fData.getLibraryPaths()[i])) {
				manifests.add(jarFile.getManifest());
			} catch (IOException e) {
				PDEPlugin.log(e);
			}
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (IClasspathEntry cpe : cp) {
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
				try (JarFile jarFile = new JarFile(path)) {

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
			return result.toArray(new IClasspathEntry[result.size()]);
		}
		return null;
	}

	@Override
	protected void createContents(IProgressMonitor monitor, IProject project) throws CoreException, JavaModelException, InvocationTargetException, InterruptedException {
		// copy jars
		String[] paths = fData.getLibraryPaths();
		SubMonitor subMonitor = SubMonitor.convert(monitor, paths.length + 2);
		for (int i = paths.length - 1; i >= 0; i--) {
			File jarFile = new File(paths[i]);
			if (fData.isUnzipLibraries()) {
				importJar(jarFile, project, subMonitor.split(1));
			} else {
				addJar(jarFile, project, subMonitor);
			}
		}

		// delete manifest.mf imported from libraries
		IFile importedManifest = PDEProject.getManifest(project);
		if (importedManifest.exists()) {
			importedManifest.delete(true, false, subMonitor.split(1));
			subMonitor.setWorkRemaining(1);
			if (!fData.hasBundleStructure()) {
				IFolder meta_inf = project.getFolder("META-INF"); //$NON-NLS-1$
				if (meta_inf.members().length == 0) {
					meta_inf.delete(true, false, subMonitor.split(1));
				}
			}
		}
		subMonitor.setWorkRemaining(0);
	}

	@Override
	protected void fillBinIncludes(IProject project, IBuildEntry binEntry) throws CoreException {
		if (fData.hasBundleStructure())
			binEntry.addToken(ICoreConstants.MANIFEST_FOLDER_NAME);
		else
			binEntry.addToken(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);

		if (fData.isUnzipLibraries()) {
			IResource[] resources = project.members(false);
			for (IResource resource : resources) {
				String resourceName = resource.getName();
				if (resource instanceof IFolder) {
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
			for (String libraryPath : libraryPaths) {
				File jarFile = new File(libraryPath);
				String name = jarFile.getName();
				if (!binEntry.contains(name))
					binEntry.addToken(name);
			}
		}
	}

	@Override
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

	@Override
	protected int getNumberOfWorkUnits() {
		int numUnits = super.getNumberOfWorkUnits();
		numUnits += fData.getLibraryPaths().length;
		return numUnits;
	}

	private void importJar(File jar, IResource destination, IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		try (ZipFile input = new ZipFile(jar);) {
			ZipFileStructureProvider provider = new ZipFileStructureProvider(input);
			ImportOperation op = new ImportOperation(destination.getFullPath(), provider.getRoot(), provider,
					pathString -> IOverwriteQuery.ALL);
			op.run(monitor);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.OK,
					NLS.bind(PDEUIMessages.NewProjectCreationOperation_errorImportingJar, jar), e));
		}
	}

	private void removeExportRoot(IBundle bundle) {
		String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
		if (value == null)
			value = "."; //$NON-NLS-1$
		try {
			ManifestElement[] elems = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
			StringBuilder buff = new StringBuilder(value.length());
			for (int i = 0; i < elems.length; i++) {
				if (!elems[i].getValue().equals(".")) //$NON-NLS-1$
					buff.append(elems[i].getValue());
			}
			bundle.setHeader(Constants.BUNDLE_CLASSPATH, buff.toString());
		} catch (BundleException e) {
		}
	}

	@Override
	protected void setPluginLibraries(WorkspacePluginModelBase model) throws CoreException {
		IPluginBase pluginBase = model.getPluginBase();
		if (fData.isUnzipLibraries()) {
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName("."); //$NON-NLS-1$
			library.setExported(true);
			pluginBase.add(library);
		} else {
			String[] paths = fData.getLibraryPaths();
			for (String path : paths) {
				File jarFile = new File(path);
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				library.setName(jarFile.getName());
				library.setExported(true);
				pluginBase.add(library);
			}
		}
	}

	@Override
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
			HashMap<String, ArrayList<String>> map = new HashMap<>();
			for (ManifestElement elem : elems) {
				ArrayList<String> filter = new ArrayList<>();
				filter.add("*"); //$NON-NLS-1$
				map.put(elem.getValue(), filter);
			}
			Set<String> packages = getExports(project, map);
			String pkgValue = getCommaValuesFromPackagesSet(packages, fData.getVersion());
			bundle.setHeader(Constants.EXPORT_PACKAGE, pkgValue);
		} catch (BundleException e) {
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Set<String> getExports(IProject proj, Map libs) {
		IFile buildProperties = PDEProject.getBuildProperties(proj);
		IBuild build = null;
		if (buildProperties != null) {
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildProperties);
			build = buildModel.getBuild();
		} else
			build = new Build();
		return findPackages(proj, libs, build);
	}

	private Set<String> findPackages(IProject proj, Map<?, List<?>> libs, IBuild build) {
		TreeSet<String> result = new TreeSet<>();
		IJavaProject jp = JavaCore.create(proj);
		Iterator<?> it = libs.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry entry = (Map.Entry) it.next();
			String libName = entry.getKey().toString();
			List<?> filter = (List<?>) entry.getValue();
			IBuildEntry libEntry = build.getEntry(SOURCE_PREFIX + libName);
			if (libEntry != null) {
				String[] tokens = libEntry.getTokens();
				for (String token : tokens) {
					IResource folder = null;
					if (token.equals(".")) //$NON-NLS-1$
						folder = proj;
					else
						folder = proj.getFolder(token);
					if (folder != null)
						addPackagesFromFragRoot(jp.getPackageFragmentRoot(folder), result, filter);
				}
			} else {
				IResource res = proj.findMember(libName);
				if (res != null)
					addPackagesFromFragRoot(jp.getPackageFragmentRoot(res), result, filter);
			}
		}
		return result;
	}

	private void addPackagesFromFragRoot(IPackageFragmentRoot root, Collection<String> result, List<?> filter) {
		if (root == null)
			return;
		try {
			if (filter != null && !filter.contains("*")) { //$NON-NLS-1$
				ListIterator<?> li = filter.listIterator();
				while (li.hasNext()) {
					String pkgName = li.next().toString();
					if (pkgName.endsWith(".*")) //$NON-NLS-1$
						pkgName = pkgName.substring(0, pkgName.length() - 2);

					IPackageFragment frag = root.getPackageFragment(pkgName);
					if (frag != null)
						result.add(pkgName);
				}
				return;
			}
			IJavaElement[] children = root.getChildren();
			for (IJavaElement child : children) {
				IPackageFragment fragment = (IPackageFragment) child;
				String name = fragment.getElementName();
				if (fragment.hasChildren() && !result.contains(name)) {
					result.add(name);
				}
			}
		} catch (JavaModelException e) {
		}
	}

	private void addDependencies(IProject project, ISharedPluginModel model, IProgressMonitor monitor) {
		if (!(model instanceof IBundlePluginModelBase)) {
			return;
		}
		final boolean unzip = fData.isUnzipLibraries();
		try {
			new AddNewBinaryDependenciesOperation(project, (IBundlePluginModelBase) model) {
				// Need to override this function to include every bundle in the
				// target platform as a possible dependency
				@Override
				protected String[] findSecondaryBundles(IBundle bundle, Set<String> ignorePkgs) {
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
				@Override
				protected void addProjectPackages(IBundle bundle, Set<String> ignorePkgs) {
					if (!unzip)
						super.addProjectPackages(bundle, ignorePkgs);
					Stack<IResource> stack = new Stack<>();
					stack.push(fProject);
					try {
						while (!stack.isEmpty()) {
							IContainer folder = (IContainer) stack.pop();
							IResource[] children = folder.members();
							for (IResource child : children) {
								if (child instanceof IContainer)
									stack.push(child);
								else if ("class".equals(((IFile) child).getFileExtension())) { //$NON-NLS-1$
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
