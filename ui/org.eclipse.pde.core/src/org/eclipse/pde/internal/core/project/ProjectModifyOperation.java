/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IHostDescription;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.core.project.IPackageImportDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.BundleModelFactory;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageFriend;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Operation to create or modify a PDE project based on a bundle project description.
 *
 * @since 3.6
 */
public class ProjectModifyOperation {

	private WorkspacePluginModelBase fModel;

	private final static IPath[] EXCLUDE_NONE = {};

	/**
	 * Creates or modifies a project based on the given description.
	 *
	 * @param monitor progress monitor or <code>null</code>
	 * @param description project description
	 * @throws CoreException if project creation fails
	 */
	public void execute(IProgressMonitor monitor, IBundleProjectDescription description) throws CoreException {
		// retrieve current description of the project to detect differences
		IProject project = description.getProject();
		IBundleProjectService service = PDECore.getDefault().acquireService(IBundleProjectService.class);
		IBundleProjectDescription before = service.getDescription(project);
		boolean considerRoot = !project.exists();
		String taskName = null;
		boolean jpExisted = false;
		if (project.exists()) {
			taskName = Messages.ProjectModifyOperation_0;
			jpExisted = before.hasNature(JavaCore.NATURE_ID);
		} else {
			taskName = Messages.ProjectModifyOperation_1;
			// new bundle projects get Java and Plug-in natures
			if (description.getNatureIds().length == 0) {
				description.setNatureIds(new String[] {IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID});
			}
		}
		boolean becomeBundle = !before.hasNature(IBundleProjectDescription.PLUGIN_NATURE) && description.hasNature(IBundleProjectDescription.PLUGIN_NATURE);

		// set default values when migrating from Java project to bundle project
		if (jpExisted && becomeBundle) {
			if (description.getExecutionEnvironments() == null) {
				// use EE from Java project when unspecified in the description, and a bundle nature is being added
				IJavaProject jp = JavaCore.create(project);
				if (jp.exists()) {
					IClasspathEntry[] classpath = jp.getRawClasspath();
					for (IClasspathEntry entry : classpath) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
							String id = JavaRuntime.getExecutionEnvironmentId(entry.getPath());
							if (id != null) {
								description.setExecutionEnvironments(new String[] {id});
								break;
							}
						}
					}
				}
			}
		}
		// other default values when becoming a bundle
		if (becomeBundle) {
			// set default values for where unspecified
			if (description.getBundleVersion() == null) {
				description.setBundleVersion(new Version(1, 0, 0, "qualifier")); //$NON-NLS-1$
			}
		}

		SubMonitor sub = SubMonitor.convert(monitor, taskName, 6);
		// create and open project
		createProject(description);
		// set bundle root for new projects
		if (considerRoot) {
			IFolder folder = null;
			IPath root = description.getBundleRoot();
			if (root != null && !root.isEmpty()) {
				folder = project.getFolder(root);
				CoreUtility.createFolder(folder);
			}
			PDEProject.setBundleRoot(project, folder);
		}
		sub.split(1);
		configureNatures(description);
		sub.split(1);
		if (project.hasNature(JavaCore.NATURE_ID)) {
			configureJavaProject(description, before, jpExisted);
		}
		sub.split(1);
		configureManifest(description, before);
		sub.split(1);
		configureBuildPropertiesFile(description, before);
		sub.split(1);

		// project settings for Equinox, Extension Registry, Automated dependency policy,
		// manifest editor launch shortcuts and export wizard
		IEclipsePreferences pref = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
		if (pref != null) {
			// best guess for automated dependency management: Equinox + Extensions = use required bundle
			if (description.isEquinox() && description.isExtensionRegistry()) {
				pref.remove(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE); // i.e. use required bundle
			} else {
				pref.putBoolean(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE, false);
			}
			if (description.isExtensionRegistry()) {
				pref.remove(ICoreConstants.EXTENSIONS_PROPERTY); // i.e. support extensions
			} else {
				pref.putBoolean(ICoreConstants.EXTENSIONS_PROPERTY, false);
			}
			if (description.isEquinox()) {
				pref.remove(ICoreConstants.EQUINOX_PROPERTY); // i.e. using Equinox
			} else {
				pref.putBoolean(ICoreConstants.EQUINOX_PROPERTY, false);
			}
			String[] shorts = description.getLaunchShortcuts();
			if (shorts == null || shorts.length == 0) {
				pref.remove(ICoreConstants.MANIFEST_LAUNCH_SHORTCUTS); // use defaults
			} else {
				StringBuilder value = new StringBuilder();
				for (int i = 0; i < shorts.length; i++) {
					if (i > 0) {
						value.append(',');
					}
					value.append(shorts[i]);
				}
				pref.put(ICoreConstants.MANIFEST_LAUNCH_SHORTCUTS, value.toString());
			}
			if (description.getExportWizardId() == null) {
				pref.remove(ICoreConstants.MANIFEST_EXPORT_WIZARD);
			} else {
				pref.put(ICoreConstants.MANIFEST_EXPORT_WIZARD, description.getExportWizardId());
			}
			try {
				pref.flush();
			} catch (BackingStoreException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.ProjectModifyOperation_2, e));
			}
		}

		if (fModel.isDirty()) {
			fModel.save();
		}
		sub.split(1);
	}

	/**
	 * Returns the model created by this operation.
	 *
	 * @return model
	 */
	public WorkspacePluginModelBase getModel() {
		return fModel;
	}

	/**
	 * Configures the build path and output location of the described Java project.
	 * If the Java project existed before this operation, new build path entries are
	 * added for the bundle class path, if required, but we don't change the exiting
	 * build path.
	 *
	 * @param description desired project description
	 * @param before state before the operation
	 * @param existed whether the Java project existed before the operation
	 */
	private void configureJavaProject(IBundleProjectDescription description, IBundleProjectDescription before, boolean existed) throws CoreException {
		IProject project = description.getProject();
		IJavaProject javaProject = JavaCore.create(project);
		// create source folders as required
		IBundleClasspathEntry[] bces = description.getBundleClasspath();
		if (bces != null && bces.length > 0) {
			for (IBundleClasspathEntry bce : bces) {
				IPath folder = bce.getSourcePath();
				if (folder != null) {
					CoreUtility.createFolder(project.getFolder(folder));
				}
			}
		}
		// Set default output folder
		if (description.getDefaultOutputFolder() != null) {
			IPath path = project.getFullPath().append(description.getDefaultOutputFolder());
			javaProject.setOutputLocation(path, null);
		}

		// merge the class path if the project existed before
		IBundleClasspathEntry[] prev = before.getBundleClasspath();
		if (!isEqual(bces, prev)) {
			if (existed) {
				// add entries not already present
				IClasspathEntry[] entries = getSourceFolderEntries(javaProject, description);
				IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
				List<IClasspathEntry> add = new ArrayList<>();
				for (IClasspathEntry entry : entries) {
					boolean present = false;
					for (IClasspathEntry existingEntry : rawClasspath) {
						if (existingEntry.getEntryKind() == entry.getEntryKind()) {
							if (existingEntry.getPath().equals(entry.getPath())) {
								present = true;
								break;
							}
						}
					}
					if (!present) {
						add.add(entry);
					}
				}
				// check if the 'required plug-ins' container is present
				boolean addRequired = false;
				if (description.hasNature(IBundleProjectDescription.PLUGIN_NATURE)) {
					addRequired = true;
					for (IClasspathEntry cpe : rawClasspath) {
						if (cpe.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
							if (PDECore.REQUIRED_PLUGINS_CONTAINER_PATH.equals(cpe.getPath())) {
								addRequired = false;
								break;
							}
						}
					}
				}
				if (addRequired) {
					add.add(ClasspathComputer.createContainerEntry());
				}
				if (!add.isEmpty()) {
					List<IClasspathEntry> all = new ArrayList<>();
					for (IClasspathEntry cpe : rawClasspath) {
						all.add(cpe);
					}
					all.addAll(add);
					javaProject.setRawClasspath(all.toArray(new IClasspathEntry[all.size()]), null);
				}
			} else {
				IClasspathEntry[] entries = getClassPathEntries(javaProject, description);
				javaProject.setRawClasspath(entries, null);
			}
		}
	}

	/**
	 * Returns whether the arrays are equal.
	 *
	 * @param array1 an object array or <code>null</code>
	 * @param array2 an object array or <code>null</code>
	 * @return whether the arrays are equal
	 */
	private boolean isEqual(Object[] array1, Object[] array2) {
		if (array1 == null || array1.length == 0) {
			return array2 == null || array2.length == 0;
		}
		if (array2 == null || array2.length == 0) {
			return false;
		}
		if (array1.length != array2.length) {
			return false;
		}
		for (int i = 0; i < array1.length; i++) {
			if (!array1[i].equals(array2[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns whether the two objects are equal or both <code>null</code>.
	 *
	 * @param st1
	 * @param st2
	 * @return whether equal or both <code>null</code>
	 */
	private boolean isEqual(Object st1, Object st2) {
		if (st1 == null) {
			return st2 == null;
		}
		if (st2 == null) {
			return false;
		}
		return st1.equals(st2);
	}

	/**
	 * Returns the class path entries to create for the given project.
	 *
	 * @param project Java project
	 * @param description project description
	 * @return class path entries
	 * @throws CoreException if class path creation fails
	 */
	private IClasspathEntry[] getClassPathEntries(IJavaProject project, IBundleProjectDescription description) throws CoreException {
		IClasspathEntry[] internalClassPathEntries = getSourceFolderEntries(project, description);
		IClasspathEntry[] entries = new IClasspathEntry[internalClassPathEntries.length + 2];
		System.arraycopy(internalClassPathEntries, 0, entries, 2, internalClassPathEntries.length);

		String[] ids = description.getExecutionEnvironments();
		String executionEnvironment = null;
		if (ids != null && ids.length > 0) {
			executionEnvironment = ids[0];
		}
		ClasspathComputer.setComplianceOptions(project, executionEnvironment);
		entries[0] = ClasspathComputer.createJREEntry(executionEnvironment);
		entries[1] = ClasspathComputer.createContainerEntry();
		return entries;
	}

	/**
	 * Returns source folder class path entries.
	 *
	 * @param project Java project
	 * @param description project description
	 * @return source folder class path entries, possibly empty.
	 * @exception CoreException if source folder class path entry creation fails
	 */
	private IClasspathEntry[] getSourceFolderEntries(IJavaProject project, IBundleProjectDescription description) throws CoreException {
		IBundleClasspathEntry[] folders = description.getBundleClasspath();
		if (folders == null || folders.length == 0) {
			return new IClasspathEntry[0];
		}
		List<IClasspathEntry> entries = new ArrayList<>();
		for (IBundleClasspathEntry folder : folders) {
			if (folder.getSourcePath() == null) {
				// no source indicates class file folder or library
				IPath bin = folder.getBinaryPath();
				if (bin == null) {
					// nested library
					bin = folder.getLibrary();
				}
				if (bin != null) {
					IPath output = project.getProject().getFullPath().append(bin);
					entries.add(JavaCore.newLibraryEntry(output, null, null));
				}
			} else {
				// source folder
				IPath path = project.getProject().getFullPath().append(folder.getSourcePath());
				IPath output = folder.getBinaryPath();
				if (output != null) {
					output = project.getProject().getFullPath().append(output);
				}
				entries.add(JavaCore.newSourceEntry(path, EXCLUDE_NONE, output));
			}
		}
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

	/**
	 * Create and open the described project, as necessary.
	 *
	 * @param description project description
	 * @exception CoreException on failure
	 */
	private void createProject(IBundleProjectDescription description) throws CoreException {
		IProject project = description.getProject();
		if (!project.exists()) {
			IProjectDescription pd = project.getWorkspace().newProjectDescription(project.getName());
			pd.setLocationURI(description.getLocationURI());
			project.create(pd, null);
		}
		if (!project.isOpen()) {
			project.open(null);
		}
	}

	/**
	 * Configures project natures.
	 *
	 * @param description description of project to modify
	 * @exception CoreException if something goes wrong
	 */
	private void configureNatures(IBundleProjectDescription description) throws CoreException {
		IProject project = description.getProject();
		IProjectDescription projectDescription = project.getDescription();
		String[] curr = projectDescription.getNatureIds();
		Set<String> before = new HashSet<>();
		for (String element : curr) {
			before.add(element);
		}
		String[] natureIds = description.getNatureIds();
		Set<String> after = new HashSet<>();
		for (String natureId : natureIds) {
			after.add(natureId);
		}
		if (!before.equals(after)) {
			projectDescription.setNatureIds(natureIds);
			project.setDescription(projectDescription, null);
		}
	}

	/**
	 * Configures the MANIFEST.MF for the given description.
	 *
	 * @param description project description
	 * @param before description before operation began
	 * @throws CoreException
	 */
	private void configureManifest(IBundleProjectDescription description, IBundleProjectDescription before) throws CoreException {
		IProject project = description.getProject();
		IFile manifest = PDEProject.getManifest(project);
		if (description.getHost() == null) {
			fModel = new WorkspaceBundlePluginModel(manifest, PDEProject.getPluginXml(project));
		} else {
			fModel = new WorkspaceBundleFragmentModel(manifest, PDEProject.getFragmentXml(project));
		}

		IPluginBase pluginBase = fModel.getPluginBase();

		// target version
		String targetVersion = getTargetVersion(description.getTargetVersion());
		if (!isEqual(targetVersion, getTargetVersion(before.getTargetVersion()))) {
			String schemaVersion = TargetPlatformHelper.getSchemaVersionForTargetVersion(targetVersion);
			pluginBase.setSchemaVersion(schemaVersion);
		}

		// symbolic name
		if (!isEqual(description.getSymbolicName(), before.getSymbolicName())) {
			pluginBase.setId(description.getSymbolicName());
		}

		// bundle version
		if (!isEqual(description.getBundleVersion(), before.getBundleVersion())) {
			pluginBase.setVersion(description.getBundleVersion().toString());
		}

		// bundle name
		String bundleName = description.getBundleName();
		if (bundleName == null) {
			// for new projects, bundle name must be specified
			bundleName = description.getSymbolicName();
		}
		if (!isEqual(bundleName, before.getBundleName())) {
			pluginBase.setName(bundleName);
		}

		// bundle vendor
		if (!isEqual(description.getBundleVendor(), before.getBundleVendor())) {
			pluginBase.setProviderName(description.getBundleVendor());
		}

		// manifest version
		if (fModel instanceof IBundlePluginModelBase) {
			IBundlePluginModelBase bmodel = ((IBundlePluginModelBase) fModel);
			// Target version is not persisted and does not make the model dirty.
			// It is only used by the new project wizard to determine which templates are available.
			((IBundlePluginBase) bmodel.getPluginBase()).setTargetVersion(targetVersion);
			String ver = bmodel.getBundleModel().getBundle().getHeader(Constants.BUNDLE_MANIFESTVERSION);
			if (!isEqual("2", ver)) { //$NON-NLS-1$
				bmodel.getBundleModel().getBundle().setHeader(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
			}
		}
		if (pluginBase instanceof IFragment) {
			// host specification
			IFragment fragment = (IFragment) pluginBase;
			IHostDescription host = description.getHost();
			if (!isEqual(host, before.getHost())) {
				fragment.setPluginId(host.getName());
				if (host.getVersionRange() != null) {
					fragment.setPluginVersion(host.getVersionRange().toString());
				} else {
					// must explicitly set to null, else it appears as 0.0.0
					fragment.setPluginVersion(null);
				}
			}
		} else {
			// bundle activator class
			String activator = description.getActivator();
			if (!isEqual(activator, before.getActivator())) {
				((IPlugin) pluginBase).setClassName(activator);
			}
		}
		// bundle classpath
		configureBundleClasspath(description, before);

		// required bundles
		IRequiredBundleDescription[] dependencies = description.getRequiredBundles();
		if (!isEqual(dependencies, before.getRequiredBundles())) {
			// remove all existing imports, then add new ones
			IPluginImport[] imports = pluginBase.getImports();
			if (imports != null && imports.length > 0) {
				for (IPluginImport pluginImport : imports) {
					pluginBase.remove(pluginImport);
				}
			}
			if (dependencies != null) {
				for (IRequiredBundleDescription req : dependencies) {
					VersionRange range = req.getVersionRange();
					IPluginImport iimport = fModel.getPluginFactory().createImport();
					iimport.setId(req.getName());
					if (range != null) {
						iimport.setVersion(range.toString());
						iimport.setMatch(IMatchRules.COMPATIBLE);
					}
					iimport.setReexported(req.isExported());
					iimport.setOptional(req.isOptional());
					pluginBase.add(iimport);
				}
			}
		}
		// add Bundle Specific fields if applicable
		if (pluginBase instanceof BundlePluginBase) {
			IBundle bundle = ((BundlePluginBase) pluginBase).getBundle();
			BundleModelFactory factory = new BundleModelFactory(bundle.getModel());
			// Remove host specification if no longer a fragment
			if (before.getHost() != null && description.getHost() == null) {
				bundle.setHeader(Constants.FRAGMENT_HOST, null);
			}
			// Singleton
			if (description.isSingleton() != before.isSingleton()) {
				if (description.isSingleton()) {
					IManifestHeader header = factory.createHeader(Constants.BUNDLE_SYMBOLICNAME, description.getSymbolicName());
					if (header instanceof BundleSymbolicNameHeader) {
						((BundleSymbolicNameHeader) header).setSingleton(description.isSingleton());
						bundle.setHeader(Constants.BUNDLE_SYMBOLICNAME, header.getValue());
					}
				}
			}
			// Set required EEs
			String[] ees = description.getExecutionEnvironments();
			if (!isEqual(ees, before.getExecutionEnvironments())) {
				if (ees == null) {
					bundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, null); // remove
				} else {
					StringBuilder buffer = new StringBuilder();
					for (String id : ees) {
						if (buffer.length() > 0) {
							buffer.append(",\n "); //comma, new-line, space //$NON-NLS-1$
						}
						buffer.append(id);
					}
					bundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, buffer.toString());
				}
			}
			// package imports
			IPackageImportDescription[] packages = description.getPackageImports();
			if (!isEqual(packages, before.getPackageImports())) {
				if (packages == null) {
					bundle.setHeader(Constants.IMPORT_PACKAGE, null); // remove
				} else {
					ImportPackageHeader header = (ImportPackageHeader) factory.createHeader(Constants.IMPORT_PACKAGE, ""); //$NON-NLS-1$
					for (IPackageImportDescription pkg : packages) {
						ImportPackageObject ip = header.addPackage(pkg.getName());
						VersionRange range = pkg.getVersionRange();
						if (range != null) {
							ip.setVersion(range.toString());
						}
						ip.setOptional(pkg.isOptional());
					}
					header.update();
					bundle.setHeader(Constants.IMPORT_PACKAGE, header.getValue());
				}
			}
			// package exports
			IPackageExportDescription[] exports = description.getPackageExports();
			if (!isEqual(exports, before.getPackageExports())) {
				if (exports == null) {
					bundle.setHeader(Constants.EXPORT_PACKAGE, null); // remove
				} else {
					ExportPackageHeader header = (ExportPackageHeader) factory.createHeader(Constants.EXPORT_PACKAGE, ""); //$NON-NLS-1$
					for (IPackageExportDescription pkg : exports) {
						ExportPackageObject epo = header.addPackage(pkg.getName());
						Version version = pkg.getVersion();
						if (version != null) {
							epo.setVersion(version.toString());
						}
						String[] friends = pkg.getFriends();
						if (friends != null) {
							for (String friend : friends) {
								epo.addFriend(new PackageFriend(epo, friend));
							}
						} else {
							epo.setInternal(!pkg.isApi());
						}
					}
					header.update();
					bundle.setHeader(Constants.EXPORT_PACKAGE, header.getValue());
				}
			}
			// Activation policy
			boolean removeActivation = false;
			if (!isEqual(description.getActivationPolicy(), before.getActivationPolicy())) {
				if (Constants.ACTIVATION_LAZY.equals(description.getActivationPolicy())) {
					if (description.isEquinox()) {
						if (targetVersion.equals(IBundleProjectDescription.VERSION_3_1)) {
							bundle.setHeader(ICoreConstants.ECLIPSE_AUTOSTART, "true"); //$NON-NLS-1$
						} else {
							double version = Double.parseDouble(targetVersion);
							if (version >= 3.4) {
								// use OSGi R4.1 header
								bundle.setHeader(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
							} else {
								bundle.setHeader(ICoreConstants.ECLIPSE_LAZYSTART, "true"); //$NON-NLS-1$
							}
						}
					} else {
						// use OSGi R4.1 header
						bundle.setHeader(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
					}
				} else { // remove activation policy headers
					removeActivation = true;
				}
			}
			if (description.getHost() != null && before.getHost() == null) {
				// remove activation policy if becoming a fragment
				removeActivation = true;
			}
			if (removeActivation) {
				bundle.setHeader(ICoreConstants.ECLIPSE_AUTOSTART, null);
				bundle.setHeader(Constants.BUNDLE_ACTIVATIONPOLICY, null);
				bundle.setHeader(ICoreConstants.ECLIPSE_LAZYSTART, null);
			}
			// Localization
			IPath localization = description.getLocalization();
			if (!isEqual(localization, before.getLocalization())) {
				if (localization == null) {
					bundle.setHeader(Constants.BUNDLE_LOCALIZATION, null);
				} else {
					bundle.setHeader(Constants.BUNDLE_LOCALIZATION, localization.toString());
				}
			}
			// if the bundle model has been made dirty, ensure that propagates back to the root model
			IBundleModel bundleModel = bundle.getModel();
			if (bundleModel instanceof WorkspaceBundleModel) {
				WorkspaceBundleModel wbm = (WorkspaceBundleModel) bundleModel;
				if (wbm.isDirty()) {
					fModel.setDirty(true);
				}
			}
			// apply any other headers that have been specified
			BundleProjectDescription bpd = (BundleProjectDescription) description;
			Map<?, ?> extraHeaders = bpd.getExtraHeaders();
			Iterator<?> iterator = extraHeaders.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<?, ?> entry = (Entry<?, ?>) iterator.next();
				String name = (String) entry.getKey();
				String value = (String) entry.getValue();
				// translate empty header to a single space to ensure inclusion of empty headers
				if (value != null && value.trim().length() == 0) {
					value = " "; //$NON-NLS-1$
				}
				if (!isEqual(value, bundle.getHeader(name))) {
					bundle.setHeader(name, value);
				}
			}
		}
	}

	/**
	 * Returns the specified target version or the latest version when <code>null</code>.
	 * @param targetVersion version or <code>null</code>
	 * @return non-null target version
	 */
	private String getTargetVersion(String targetVersion) {
		if (targetVersion == null) {
			return ICoreConstants.TARGET_VERSION_LATEST;
		}
		return targetVersion;
	}

	/**
	 * Returns the names of libraries as they should appear on the Bundle-Classpath header
	 * or <code>null</code> if none.
	 *
	 * @param description project description
	 * @return library names in the order they should appear or <code>null</code>
	 */
	protected static String[] getLibraryNames(IBundleProjectDescription description) {
		// collect unique entries
		IBundleClasspathEntry[] libs = description.getBundleClasspath();
		if (libs != null && libs.length > 0) {
			Set<String> names = new LinkedHashSet<>();
			for (IBundleClasspathEntry cpe : libs) {
				IPath libPath = cpe.getLibrary();
				String libName = "."; //$NON-NLS-1$
				if (libPath != null) {
					libName = libPath.toString();
				}
				names.add(libName);
			}
			return names.toArray(new String[names.size()]);
		}
		return null;
	}

	/**
	 * Sets the bundle class path entries in the manifest. If there is only
	 * one entry with the default name '.', it is not added to the Bundle-Classpath
	 * header.
	 *
	 * @param description project description
	 * @param before original state of project
	 * @throws CoreException
	 */
	private void configureBundleClasspath(IBundleProjectDescription description, IBundleProjectDescription before) throws CoreException {
		IBundleClasspathEntry[] cp = description.getBundleClasspath();
		if (!isEqual(cp, before.getBundleClasspath())) {
			// remove all libraries and start again
			IPluginBase pluginBase = fModel.getPluginBase();
			IPluginLibrary[] libraries = pluginBase.getLibraries();
			if (libraries != null && libraries.length > 0) {
				for (IPluginLibrary library : libraries) {
					pluginBase.remove(library);
				}
			}
			String[] names = getLibraryNames(description);
			if (names != null) {
				if (names.length == 1 && ".".equals(names[0])) { //$NON-NLS-1$
					return; // default library does not need to be added
				}
				for (String name : names) {
					IPluginLibrary library = fModel.getPluginFactory().createLibrary();
					library.setName(name);
					library.setExported(false);
					pluginBase.add(library);
				}
			}
		}
	}

	private void configureBuildPropertiesFile(IBundleProjectDescription description, IBundleProjectDescription before) throws CoreException {
		IProject project = description.getProject();
		IFile file = PDEProject.getBuildProperties(project);
		WorkspaceBuildModel model = new WorkspaceBuildModel(file);
		IBuildModelFactory factory = model.getFactory();

		// BIN.INCLUDES
		IBuildEntry binEntry = model.getBuild().getEntry(IBuildEntry.BIN_INCLUDES);
		if (binEntry == null) {
			binEntry = factory.createEntry(IBuildEntry.BIN_INCLUDES);
			model.getBuild().add(binEntry);
		}
		boolean modified = fillBinIncludes(project, binEntry, description, before);
		modified = createSourceOutputBuildEntries(model, factory, description, before) | modified;
		if (modified) {
			model.save();
		}
	}

	/**
	 * Configures the bin.includes entry based on the included libraries and explicit entries to add.
	 *
	 * @param project
	 * @param binEntry
	 * @param description
	 * @param before
	 * @return whether the entry was modified
	 * @throws CoreException
	 */
	private boolean fillBinIncludes(IProject project, IBuildEntry binEntry, IBundleProjectDescription description, IBundleProjectDescription before) throws CoreException {
		boolean modified = false;
		if (!binEntry.contains("META-INF/")) { //$NON-NLS-1$
			modified = true;
			binEntry.addToken("META-INF/"); //$NON-NLS-1$
		}
		// add bundle class path entries
		String[] names = getLibraryNames(description);
		String[] prevNames = getLibraryNames(before);
		if (!isEqual(names, prevNames)) {
			// remove old libraries
			if (prevNames != null) {
				for (String prevName : prevNames) {
					if (binEntry.contains(prevName)) {
						modified = true;
						binEntry.removeToken(prevName);
					}
				}
			}
			// add new libraries
			if (names != null) {
				for (int i = 0; i < names.length; i++) {
					if (!binEntry.contains(names[i])) {
						modified = true;
						// folders need trailing slash - see bug 306991
						String name = names[i];
						IPath path = new Path(names[i]);
						String extension = path.getFileExtension();
						if (extension == null) {
							if (!name.endsWith("/")) { //$NON-NLS-1$
								name = name + "/"; //$NON-NLS-1$
							}
						}
						binEntry.addToken(name);
					}
				}
			}
		}

		// extra files be added to build.properties
		IPath[] paths = description.getBinIncludes();
		IPath[] prevPaths = before.getBinIncludes();
		if (!isEqual(paths, prevPaths)) {
			// remove old paths
			if (prevPaths != null) {
				for (IPath prevPath : prevPaths) {
					String token = prevPath.toString();
					if (binEntry.contains(token)) {
						binEntry.removeToken(token);
						modified = true;
					}
				}
			}
			// add new paths
			if (paths != null) {
				for (IPath path : paths) {
					String name = path.toString();
					if (!binEntry.contains(name)) {
						binEntry.addToken(name);
						modified = true;
					}
				}
			}
		}
		return modified;
	}

	private boolean createSourceOutputBuildEntries(WorkspaceBuildModel model, IBuildModelFactory factory, IBundleProjectDescription description, IBundleProjectDescription before) throws CoreException {
		boolean modified = false;
		IBundleClasspathEntry[] folders = description.getBundleClasspath();
		IBundleClasspathEntry[] prev = before.getBundleClasspath();
		if (!isEqual(folders, prev)) {
			modified = true;
			// remove the old ones
			String[] oldNames = getLibraryNames(before);
			IBuild build = model.getBuild();
			if (oldNames != null) {
				for (String oldName : oldNames) {
					removeBuildEntry(build, IBuildEntry.JAR_PREFIX + oldName);
					removeBuildEntry(build, IBuildEntry.OUTPUT_PREFIX + oldName);
				}
			}
			// configure the new ones
			if (folders != null && folders.length > 0) {
				for (IBundleClasspathEntry folder : folders) {
					String libraryName = null;
					IPath libPath = folder.getLibrary();
					if (libPath == null) {
						libraryName = "."; //$NON-NLS-1$
					} else {
						libraryName = folder.getLibrary().toString();
					}

					// SOURCE.<LIBRARY_NAME>
					IPath srcFolder = folder.getSourcePath();
					if (srcFolder != null) {
						IBuildEntry entry = getBuildEntry(build, factory, IBuildEntry.JAR_PREFIX + libraryName);
						if (!srcFolder.isEmpty()) {
							entry.addToken(srcFolder.addTrailingSeparator().toString());
						}
						else {
							entry.addToken("."); //$NON-NLS-1$
						}
					}

					// OUTPUT.<LIBRARY_NAME>
					IPath outFolder = folder.getBinaryPath();
					if (srcFolder != null && outFolder == null) {
						// default output folder
						IJavaProject project = JavaCore.create(description.getProject());
						outFolder = project.getOutputLocation().removeFirstSegments(1);
					}
					if (outFolder != null) {
						IBuildEntry entry = getBuildEntry(build, factory, IBuildEntry.OUTPUT_PREFIX + libraryName);
						String token = null;
						if (!outFolder.isEmpty()) {
							token = outFolder.addTrailingSeparator().toString();
						} else {
							token = "."; //$NON-NLS-1$
						}
						if (!entry.contains(token)) {
							entry.addToken(token);
						}
					}

				}
			}
		}
		return modified;
	}

	/**
	 * Gets the specified build entry, creating and adding it if not already present.
	 *
	 * @param build build
	 * @param factory factory to create new entries
	 * @param key the entry to create
	 * @return build entry
	 * @exception CoreException if unable to add the build entry
	 */
	private IBuildEntry getBuildEntry(IBuild build, IBuildModelFactory factory, String key) throws CoreException {
		IBuildEntry entry = build.getEntry(key);
		if (entry == null) {
			entry = factory.createEntry(key);
			build.add(entry);
		}
		return entry;
	}

	/**
	 * Remove a build entry from the model.
	 *
	 * @param build
	 * @param key
	 * @throws CoreException
	 */
	private void removeBuildEntry(IBuild build, String key) throws CoreException {
		IBuildEntry entry = build.getEntry(key);
		if (entry != null) {
			build.remove(entry);
		}
	}
}
