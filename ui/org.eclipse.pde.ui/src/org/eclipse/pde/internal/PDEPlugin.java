package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.editor.feature.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.debug.core.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.editor.schema.*;
import org.eclipse.pde.internal.editor.manifest.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.model.plugin.IMatchRules;
import org.eclipse.pde.internal.schema.*;
import java.util.*;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.pde.internal.preferences.*;
import org.eclipse.jface.dialogs.*;
import java.net.URL;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.pde.model.plugin.IMatchRules;
import org.eclipse.pde.internal.launcher.ICurrentLaunchListener;

public class PDEPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.eclipse.pde";

	public static final String MANIFEST_EDITOR_ID = PLUGIN_ID + ".manifestEditor";
	public static final String FRAGMENT_EDITOR_ID = PLUGIN_ID + ".fragmentEditor";
	public static final String FEATURE_EDITOR_ID = PLUGIN_ID + ".featureEditor";
	public static final String JARS_EDITOR_ID = PLUGIN_ID + ".jarsEditor";
	public static final String BUILD_EDITOR_ID = PLUGIN_ID + ".buildEditor";
	public static final String SCHEMA_EDITOR_ID = PLUGIN_ID + ".schemaEditor";
	public static final String PLUGINS_VIEW_ID = "org.eclipse.pde.pluginsView";

	public static final String MANIFEST_BUILDER_ID =
		PLUGIN_ID + "." + "ManifestBuilder";
	public static final String SCHEMA_BUILDER_ID =
		PLUGIN_ID + "." + "SchemaBuilder";
	public static final String PLUGIN_NATURE = PLUGIN_ID + "." + "PluginNature";
	public static final String FEATURE_NATURE =
		PLUGIN_ID + "." + "FeatureNature";
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
	static {
		try {
			Class.forName("com.ibm.uvm.lang.ProjectClassLoader");
			inVAJ = true;
		} catch (Exception e) {
			inVAJ = false;
		}
	}

	class DebugListener implements IDebugEventListener {
		public void handleDebugEvent(DebugEvent e) {
			if (currentLaunch == null)
				return;
			Object obj = e.getSource();
			if (obj instanceof IProcess) {
				if ((e.getKind() & DebugEvent.TERMINATE) != 0) {
					ILaunch launch = ((IProcess) obj).getLaunch();
					if (launch.equals(currentLaunch) && currentLaunch.isTerminated()) {
						currentLaunch = null;
						fireCurrentLaunchChanged();
					}
				}
			}
		}

		private void fireCurrentLaunchChanged() {
			for (Enumeration enum = currentLaunchListeners.elements();
				enum.hasMoreElements();
				) {
				((ICurrentLaunchListener) enum.nextElement()).currentLaunchChanged();
			}
		}
	}
	private java.util.Hashtable counters;
	private WorkspaceModelManager workspaceModelManager;
	private Vector currentLaunchListeners = new Vector();
	private IDebugEventListener debugListener;
	private ILaunch currentLaunch = null;

	public PDEPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle("org.eclipse.pde.internal.pderesources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	public void addCurrentLaunchListener(ICurrentLaunchListener listener) {
		if (currentLaunchListeners.contains(listener) == false)
			currentLaunchListeners.add(listener);
	}
	public void removeCurrentLaunchListener(ICurrentLaunchListener listener) {
		if (currentLaunchListeners.contains(listener))
			currentLaunchListeners.remove(listener);
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
		PluginVersionIdentifier vid = null;

		if (version != null)
			vid = new PluginVersionIdentifier(version);
		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
			if (model.isEnabled() == false)
				continue;
			IPlugin plugin = model.getPlugin();
			String pid = plugin.getId();
			String pversion = plugin.getVersion();
			if (pid != null && pid.equals(id)) {
				if (version == null)
					return plugin;
				PluginVersionIdentifier pvid = new PluginVersionIdentifier(pversion);

				switch (match) {
					case IMatchRules.NONE :
					case IMatchRules.COMPATIBLE :
						if (pvid.isCompatibleWith(vid))
							return plugin;
						break;
					case IMatchRules.EQUIVALENT :
						if (pvid.isEquivalentTo(vid))
							return plugin;
						break;
					case IMatchRules.PERFECT :
						if (pvid.isPerfect(vid))
							return plugin;
						break;
					case IMatchRules.GREATER_OR_EQUAL :
						if (pvid.isGreaterOrEqualTo(vid))
							return plugin;
						break;
				}
			}
		}
		return null;
	}
	public IPlugin findPlugin(String id) {
		return findPlugin(id, null, 0);
	}
	public IPlugin findPlugin(String id, String version, int match) {
		WorkspaceModelManager manager = getWorkspaceModelManager();
		IPlugin plugin =
			findPlugin(manager.getWorkspacePluginModels(), id, version, match);
		if (plugin != null)
			return plugin;
		ExternalModelManager exmanager = getExternalModelManager();
		if (exmanager.hasEnabledModels()) {
			return findPlugin(exmanager.getModels(), id, version, match);
		}
		return null;
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

	public static void logException(Throwable e, String title, String message) {
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
		ErrorDialog.openError(getActiveWorkbenchShell(), title, null, status);
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
	public void shutdown() throws CoreException {
		if (schemaRegistry != null)
			schemaRegistry.shutdown();
		if (workspaceModelManager != null)
			workspaceModelManager.shutdown();
		detachFromLaunchManager();
		super.shutdown();
	}
	public void startup() throws CoreException {
		super.startup();

		if (isVAJ() == false)
			initializePlatformPath();

		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				JavaRuntime.initializeJREVariables(monitor);
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
		// set eclipse home variable if not sets

		getWorkspaceModelManager().reset();
		attachToLaunchManager();
	}

	private void attachToLaunchManager() {
		debugListener = new DebugListener();
		DebugPlugin.getDefault().addDebugEventListener(debugListener);
	}

	private void detachFromLaunchManager() {
		DebugPlugin.getDefault().removeDebugEventListener(debugListener);
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
		if (labelProvider==null) labelProvider = new PDELabelProvider();
		return labelProvider;
	}

	public void registerLaunch(ILaunch launch) {
		this.currentLaunch = launch;
	}

	public IStatus getCurrentLaunchStatus() {
		if (currentLaunch == null)
			return null;

		if (currentLaunch.isTerminated()) {
			currentLaunch = null;
			return null;
		}
		// The run-time workbench is still running
		String message = getResourceString(KEY_RUNNING);
		Status status =
			new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, null);
		return status;
	}
}