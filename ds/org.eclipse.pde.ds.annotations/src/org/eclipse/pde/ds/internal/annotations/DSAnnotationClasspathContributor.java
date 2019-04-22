/*******************************************************************************
 * Copyright (c) 2015, 2019 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;

@SuppressWarnings("restriction")
public class DSAnnotationClasspathContributor implements IClasspathContributor {

	// private static final IAccessRule[] ANNOTATION_ACCESS_RULES = { JavaCore.newAccessRule(new Path("org/osgi/service/component/annotations/*"), IAccessRule.K_DISCOURAGED | IAccessRule.IGNORE_IF_BETTER) };
	private static final IAccessRule[] ANNOTATION_ACCESS_RULES = { };

	private static final IClasspathAttribute[] DS_ATTRS = { JavaCore.newClasspathAttribute(Activator.CP_ATTRIBUTE, Boolean.toString(true)) };

	@Override
	public List<IClasspathEntry> getInitialEntries(BundleDescription project) {
		IPluginModelBase model = PluginRegistry.findModel(project);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource == null || WorkspaceModelManager.isBinaryProject(resource.getProject())) {
				return Collections.emptyList();
			}

			IPreferencesService prefs = Platform.getPreferencesService();
			IScopeContext[] scope = new IScopeContext[] { new ProjectScope(resource.getProject()),
					InstanceScope.INSTANCE, DefaultScope.INSTANCE };
			boolean enabled = prefs.getBoolean(Activator.PLUGIN_ID, Activator.PREF_ENABLED, false, scope);
			if (enabled) {
				boolean autoClasspath = prefs.getBoolean(Activator.PLUGIN_ID, Activator.PREF_CLASSPATH, true, scope);
				if (autoClasspath) {
					DSAnnotationVersion specVersion;
					try {
						specVersion = DSAnnotationVersion.valueOf(prefs.getString(Activator.PLUGIN_ID,
								Activator.PREF_SPEC_VERSION, DSAnnotationVersion.V1_3.name(), scope));
					} catch (IllegalArgumentException e) {
						specVersion = DSAnnotationVersion.V1_3;
					}

					String libBundleName;
					if (specVersion == DSAnnotationVersion.V1_3) {
						libBundleName = "org.eclipse.pde.ds.lib"; //$NON-NLS-1$
					} else {
						libBundleName = "org.eclipse.pde.ds1_2.lib"; //$NON-NLS-1$
					}

					IPluginModelBase bundle = PluginRegistry.findModel(libBundleName);
					if (bundle != null && bundle.isEnabled()) {
						String location = bundle.getInstallLocation();
						if (location != null) {
							IPath srcPath = getSrcPath(libBundleName);
							IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(location + "/annotations.jar"), //$NON-NLS-1$
									srcPath, Path.ROOT, ANNOTATION_ACCESS_RULES, DS_ATTRS, false);
							DSLibPluginModelListener.addProject(JavaCore.create(resource.getProject()), libBundleName);
							return Collections.singletonList(entry);
						}
					}
				}
			}

			DSLibPluginModelListener.removeProject(JavaCore.create(resource.getProject()));
		}

		return Collections.emptyList();
	}

	private IPath getSrcPath(String libBundleName) {
		IPluginModelBase bundle = PluginRegistry.findModel(libBundleName + ".source"); //$NON-NLS-1$
		if (bundle != null && bundle.isEnabled()) {
			String location = bundle.getInstallLocation();
			if (location != null) {
				return new Path(location);
			}
		}

		return null;
	}

	@Override
	public List<IClasspathEntry> getEntriesForDependency(BundleDescription project, BundleDescription addedDependency) {
		return Collections.emptyList();
	}
}
