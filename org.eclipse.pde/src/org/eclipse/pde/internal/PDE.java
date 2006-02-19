/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.builders.CompilerFlags;
import org.eclipse.pde.internal.builders.FeatureRebuilder;
import org.osgi.framework.BundleContext;


public class PDE extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.pde"; //$NON-NLS-1$

	public static final String MANIFEST_BUILDER_ID =
		PLUGIN_ID + "." + "ManifestBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SCHEMA_BUILDER_ID =
		PLUGIN_ID + "." + "SchemaBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String PLUGIN_NATURE = PLUGIN_ID + "." + "PluginNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String FEATURE_NATURE = PLUGIN_ID + "." + "FeatureNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SITE_NATURE = PLUGIN_ID + "." + "UpdateSiteNature"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String FEATURE_BUILDER_ID =
		PLUGIN_ID + "." + "FeatureBuilder"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String SITE_BUILDER_ID =
		PLUGIN_ID + "." + "UpdateSiteBuilder"; //$NON-NLS-1$ //$NON-NLS-2$

	// Shared instance
	private static PDE fInstance;
	
	private BundleContext fBundleContext;
	
	private FeatureRebuilder fFeatureRebuilder;
	
	public PDE() {
		fInstance = this;
	}
	public BundleContext getBundleContext(){
		return fBundleContext;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		fBundleContext = context;
		super.start(context);
		CompilerFlags.initializeDefaults();
		fFeatureRebuilder = new FeatureRebuilder();
		fFeatureRebuilder.start();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		fFeatureRebuilder.stop();
		fFeatureRebuilder = null;
		super.stop(context);
		fInstance = null;
		fBundleContext = null;
	}

	public URL getInstallURL() {
		try {
			return FileLocator.resolve(getDefault().getBundle().getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
	}
	
	public static boolean hasPluginNature(IProject project) {
		try {
			return project.hasNature(PLUGIN_NATURE);
		} catch (CoreException e) {
			log(e);
			return false;
		}
	}
	
	public static boolean hasFeatureNature(IProject project) {
		try {
			return project.hasNature(FEATURE_NATURE);
		} catch (CoreException e) {
			log(e);
			return false;
		}
	}

	public static boolean hasUpdateSiteNature(IProject project) {
		try {
			return project.hasNature(SITE_NATURE);
		} catch (CoreException e) {
			log(e);
			return false;
		}
	}
	
	public static PDE getDefault() {
		return fInstance;
	}

	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}
	
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static void log(IStatus status) {
		ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
	}

	public static void logException(
		Throwable e,
		final String title,
		String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else {
			if (message == null)
				message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void logException(Throwable e) {
		logException(e, null, null);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else
			status =
				new Status(IStatus.ERROR, getPluginId(), IStatus.OK, e.getMessage(), e);
		log(status);
	}
}
