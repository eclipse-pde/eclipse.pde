/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.osgi.framework.Version;

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

	/**
	 * Filter used when searching for plug-in models.
	 * <p>
	 * Clients may subclass this class to implement custom filters.
	 * </p>
	 * @see PluginRegistry#findModel(String, String, int, PluginFilter)
	 * @see PluginRegistry#findModel(String, VersionRange, PluginFilter)
	 * @since 3.6
	 */
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
	 * @param desc the bundle description
	 * 
	 * @return a plug-in model associated with the given bundle description or <code>null</code>
	 * 			if none exists
	 */
	public static IPluginModelBase findModel(BundleDescription desc) {
		return PDECore.getDefault().getModelManager().findModel(desc);
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
	 * Returns whether the given model matches the given id, version, and match rule.
	 * 
	 * @param base match candidate
	 * @param id id to match
	 * @param version version to match or <code>null</code>
	 * @param match version match rule
	 * @return whether the model is a match
	 */
	private static boolean isMatch(IPluginBase base, String id, String version, int match) {
		// if version is null, then match any version with same ID
		if (base == null) {
			return false; // guard against invalid plug-ins
		}
		if (base.getId() == null) {
			return false; // guard against invalid plug-ins
		}
		if (version == null)
			return base.getId().equals(id);
		return VersionUtil.compare(base.getId(), base.getVersion(), id, version, match);
	}

	/**
	 * Returns a model matching the given id, version, match rule, and optional filter,
	 * or <code>null</code> if none.
	 * p>
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
	 * @param id symbolic name of a plug-in to find
	 * @param version minimum version, or <code>null</code> to only match on symbolic name
	 * @param match one of {@link IMatchRules#COMPATIBLE}, {@link IMatchRules#EQUIVALENT},
	 *  {@link IMatchRules#GREATER_OR_EQUAL}, {@link IMatchRules#PERFECT}, or {@link IMatchRules#NONE}
	 *  when a version is unspecified
	 * @param filter a plug-in filter or <code>null</code> 
	 * 
	 * @return a matching model or <code>null</code>
	 * @since 3.6
	 */
	public static IPluginModelBase findModel(String id, String version, int match, PluginFilter filter) {
		return getMax(findModels(id, version, match, filter));
	}

	/**
	 * Returns all models matching the given id, version, match rule, and optional filter.
	 * <p>
	 * Target (external) plug-ins/fragments with the same ID as workspace counterparts are not
	 * considered.
	 * </p>
	 * <p>
	 * Returns plug-ins regardless of whether they are checked/enabled or unchecked/disabled
	 * on the Target Platform preference page.
	 * </p>
	 * @param id symbolic name of a plug-ins to find
	 * @param version minimum version, or <code>null</code> to only match on symbolic name
	 * @param match one of {@link IMatchRules#COMPATIBLE}, {@link IMatchRules#EQUIVALENT},
	 *  {@link IMatchRules#GREATER_OR_EQUAL}, {@link IMatchRules#PERFECT}, or {@link IMatchRules#NONE}
	 *  when a version is unspecified
	 * @param filter a plug-in filter or <code>null</code> 
	 * 
	 * @return a matching models, possibly an empty collection
	 * @since 3.6
	 */
	public static IPluginModelBase[] findModels(String id, String version, int match, PluginFilter filter) {
		IPluginModelBase[] models = PluginRegistry.getAllModels();
		List results = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if ((filter == null || filter.accept(model)) && isMatch(model.getPluginBase(), id, version, match))
				results.add(model);
		}
		return (IPluginModelBase[]) results.toArray(new IPluginModelBase[results.size()]);
	}

	/**
	 * Returns a model matching the given id, version range, and optional filter,
	 * or <code>null</code> if none.
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
	 * @param filter a plug-in filter or <code>null</code>
	 * 
	 * @return a matching model or <code>null</code>
	 * @since 3.6
	 */
	public static IPluginModelBase findModel(String id, VersionRange range, PluginFilter filter) {
		return getMax(findModels(id, range, filter));
	}

	/**
	 * Returns the plug-in with the highest version, or <code>null</code> if empty.
	 * 
	 * @param models models
	 * @return plug-in with the highest version or <code>null</code>
	 */
	private static IPluginModelBase getMax(IPluginModelBase[] models) {
		if (models.length == 0) {
			return null;
		}
		if (models.length == 1) {
			return models[0];
		}
		IPluginModelBase max = null;
		Version maxV = null;
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			String versionStr = model.getPluginBase().getVersion();
			Version version = VersionUtil.validateVersion(versionStr).isOK() ? new Version(versionStr) : Version.emptyVersion;
			if (max == null) {
				max = model;
				maxV = version;
			} else {
				if (VersionUtil.isGreaterOrEqualTo(version, maxV)) {
					max = model;
					maxV = version;
				}
			}
		}
		return max;
	}

	/**
	 * Returns all models matching the given id, version range, and optional filter.
	 * <p>
	 * Target (external) plug-ins/fragments with the same ID as workspace counterparts are not
	 * considered.
	 * </p>
	 * <p>
	 * Returns plug-ins regardless of whether they are checked/enabled or unchecked/disabled
	 * on the Target Platform preference page.
	 * </p>
	 * @param id symbolic name of plug-ins to find
	 * @param range acceptable version range to match, or <code>null</code> for any range
	 * @param filter a plug-in filter or <code>null</code>
	 * 
	 * @return a matching models, possibly empty
	 * @since 3.6
	 */
	public static IPluginModelBase[] findModels(String id, VersionRange range, PluginFilter filter) {
		IPluginModelBase[] models = PluginRegistry.getAllModels();
		List results = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if ((filter == null || filter.accept(model)) && id.equals(model.getPluginBase().getId())) {
				String versionStr = model.getPluginBase().getVersion();
				Version version = VersionUtil.validateVersion(versionStr).isOK() ? new Version(versionStr) : Version.emptyVersion;
				if (range == null || range.isIncluded(version)) {
					results.add(model);
				}
			}
		}
		return (IPluginModelBase[]) results.toArray(new IPluginModelBase[results.size()]);
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
		IProject project = model.getUnderlyingResource().getProject();
		if (project != null) {
			IFile buildFile = PDEProject.getBuildProperties(project);
			if (buildFile.exists()) {
				IBuildModel buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
				return buildModel;
			}
		}
		return null;
	}
}
