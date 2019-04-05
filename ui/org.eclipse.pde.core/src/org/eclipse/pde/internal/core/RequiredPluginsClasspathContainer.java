/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;

public class RequiredPluginsClasspathContainer extends PDEClasspathContainer implements IClasspathContainer {

	private final IPluginModelBase fModel;
	private IBuild fBuild;

	private IClasspathEntry[] fEntries;
	private boolean addImportedPackages;

	/**
	 * Cached list of {@link IClasspathContributor} from plug-in extensions
	 * @see #getClasspathContributors()
	 */
	private static List<IClasspathContributor> fClasspathContributors = null;

	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	public RequiredPluginsClasspathContainer(IPluginModelBase model) {
		this(model, null);
	}

	public RequiredPluginsClasspathContainer(IPluginModelBase model, IBuild build) {
		fModel = model;
		fBuild = build;
	}

	@Override
	public int getKind() {
		return K_APPLICATION;
	}

	@Override
	public IPath getPath() {
		return PDECore.REQUIRED_PLUGINS_CONTAINER_PATH;
	}

	@Override
	public String getDescription() {
		return PDECoreMessages.RequiredPluginsClasspathContainer_description;
	}

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		if (fModel == null) {
			if (PDECore.DEBUG_CLASSPATH) {
				System.out.println("********Returned an empty container"); //$NON-NLS-1$
			}
			return new IClasspathEntry[0];
		}
		if (fEntries == null) {
			fEntries = computePluginEntries();
		}
		if (PDECore.DEBUG_CLASSPATH) {
			System.out.println("Dependencies for plugin '" + fModel.getPluginBase().getId() + "':"); //$NON-NLS-1$ //$NON-NLS-2$
			for (IClasspathEntry entry : fEntries) {
				System.out.println("\t" + entry); //$NON-NLS-1$
			}
		}
		return fEntries;
	}

	private IClasspathEntry[] computePluginEntries() {
		ArrayList<IClasspathEntry> entries = new ArrayList<>();
		try {
			BundleDescription desc = fModel.getBundleDescription();
			if (desc == null) {
				return new IClasspathEntry[0];
			}

			Map<BundleDescription, ArrayList<Rule>> map = retrieveVisiblePackagesFromState(desc);

			// Add any library entries contributed via classpath contributor extension (Bug 363733)
			for (IClasspathContributor cc : getClasspathContributors()) {
				List<IClasspathEntry> classpathEntries = cc.getInitialEntries(desc);
				if (classpathEntries == null || classpathEntries.isEmpty()) {
					continue;
				}
				entries.addAll(classpathEntries);
			}

			HashSet<BundleDescription> added = new HashSet<>();

			// to avoid cycles, e.g. when a bundle imports a package it exports
			added.add(desc);

			HostSpecification host = desc.getHost();
			if (host != null) {
				addHostPlugin(host, added, map, entries);
			} else if ("true".equals(System.getProperty("pde.allowCycles"))) { //$NON-NLS-1$ //$NON-NLS-2$
				BundleDescription[] fragments = desc.getFragments();
				for (BundleDescription fragment : fragments) {
					if (fragment.isResolved()) {
						addPlugin(fragment, false, map, entries);
					}
				}
			}

			// add dependencies
			BundleSpecification[] required = desc.getRequiredBundles();
			for (BundleSpecification element : required) {
				addDependency((BundleDescription) element.getSupplier(), added, map, entries);
			}

			if (fBuild == null) {
				fBuild = ClasspathUtilCore.getBuild(fModel);
			}
			if (fBuild != null) {
				addSecondaryDependencies(desc, added, entries);
			}

			// add Import-Package
			// sort by symbolicName_version to get a consistent order
			Map<String, BundleDescription> sortedMap = new TreeMap<>();
			Iterator<BundleDescription> iter = map.keySet().iterator();
			while (iter.hasNext()) {
				BundleDescription bundle = iter.next();
				sortedMap.put(bundle.toString(), bundle);
			}

			iter = sortedMap.values().iterator();
			while (iter.hasNext()) {
				BundleDescription bundle = iter.next();
				IPluginModelBase model = PluginRegistry.findModel(bundle);
				if (model != null && model.isEnabled()) {
					addDependencyViaImportPackage(model.getBundleDescription(), added, map, entries);
				}
			}

			if (fBuild != null) {
				addExtraClasspathEntries(added, entries);
			}

		} catch (CoreException e) {
		}
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

	/**
	 * Return the list of {@link IClasspathContributor}s provided by the
	 * <code>org.eclipse.pde.core.pluginClasspathContributors</code> extension point.
	 * @return list of classpath contributors from the extension point
	 */
	synchronized private static List<IClasspathContributor> getClasspathContributors() {
		if (fClasspathContributors == null) {
			fClasspathContributors = new ArrayList<>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.core.pluginClasspathContributors"); //$NON-NLS-1$
			for (IConfigurationElement element : elements) {
				try {
					fClasspathContributors.add((IClasspathContributor) element.createExecutableExtension("class")); //$NON-NLS-1$
				} catch (CoreException e) {
					PDECore.log(e.getStatus());
				}
			}
		}
		return fClasspathContributors;
	}

	private Map<BundleDescription, ArrayList<Rule>> retrieveVisiblePackagesFromState(BundleDescription desc) {
		Map<BundleDescription, ArrayList<Rule>> visiblePackages = new HashMap<>();
		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		addVisiblePackagesFromState(helper, desc, visiblePackages);
		if (desc.getHost() != null) {
			addVisiblePackagesFromState(helper, (BundleDescription) desc.getHost().getSupplier(), visiblePackages);
		}
		return visiblePackages;
	}

	private void addVisiblePackagesFromState(StateHelper helper, BundleDescription desc, Map<BundleDescription, ArrayList<Rule>> visiblePackages) {
		if (desc == null) {
			return;
		}
		ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
		for (ExportPackageDescription export : exports) {
			BundleDescription exporter = export.getExporter();
			if (exporter == null) {
				continue;
			}
			ArrayList<Rule> list = visiblePackages.get(exporter);
			if (list == null) {
				list = new ArrayList<>();
				visiblePackages.put(exporter, list);
			}
			Rule rule = getRule(helper, desc, export);
			if (!list.contains(rule)) {
				list.add(rule);
			}
		}
	}

	private Rule getRule(StateHelper helper, BundleDescription desc, ExportPackageDescription export) {
		Rule rule = new Rule();
		rule.discouraged = helper.getAccessCode(desc, export) == StateHelper.ACCESS_DISCOURAGED;
		String name = export.getName();
		rule.path = (name.equals(".")) ? new Path("*") : new Path(name.replaceAll("\\.", "/") + "/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		return rule;
	}

	protected void addDependencyViaImportPackage(BundleDescription desc, HashSet<BundleDescription> added, Map<BundleDescription, ArrayList<Rule>> map, ArrayList<IClasspathEntry> entries) throws CoreException {
		if (desc == null || !added.add(desc)) {
			return;
		}

		addPlugin(desc, true, map, entries);

		if (hasExtensibleAPI(desc) && desc.getContainingState() != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (BundleDescription fragment : fragments) {
				if (fragment.isResolved()) {
					addDependencyViaImportPackage(fragment, added, map, entries);
				}
			}
		}
	}

	private void addDependency(BundleDescription desc, HashSet<BundleDescription> added, Map<BundleDescription, ArrayList<Rule>> map, ArrayList<IClasspathEntry> entries) throws CoreException {
		addDependency(desc, added, map, entries, true);
	}

	private void addDependency(BundleDescription desc, HashSet<BundleDescription> added, Map<BundleDescription, ArrayList<Rule>> map, ArrayList<IClasspathEntry> entries, boolean useInclusion) throws CoreException {
		if (desc == null || !added.add(desc)) {
			return;
		}

		BundleDescription[] fragments = hasExtensibleAPI(desc) ? desc.getFragments() : new BundleDescription[0];

		// add fragment patches before host
		for (BundleDescription fragment : fragments) {
			if (fragment.isResolved() && ClasspathUtilCore.isPatchFragment(fragment)) {
				addDependency(fragment, added, map, entries, useInclusion);
			}
		}

		addPlugin(desc, useInclusion, map, entries);

		// add fragments that are not patches after the host
		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i].isResolved() && !ClasspathUtilCore.isPatchFragment(fragments[i])) {
				addDependency(fragments[i], added, map, entries, useInclusion);
			}
		}

		BundleSpecification[] required = desc.getRequiredBundles();
		for (BundleSpecification element : required) {
			if (element.isExported()) {
				addDependency((BundleDescription) element.getSupplier(), added, map, entries, useInclusion);
			}
		}

		if (addImportedPackages) {
			// add Import-Package
			ImportPackageSpecification[] imports = desc.getImportPackages();
			for (ImportPackageSpecification importSpec : imports) {
				BaseDescription supplier = importSpec.getSupplier();
				if (supplier instanceof ExportPackageDescription) {
					addDependencyViaImportPackage(((ExportPackageDescription) supplier).getExporter(), added, map,
							entries);
				}
			}
		}
	}

	private boolean addPlugin(BundleDescription desc, boolean useInclusions, Map<BundleDescription, ArrayList<Rule>> map, ArrayList<IClasspathEntry> entries) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(desc);
		if (model == null || !model.isEnabled()) {
			return false;
		}

		IResource resource = model.getUnderlyingResource();
		Rule[] rules = useInclusions ? getInclusions(map, model) : null;

		BundleDescription hostBundle = fModel.getBundleDescription();
		if (desc == null) {
			return false;
		}

		// Add any library entries contributed via classpath contributor extension (Bug 363733)
		for (IClasspathContributor cc : getClasspathContributors()) {
			List<IClasspathEntry> classpathEntries = cc.getEntriesForDependency(hostBundle, desc);
			if (classpathEntries == null || classpathEntries.isEmpty()) {
				continue;
			}
			entries.addAll(classpathEntries);
		}

		if (resource != null) {
			addProjectEntry(resource.getProject(), rules, entries);
		} else {
			addExternalPlugin(model, rules, entries);
		}
		return true;
	}

	private Rule[] getInclusions(Map<BundleDescription, ArrayList<Rule>> map, IPluginModelBase model) {
		BundleDescription desc = model.getBundleDescription();
		if (desc == null || "false".equals(System.getProperty("pde.restriction")) //$NON-NLS-1$ //$NON-NLS-2$
				|| !(fModel instanceof IBundlePluginModelBase) || TargetPlatformHelper.getTargetVersion() < 3.1) {
			return null;
		}

		Rule[] rules;

		if (desc.getHost() != null) {
			rules = getInclusions(map, (BundleDescription) desc.getHost().getSupplier());
		} else {
			rules = getInclusions(map, desc);
		}

		return (rules.length == 0 && !ClasspathUtilCore.hasBundleStructure(model)) ? null : rules;
	}

	private Rule[] getInclusions(Map<BundleDescription, ArrayList<Rule>> map, BundleDescription desc) {
		ArrayList<?> list = map.get(desc);
		return list != null ? (Rule[]) list.toArray(new Rule[list.size()]) : new Rule[0];
	}

	private void addHostPlugin(HostSpecification hostSpec, HashSet<BundleDescription> added, Map<BundleDescription, ArrayList<Rule>> map, ArrayList<IClasspathEntry> entries) throws CoreException {
		BaseDescription desc = hostSpec.getSupplier();

		if (desc instanceof BundleDescription) {
			BundleDescription host = (BundleDescription) desc;

			// add host plug-in
			if (added.add(host) && addPlugin(host, false, map, entries)) {
				BundleSpecification[] required = host.getRequiredBundles();
				for (BundleSpecification bundleSpec : required) {
					addDependency((BundleDescription) bundleSpec.getSupplier(), added, map, entries);
				}

				// add Import-Package
				ImportPackageSpecification[] imports = host.getImportPackages();
				for (ImportPackageSpecification importSpec : imports) {
					BaseDescription supplier = importSpec.getSupplier();
					if (supplier instanceof ExportPackageDescription) {
						addDependencyViaImportPackage(((ExportPackageDescription) supplier).getExporter(), added, map, entries);
					}
				}
			}
		}
	}

	private boolean hasExtensibleAPI(BundleDescription desc) {
		IPluginModelBase model = PluginRegistry.findModel(desc);
		return model != null ? ClasspathUtilCore.hasExtensibleAPI(model) : false;
	}

	protected void addExtraClasspathEntries(HashSet<BundleDescription> added, ArrayList<IClasspathEntry> entries) {
		IBuildEntry[] buildEntries = fBuild.getBuildEntries();
		for (IBuildEntry entry : buildEntries) {
			String name = entry.getName();
			if (name.equals(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH) || name.startsWith(IBuildPropertiesConstants.PROPERTY_EXTRAPATH_PREFIX)) {
				addExtraClasspathEntries(added, entries, entry.getTokens());
			}
		}
	}

	protected void addExtraClasspathEntries(HashSet<BundleDescription> added, ArrayList<IClasspathEntry> entries, String[] tokens) {
		for (String token : tokens) {
			IPath path = Path.fromPortableString(token);
			if (!path.isAbsolute()) {
				File file = new File(fModel.getInstallLocation(), path.toString());
				if (file.exists()) {
					IFile resource = PDECore.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
					if (resource != null && resource.getProject().equals(fModel.getUnderlyingResource().getProject())) {
						addExtraLibrary(resource.getFullPath(), null, entries);
						continue;
					}
				}
				if (path.segmentCount() >= 3 && "..".equals(path.segment(0))) { //$NON-NLS-1$
					path = path.removeFirstSegments(1);
					path = Path.fromPortableString("platform:/plugin/").append(path); //$NON-NLS-1$
				} else {
					continue;
				}
			}

			if (!path.toPortableString().startsWith("platform:")) { //$NON-NLS-1$
				addExtraLibrary(path, null, entries);
			} else {
				int count = path.getDevice() == null ? 4 : 3;
				if (path.segmentCount() >= count) {
					String pluginID = path.segment(count - 2);
					IPluginModelBase model = PluginRegistry.findModel(pluginID);
					if (model != null && model.isEnabled()) {
						path = path.setDevice(null);
						path = path.removeFirstSegments(count - 1);
						IResource underlyingResource = model.getUnderlyingResource();
						if (underlyingResource == null) {
							IPath result = PDECore.getDefault().getModelManager().getExternalModelManager()
									.getNestedLibrary(model, path.toString());
							if (result != null) {
								addExtraLibrary(result, model, entries);
							}
						} else {
							IProject project = underlyingResource.getProject();
							IFile file = project.getFile(path);
							if (file.exists()) {
								addExtraLibrary(file.getFullPath(), model, entries);
							}
						}
					}
				}
			}
		}
	}

	private void addSecondaryDependencies(BundleDescription desc, HashSet<BundleDescription> added, ArrayList<IClasspathEntry> entries) {
		try {
			IBuildEntry entry = fBuild.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (String pluginId : tokens) {
					// Get PluginModelBase first to resolve system.bundle entry if it exists
					IPluginModelBase model = PluginRegistry.findModel(pluginId);
					if (model != null) {
						BundleDescription bundleDesc = model.getBundleDescription();
						if (added.contains(bundleDesc)) {
							continue;
						}
						Map<BundleDescription, ArrayList<Rule>> rules = new HashMap<>();
						findExportedPackages(bundleDesc, desc, rules);
						addDependency(bundleDesc, added, rules, entries, true);
					}
				}
			}
		} catch (CoreException e) {
			return;
		}
	}

	protected final void findExportedPackages(BundleDescription desc, BundleDescription projectDesc, Map<BundleDescription, ArrayList<Rule>> map) {
		if (desc != null) {
			Stack<BaseDescription> stack = new Stack<>();
			stack.add(desc);
			while (!stack.isEmpty()) {
				BundleDescription bdesc = (BundleDescription) stack.pop();
				ExportPackageDescription[] expkgs = bdesc.getExportPackages();
				ArrayList<Rule> rules = new ArrayList<>();
				for (ExportPackageDescription expkg : expkgs) {
					Rule rule = new Rule();
					rule.discouraged = restrictPackage(projectDesc, expkg);
					rule.path = new Path(expkg.getName().replaceAll("\\.", "/") + "/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					rules.add(rule);
				}
				map.put(bdesc, rules);

				// Look at re-exported Require-Bundles for any other exported packages
				BundleSpecification[] requiredBundles = bdesc.getRequiredBundles();
				for (BundleSpecification requiredBundle : requiredBundles) {
					if (requiredBundle.isExported()) {
						BaseDescription bd = requiredBundle.getSupplier();
						if (bd != null && bd instanceof BundleDescription) {
							stack.add(bd);
						}
					}
				}
			}
		}
	}

	private boolean restrictPackage(BundleDescription desc, ExportPackageDescription pkg) {
		String[] friends = (String[]) pkg.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
		if (friends != null) {
			String symbolicName = desc.getSymbolicName();
			for (String friend : friends) {
				if (symbolicName.equals(friend)) {
					return false;
				}

			}
			return true;
		}
		return (((Boolean) pkg.getDirective(ICoreConstants.INTERNAL_DIRECTIVE)).booleanValue());
	}

	private void addExtraLibrary(IPath path, IPluginModelBase model, ArrayList<IClasspathEntry> entries) {
		if (path.segmentCount() > 1) {
			IPath srcPath = null;
			if (model != null) {
				IPath shortPath = path.removeFirstSegments(path.matchingFirstSegments(new Path(model.getInstallLocation())));
				srcPath = ClasspathUtilCore.getSourceAnnotation(model, shortPath.toString());
			} else {
				String filename = ClasspathUtilCore.getSourceZipName(path.lastSegment());
				IPath candidate = path.removeLastSegments(1).append(filename);
				if (PDECore.getWorkspace().getRoot().getFile(candidate).exists()) {
					srcPath = candidate;
				}
			}
			IClasspathEntry clsEntry = JavaCore.newLibraryEntry(path, srcPath, null);
			if (!entries.contains(clsEntry)) {
				entries.add(clsEntry);
			}
		}
	}

	/**
	 * Tries to compute a full set of bundle dependencies, including not exported
	 * bundle dependencies and bundles contributing packages possibly imported by
	 * any of bundles in the dependency graph.
	 *
	 * @return never null, but possibly empty project list which all projects in the
	 *         workspace this container depends on, directly or indirectly.
	 */
	public List<IProject> getAllProjectDependencies() {
		List<IProject> projects = new ArrayList<>();
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		try {
			addImportedPackages = true;
			IClasspathEntry[] entries = computePluginEntries();
			for (IClasspathEntry cpe : entries) {
				if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					IProject project = root.getProject(cpe.getPath().lastSegment());
					if (project.exists()) {
						projects.add(project);
					}
				}
			}
		} finally {
			addImportedPackages = false;
		}
		return projects;
	}
}
