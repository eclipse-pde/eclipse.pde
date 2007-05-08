/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.FeatureRebuilder;
import org.eclipse.pde.internal.core.builders.PluginRebuilder;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.osgi.framework.BundleContext;

public class PDECore extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.pde.core"; //$NON-NLS-1$
	
	public static final IPath REQUIRED_PLUGINS_CONTAINER_PATH = new Path(PLUGIN_ID + ".requiredPlugins"); //$NON-NLS-1$
	public static final IPath JAVA_SEARCH_CONTAINER_PATH = new Path(PLUGIN_ID + ".externalJavaSearch"); //$NON-NLS-1$

	public static final String BINARY_PROJECT_VALUE = "binary"; //$NON-NLS-1$
	public static final String BINARY_REPOSITORY_PROVIDER = PLUGIN_ID + "." + "BinaryRepositoryProvider"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY = new QualifiedName(PLUGIN_ID, "imported"); //$NON-NLS-1$
	public static final QualifiedName TOUCH_PROJECT = new QualifiedName(PLUGIN_ID, "TOUCH_PROJECT"); //$NON-NLS-1$
	
	// Shared instance
	private static PDECore inst;
	
	private static IPluginModelBase[] registryPlugins;

	public static PDECore getDefault() {
		return inst;
	}
	
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public static void log(IStatus status) {
		if (status != null)
			ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else if (e.getMessage() != null) {
			status = new Status(
					IStatus.ERROR,
					PLUGIN_ID,
					IStatus.OK,
					e.getMessage(),
					e);
		}
		log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(
				IStatus.ERROR,
				PLUGIN_ID,
				IStatus.ERROR,
				message,
				null));
	}

	public static void logException(Throwable e) {
		logException(e, null);
	}

	public static void logException(Throwable e, String message) {
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
			status =
				new Status(
					IStatus.ERROR,
					PLUGIN_ID,
					IStatus.OK,
					message,
					e);
		}
		log(status);
	}
	
	private PluginModelManager fModelManager;
	private FeatureModelManager fFeatureModelManager;
	
	private TargetDefinitionManager fTargetProfileManager;

	// Schema registry
	private SchemaRegistry fSchemaRegistry;

	private SourceLocationManager fSourceLocationManager;
	private JavadocLocationManager fJavadocLocationManager;
	private SearchablePluginsManager fSearchablePluginsManager;

	// Tracing options manager
	private TracingOptionsManager fTracingOptionsManager;
	private BundleContext fBundleContext;
	private JavaElementChangeListener fJavaElementChangeListener;

	private FeatureRebuilder fFeatureRebuilder;

	private PluginRebuilder fPluginRebuilder;

	public PDECore() {
		inst = this;
	}

	public URL getInstallURL() {
		try {
			return FileLocator.resolve(getDefault().getBundle().getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
	}
	
	public IPluginModelBase findPluginInHost(String id) {
		if (registryPlugins == null) {
			URL[] pluginPaths = ConfiguratorUtils.getCurrentPlatformConfiguration().getPluginPath();
			PDEState state = new PDEState(pluginPaths, false, new NullProgressMonitor());
			registryPlugins = state.getTargetModels();
		}

		for (int i = 0; i < registryPlugins.length; i++) {
			if (registryPlugins[i].getPluginBase().getId().equals(id))
				return registryPlugins[i];
		}
		return null;
	}

	public PluginModelManager getModelManager() {
		if (fModelManager == null)
			fModelManager = new PluginModelManager();
		return fModelManager;
	}
	
	public TargetDefinitionManager getTargetProfileManager() {
		if (fTargetProfileManager == null)
			fTargetProfileManager = new TargetDefinitionManager();
		return fTargetProfileManager;
	}
	
	public FeatureModelManager getFeatureModelManager() {
		if (fFeatureModelManager == null)
			fFeatureModelManager = new FeatureModelManager();
		return fFeatureModelManager;
	}
	
	public JavaElementChangeListener getJavaElementChangeListener() {
		return fJavaElementChangeListener;
	}
	
	public SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = new SchemaRegistry();
		return fSchemaRegistry;
	}

	public SourceLocationManager getSourceLocationManager() {
		if (fSourceLocationManager == null)
			fSourceLocationManager = new SourceLocationManager();
		return fSourceLocationManager;
	}
	
	public JavadocLocationManager getJavadocLocationManager() {
		if (fJavadocLocationManager == null)
			fJavadocLocationManager = new JavadocLocationManager();
		return fJavadocLocationManager;
	}

	public TracingOptionsManager getTracingOptionsManager() {
		if (fTracingOptionsManager == null)
			fTracingOptionsManager = new TracingOptionsManager();
		return fTracingOptionsManager;
	}
	
	public SearchablePluginsManager getSearchablePluginsManager() {
		if (fSearchablePluginsManager == null) {
			fSearchablePluginsManager = new SearchablePluginsManager();
			try {
				getWorkspace().addSaveParticipant(inst, fSearchablePluginsManager);
			} catch (CoreException e) {
				log(e);
			}
		}
		return fSearchablePluginsManager;
	}
	
	public boolean areModelsInitialized() {
		return fModelManager != null && fModelManager.isInitialized();
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;
		CompilerFlags.initializeDefaults();
		fJavaElementChangeListener = new JavaElementChangeListener();
		fJavaElementChangeListener.start();
		fPluginRebuilder = new PluginRebuilder();
		fPluginRebuilder.start();
		fFeatureRebuilder = new FeatureRebuilder();
		fFeatureRebuilder.start();
	}

	public BundleContext getBundleContext() {
		return fBundleContext;
	}

	public void stop(BundleContext context) throws CoreException {
		PDECore.getDefault().savePluginPreferences();
		
		fJavaElementChangeListener.shutdown();
		fPluginRebuilder.stop();
		fFeatureRebuilder.stop();
		
		if (fSchemaRegistry != null) {
			fSchemaRegistry.shutdown();
			fSchemaRegistry = null;
		}
		if (fTargetProfileManager != null) {
			fTargetProfileManager.shutdown();
			fTargetProfileManager = null;
		}
		if (fSearchablePluginsManager != null) {
			getWorkspace().removeSaveParticipant(inst);
			fSearchablePluginsManager.shutdown();
			fSearchablePluginsManager = null;
		}
		if (fFeatureModelManager != null) {
			fFeatureModelManager.shutdown();
			fFeatureModelManager = null;
		}
		if (fModelManager != null) {
			fModelManager.shutdown();
			fModelManager = null;
		}
	}
}
