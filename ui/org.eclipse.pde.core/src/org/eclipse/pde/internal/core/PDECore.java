package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;

public class PDECore extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.pde.core";

	public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME";
	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY =
		new QualifiedName(PLUGIN_ID, "imported");
	public static final String EXTERNAL_PROJECT_VALUE = "external";
	public static final String BINARY_PROJECT_VALUE = "binary";
	
	public static final String CLASSPATH_CONTAINER_ID = PLUGIN_ID+".requiredPlugins";

	// Shared instance
	private static PDECore inst;
	// Resource bundle
	private ResourceBundle resourceBundle;
	// External model manager
	private ExternalModelManager externalModelManager;
	// Tracing options manager
	private TracingOptionsManager tracingOptionsManager;
	// Schema registry
	private SchemaRegistry schemaRegistry;
	private WorkspaceModelManager workspaceModelManager;
	private PluginModelManager modelManager;
	private SourceLocationManager sourceLocationManager;
	private CoreSettings settings;

	public PDECore(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle(
					"org.eclipse.pde.internal.core.pderesources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
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
	private IPlugin findPlugin(IPluginModel[] models, String id) {
		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
			if (model.isEnabled() == false)
				continue;
			IPlugin plugin = model.getPlugin();
			String pid = plugin.getId();
			if (pid != null && pid.equals(id))
				return plugin;
		}
		return null;
	}
	private IPlugin findPlugin(
		IPluginModel[] models,
		String id,
		String version,
		int match) {

		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
			if (model.isEnabled() == false)
				continue;
			IPlugin plugin = model.getPlugin();
			String pid = plugin.getId();
			String pversion = plugin.getVersion();
			if (compare(id, version, pid, pversion, match))
				return plugin;
		}
		return null;
	}

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

	public IPlugin findPlugin(String id) {
		return findPlugin(id, null, IMatchRules.NONE);
	}
	public IPlugin findPlugin(String id, String version, int match) {
		WorkspaceModelManager manager = getWorkspaceModelManager();
		IPlugin plugin =
			findPlugin(manager.getWorkspacePluginModels(), id, version, match);
		if (plugin != null)
			return plugin;
		ExternalModelManager exmanager = getExternalModelManager();
		return findPlugin(exmanager.getModels(), id, version, match);
	}
	public static PDECore getDefault() {
		return inst;
	}

	public CoreSettings getSettings() {
		return settings;
	}
	public ExternalModelManager getExternalModelManager() {
		if (externalModelManager == null)
			externalModelManager = new ExternalModelManager();
		return externalModelManager;
	}
	public static String getFormattedMessage(String key, String[] args) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, args);
	}
	public static String getFormattedMessage(String key, String arg) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, new Object[] { arg });
	}
	static IPath getInstallLocation() {
		return new Path(inst.getDescriptor().getInstallURL().getFile());
	}
	public static String getPluginId() {
		return inst.getDescriptor().getUniqueIdentifier();
	}
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	public static String getResourceString(String key) {
		ResourceBundle bundle = inst.getResourceBundle();
		if (bundle != null) {
			try {
				String bundleString = bundle.getString(key);
				//return "$"+bundleString;
				return bundleString;
			} catch (MissingResourceException e) {
				// default actions is to return key, which is OK
			}
		}
		return key;
	}
	public SchemaRegistry getSchemaRegistry() {
		if (schemaRegistry == null)
			schemaRegistry = new SchemaRegistry();
		return schemaRegistry;
	}
	public TracingOptionsManager getTracingOptionsManager() {
		if (tracingOptionsManager == null)
			tracingOptionsManager = new TracingOptionsManager();
		return tracingOptionsManager;
	}
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	public WorkspaceModelManager getWorkspaceModelManager() {
		if (workspaceModelManager == null)
			workspaceModelManager = new WorkspaceModelManager();
		return workspaceModelManager;
	}
	public PluginModelManager getModelManager() {
		return modelManager;
	}
	public SourceLocationManager getSourceLocationManager() {
		if (sourceLocationManager == null)
			sourceLocationManager = new SourceLocationManager();
		return sourceLocationManager;
	}
	private void initializePlatformPath() {
		ExternalModelManager.initializePlatformPath();
	}
	public static void log(IStatus status) {
		ResourcesPlugin.getPlugin().getLog().log(status);
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
				new Status(
					IStatus.ERROR,
					getPluginId(),
					IStatus.OK,
					e.getMessage(),
					e);
		log(status);
	}

	public void startup() throws CoreException {
		super.startup();
		loadSettings();
		workspaceModelManager = new WorkspaceModelManager();
		externalModelManager = new ExternalModelManager();

		initializePlatformPath();

		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				getExternalModelManager().getEclipseHome(monitor);
				getSourceLocationManager().initializeClasspathVariables(monitor);
			}
		};
		try {
			getWorkspace().run(runnable, null);
		} catch (CoreException e) {
			log(e);
		}
		getWorkspaceModelManager().reset();

		modelManager = new PluginModelManager();
		modelManager.connect(workspaceModelManager, externalModelManager);
	}

	public void shutdown() throws CoreException {
		storeSettings();
		if (schemaRegistry != null)
			schemaRegistry.shutdown();

		modelManager.shutdown();
		workspaceModelManager.shutdown();
		super.shutdown();
	}

	private void loadSettings() {
		settings = new CoreSettings();
		settings.load(getStateLocation());
	}

	private void storeSettings() {
		settings.store();
	}

}