/*******************************************************************************
 *  Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Christoph Läubrich - add bnd support
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import static org.eclipse.pde.internal.core.DependencyManager.Options.INCLUDE_OPTIONAL_DEPENDENCIES;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathAttribute;
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
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.PDEClasspathContainer.Rule;
import org.eclipse.pde.internal.core.bnd.BndProjectManager;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.osgi.resource.Resource;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;

class RequiredPluginsClasspathContainer implements IClasspathContainer {

	@SuppressWarnings("nls")
	private static final Set<String> JUNIT5_RUNTIME_PLUGINS = Set.of("org.junit", //
			"junit-jupiter-engine", // BSN of the bundle from Maven-Central
			"org.junit.jupiter.engine"); // BSN of the bundle from Eclipse-Orbit
	@SuppressWarnings("nls")
	private static final Set<String> JUNIT5_API_PLUGINS = Set.of( //
			"junit-jupiter-api", // BSN of the bundle from Maven-Central
			"org.junit.jupiter.api"); // BSN of the bundle from Eclipse-Orbit

	private final IPluginModelBase fModel;
	private final IBuild fBuild;

	private List<BundleDescription> junit5RuntimeClosure;
	private IClasspathEntry[] fEntries;
	private boolean addImportedPackages;

	/**
	 * Cached list of {@link IClasspathContributor} from plug-in extensions
	 *
	 * @see #getClasspathContributors()
	 */
	private static List<IClasspathContributor> fClasspathContributors;

	private final IProject project;

	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	RequiredPluginsClasspathContainer(IPluginModelBase model, IProject project) {
		fModel = model;
		IBuildModel buildModel;
		try {
			buildModel = PluginRegistry.createBuildModel(model);
		} catch (CoreException e) {
			buildModel = null;
		}
		fBuild = buildModel != null ? buildModel.getBuild() : null;
		this.project = project;
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
		try {
			return computeEntries();
		} catch (CoreException e) {
			return new IClasspathEntry[0];
		}
	}

	IClasspathEntry[] computeEntries() throws CoreException {
		if (fEntries == null) {
			if (fModel == null) {
				fEntries = computePluginEntriesByProject();
			} else {
				fEntries = computePluginEntriesByModel().toArray(IClasspathEntry[]::new);
			}
			if (PDECore.DEBUG_CLASSPATH) {
				System.out.println("Dependencies for plugin '" + fModel.getPluginBase().getId() + "':"); //$NON-NLS-1$ //$NON-NLS-2$
				for (IClasspathEntry entry : fEntries) {
					System.out.println("\t" + entry); //$NON-NLS-1$
				}
			}
			// see for example
			// https://github.com/eclipse-jdt/eclipse.jdt.core/issues/4133
			// we need to make sure that regular entries are before test
			// entries, this is also the "natural" order of items in regular
			// classpath
			Arrays.sort(fEntries, Comparator.comparingInt(cpe -> isTestEntry(cpe) ? 1 : 0));
		}
		return fEntries;
	}

	private boolean isTestEntry(IClasspathEntry cpe) {
		for (IClasspathAttribute attr : cpe.getExtraAttributes()) {
			if (IClasspathAttribute.TEST.equals(attr.getName())) {
				return Boolean.parseBoolean(attr.getValue());
			}
		}
		return false;
	}

	private IClasspathEntry[] computePluginEntriesByProject() {
		try {
			Optional<Project> bndProject = BndProjectManager.getBndProject(project);
			if (bndProject.isPresent()) {
				try (Project bnd = bndProject.get()) {
					IClasspathEntry[] entries = BndProjectManager
							.getClasspathEntries(bnd, project.getWorkspace().getRoot()).toArray(IClasspathEntry[]::new);
					debugProblems(bnd);
					project.setSessionProperty(PDECore.BND_CLASSPATH_INSTRUCTION_FILE,
							project.getFile(BndProject.INSTRUCTIONS_FILE));
					return entries;
				}
			}
			// maybe it is a plain bnd workspace project, the classpath
			// container should support these as well
			IFile bnd = project.getFile(Project.BNDFILE);
			if (bnd.exists()) {
				IPath location = project.getLocation();
				if (location != null) {
					File projectDir = location.toFile();
					if (projectDir != null) {
						Project plainBnd = Workspace.getProject(projectDir);
						plainBnd.refresh();
						IClasspathEntry[] entries = BndProjectManager
								.getClasspathEntries(plainBnd, project.getWorkspace().getRoot())
								.toArray(IClasspathEntry[]::new);
						debugProblems(plainBnd);
						plainBnd.clear();
						project.setSessionProperty(PDECore.BND_CLASSPATH_INSTRUCTION_FILE, bnd);
						return entries;
					}
				}
			}
		} catch (Exception e) {
			PDECore.getDefault().getLog().error("Can't compute classpath!", e); //$NON-NLS-1$
		}
		if (PDECore.DEBUG_CLASSPATH) {
			System.out.println("********Returned an empty container"); //$NON-NLS-1$
		}
		return new IClasspathEntry[0];
	}

	protected void debugProblems(Project bnd) {
		if (PDECore.DEBUG_CLASSPATH) {
			for (String err : bnd.getErrors()) {
				System.out.println("ERR: " + err); //$NON-NLS-1$
			}
			for (String warn : bnd.getWarnings()) {
				System.out.println("WARN: " + warn); //$NON-NLS-1$
			}
		}
	}

	private List<IClasspathEntry> computePluginEntriesByModel() throws CoreException {
		List<IClasspathEntry> entries = new ArrayList<>();
			BundleDescription desc = fModel.getBundleDescription();
			if (desc == null) {
				return List.of();
			}

			Map<BundleDescription, List<Rule>> map = retrieveVisiblePackagesFromState(desc);

			// Add any library entries contributed via classpath contributor
			// extension (Bug 363733)
			getClasspathContributors().map(cc -> cc.getInitialEntries(desc))
					.flatMap(Collection::stream).forEach(entries::add);

			Set<BundleDescription> added = new HashSet<>();

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

			if (fBuild != null) {
				addSecondaryDependencies(desc, added, entries);
			}
			addBndClasspath(desc, added, entries);

			// add Import-Package
			// sort by symbolicName_version to get a consistent order
			Map<String, BundleDescription> sortedMap = new TreeMap<>();
			for (BundleDescription bundle : map.keySet()) {
				sortedMap.put(bundle.toString(), bundle);
			}
			for (Resource bundle : sortedMap.values()) {
				IPluginModelBase model = PluginRegistry.findModel(bundle);
				if (model != null && model.isEnabled()) {
					addDependencyViaImportPackage(model.getBundleDescription(), added, map, entries);
				}
			}

			if (fBuild != null) {
				addExtraClasspathEntries(entries);
			}

			addJunit5RuntimeDependencies(added, entries);
			addImplicitDependencies(desc, added, entries);

		return entries;
	}

	private void addBndClasspath(BundleDescription desc, Set<BundleDescription> added, List<IClasspathEntry> entries) {
		try {
			Optional<Project> bndProject = BndProjectManager.getBndProject(project);
			if (bndProject.isPresent()) {
				Project bnd = bndProject.get();
				for (Container container : bnd.getBuildpath()) {
					addExtraModel(desc, added, entries, container.getBundleSymbolicName());
				}
				for (Container container : bnd.getTestpath()) {
					addExtraModel(desc, added, entries, container.getBundleSymbolicName());
				}
				String cp = bnd.getProperty(Constants.CLASSPATH);
				if (cp != null) {
					String[] tokens = cp.split(","); //$NON-NLS-1$
					for (int i = 0; i < tokens.length; i++) {
						tokens[i] = tokens[i].trim();
					}
					addExtraClasspathEntries(entries, tokens);
				}
			}
		} catch (Exception e) {
			PDECore.logException(e, "Can't set classpath from bnd!"); //$NON-NLS-1$
		}
	}

	/**
	 * Return the list of {@link IClasspathContributor}s provided by the
	 * <code>org.eclipse.pde.core.pluginClasspathContributors</code> extension
	 * point.
	 *
	 * @return list of classpath contributors from the extension point
	 */
	private static synchronized Stream<IClasspathContributor> getClasspathContributors() {
		if (fClasspathContributors == null) {
			fClasspathContributors = new ArrayList<>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry
					.getConfigurationElementsFor("org.eclipse.pde.core.pluginClasspathContributors"); //$NON-NLS-1$
			for (IConfigurationElement element : elements) {
				try {
					fClasspathContributors.add((IClasspathContributor) element.createExecutableExtension("class")); //$NON-NLS-1$
				} catch (CoreException e) {
					PDECore.log(e.getStatus());
				}
			}
		}
		return Stream.concat(fClasspathContributors.stream(), PDECore.getDefault().getClasspathContributors());
	}

	private Map<BundleDescription, List<Rule>> retrieveVisiblePackagesFromState(BundleDescription desc) {
		Map<BundleDescription, List<Rule>> visiblePackages = new HashMap<>();
		StateHelper helper = BundleHelper.getPlatformAdmin().getStateHelper();
		addVisiblePackagesFromState(helper, desc, visiblePackages);
		if (desc.getHost() != null) {
			addVisiblePackagesFromState(helper, (BundleDescription) desc.getHost().getSupplier(), visiblePackages);
		}
		return visiblePackages;
	}

	private void addVisiblePackagesFromState(StateHelper helper, BundleDescription desc,
			Map<BundleDescription, List<Rule>> visiblePackages) {
		if (desc == null) {
			return;
		}
		ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
		for (ExportPackageDescription export : exports) {
			BundleDescription exporter = export.getExporter();
			if (exporter == null) {
				continue;
			}
			List<Rule> list = visiblePackages.computeIfAbsent(exporter, e -> new ArrayList<>());
			Rule rule = getRule(helper, desc, export);
			if (!list.contains(rule)) {
				list.add(rule);
			}
		}
	}

	private Rule getRule(StateHelper helper, BundleDescription desc, ExportPackageDescription export) {
		boolean discouraged = helper.getAccessCode(desc, export) == StateHelper.ACCESS_DISCOURAGED;
		String name = export.getName();
		IPath path = name.equals(".") ? IPath.fromOSString("*") : IPath.fromOSString(name.replace('.', '/') + "/*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new Rule(path, discouraged);
	}

	protected void addDependencyViaImportPackage(BundleDescription desc, Set<BundleDescription> added,
			Map<BundleDescription, List<Rule>> map, List<IClasspathEntry> entries) throws CoreException {
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

	private void addDependency(BundleDescription desc, Set<BundleDescription> added,
			Map<BundleDescription, List<Rule>> map, List<IClasspathEntry> entries) throws CoreException {
		addDependency(desc, added, map, entries, true);
	}

	private void addDependency(BundleDescription desc, Set<BundleDescription> added,
			Map<BundleDescription, List<Rule>> map, List<IClasspathEntry> entries, boolean useInclusion)
			throws CoreException {
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
				if (supplier instanceof ExportPackageDescription exportPackageDescription) {
					addDependencyViaImportPackage(exportPackageDescription.getExporter(), added, map, entries);
				}
			}
		}
	}

	private boolean addPlugin(BundleDescription desc, boolean useInclusions, Map<BundleDescription, List<Rule>> map,
			List<IClasspathEntry> entries) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel((Resource) desc);
		if (model == null || !model.isEnabled()) {
			return false;
		}

		IResource resource = model.getUnderlyingResource();
		List<Rule> rules = useInclusions ? getInclusions(map, model) : null;

		BundleDescription hostBundle = fModel.getBundleDescription();
		if (desc == null) {
			return false;
		}

		// Add any library entries contributed via classpath contributor
		// extension (Bug 363733)
		getClasspathContributors().map(cc -> cc.getEntriesForDependency(hostBundle, desc)).flatMap(Collection::stream)
				.forEach(entries::add);
		if (resource != null) {
			PDEClasspathContainer.addProjectEntry(resource.getProject(), rules,
					model.getPluginBase().exportsExternalAnnotations(), entries);
		} else {
			PDEClasspathContainer.addExternalPlugin(model, rules, entries);
		}
		return true;
	}

	private List<Rule> getInclusions(Map<BundleDescription, List<Rule>> map, IPluginModelBase model) {
		BundleDescription desc = model.getBundleDescription();
		if (desc == null || "false".equals(System.getProperty("pde.restriction")) //$NON-NLS-1$ //$NON-NLS-2$
				|| !(fModel instanceof IBundlePluginModelBase) || TargetPlatformHelper.getTargetVersion() < 3.1) {
			return null;
		}

		if (desc.getHost() != null) {
			desc = (BundleDescription) desc.getHost().getSupplier();
		}
		List<Rule> rules = map.getOrDefault(desc, List.of());
		return (rules.isEmpty() && !ClasspathUtilCore.hasBundleStructure(model)) ? null : rules;
	}

	private void addHostPlugin(HostSpecification hostSpec, Set<BundleDescription> added,
			Map<BundleDescription, List<Rule>> map, List<IClasspathEntry> entries) throws CoreException {
		BaseDescription desc = hostSpec.getSupplier();

		if (desc instanceof BundleDescription host) {
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
					if (supplier instanceof ExportPackageDescription exportPackageDescription) {
						addDependencyViaImportPackage(exportPackageDescription.getExporter(), added, map, entries);
					}
				}
			}
		}
	}

	private boolean hasExtensibleAPI(Resource desc) {
		IPluginModelBase model = PluginRegistry.findModel(desc);
		return model != null && ClasspathUtilCore.hasExtensibleAPI(model);
	}

	protected void addExtraClasspathEntries(List<IClasspathEntry> entries) {
		IBuildEntry[] buildEntries = fBuild.getBuildEntries();
		for (IBuildEntry entry : buildEntries) {
			String name = entry.getName();
			if (name.equals(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH)
					|| name.startsWith(IBuildPropertiesConstants.PROPERTY_EXTRAPATH_PREFIX)) {
				addExtraClasspathEntries(entries, entry.getTokens());
			}
		}
	}

	protected void addExtraClasspathEntries(List<IClasspathEntry> entries, String[] tokens) {
		for (String token : tokens) {
			IPath path = IPath.fromPortableString(token);
			if (!path.isAbsolute()) {
				File file = new File(fModel.getInstallLocation(), path.toString());
				if (file.exists()) {
					IFile resource = PDECore.getWorkspace().getRoot()
							.getFileForLocation(IPath.fromOSString(file.getAbsolutePath()));
					if (resource != null && resource.getProject().equals(fModel.getUnderlyingResource().getProject())) {
						addExtraLibrary(resource.getFullPath(), null, entries);
						continue;
					}
				}
				if (path.segmentCount() >= 3 && "..".equals(path.segment(0))) { //$NON-NLS-1$
					path = path.removeFirstSegments(1);
					path = IPath.fromPortableString("platform:/plugin/").append(path); //$NON-NLS-1$
				} else {
					continue;
				}
			}

			if (!path.toPortableString().startsWith("platform:")) { //$NON-NLS-1$
				addExtraLibrary(path, null, entries);
			} else {
				int count = path.getDevice() == null ? 4 : 3;
				int segments = path.segmentCount();
				if (segments >= count - 1) {
					String pluginID = path.segment(count - 2);
					IPluginModelBase model = PluginRegistry.findModel(pluginID);
					if (model != null && model.isEnabled()) {
						path = path.setDevice(null);
						path = path.removeFirstSegments(count - 1);
						IResource underlyingResource = model.getUnderlyingResource();
						if (underlyingResource == null) {
							if (path.segmentCount() == 0) {
								String installLocation = model.getInstallLocation();
								if (installLocation != null) {
									addExtraLibrary(IPath.fromOSString(installLocation), model, entries);
								}
							} else {
								IPath result = PDECore.getDefault().getModelManager().getExternalModelManager()
										.getNestedLibrary(model, path.toString());
								if (result != null) {
									addExtraLibrary(result, model, entries);
								}
							}
						} else {
							if (path.segmentCount() == 0) {
								IProject p = underlyingResource.getProject();
								IClasspathEntry clsEntry = JavaCore.newProjectEntry(p.getFullPath());
								if (!entries.contains(clsEntry)) {
									entries.add(clsEntry);
								}
							} else {
								IFile file = underlyingResource.getProject().getFile(path);
								if (file.exists()) {
									addExtraLibrary(file.getFullPath(), model, entries);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Adds JUnit5 dependencies that are required at runtime in eclipse, but not
	 * at compile-time or in tycho.
	 */
	private void addJunit5RuntimeDependencies(Set<BundleDescription> added, List<IClasspathEntry> entries)
			throws CoreException {
		if (!containsJunit5Dependency(added)) {
			return;
		}

		if (junit5RuntimeClosure == null) {
			junit5RuntimeClosure = collectJunit5RuntimeRequirements();
		}

		String id = fModel.getPluginBase().getId();
		if (id != null && junit5RuntimeClosure.stream().map(BundleDescription::getSymbolicName).anyMatch(id::equals)) {
			return; // never extend the classpath of a junit bundle
		}

		for (BundleDescription desc : junit5RuntimeClosure) {
			if (added.contains(desc)) {
				continue; // bundle has explicit dependency
			}

			// add dependency with exclude all rule
			Map<BundleDescription, List<Rule>> rules = Map.of(desc, List.of());
			addPlugin(desc, true, rules, entries);
		}
	}

	private boolean containsJunit5Dependency(Collection<BundleDescription> dependencies) {
		return dependencies.stream().map(BundleDescription::getSymbolicName).anyMatch(JUNIT5_API_PLUGINS::contains);
	}

	private static List<BundleDescription> collectJunit5RuntimeRequirements() {
		List<BundleDescription> roots = JUNIT5_RUNTIME_PLUGINS.stream().map(PluginRegistry::findModel)
				.filter(Objects::nonNull).filter(IPluginModelBase::isEnabled)
				.map(IPluginModelBase::getBundleDescription).toList();
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(roots,
				INCLUDE_OPTIONAL_DEPENDENCIES);
		String systemBundleBSN = TargetPlatformHelper.getPDEState().getSystemBundle();
		return closure.stream().filter(b -> !b.getSymbolicName().equals(systemBundleBSN))
				.sorted(Comparator.comparing(BundleDescription::getSymbolicName)).toList();
	}

	private void addSecondaryDependencies(BundleDescription desc, Set<BundleDescription> added,
			List<IClasspathEntry> entries) {
		try {
			IBuildEntry entry = fBuild.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (String pluginId : tokens) {
					// Get PluginModelBase first to resolve system.bundle entry
					// if it exists
					addExtraModel(desc, added, entries, pluginId);
				}
			}
		} catch (CoreException e) {
			return;
		}
	}

	private void addImplicitDependencies(BundleDescription desc, Set<BundleDescription> added,
			List<IClasspathEntry> entries) throws CoreException {
		for (NameVersionDescriptor entry : DependencyManager.getImplicitDependencies()) {
			String id = entry.getId();
			if (id != null) {
				addExtraModel(desc, added, entries, id);
			}
		}
	}

	private void addExtraModel(BundleDescription desc, Set<BundleDescription> added, List<IClasspathEntry> entries,
			String pluginId) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(pluginId);
		if (model != null) {
			BundleDescription bundleDesc = model.getBundleDescription();
			if (added.contains(bundleDesc)) {
				return;
			}
			Map<BundleDescription, List<Rule>> rules = new HashMap<>();
			findExportedPackages(bundleDesc, desc, rules);
			addDependency(bundleDesc, added, rules, entries, true);
		}
	}

	protected final void findExportedPackages(BundleDescription desc, BundleDescription projectDesc,
			Map<BundleDescription, List<Rule>> map) {
		if (desc != null) {
			Queue<BundleDescription> queue = new ArrayDeque<>();
			queue.add(desc);
			while (!queue.isEmpty()) {
				BundleDescription bdesc = queue.remove();
				ExportPackageDescription[] expkgs = bdesc.getExportPackages();
				List<Rule> rules = new ArrayList<>();
				for (ExportPackageDescription expkg : expkgs) {
					boolean discouraged = restrictPackage(projectDesc, expkg);
					IPath path = IPath.fromOSString(expkg.getName().replace('.', '/') + "/*"); //$NON-NLS-1$
					rules.add(new Rule(path, discouraged));
				}
				map.put(bdesc, rules);

				// Look at re-exported Require-Bundles for any other exported
				// packages
				BundleSpecification[] requiredBundles = bdesc.getRequiredBundles();
				for (BundleSpecification requiredBundle : requiredBundles) {
					if (requiredBundle.isExported()) {
						BaseDescription bd = requiredBundle.getSupplier();
						if (bd instanceof BundleDescription description) {
							queue.add(description);
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
			return Arrays.stream(friends).noneMatch(symbolicName::equals);
		}
		return (((Boolean) pkg.getDirective(ICoreConstants.INTERNAL_DIRECTIVE)).booleanValue());
	}

	private void addExtraLibrary(IPath path, IPluginModelBase model, List<IClasspathEntry> entries) {
		if (path.segmentCount() > 1) {
			IPath srcPath = null;
			if (model != null) {
				IPath shortPath = path.removeFirstSegments(
						path.matchingFirstSegments(IPath.fromOSString(model.getInstallLocation())));
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

}
