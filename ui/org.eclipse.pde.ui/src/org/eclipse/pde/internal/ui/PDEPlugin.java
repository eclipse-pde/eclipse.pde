package org.eclipse.pde.internal.ui;
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
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.editor.feature.FeatureAdapterFactory;
import org.eclipse.pde.internal.ui.editor.manifest.PluginAdapterFactory;
import org.eclipse.pde.internal.ui.editor.schema.SchemaAdapterFactory;
import org.eclipse.pde.internal.ui.ischema.ISchemaObject;
import org.eclipse.pde.internal.ui.model.ifeature.IFeatureObject;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferencePage;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.view.PluginsViewAdapterFactory;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class PDEPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.eclipse.pde.ui";

	public static final String MANIFEST_EDITOR_ID = PLUGIN_ID + ".manifestEditor";
	public static final String FRAGMENT_EDITOR_ID = PLUGIN_ID + ".fragmentEditor";
	public static final String FEATURE_EDITOR_ID = PLUGIN_ID + ".featureEditor";
	public static final String JARS_EDITOR_ID = PLUGIN_ID + ".jarsEditor";
	public static final String BUILD_EDITOR_ID = PLUGIN_ID + ".buildEditor";
	public static final String SCHEMA_EDITOR_ID = PLUGIN_ID + ".schemaEditor";
	public static final String PLUGINS_VIEW_ID = "org.eclipse.pde.ui.internal.view.PluginsView";

	public static final String MANIFEST_BUILDER_ID =
		PLUGIN_ID + "." + "ManifestBuilder";
	public static final String SCHEMA_BUILDER_ID =
		PLUGIN_ID + "." + "SchemaBuilder";
	public static final String PLUGIN_NATURE = PLUGIN_ID + "." + "PluginNature";
	public static final String FEATURE_NATURE = PLUGIN_ID + "." + "FeatureNature";
	public static final String FEATURE_BUILDER_ID =
		PLUGIN_ID + "." + "FeatureBuilder";

	public static final String RUN_LAUNCHER_ID =
		PLUGIN_ID + "." + "WorkbenchRunLauncher";
	public static final String DEBUG_LAUNCHER_ID =
		PLUGIN_ID + "." + "WorkbenchDebugLauncher";

	public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME";

	private static final String KEY_RUNNING = "RunningEclipse.message";

	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY =
		new QualifiedName(PLUGIN_ID, "imported");
	public static final String EXTERNAL_PROJECT_VALUE = "external";
	public static final String BINARY_PROJECT_VALUE = "binary";

	// Shared instance
	private static PDEPlugin inst;
	// Resource bundle
	private ResourceBundle resourceBundle;
	// External model manager
	private ExternalModelManager externalModelManager;
	// Tracing options manager
	private TracingOptionsManager tracingOptionsManager;
	// Schema registry
	private SchemaRegistry schemaRegistry;
	// Shared label labelProvider
	private PDELabelProvider labelProvider;
	// A flag that indicates if we are running inside VAJ or not.
	private static boolean inVAJ;
	private java.util.Hashtable counters;
	private WorkspaceModelManager workspaceModelManager;
	private PluginModelManager modelManager;
	private Vector currentLaunchListeners = new Vector();
	private RunningInstanceManager runningInstanceManager;

	static {
		try {
			Class.forName("com.ibm.uvm.lang.ProjectClassLoader");
			inVAJ = true;
		} catch (Exception e) {
			inVAJ = false;
		}
	}

	public PDEPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle("org.eclipse.pde.internal.ui.pderesources");
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
		PluginVersionIdentifier pid1 = new PluginVersionIdentifier(version1);
		PluginVersionIdentifier pid2 = new PluginVersionIdentifier(version2);

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
	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	public static PDEPlugin getDefault() {
		return inst;
	}
	public Hashtable getDefaultNameCounters() {
		if (counters == null)
			counters = new Hashtable();
		return counters;
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
		return new Path(getDefault().getDescriptor().getInstallURL().getFile());
	}
	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	public static String getResourceString(String key) {
		ResourceBundle bundle = PDEPlugin.getDefault().getResourceBundle();
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
	private void initializePlatformPath() {
		TargetPlatformPreferencePage.initializePlatformPath();
	}
	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
	public boolean isVAJ() {
		return inVAJ;
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
		Display display = SWTUtil.getStandardDisplay();
		final IStatus fstatus = status;
		display.asyncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(null, title, null, fstatus);
			}
		});
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

	public static void registerPlatformLaunchers(IProject project) {
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILauncher[] launchers = manager.getLaunchers();
			for (int i = 0; i < launchers.length; i++) {
				ILauncher launcher = launchers[i];
				if (launcher.getIdentifier().equals(RUN_LAUNCHER_ID)) {
					manager.setDefaultLauncher(project, launcher);
					break;
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void startup() throws CoreException {
		super.startup();
		workspaceModelManager = new WorkspaceModelManager();
		externalModelManager = new ExternalModelManager();

		if (isVAJ() == false)
			initializePlatformPath();

		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				//This causes PDE to bomb - problem in Debug UI
				//JavaRuntime.initializeJREVariables(monitor);
				getExternalModelManager().getEclipseHome(monitor);
			}
		};
		try {
			getWorkspace().run(runnable, null);
		} catch (CoreException e) {
			log(e);
		}
		IAdapterManager manager = Platform.getAdapterManager();
		manager.registerAdapters(new SchemaAdapterFactory(), ISchemaObject.class);
		manager.registerAdapters(new PluginAdapterFactory(), IPluginObject.class);
		manager.registerAdapters(new FeatureAdapterFactory(), IFeatureObject.class);
		PluginsViewAdapterFactory factory = new PluginsViewAdapterFactory();
		manager.registerAdapters(factory, ModelEntry.class);
		manager.registerAdapters(factory, FileAdapter.class);
		// set eclipse home variable if not sets

		getWorkspaceModelManager().reset();
		
		modelManager = new PluginModelManager();
		modelManager.connect(workspaceModelManager, externalModelManager);
		attachToLaunchManager();
	}
	
	public void shutdown() throws CoreException {
		if (schemaRegistry != null)
			schemaRegistry.shutdown();

		detachFromLaunchManager();
		modelManager.shutdown();
		workspaceModelManager.shutdown();
		super.shutdown();
	}

	private void attachToLaunchManager() {
		runningInstanceManager = new RunningInstanceManager();
		DebugPlugin.getDefault().addDebugEventListener(runningInstanceManager);
	}

	private void detachFromLaunchManager() {
		DebugPlugin.getDefault().removeDebugEventListener(runningInstanceManager);
		runningInstanceManager.clear();
	}

	public static File getFileInPlugin(IPath path) {
		try {
			URL installURL =
				new URL(getDefault().getDescriptor().getInstallURL(), path.toString());
			URL localURL = Platform.asLocalURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}

	public PDELabelProvider getLabelProvider() {
		if (labelProvider == null)
			labelProvider = new PDELabelProvider();
		return labelProvider;
	}

	public void registerLaunch(ILaunch launch, IPath path) {
		runningInstanceManager.register(launch, path);
	}
	
	public IStatus getCurrentLaunchStatus(IPath path) {
		if (!runningInstanceManager.isRunning(path)) return null;
		// The run-time workbench is still running with the
		// specified workspace path (if path is null, at least
		// one workbench is still running)
		String message = getResourceString(KEY_RUNNING);
		Status status =
			new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, null);
		return status;
	}
}