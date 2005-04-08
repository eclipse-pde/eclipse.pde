/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.schema.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;

public class PDECore extends Plugin implements IEnvironmentVariables {
	public static final String PLUGIN_ID = "org.eclipse.pde.core"; //$NON-NLS-1$
	public static final String SELFHOSTING_BIN_EXLCUDES = "selfhosting.binExcludes"; //$NON-NLS-1$
	public static final String BINARY_PROJECT_VALUE = "binary"; //$NON-NLS-1$
	public static final String BINARY_REPOSITORY_PROVIDER =
		PLUGIN_ID + "." + "BinaryRepositoryProvider"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String CLASSPATH_CONTAINER_ID =
		PLUGIN_ID + ".requiredPlugins"; //$NON-NLS-1$
	public static final String JAVA_SEARCH_CONTAINER_ID =
		PLUGIN_ID + ".externalJavaSearch"; //$NON-NLS-1$

	public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME"; //$NON-NLS-1$
	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY =
		new QualifiedName(PLUGIN_ID, "imported"); //$NON-NLS-1$
	public static final String EXTERNAL_PROJECT_VALUE = "external"; //$NON-NLS-1$

	// Shared instance
	private static PDECore inst;
	public static final String SITEBUILD_LOG = "build.log"; //$NON-NLS-1$
	public static final String SITEBUILD_SCRIPTS = "scripts"; //$NON-NLS-1$
	public static final String SITEBUILD_TEMP_FOLDER = "temp.folder"; //$NON-NLS-1$
	
	private static boolean isDevLaunchMode = false;

	public static boolean compare(
		String id1,
		String version1,
		String id2,
		String version2,
		int match) {
		if (!(id1.equals(id2)))
			return false;
		if (version1 == null)
			return true;
		if (version2 == null)
			return false;
		PluginVersionIdentifier pid1 = null;
		PluginVersionIdentifier pid2 = null;

		try {
			pid1 = new PluginVersionIdentifier(version1);
			pid2 = new PluginVersionIdentifier(version2);
		} catch (RuntimeException e) {
			// something is wrong with either - try direct comparison
			return version2.equals(version1);
		}

		switch (match) {
			case IMatchRules.NONE :
			case IMatchRules.COMPATIBLE :
				if (pid2.isCompatibleWith(pid1))
					return true;
				break;
			case IMatchRules.EQUIVALENT :
				if (pid2.isEquivalentTo(pid1))
					return true;
				break;
			case IMatchRules.PERFECT :
				if (pid2.isPerfect(pid1))
					return true;
				break;
			case IMatchRules.GREATER_OR_EQUAL :
				if (pid2.isGreaterOrEqualTo(pid1))
					return true;
				break;
		}
		return false;
	}

	public static PDECore getDefault() {
		return inst;
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

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else
			status =
				new Status(
					IStatus.ERROR,
					getPluginId(),
					IStatus.OK,
					e.getMessage(),
					e);
		log(status);
	}

	public static void logErrorMessage(String message) {
		log(
			new Status(
				IStatus.ERROR,
				getPluginId(),
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
					getPluginId(),
					IStatus.OK,
					message,
					e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
	}
	
	public static boolean isDevLaunchMode() {
		String[] args = Platform.getApplicationArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-pdelaunch")) //$NON-NLS-1$
				isDevLaunchMode = true;			
		}
		return isDevLaunchMode;
	}
	// External model manager
	private PluginModelManager modelManager;
	//private boolean modelsLocked = false;

	// Schema registry
	private SchemaRegistry schemaRegistry;

	// User-defined attachments manager
	private SourceAttachmentManager sourceAttachmentManager;
	private SourceLocationManager sourceLocationManager;

	// Tracing options manager
	private TracingOptionsManager tracingOptionsManager;
	private BundleContext context;
	private ServiceTracker tracker;
	private ExternalModelManager externalModelManager;
	private WorkspaceModelManager workspaceModelManager;
	private JavaElementChangeListener fJavaElementChangeListener;
	private FeatureModelManager fFeatureModelManager;

	public PDECore() {
		inst = this;
	}

	public URL getInstallURL() {
		try {
			return Platform.resolve(getDefault().getBundle().getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
	}
	
	public IPluginExtensionPoint findExtensionPoint(String fullID) {
		if (fullID == null || fullID.length() == 0)
			return null;
		// separate plugin ID first
		int lastDot = fullID.lastIndexOf('.');
		if (lastDot == -1)
			return null;
		String pluginID = fullID.substring(0, lastDot);
		IPlugin plugin = findPlugin(pluginID);
		if (plugin == null)
			return null;
		String pointID = fullID.substring(lastDot + 1);
		IPluginExtensionPoint[] points = plugin.getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			IPluginExtensionPoint point = points[i];
			if (point.getId().equals(pointID))
				return point;
		}
		return null;
	}

	private IFeature findFeature(
		IFeatureModel[] models,
		String id,
		String version,
		int match) {

		for (int i = 0; i < models.length; i++) {
			IFeatureModel model = models[i];

			IFeature feature = model.getFeature();
			String pid = feature.getId();
			String pversion = feature.getVersion();
			if (compare(id, version, pid, pversion, match))
				return feature;
		}
		return null;
	}

	/**
	 * Finds a feature with the given ID, any version
	 * @param id
	 * @return IFeature or null
	 */
	public IFeature findFeature(String id) {
		IFeatureModel[] models = getFeatureModelManager().findFeatureModels(id);
		if (models.length > 0)
			return models[0].getFeature();
		return null;
	}

	/**
	 * Finds a feature with the given ID and satisfying constraints
	 * of the version and the match.
	 * @param id
	 * @param version
	 * @param match
	 * @return IFeature or null
	 */
	public IFeature findFeature(String id, String version, int match) {
		IFeatureModel[] models = getFeatureModelManager().findFeatureModels(id);
		return findFeature(models, id, version, match);
	}

	public IFragment[] findFragmentsFor(String id, String version) {
		IFragmentModel[] models = getModelManager().getFragments();
		ArrayList list = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			if (!models[i].isEnabled())
				continue;
			IFragment fragment = models[i].getFragment();
			if (compare(
					fragment.getPluginId(),
					fragment.getPluginVersion(),
					id,
					version,
					fragment.getRule())) {
				list.add(fragment);
			}
		}	
		return (IFragment[]) list.toArray(new IFragment[list.size()]);
	}

	public IPlugin findPlugin(String id) {
		return findPlugin(id, null, 0);
	}

	public IPlugin findPlugin(String id, String version, int match) {
		IPluginModel model = getModelManager().findPluginModel(id);
		return (model != null && model.isEnabled()) ? model.getPlugin() : null;
	}

	public ExternalModelManager getExternalModelManager() {
		initializeModels();
		return externalModelManager;
	}
	public PluginModelManager getModelManager() {
		initializeModels();
		return modelManager;
	}
	
	public FeatureModelManager getFeatureModelManager() {
		initializeModels();
		return fFeatureModelManager;
	}
	
	public JavaElementChangeListener getJavaElementChangeListener() {
		if (fJavaElementChangeListener == null)
			fJavaElementChangeListener = new JavaElementChangeListener();
		return fJavaElementChangeListener;
	}
	
	public SchemaRegistry getSchemaRegistry() {
		if (schemaRegistry == null)
			schemaRegistry = new SchemaRegistry();
		return schemaRegistry;
	}

	public SourceAttachmentManager getSourceAttachmentManager() {
		if (sourceAttachmentManager == null)
			sourceAttachmentManager = new SourceAttachmentManager();
		return sourceAttachmentManager;
	}
	public SourceLocationManager getSourceLocationManager() {
		if (sourceLocationManager == null)
			sourceLocationManager = new SourceLocationManager();
		return sourceLocationManager;
	}

	public TracingOptionsManager getTracingOptionsManager() {
		if (tracingOptionsManager == null)
			tracingOptionsManager = new TracingOptionsManager();
		return tracingOptionsManager;
	}
	public WorkspaceModelManager getWorkspaceModelManager() {
		initializeModels();
		return workspaceModelManager;
	}

	/**
	 * @see org.eclipse.core.runtime.Plugin#initializeDefaultPluginPreferences()
	 */
	protected void initializeDefaultPluginPreferences() {
		Preferences preferences = getPluginPreferences();
		preferences.setDefault(
			ICoreConstants.TARGET_MODE,
			ICoreConstants.VALUE_USE_THIS);
		preferences.setDefault(
			ICoreConstants.CHECKED_PLUGINS,
			ICoreConstants.VALUE_SAVED_ALL);
		if (preferences
			.getString(ICoreConstants.TARGET_MODE)
			.equals(ICoreConstants.VALUE_USE_THIS))
			preferences.setValue(
				ICoreConstants.PLATFORM_PATH,
				ExternalModelManager.computeDefaultPlatformPath());
		else
			preferences.setDefault(
				ICoreConstants.PLATFORM_PATH,
				ExternalModelManager.computeDefaultPlatformPath());

		// set defaults for the target environment variables.
		preferences.setDefault(OS, Platform.getOS());
		preferences.setDefault(WS, Platform.getWS());
		preferences.setDefault(NL, Locale.getDefault().toString());
		preferences.setDefault(ARCH, Platform.getOSArch());
		
		// set the defaults for the target JRE
		preferences.setDefault(ICoreConstants.USE_DEFAULT_JRE, true);
	}

	private synchronized void initializeModels() {
		if (modelManager != null && externalModelManager != null && workspaceModelManager != null)
			return;
		externalModelManager = new ExternalModelManager();
		workspaceModelManager = new WorkspaceModelManager();
		modelManager = new PluginModelManager();
		modelManager.connect(workspaceModelManager, externalModelManager);
		fFeatureModelManager = new FeatureModelManager(workspaceModelManager);
	}

	public void releasePlatform() {
		if (tracker == null)
			return;
		tracker.close();
		tracker = null;
	}
	
	public PlatformAdmin acquirePlatform() {
		if (tracker==null) {
			tracker = new ServiceTracker(context, PlatformAdmin.class.getName(), null);
			tracker.open();
		}
		PlatformAdmin result = (PlatformAdmin)tracker.getService();
		while (result == null) {
			try {
				tracker.waitForService(1000);
				result = (PlatformAdmin) tracker.getService();
			} catch (InterruptedException ie) {
			}
		}
		return result;
	}
	
	public String getTargetVersion() {
		return getModelManager().getTargetVersion();
	}
	
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
		fJavaElementChangeListener = new JavaElementChangeListener();
	}

	public BundleContext getBundleContext() {
		return this.context;
	}

	public void stop(BundleContext context) throws CoreException {
		PDECore.getDefault().savePluginPreferences();
		if (fJavaElementChangeListener != null) {
			fJavaElementChangeListener.shutdown();
			fJavaElementChangeListener = null;
		}
		if (schemaRegistry != null) {
			schemaRegistry.shutdown();
			schemaRegistry = null;
		}
		if (modelManager != null) {
			modelManager.shutdown();
			modelManager = null;
		}
		if (fFeatureModelManager!=null) {
			fFeatureModelManager.shutdown();
			fFeatureModelManager = null;
		}
		if (externalModelManager!=null) {
			externalModelManager.shutdown();
			externalModelManager=null;
		}
		if (workspaceModelManager!=null) {
			workspaceModelManager.shutdown();
			workspaceModelManager = null;
		}
	}
}
