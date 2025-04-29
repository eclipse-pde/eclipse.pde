/*******************************************************************************
 *  Copyright (c) 2005, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 539637 major rework to consider versions and improve runtime behavior
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Resource;

/**
 * Utility class to return bundle description collections for a variety of
 * dependency scenarios
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class DependencyManager {

	private DependencyManager() { // static use only
	}

	public enum Options {
		/** Specifies to include all optional dependencies into the closure. */
		INCLUDE_OPTIONAL_DEPENDENCIES,

		/**
		 * Specifies to include all fragments into the closure (must not be
		 * combined with {@link #INCLUDE_NON_TEST_FRAGMENTS}).
		 */
		INCLUDE_ALL_FRAGMENTS,
		/**
		 * Specifies to include all non-test fragments into the closure (must
		 * not be combined with {@link #INCLUDE_ALL_FRAGMENTS}).
		 */
		INCLUDE_NON_TEST_FRAGMENTS;
	}

	/**
	 * Returns a {@link Set} of bundle descriptions of the given
	 * {@link IPluginModelBase}s and all of their required dependencies
	 * (including optional) and fragments.
	 * <p>
	 * The set includes the descriptions of the given model bases as well as all
	 * transitively computed explicit, implicit (defined in the target-platform)
	 * and optional dependencies. So it is the self-contained closure for all
	 * required dependencies of the given set of plug-ins plus the implicit
	 * dependencies defined in the target platform.
	 * </p>
	 *
	 * @param plugins
	 *            the collection of {@link IPluginModelBase}s to compute
	 *            dependencies for
	 * @return a set of bundle descriptions
	 */
	public static Set<BundleDescription> getSelfAndDependencies(Collection<IPluginModelBase> plugins) {
		Collection<NameVersionDescriptor> implicit = getImplicitDependencies();
		List<BundleDescription> bundles = mergeBundleDescriptions(plugins, implicit, TargetPlatformHelper.getState());
		return findRequirementsClosure(bundles, Options.INCLUDE_OPTIONAL_DEPENDENCIES, Options.INCLUDE_ALL_FRAGMENTS);
	}

	/**
	 * Returns a {@link Set} of bundle descriptions for all required
	 * dependencies of the given objects from the given {@link State}.
	 * <p>
	 * The set includes the descriptions of all transitively computed explicit
	 * dependencies. The set does not include the descriptions of the given
	 * objects and only includes optional dependencies if requested.
	 * </p>
	 *
	 * @param plugins
	 *            the group of {@link IPluginModelBase}s to compute dependencies
	 *            for.
	 * @param implicit
	 *            the array of additional implicit dependencies to add to the
	 *            {@link Set}
	 * @param state
	 *            the {@link State} to compute the dependencies in
	 * @param options
	 *            the specified {@link Options} for computing the closure
	 * @return a set of bundle descriptions
	 */
	public static Set<BundleDescription> getDependencies(Collection<IPluginModelBase> plugins,
			Collection<NameVersionDescriptor> implicit, State state, Options... options) {
		List<BundleDescription> bundles = mergeBundleDescriptions(plugins, implicit, state);
		Set<BundleDescription> closure = findRequirementsClosure(bundles, options);
		plugins.forEach(p -> closure.remove(p.getBundleDescription()));
		return closure;
	}

	/**
	 * Returns a {@link Set} of bundle descriptions for all required
	 * dependencies of the given {@link IPluginModelBase}s.
	 * <p>
	 * The set includes the descriptions of the transitively computed explicit,
	 * implicit (defined in the target-platform) and optional (if requested)
	 * dependencies. The set does not include the descriptions of the given
	 * objects.
	 * </p>
	 *
	 * @param plugins
	 *            selected the group of {@link IPluginModelBase}s to compute
	 *            dependencies for.
	 * @param options
	 *            the specified {@link Options} for computing the closure
	 * @return a set of bundle descriptions
	 */
	public static Set<BundleDescription> getDependencies(Collection<IPluginModelBase> plugins, Options... options) {
		return getDependencies(plugins, getImplicitDependencies(), TargetPlatformHelper.getState(), options);
	}

	/**
	 * Returns a {@link Set} of bundle descriptions of the given
	 * {@link IPluginModelBase}s and all of their required dependencies.
	 * <p>
	 * The set includes the descriptions of the given bundle descriptions as
	 * well as all transitively computed explicit and optional (if requested)
	 * dependencies. So it is the self-contained closure for all required
	 * dependencies of the given set of plug-ins.
	 * </p>
	 *
	 * @param bundles
	 *            the group of {@link BundleDescription}s to compute
	 *            dependencies for.
	 * @param options
	 *            the specified {@link Options} for computing the closure
	 * @return a set of bundle descriptions
	 */
	public static Set<BundleDescription> findRequirementsClosure(Collection<BundleDescription> bundles,
			Options... options) {

		Set<Options> optionSet = Set.of(options);
		boolean includeOptional = optionSet.contains(Options.INCLUDE_OPTIONAL_DEPENDENCIES);
		boolean includeAllFragments = optionSet.contains(Options.INCLUDE_ALL_FRAGMENTS);
		boolean includeNonTestFragments = optionSet.contains(Options.INCLUDE_NON_TEST_FRAGMENTS);
		if (includeAllFragments && includeNonTestFragments) {
			throw new AssertionError("Cannot combine INCLUDE_ALL_FRAGMENTS and INCLUDE_NON_TEST_FRAGMENTS"); //$NON-NLS-1$
		}

		Set<BundleDescription> closure = new HashSet<>(bundles.size() * 4 / 3 + 1);
		Queue<BundleDescription> pending = new ArrayDeque<>(bundles.size());

		// initialize with given bundles
		for (BundleDescription bundle : bundles) {
			addNewRequiredBundle(bundle, closure, pending);
		}

		// perform exhaustive iterative bfs for required wires
		while (!pending.isEmpty()) {
			BundleDescription bundle = pending.remove();

			BundleWiring wiring = bundle.getWiring();
			if (wiring == null || !wiring.isInUse()) {
				continue;
			}

			if (includeAllFragments || includeNonTestFragments) {
				// A fragment's host is already required by a wire
				for (BundleDescription fragment : bundle.getFragments()) {
					if (includeAllFragments || !isTestWorkspaceProject(fragment)) {
						addNewRequiredBundle(fragment, closure, pending);
					}
				}
			}

			if (isFragment(wiring.getRevision())) {
				// Requirements of a fragment are hosted at the host, which
				// therefore requires the corresponding wires: OSGi Core spec,
				// chapter 6.4.1 - Hosted Requirements and Capabilities
				for (BundleWire hostWire : wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE)) {
					// Temporarily remove this fragment's host from the closure
					// to ensure it's added again below. In the subsequent
					// processing this fragment's requirements will then also be
					// considered (before it was discarded).
					closure.remove(hostWire.getProvider());
				}
			}

			List<BundleWire> requiredWires = wiring.getRequiredWires(null);
			for (BundleWire wire : requiredWires) {
				BundleRevision declaringBundle = wire.getRequirement().getRevision();
				if (declaringBundle != bundle && !closure.contains(declaringBundle)) {
					// Requirement is declared by an attached fragment, which is
					// not included into the closure.
					continue;
				}
				BundleRevision provider = wire.getCapability().getRevision();
				// Use revision of required capability to support the case if
				// fragments contribute new packages to their host's API.
				if (provider instanceof BundleDescription requiredBundle && (includeOptional || !isOptional(wire.getRequirement()))) {
					addNewRequiredBundle(requiredBundle, closure, pending);
				}
			}
		}
		return closure;
	}

	private static void addNewRequiredBundle(BundleDescription bundle, Set<BundleDescription> requiredBundles,
			Queue<BundleDescription> pending) {
		if (bundle != null && bundle.isResolved() && !bundle.isRemovalPending() && requiredBundles.add(bundle)) {
			pending.add(bundle);
		}
	}

	private static boolean isFragment(BundleRevision bundle) {
		return (bundle.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	private static boolean isOptional(BundleRequirement requirement) {
		return Constants.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE));
	}

	private static boolean isTestWorkspaceProject(Resource f) {
		// Be defensive when declaring a fragment as 'test'-fragment
		IPluginModelBase pluginModel = PluginRegistry.findModel(f);
		if (pluginModel != null) {
			IResource resource = pluginModel.getUnderlyingResource();
			if (resource != null) {
				return ClasspathComputer.hasTestOnlyClasspath(resource.getProject());
			} // test-fragments are usually not part of the target-platform
		}
		return false;
	}

	/**
	 * Computes the set of implicit dependencies from the
	 * {@link PDEPreferencesManager}.
	 *
	 * @return a set of bundle ids
	 */
	private static Collection<NameVersionDescriptor> getImplicitDependencies() {
		try {
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (service != null) {
				NameVersionDescriptor[] implicit = service.getWorkspaceTargetDefinition().getImplicitDependencies();
				if (implicit != null) {
					return Arrays.asList(implicit);
				}
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return Collections.emptyList();
	}

	private static List<BundleDescription> mergeBundleDescriptions(Collection<IPluginModelBase> plugins,
			Collection<NameVersionDescriptor> descriptors, State state) {
		List<BundleDescription> bundles = new ArrayList<>();
		for (IPluginModelBase plugin : plugins) {
			if (plugin != null) {
				bundles.add(plugin.getBundleDescription());
			}
		}
		for (NameVersionDescriptor descriptor : descriptors) {
			String versionStr = descriptor.getVersion();
			Version version = versionStr != null ? Version.parseVersion(versionStr) : null;
			BundleDescription bundle = state.getBundle(descriptor.getId(), version);
			bundles.add(bundle);
		}
		return bundles;
	}
}
