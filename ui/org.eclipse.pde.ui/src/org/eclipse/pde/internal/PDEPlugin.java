package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.editor.component.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.debug.core.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.editor.schema.*;
import org.eclipse.pde.internal.editor.manifest.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.schema.*;
import java.util.*;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.pde.internal.preferences.*;
import org.eclipse.jface.dialogs.*;

public class PDEPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.eclipse.pde";

	public static final String MANIFEST_EDITOR_ID = PLUGIN_ID + ".manifestEditor";
	public static final String FRAGMENT_EDITOR_ID = PLUGIN_ID + ".fragmentEditor";
	public static final String COMPONENT_EDITOR_ID = PLUGIN_ID + ".componentEditor";
	public static final String JARS_EDITOR_ID = PLUGIN_ID + ".jarsEditor";
	public static final String BUILD_EDITOR_ID = PLUGIN_ID + ".buildEditor";
	public static final String SCHEMA_EDITOR_ID = PLUGIN_ID + ".schemaEditor";

	public static final String MANIFEST_BUILDER_ID = PLUGIN_ID + "." + "ManifestBuilder";
	public static final String SCHEMA_BUILDER_ID = PLUGIN_ID + "." + "SchemaBuilder";
	public static final String PLUGIN_NATURE = PLUGIN_ID + "." + "PluginNature";
	public static final String COMPONENT_NATURE = PLUGIN_ID + "." + "ComponentNature";
	public static final String COMPONENT_BUILDER_ID = PLUGIN_ID + "." + "ComponentBuilder";

	public static final String RUN_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchRunLauncher";
	public static final String DEBUG_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchDebugLauncher";

	public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME";

// Shared instance
	private static PDEPlugin inst;
// Resource bundle
	private ResourceBundle resourceBundle;
// Plugin Info
	private ExternalModelManager externalModelManager;
// Plugin Info
	private TracingOptionsManager tracingOptionsManager;
// Plugin Info
	private SchemaRegistry schemaRegistry;
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
	private java.util.Hashtable counters;
	private WorkspaceModelManager workspaceModelManager;

public PDEPlugin(IPluginDescriptor descriptor) {
	super(descriptor);
	inst = this;
	try {
		resourceBundle = ResourceBundle.getBundle("org.eclipse.pde.internal.pderesources");
	} catch (MissingResourceException x) {
		resourceBundle = null;
	}
}
public IPluginExtensionPoint findExtensionPoint(String fullID) {
	if (fullID==null || fullID.length()==0) return null;
	// separate plugin ID first
	int lastDot = fullID.lastIndexOf('.');
	if (lastDot == -1) return null;
	String pluginID = fullID.substring(0, lastDot);
	IPlugin plugin = findPlugin(pluginID);
	if (plugin==null) return null;
	String pointID = fullID.substring(lastDot+1);
	IPluginExtensionPoint [] points = plugin.getExtensionPoints();
	for (int i=0; i<points.length; i++) {
		IPluginExtensionPoint point = points[i];
		if (point.getId().equals(pointID)) return point;
	}
	return null;
}
private IPlugin findPlugin(IPluginModel[] models, String id) {
	for (int i = 0; i < models.length; i++) {
		IPluginModel model = models[i];
		if (model.isEnabled()==false) continue;
		IPlugin plugin = model.getPlugin();
		if (plugin.getId().equals(id))
			return plugin;
	}
	return null;
}
private IPlugin findPlugin(IPluginModel[] models, String id, String version) {
	for (int i = 0; i < models.length; i++) {
		IPluginModel model = models[i];
		if (model.isEnabled() == false)
			continue;
		IPlugin plugin = model.getPlugin();
		if (plugin.getId().equals(id)) {
			if (version == null || plugin.getVersion().equals(version))
				return plugin;
		}
	}
	return null;
}
public IPlugin findPlugin(String id) {
	return findPlugin(id, null);
}
public IPlugin findPlugin(String id, String version) {
	WorkspaceModelManager manager = getWorkspaceModelManager();
	IPlugin plugin = findPlugin(manager.getWorkspacePluginModels(), id, version);
	if (plugin!=null) return plugin;
	ExternalModelManager exmanager = getExternalModelManager();
	if (exmanager.hasEnabledModels()) {
		return findPlugin(exmanager.getModels(), id, version);
	}
	return null;
}
public static IWorkbenchPage getActivePage() {
	return getDefault().internalGetActivePage();
}
public static Shell getActiveWorkbenchShell() {
	return getActiveWorkbenchWindow().getShell();
}
public static IWorkbenchWindow getActiveWorkbenchWindow() {
	return getDefault().getWorkbench().getActiveWorkbenchWindow();
}
public static PDEPlugin getDefault() {
	return inst;
}
public Hashtable getDefaultNameCounters() {
	if (counters==null) counters = new Hashtable();
	return counters;
}
public ExternalModelManager getExternalModelManager() {
	if (externalModelManager==null) externalModelManager = new ExternalModelManager();
	return externalModelManager;
}
public static String getFormattedMessage(String key, String [] args) {
	String text = getResourceString(key);
	return java.text.MessageFormat.format(text, args);
}
public static String getFormattedMessage(String key, String arg) {
	String text = getResourceString(key);
	return java.text.MessageFormat.format(text, new Object [] { arg });
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
	if (schemaRegistry==null) schemaRegistry = new SchemaRegistry();
	return schemaRegistry;
}
public TracingOptionsManager getTracingOptionsManager() {
	if (tracingOptionsManager==null) tracingOptionsManager = new TracingOptionsManager();
	return tracingOptionsManager;
}
public static IWorkspace getWorkspace() {
	return ResourcesPlugin.getWorkspace();
}
public WorkspaceModelManager getWorkspaceModelManager() {
	if (workspaceModelManager==null) workspaceModelManager = new WorkspaceModelManager();
	return workspaceModelManager;
}
private void initializePlatformPath() {
	PDEBasePreferencePage.initializePlatformPath();
}
private IWorkbenchPage internalGetActivePage() {
	return getWorkbench().getActiveWorkbenchWindow().getActivePage();
}
public boolean isVAJ() {
	return inVAJ;
}
public static void log(IStatus status) {
	getDefault().getLog().log(status);
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
	if (e instanceof InvocationTargetException) {
		e = ((InvocationTargetException)e).getTargetException();
	}
	String message = e.getMessage();
	if (message==null)
	   message = e.toString();
	Status status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
	ErrorDialog.openError(getActiveWorkbenchShell(), null, null, status);
	ResourcesPlugin.getPlugin().getLog().log(status);
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
	super.shutdown();
}
public void startup() throws CoreException {
	IAdapterManager manager = Platform.getAdapterManager();
	manager.registerAdapters(new SchemaAdapterFactory(), ISchemaObject.class);
	manager.registerAdapters(new PluginAdapterFactory(), IPluginObject.class);
	manager.registerAdapters(new ComponentAdapterFactory(), IComponentObject.class);
	// set eclipse home variable if not sets
	if (isVAJ()==false) initializePlatformPath();
	getExternalModelManager().getEclipseHome();
	getWorkspaceModelManager().reset();
}
}
