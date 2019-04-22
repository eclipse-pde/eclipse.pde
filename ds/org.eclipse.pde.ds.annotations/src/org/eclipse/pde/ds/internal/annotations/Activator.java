/*******************************************************************************
 * Copyright (c) 2012, 2019 Ecliptical Software Inc. and others.
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

import java.util.HashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.pde.ds.annotations"; //$NON-NLS-1$

	public static final String PREF_ENABLED = "enabled"; //$NON-NLS-1$

	public static final String PREF_PATH = "path"; //$NON-NLS-1$

	public static final String PREF_SPEC_VERSION = "dsVersion"; //$NON-NLS-1$

	public static final String PREF_VALIDATION_ERROR_LEVEL = "validationErrorLevel"; //$NON-NLS-1$

	public static final String PREF_MISSING_UNBIND_METHOD_ERROR_LEVEL = "validationErrorLevel.missingImplicitUnbindMethod"; //$NON-NLS-1$

	public static final String PREF_GENERATE_BAPL = "generateBundleActivationPolicyLazy"; //$NON-NLS-1$

	public static final String DEFAULT_PATH = "OSGI-INF"; //$NON-NLS-1$

	public static final String CP_ATTRIBUTE = "org.eclipse.pde.ds.annotations.cp"; //$NON-NLS-1$

	public static final String PREF_CLASSPATH = "classpath"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private DSAnnotationPreferenceListener dsPrefListener;

	private final HashMap<IJavaProject, ProjectClasspathPreferenceChangeListener> projectPrefListeners = new HashMap<>();

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		dsPrefListener = new DSAnnotationPreferenceListener();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		DSLibPluginModelListener.dispose();
		dsPrefListener.dispose();

		synchronized (projectPrefListeners) {
			for (ProjectClasspathPreferenceChangeListener listener : projectPrefListeners.values()) {
				listener.dispose();
			}

			projectPrefListeners.clear();
		}

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, e.getMessage(), e));
	}

	void listenForClasspathPreferenceChanges(IJavaProject project) {
		synchronized (projectPrefListeners) {
			if (!projectPrefListeners.containsKey(project)) {
				projectPrefListeners.put(project, new ProjectClasspathPreferenceChangeListener(project));
			}
		}
	}

	void disposeProjectClasspathPreferenceChangeListener(IJavaProject project) {
		synchronized (projectPrefListeners) {
			ProjectClasspathPreferenceChangeListener listener = projectPrefListeners.remove(project);
			if (listener != null) {
				listener.dispose();
			}
		}
	}
}
