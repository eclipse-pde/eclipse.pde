/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import java.util.Hashtable;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.IBundleClasspathResolver;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.builders.FeatureRebuilder;
import org.eclipse.pde.internal.core.builders.PluginRebuilder;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.osgi.framework.*;

public class PDECore extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.pde.core"; //$NON-NLS-1$

	public static final IPath REQUIRED_PLUGINS_CONTAINER_PATH = new Path(PLUGIN_ID + ".requiredPlugins"); //$NON-NLS-1$
	public static final IPath JAVA_SEARCH_CONTAINER_PATH = new Path(PLUGIN_ID + ".externalJavaSearch"); //$NON-NLS-1$
	public static final IPath JRE_CONTAINER_PATH = new Path(JavaRuntime.JRE_CONTAINER);

	public static final String BINARY_PROJECT_VALUE = "binary"; //$NON-NLS-1$
	public static final String BINARY_REPOSITORY_PROVIDER = PLUGIN_ID + "." + "BinaryRepositoryProvider"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY = new QualifiedName(PLUGIN_ID, "imported"); //$NON-NLS-1$
	public static final QualifiedName TOUCH_PROJECT = new QualifiedName(PLUGIN_ID, "TOUCH_PROJECT"); //$NON-NLS-1$

	public static final QualifiedName SCHEMA_PREVIEW_FILE = new QualifiedName(PLUGIN_ID, "SCHEMA_PREVIEW_FILE"); //$NON-NLS-1$

	// Shared instance
	private static PDECore inst;

	private static IPluginModelBase[] registryPlugins;
	private static PDEExtensionRegistry fExtensionRegistry = null;

	/**
	 * The singleton preference manager instance
	 * 
	 * @since 3.5
	 */
	private static PDEPreferencesManager fPreferenceManager;

	public static PDECore getDefault() {
		return inst;
	}

	/**
	 * Returns the singleton instance of if the {@link PDEPreferencesManager} for this bundle
	 * @return the preference manager for this bundle
	 * 
	 * @since 3.5
	 */
	public synchronized PDEPreferencesManager getPreferencesManager() {
		if (fPreferenceManager == null) {
			fPreferenceManager = new PDEPreferencesManager(PLUGIN_ID);
		}
		return fPreferenceManager;
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
			status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		}
		log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, null));
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
			status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, message, e);
		}
		log(status);
	}

	private FeatureModelManager fFeatureModelManager;

	private TargetDefinitionManager fTargetProfileManager;

	// Schema registry
	private SchemaRegistry fSchemaRegistry;

	private SourceLocationManager fSourceLocationManager;
	private JavadocLocationManager fJavadocLocationManager;
	private SearchablePluginsManager fSearchablePluginsManager;
	private ClasspathContainerResolverManager fClasspathContainerResolverManager;

	// Tracing options manager
	private TracingOptionsManager fTracingOptionsManager;
	private BundleContext fBundleContext;
	private JavaElementChangeListener fJavaElementChangeListener;

	private FeatureRebuilder fFeatureRebuilder;

	private PluginRebuilder fPluginRebuilder;

	/**
	 * Target platform service.
	 */
	private ServiceRegistration fTargetPlatformService;

	/**
	 * Bundle project service.
	 */
	private ServiceRegistration fBundleProjectService;

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
		return PluginModelManager.getInstance();
	}

	public synchronized TargetDefinitionManager getTargetProfileManager() {
		if (fTargetProfileManager == null)
			fTargetProfileManager = new TargetDefinitionManager();
		return fTargetProfileManager;
	}

	public synchronized FeatureModelManager getFeatureModelManager() {
		if (fFeatureModelManager == null)
			fFeatureModelManager = new FeatureModelManager();
		return fFeatureModelManager;
	}

	public JavaElementChangeListener getJavaElementChangeListener() {
		return fJavaElementChangeListener;
	}

	public synchronized SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = new SchemaRegistry();
		return fSchemaRegistry;
	}

	public synchronized PDEExtensionRegistry getExtensionsRegistry() {
		if (fExtensionRegistry == null) {
			fExtensionRegistry = new PDEExtensionRegistry();
		}
		return fExtensionRegistry;
	}

	public synchronized SourceLocationManager getSourceLocationManager() {
		if (fSourceLocationManager == null)
			fSourceLocationManager = new SourceLocationManager();
		return fSourceLocationManager;
	}

	/**
	 * Returns the singleton instance of the classpath container resolver manager used to dynamically
	 * resolve a project's classpath. Clients may contribute a {@link IBundleClasspathResolver} to the
	 * manager through the <code>org.eclipse.pde.core.bundleClasspathResolvers</code> extension.
	 * 
	 * @return singleton instance of the classpath container resolver manager
	 */
	public synchronized ClasspathContainerResolverManager getClasspathContainerResolverManager() {
		if (fClasspathContainerResolverManager == null) {
			fClasspathContainerResolverManager = new ClasspathContainerResolverManager();
		}
		return fClasspathContainerResolverManager;
	}

	public synchronized JavadocLocationManager getJavadocLocationManager() {
		if (fJavadocLocationManager == null)
			fJavadocLocationManager = new JavadocLocationManager();
		return fJavadocLocationManager;
	}

	public synchronized TracingOptionsManager getTracingOptionsManager() {
		if (fTracingOptionsManager == null)
			fTracingOptionsManager = new TracingOptionsManager();
		return fTracingOptionsManager;
	}

	public synchronized SearchablePluginsManager getSearchablePluginsManager() {
		if (fSearchablePluginsManager == null) {
			fSearchablePluginsManager = new SearchablePluginsManager();
		}
		return fSearchablePluginsManager;
	}

	public boolean areModelsInitialized() {
		return getModelManager().isInitialized();
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;

		fJavaElementChangeListener = new JavaElementChangeListener();
		fJavaElementChangeListener.start();
		fPluginRebuilder = new PluginRebuilder();
		fPluginRebuilder.start();
		fFeatureRebuilder = new FeatureRebuilder();
		fFeatureRebuilder.start();

		fTargetPlatformService = context.registerService(ITargetPlatformService.class.getName(), TargetPlatformService.getDefault(), new Hashtable());
		fBundleProjectService = context.registerService(IBundleProjectService.class.getName(), BundleProjectService.getDefault(), new Hashtable());

		// use save participant to clean orphaned profiles.
		ResourcesPlugin.getWorkspace().addSaveParticipant(PLUGIN_ID, new ISaveParticipant() {
			public void saving(ISaveContext saveContext) throws CoreException {
				P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
				if (fSearchablePluginsManager != null) {
					fSearchablePluginsManager.saving(saveContext);
				}
				PluginModelManager.saveInstance();
			}

			public void rollback(ISaveContext saveContext) {
				if (fSearchablePluginsManager != null) {
					fSearchablePluginsManager.rollback(saveContext);
				}
			}

			public void prepareToSave(ISaveContext saveContext) throws CoreException {
				if (fSearchablePluginsManager != null) {
					fSearchablePluginsManager.prepareToSave(saveContext);
				}
			}

			public void doneSaving(ISaveContext saveContext) {
				if (fSearchablePluginsManager != null) {
					fSearchablePluginsManager.doneSaving(saveContext);
				}
			}
		});

	}

	public BundleContext getBundleContext() {
		return fBundleContext;
	}

	public void stop(BundleContext context) throws CoreException {

		if (fPreferenceManager != null) {
			fPreferenceManager.savePluginPreferences();
		}

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
			fSearchablePluginsManager.shutdown();
			fSearchablePluginsManager = null;
		}
		if (fFeatureModelManager != null) {
			fFeatureModelManager.shutdown();
			fFeatureModelManager = null;
		}
		// always shut down extension registry before model manager (since it needs data from model manager)
		if (fExtensionRegistry != null) {
			fExtensionRegistry.stop();
			fExtensionRegistry = null;
		}

		PluginModelManager.shutdownInstance();

		if (fTargetPlatformService != null) {
			fTargetPlatformService.unregister();
			fTargetPlatformService = null;
		}
		if (fBundleProjectService != null) {
			fBundleProjectService.unregister();
			fBundleProjectService = null;
		}

		ResourcesPlugin.getWorkspace().removeSaveParticipant(PLUGIN_ID);
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 * 
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 */
	public Object acquireService(String serviceName) {
		ServiceReference reference = fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		Object service = fBundleContext.getService(reference);
		if (service != null) {
			fBundleContext.ungetService(reference);
		}
		return service;
	}
}
