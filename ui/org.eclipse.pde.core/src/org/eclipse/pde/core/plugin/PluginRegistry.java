/*******************************************************************************
 * Copyright (c) 2007, 2024 IBM Corporation and others.
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
 *     Hannes Wellmann - Rework PluginRegistry API to modernize it and replace Equinox resolver's VersionRange
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Resource;

/**
 * The central access point for models representing plug-ins found in the workspace
 * and in the target platform.
 * <p>
 * This class provides static methods only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 *
 * @since 3.3
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class PluginRegistry {

	private PluginRegistry() { // static use only
	}

	/**
	 * Filter used when searching for plug-in models.
	 * <p>
	 * Clients may subclass this class to implement custom filters.
	 * </p>
	 *
	 * @see PluginRegistry#findModels(String, String, VersionMatchRule)
	 * @see PluginRegistry#findModels(String, VersionRange)
	 * @since 3.6
	 * @deprecated Instead use {@link Predicate} and filter the stream returned
	 *             by
	 *             {@link PluginRegistry#findModels(String, String, VersionMatchRule)}
	 *             or {@link PluginRegistry#findModels(String, VersionRange)}
	 */
	@Deprecated(forRemoval = true, since = "3.19 (removal in 2026-09 or later)")
	public static class PluginFilter {

		/**
		 * Returns whether the given model is accepted by this filter.
		 *
		 * @param model plug-in model
		 * @return whether accepted by this filter
		 */
		public boolean accept(IPluginModelBase model) {
			return true;
		}

	}

	private static Predicate<IPluginModelBase> asPredicate(PluginFilter filter) {
		return filter != null ? filter::accept : p -> true;
	}

	/**
	 * Returns a model entry containing all workspace and target plug-ins by the given ID
	 *
	 * @param id the plug-in ID
	 *
	 * @return a model entry containing all workspace and target plug-ins by the given ID
	 */
	public static ModelEntry findEntry(String id) {
		return PDECore.getDefault().getModelManager().findEntry(id);
	}

	/**
	 * Returns the plug-in model for the best match plug-in with the given ID.
	 * A null value is returned if no such bundle is found in the workspace or target platform.
	 * <p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page is always
	 * preferably returned over a target plug-in that is unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the same version,
	 * one of those plug-ins is randomly returned.
	 * </p>
	 *
	 * @param id the plug-in ID
	 * @return the plug-in model for the best match plug-in with the given ID
	 */
	public static IPluginModelBase findModel(String id) {
		return PDECore.getDefault().getModelManager().findModel(id);
	}

	/**
	 * Returns the plug-in model corresponding to the given project, or <code>null</code>
	 * if the project does not represent a plug-in project or if it contains a manifest file
	 * that is malformed or missing vital information.
	 *
	 * @param project the project
	 * @return a plug-in model corresponding to the project or <code>null</code> if the project
	 * 			is not a plug-in project
	 */
	public static IPluginModelBase findModel(IProject project) {
		return PDECore.getDefault().getModelManager().findModel(project);
	}

	/**
	 * Returns a plug-in model associated with the given bundle description
	 *
	 * @param desc
	 *            the bundle description
	 *
	 * @return a plug-in model associated with the given bundle description or
	 *         <code>null</code> if none exists
	 * @deprecated Instead use {@link #findModel(Resource)}
	 */
	@Deprecated(forRemoval = true, since = "3.18 (removal in 2026-06 or later)")
	public static IPluginModelBase findModel(BundleDescription desc) {
		return findModel((Resource) desc);
	}

	/**
	 * Returns a plug-in model associated with the given resource
	 *
	 * @param resource
	 *            the (OSGi) resource to find a model for
	 *
	 * @return a plug-in model associated with the given bundle description or
	 *         <code>null</code> if none exists
	 * @since 3.18
	 */
	public static IPluginModelBase findModel(Resource resource) {
		return PDECore.getDefault().getModelManager().findModel(resource);
	}

	/**
	 * Returns all plug-ins and fragments in the workspace as well as all plug-ins and fragments that are
	 * checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in/fragment, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * Equivalent to <code>getActiveModels(true)</code>
	 * </p>
	 *
	 * @return   all plug-ins and fragments in the workspace as well as all plug-ins and fragments that are
	 * 			checked on the Target Platform preference page.
	 */
	public static IPluginModelBase[] getActiveModels() {
		return getActiveModels(true);
	}

	/**
	 * Returns all plug-ins and (possibly) fragments in the workspace as well as all plug-ins and (possibly)
	 *  fragments that are checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * The returned result includes fragments only if <code>includeFragments</code>
	 * is set to true
	 * </p>
	 * @param includeFragments  a boolean indicating if fragments are desired in the returned
	 *							result
	 * @return all plug-ins and (possibly) fragments in the workspace as well as all plug-ins and
	 * (possibly) fragments that are checked on the Target Platform preference page.
	 */
	public static IPluginModelBase[] getActiveModels(boolean includeFragments) {
		return PDECore.getDefault().getModelManager().getActiveModels(includeFragments);
	}

	/**
	 * Returns all plug-ins and fragments in the workspace as well as all target plug-ins and fragments, regardless
	 * whether or not they are checked or not on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * Equivalent to <code>getAllModels(true)</code>
	 * </p>
	 *
	 * @return   all plug-ins and fragments in the workspace as well as all target plug-ins and fragments, regardless
	 * whether or not they are checked on the Target Platform preference page.
	 */
	public static IPluginModelBase[] getAllModels() {
		return getAllModels(true);
	}

	/**
	 * Returns all plug-ins and (possibly) fragments in the workspace as well as all plug-ins
	 * and (possibly) fragments, regardless whether or not they are
	 * checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in/fragment, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * The returned result includes fragments only if <code>includeFragments</code>
	 * is set to true
	 * </p>
	 * @param includeFragments  a boolean indicating if fragments are desired in the returned
	 *							result
	 * @return ll plug-ins and (possibly) fragments in the workspace as well as all plug-ins
	 * and (possibly) fragments, regardless whether or not they are
	 * checked on the Target Platform preference page.
	 */
	public static IPluginModelBase[] getAllModels(boolean includeFragments) {
		return PDECore.getDefault().getModelManager().getAllModels(includeFragments);
	}

	/**
	 * Returns all plug-in models in the workspace
	 *
	 * @return all plug-in models in the workspace
	 */
	public static IPluginModelBase[] getWorkspaceModels() {
		return PDECore.getDefault().getModelManager().getWorkspaceModels();
	}

	/**
	 * Return the model manager that keeps track of plug-ins in the target platform
	 *
	 * @return  the model manager that keeps track of plug-ins in the target platform
	 */
	public static IPluginModelBase[] getExternalModels() {
		return PDECore.getDefault().getModelManager().getExternalModels();
	}

	/**
	 * Returns a model matching the given id, version, match rule, or
	 * <code>null</code> if none.
	 * <p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page
	 * is always preferably returned over a target plug-in that is
	 * unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the
	 * same version, one of those plug-ins is randomly returned.
	 * </p>
	 *
	 * @param id
	 *            symbolic name of a plug-in to find
	 * @param version
	 *            minimum version, or <code>null</code> to only match on
	 *            symbolic name
	 * @param matchRule
	 *            the {@link VersionMatchRule rule} a plug-in's version must
	 *            match with respect to the specified reference version in order
	 *            to be selected.
	 *
	 * @return a matching model or <code>null</code>
	 * @since 3.19
	 */
	public static IPluginModelBase findModel(String id, String version, VersionMatchRule matchRule) {
		return findModels(id, version, matchRule).findFirst().orElse(null);
	}

	/**
	 * @see #findModel(String, String, VersionMatchRule)
	 * @see #findModels(String, String, VersionMatchRule)
	 * @since 3.6
	 * @deprecated If no filter is passed use
	 *             {@link #findModel(String, String, VersionMatchRule)}, else
	 *             filter the {@code Stream} returned by
	 *             {@link #findModels(String, String, VersionMatchRule)}.
	 */
	@Deprecated(forRemoval = true, since = "3.19 (removal in 2026-09 or later)")
	public static IPluginModelBase findModel(String id, String version, int match, PluginFilter filter) {
		VersionMatchRule rule = safeToRuleLiteral(match);
		return findModels(id, version, rule).filter(asPredicate(filter)).findFirst().orElse(null);
	}

	/**
	 * Returns all models matching the given id, version and match rule sorted
	 * by descending version.
	 * <p>
	 * Target (external) plug-ins/fragments with the same ID as workspace
	 * counterparts are not considered.
	 * </p>
	 * <p>
	 * Returns plug-ins regardless of whether they are checked/enabled or
	 * unchecked/disabled on the Target Platform preference page.
	 * </p>
	 *
	 * @param id
	 *            symbolic name of a plug-ins to find
	 * @param version
	 *            minimum version, or <code>null</code> to only match on
	 *            symbolic name
	 * @param matchRule
	 *            the {@link VersionMatchRule rule} a plug-in's version must
	 *            match with respect to the specified reference version in order
	 *            to be selected.
	 *
	 * @return a stream of all matching models sorted by descending version,
	 *         possibly empty.
	 * @since 3.19
	 */
	public static Stream<IPluginModelBase> findModels(String id, String version, VersionMatchRule matchRule) {
		Version reference = version != null ? Version.valueOf(version) : null;
		return selectModels(id, version != null ? p -> matchRule.matches(VersionUtil.getVersion(p), reference) : null);
	}

	/**
	 * @see #findModels(String, String, VersionMatchRule)
	 * @since 3.6
	 * @deprecated Instead use
	 *             {@link #findModels(String, String, VersionMatchRule)} and
	 *             filter the returned {@code Stream} if a filter is passed.
	 */
	@Deprecated(forRemoval = true, since = "3.19 (removal in 2026-09 or later)")
	public static IPluginModelBase[] findModels(String id, String version, int match, PluginFilter filter) {
		VersionMatchRule rule = safeToRuleLiteral(match);
		return findModels(id, version, rule).filter(asPredicate(filter)).toArray(IPluginModelBase[]::new);
	}

	private static VersionMatchRule safeToRuleLiteral(int match) {
		VersionMatchRule rule;
		try {
			rule = VersionUtil.matchRuleFromLiteral(match);
		} catch (IllegalArgumentException e) {
			rule = VersionMatchRule.PERFECT; // Same as VersionUtil.compare
		}
		return rule;
	}

	/**
	 * Returns a model matching the given id, version range, or
	 * <code>null</code> if none.
	 * <p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page is always
	 * preferably returned over a target plug-in that is unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the same version,
	 * one of those plug-ins is randomly returned.
	 * </p>
	 * @param id symbolic name of plug-in to find
	 * @param range acceptable version range to match, or <code>null</code> for any range
	 *
	 * @return a matching model or <code>null</code>
	 * @since 3.19
	 */
	public static IPluginModelBase findModel(String id, VersionRange range) {
		return findModels(id, range).findFirst().orElse(null);
	}

	/**
	 * @see #findModel(String, VersionRange)
	 * @since 3.6
	 * @deprecated If no filter is passed use
	 *             {@link #findModel(String, VersionRange)}, else filter the
	 *             {@code Stream} returned by
	 *             {@link #findModels(String, VersionRange)}.
	 */
	@Deprecated(forRemoval = true, since = "3.19 (removal in 2026-09 or later)")
	public static IPluginModelBase findModel(String id, org.eclipse.osgi.service.resolver.VersionRange range,
			PluginFilter filter) {
		return findModels(id, range).filter(asPredicate(filter)).findFirst().orElse(null);
	}

	/**
	 * Returns all models matching the given id and version range sorted by
	 * descending version.
	 * <p>
	 * Target (external) plug-ins/fragments with the same ID as workspace
	 * counterparts are not considered.
	 * </p>
	 * <p>
	 * Returns plug-ins regardless of whether they are checked/enabled or
	 * unchecked/disabled on the Target Platform preference page.
	 * </p>
	 *
	 * @param id
	 *            symbolic name of plug-ins to find
	 * @param range
	 *            acceptable version range to match, or <code>null</code> for
	 *            any range
	 *
	 * @return a stream of all matching models sorted by descending version,
	 *         possibly empty.
	 * @since 3.19
	 */
	public static Stream<IPluginModelBase> findModels(String id, VersionRange range) {
		return selectModels(id, range != null ? p -> range.includes(VersionUtil.getVersion(p)) : null);
	}

	/**
	 * @see #findModels(String, VersionRange)
	 * @since 3.6
	 * @deprecated Instead use {@link #findModels(String, VersionRange)} and
	 *             filter the returned {@code Stream} if a filter is passed.
	 */
	@Deprecated(forRemoval = true, since = "3.19 (removal in 2026-09 or later)")
	public static IPluginModelBase[] findModels(String id, org.eclipse.osgi.service.resolver.VersionRange range,
			PluginFilter filter) {
		return findModels(id, range).filter(asPredicate(filter)).toArray(IPluginModelBase[]::new);
	}

	private static Stream<IPluginModelBase> selectModels(String id, Predicate<IPluginModelBase> versionFilter) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry == null) {
			return Stream.empty();
		}
		List<IPluginModelBase> models = entry.hasWorkspaceModels() ? entry.fWorkspaceEntries : entry.fExternalEntries;
		Stream<IPluginModelBase> plugins = models.stream().filter(m -> {
			IPluginBase base = m.getPluginBase();
			// guard against invalid plug-ins
			return base != null && base.getId() != null;
		});
		if (versionFilter != null) {
			plugins = plugins.filter(versionFilter);
		}
		return plugins.sorted(VersionUtil.BY_DESCENDING_PLUGIN_VERSION);
	}

	/**
	 * Creates and returns a model associated with the <code>build.properties</code> of a bundle
	 * in the workspace or <code>null</code> if none.
	 *
	 * @param model plug-in model base
	 * @return a build model initialized from the plug-in's <code>build.properties</code> or
	 *  <code>null</code> if none. Returns <code>null</code> for external plug-in models (i.e.
	 *  models that are not based on workspace projects).
	 *  @exception CoreException if unable to create a build model
	 * @since 3.7
	 */
	public static IBuildModel createBuildModel(IPluginModelBase model) throws CoreException {
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project != null) {
					IFile buildFile = PDEProject.getBuildProperties(project);
					if (buildFile.exists()) {
						IBuildModel buildModel = new WorkspaceBuildModel(buildFile);
						buildModel.load();
						return buildModel;
					}
				}
			}
		}
		return null;
	}
}
