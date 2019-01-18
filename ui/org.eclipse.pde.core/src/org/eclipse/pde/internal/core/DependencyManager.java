/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Karsten Thoms <karsten.thoms@itemis.de> - Bug 522332
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.GenericDescription;
import org.eclipse.osgi.service.resolver.GenericSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.osgi.framework.Constants;

/**
 * Utility class to return bundle id collections for a variety of dependency
 * scenarios
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class DependencyManager {
	/**
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * {@link IPluginModelBase}. The set includes the id of the given model base
	 * as well as all computed implicit / optional dependencies.
	 *
	 * @param model the {@link IPluginModelBase} to compute dependencies for
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set<String> getSelfAndDependencies(IPluginModelBase model, String[] excludeFragments) {
		return getDependencies(Collections.singleton(model), getImplicitDependencies(), TargetPlatformHelper.getState(),
				false, true, toSet(excludeFragments));
	}

	/**
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * {@link IPluginModelBase}s. The set includes the ids of the given model bases
	 * as well as all computed implicit / optional dependencies.
	 *
	 * @param models the array of {@link IPluginModelBase}s to compute dependencies for
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set<String> getSelfandDependencies(IPluginModelBase[] models, String[] excludeFragments) {
		return getDependencies(getPluginModels(models), getImplicitDependencies(), TargetPlatformHelper.getState(),
				false, true, toSet(excludeFragments));
	}

	/**
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * objects from the given {@link State}.
	 * The set does not include the ids of the given objects
	 * and only includes the given set of implicit dependencies.
	 *
	 * @param selected the group of objects to compute dependencies for. Any items
	 * in this array that are not {@link IPluginModelBase}s are ignored.
	 * @param implicit the array of additional implicit dependencies to add to the {@link Set}
	 * @param state the {@link State} to compute the dependencies in
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set<String> getDependencies(Object[] selected, String[] implicit, State state, String[] excludeFragments) {
		return getDependencies(getPluginModels(selected), implicit, state, true, true, toSet(excludeFragments));
	}

	/**
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * objects. The set does not include the ids of the given objects
	 * but does include the computed set of implicit dependencies.
	 *
	 * @param selected selected the group of objects to compute dependencies for. Any items
	 * in this array that are not {@link IPluginModelBase}s are ignored.
	 * @param includeOptional if optional bundle ids should be included
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set<String> getDependencies(Object[] selected, boolean includeOptional, String[] excludeFragments) {
		return getDependencies(getPluginModels(selected), getImplicitDependencies(), TargetPlatformHelper.getState(),
				true, includeOptional, toSet(excludeFragments));
	}

	/**
	 * Returns the array as a set
	 *
	 * @param array
	 *            array or <code>null</code>
	 * @return set
	 */
	private static Set<String> toSet(String[] array) {
		if (array == null || array.length == 0) {
			return Collections.emptySet();
		}
		return Arrays.stream(array).collect(Collectors.toSet());
	}

	/**
	 * Collects {@link IPluginModelBase}s into a set
	 */
	private static Set<IPluginModelBase> getPluginModels(Object[] selected) {
		return Arrays.stream(selected).filter(IPluginModelBase.class::isInstance).map(IPluginModelBase.class::cast)
				.collect(Collectors.toSet());
	}

	/**
	 * Returns a {@link Set} of bundle ids for the dependents of the given objects
	 * from the given {@link State}. The set additionally only includes the given
	 * set of implicit dependencies.
	 *
	 * @param selected
	 *            selected the group of {@link IPluginModelBase}s to compute
	 *            dependencies for.
	 * @param implicit
	 *            the array of additional implicit dependencies to add to the
	 *            {@link Set}
	 * @param state
	 *            the {@link State} to compute the dependencies in
	 * @param removeSelf
	 *            if the id of one of the bundles were are computing dependencies
	 *            for should be included in the result {@link Set} or not
	 * @param includeOptional
	 *            if optional bundle ids should be included
	 * @param excludeFragments
	 *            a collection of <b>fragment</b> bundle symbolic names to exclude
	 *            from the dependency resolution
	 * @return a set of bundle IDs
	 */
	private static Set<String> getDependencies(Set<IPluginModelBase> selected, String[] implicit, State state,
			boolean removeSelf,
			boolean includeOptional, Set<String> excludeFragments) {
		Set<String> bundleIds = new TreeSet<>();
		Set<IPluginModelBase> models = new HashSet<>(selected);

		// For all selected bundles add their bundle dependencies.
		// Also consider plugin extensions and their dependencies.
		for (IPluginModelBase model : selected) {
			addBundleAndDependencies(model.getBundleDescription(), bundleIds, includeOptional, excludeFragments);
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (IPluginExtension extension : extensions) {
				String point = extension.getPoint();
				if (point != null) {
					int dot = point.lastIndexOf('.');
					if (dot != -1) {
						String id = point.substring(0, dot);
						addBundleAndDependencies(state.getBundle(id, null), bundleIds, includeOptional, excludeFragments);
					}
				}
			}
		}

		for (String element : implicit) {
			addBundleAndDependencies(state.getBundle(element, null), bundleIds, includeOptional, excludeFragments);
		}

		Set<String> selectedBundleIds = selected.stream().map(model -> model.getPluginBase().getId())
				.collect(Collectors.toSet());

		// where any bundle ids collected that did not belong to the already selected
		// bunlde set? => recursively collect dependencies with them included
		boolean hasAdditionallySelectedBundles = bundleIds.stream()
				.anyMatch(bundleId -> !selectedBundleIds.contains(bundleId));
		if (hasAdditionallySelectedBundles) {
			// validate all models and try to add bundles that resolve constraint violations
			for (IPluginModelBase model : DependencyManager.getDependencies(TargetPlatformHelper.getState(),
					models.toArray(new IPluginModelBase[models.size()]))) {
				bundleIds.add(model.getBundleDescription().getSymbolicName());
			}

			// build array with all selected plus calculated dependencies and recurse
			// loop ends when no more additional dependencies are calculated
			for (String id : bundleIds) {
				ModelEntry entry = PluginRegistry.findEntry(id);
				if (entry != null) {
					models.add(entry.getModel());
				}
			}

			Set<String> additionalIds = getDependencies(models, implicit, state, removeSelf, includeOptional,
					excludeFragments);
			bundleIds.addAll(additionalIds);
		}

		if (removeSelf) {
			bundleIds.removeAll(selectedBundleIds);
		}

		return bundleIds;
	}

	/**
	 * Computes the set of implicit dependencies from the {@link PDEPreferencesManager}
	 * @return a set if bundle ids
	 */
	private static String[] getImplicitDependencies() {
		try {
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (service != null) {
				NameVersionDescriptor[] implicit = service.getWorkspaceTargetDefinition().getImplicitDependencies();
				if (implicit != null) {
					String[] result = new String[implicit.length];
					for (int i = 0; i < implicit.length; i++) {
						result[i] = implicit[i].getId();
					}
					return result;
				}
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return new String[0];
	}

	/**
	 * Recursively adds the given {@link BundleDescription} and its dependents to the given
	 * {@link Set}
	 * @param desc the {@link BundleDescription} to compute dependencies for
	 * @param set the {@link Set} to collect results in
	 * @param includeOptional if optional dependencies should be included
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 */
	private static void addBundleAndDependencies(BundleDescription desc, Set<String> set, boolean includeOptional, Set<String> excludeFragments) {
		if (desc != null && set.add(desc.getSymbolicName())) {
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (includeOptional || !required[i].isOptional()) {
					addBundleAndDependencies((BundleDescription) required[i].getSupplier(), set, includeOptional, excludeFragments);
				}
			}
			ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
			for (ImportPackageSpecification importedPkg : importedPkgs) {
				ExportPackageDescription exporter = (ExportPackageDescription) importedPkg.getSupplier();
				// Continue if the Imported Package is unresolved of the package is optional and don't want optional packages
				if (exporter == null || (!includeOptional && Constants.RESOLUTION_OPTIONAL.equals(importedPkg.getDirective(Constants.RESOLUTION_DIRECTIVE)))) {
					continue;
				}
				addBundleAndDependencies(exporter.getExporter(), set, includeOptional, excludeFragments);
			}
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (!fragments[i].isResolved()) {
					continue;
				}
				String id = fragments[i].getSymbolicName();
				if (!excludeFragments.contains(id)) {
					addBundleAndDependencies(fragments[i], set, includeOptional, excludeFragments);
				}
			}
			HostSpecification host = desc.getHost();
			if (host != null) {
				addBundleAndDependencies((BundleDescription) host.getSupplier(), set, includeOptional, excludeFragments);
			}
		}
	}

	/**
	 * Validates the given models and retrieves bundle IDs that satisfy violated
	 * constraints. This method uses the {@link BundleValidationOperation} to
	 * determine unsatisfied constraints for the given plugin models.
	 *
	 * @param state
	 *            the {@link State} to compute the dependencies in
	 * @param models
	 *            the array of {@link IPluginModelBase}s to compute dependencies for
	 *
	 * @return a set of bundle IDs
	 */
	private static Set<IPluginModelBase> getDependencies(State state, IPluginModelBase[] models) {
		Set<IPluginModelBase> dependencies = new HashSet<>();
		BundleValidationOperation operation = new BundleValidationOperation(models);
		try {
			operation.run(new NullProgressMonitor());
			Map<Object, Object[]> input = operation.getResolverErrors();
			// extract the unsatisfied constraints from the operation's result structure
			VersionConstraint[] unsatisfiedConstraints = input.values().stream()
					.filter(ResolverError[].class::isInstance)
					.map(ResolverError[].class::cast)
					.flatMap(arr -> Arrays.stream(arr))
					.filter(err -> err.getUnsatisfiedConstraint() != null)
					.map(err -> err.getUnsatisfiedConstraint())
					.toArray(VersionConstraint[]::new);

			for (VersionConstraint constraint : unsatisfiedConstraints) {
				// first try to find a solution in the set of additionally computed
				// bundles that satisfy constraints.
				if (dependencies.stream()
						.anyMatch(pmb -> satisfiesConstraint(pmb.getBundleDescription(), constraint))) {
					continue;
				}
				// determine all bundles from the target platform state that satisfy the current
				// constraint
				List<BundleDescription> satisfyingBundles = Arrays.stream(state.getBundles())
						.filter(desc -> satisfiesConstraint(desc, constraint)).collect(Collectors.toList());

				// It is possible to have none, exactly one, or in rare cases multiple bundles
				// that satisfy the constraint.
				for (BundleDescription bundle : satisfyingBundles) {
					ModelEntry entry = PluginRegistry.findEntry(bundle.getSymbolicName());
					if (entry != null) {
						dependencies.add(entry.getModel());
					}
				}
			}
			return dependencies;
		} catch (CoreException e) {
			PDECore.log(e);
			return Collections.emptySet();
		}
	}

	private static boolean satisfiesConstraint(BundleDescription desc, VersionConstraint constraint) {
		if (constraint instanceof GenericSpecification) {
			for (GenericDescription description : desc.getGenericCapabilities()) {
				if (constraint.isSatisfiedBy(description)) {
					return true;
				}
			}
		} else if (constraint instanceof BundleSpecification) {
			return constraint.getName().equals(desc.getName())
					&& constraint.getVersionRange().isIncluded(desc.getVersion());
		}
		return false;
	}

}
