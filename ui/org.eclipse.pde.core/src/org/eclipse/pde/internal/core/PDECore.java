package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;

public class PDECore extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.pde.core";

	public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME";
	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY =
		new QualifiedName(PLUGIN_ID, "imported");
	public static final String EXTERNAL_PROJECT_VALUE = "external";
	public static final String BINARY_PROJECT_VALUE = "binary";
	public static final String BINARY_REPOSITORY_PROVIDER = PLUGIN_ID + "."+"BinaryRepositoryProvider";
	
	public static final String CLASSPATH_CONTAINER_ID = PLUGIN_ID+".requiredPlugins";
	public static final String SITEBUILD_DIR = ".sitebuild";
	public static final String SITEBUILD_SCRIPTS = "scripts";
	public static final String SITEBUILD_TEMP_FOLDER = "temp.folder";
	public static final String SITEBUILD_LOG = "build.log";
	public static final String SITEBUILD_PROPERTIES = "sitebuild.xml";
	public static final String SITEBUILD_FILE = SITEBUILD_DIR + "/"+SITEBUILD_PROPERTIES;

	// Shared instance
	private static PDECore inst;
	// Resource bundle
	private ResourceBundle resourceBundle;
	// External model manager
	private ExternalModelManager externalModelManager;
	// Tracing options manager
	private TracingOptionsManager tracingOptionsManager;
	
	// User-defined attachments manager
	private SourceAttachmentManager sourceAttachmentManager;
	// Schema registry
	private SchemaRegistry schemaRegistry;
	private WorkspaceModelManager workspaceModelManager;
	private PluginModelManager modelManager;
	private SourceLocationManager sourceLocationManager;
	private TempFileManager tempFileManager;

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

	public ExternalModelManager getExternalModelManager() {
		if (externalModelManager == null)
			initializeModels();
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
			initializeModels();
		return workspaceModelManager;
	}
	public PluginModelManager getModelManager() {
		if (modelManager == null)
			initializeModels();
		return modelManager;
	}
	public SourceLocationManager getSourceLocationManager() {
		if (sourceLocationManager == null)
			sourceLocationManager = new SourceLocationManager();
		return sourceLocationManager;
	}
	
	public SourceAttachmentManager getSourceAttachmentManager() {
		if (sourceAttachmentManager == null)
			sourceAttachmentManager = new SourceAttachmentManager();
		return sourceAttachmentManager;
	}
	
	public TempFileManager getTempFileManager() {
		if (tempFileManager==null)
			tempFileManager = new TempFileManager();
		return tempFileManager;
	}
	
	private void initializeModels() {
		workspaceModelManager = new WorkspaceModelManager();
		externalModelManager = new ExternalModelManager();
		workspaceModelManager.reset();
		modelManager = new PluginModelManager();
		modelManager.connect(workspaceModelManager,externalModelManager);
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
		logException(e, null);
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

	public void shutdown() throws CoreException {
		PDECore.getDefault().savePluginPreferences();
		if (schemaRegistry != null)
			schemaRegistry.shutdown();
		if (modelManager != null)
			modelManager.shutdown();
		if (workspaceModelManager != null)
			workspaceModelManager.shutdown();
		if (tempFileManager!=null)
			tempFileManager.shutdown();
		super.shutdown();
	}

	private void loadSettings() {
		File file = new File(getStateLocation().append("settings.properties").toOSString());
		if (file.exists()) {
			Properties settings = new Properties();
			try {
				FileInputStream fis = new FileInputStream(file);
				settings.load(fis);
				fis.close();
			} catch (IOException e) {
			}
			Preferences preferences =
				PDECore.getDefault().getPluginPreferences();
			Enumeration propertyNames = settings.propertyNames();
			while (propertyNames.hasMoreElements()) {
				String property = propertyNames.nextElement().toString();
				preferences.setValue(property, settings.getProperty(property));
			}
			file.delete();
		}
	}

	/**
	 * @see org.eclipse.core.runtime.Plugin#initializeDefaultPluginPreferences()
	 */
	protected void initializeDefaultPluginPreferences() {
		loadSettings();
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		preferences.setDefault(ICoreConstants.TARGET_MODE, ICoreConstants.VALUE_USE_THIS);
		preferences.setDefault(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_NONE);
		if (preferences.getString(ICoreConstants.TARGET_MODE).equals(ICoreConstants.VALUE_USE_THIS))
			preferences.setValue(ICoreConstants.PLATFORM_PATH,ExternalModelManager.computeDefaultPlatformPath());
		else
			preferences.setDefault(ICoreConstants.PLATFORM_PATH, ExternalModelManager.computeDefaultPlatformPath());
		try {
			JavaCore.setClasspathVariable(
				PDECore.ECLIPSE_HOME_VARIABLE,
				new Path(preferences.getString(ICoreConstants.PLATFORM_PATH)),
				null);
		} catch (JavaModelException e) {
		}
	}


}