package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.net.*;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;

/**
 */
public class ExternalModelManager {
	private Vector models;
	private Vector fmodels;
	private static final String KEY_ERROR_TITLE = "Errors.SetupError";
	private static final String KEY_SCANNING_PROBLEMS =
		"ExternalModelManager.scanningProblems";
	private static final String KEY_ERROR_NO_HOME =
		"Errors.SetupError.NoPlatformHome";
	private static final String KEY_PROCESSING_PATH =
		"ExternalModelManager.processingPath";
	private Vector listeners = new Vector();

	public ExternalModelManager() {
	}

	public void addModelProviderListener(IModelProviderListener listener) {
		listeners.add(listener);
	}

	public void clear() {
		models = null;
		fmodels = null;
	}

	private static IPath createEclipseRelativeHome(
		String installLocation,
		IProgressMonitor monitor) {
		IPath fullPath = new Path(installLocation);
		IPath eclipseHome = getEclipseHome(monitor);
		int nmatching = fullPath.matchingFirstSegments(eclipseHome);
		return fullPath.removeFirstSegments(nmatching);
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

	public IPlugin findPlugin(String id) {
		if (models == null)
			loadModels(new NullProgressMonitor());
		if (models == null)
			return null;
		for (int i = 0; i < models.size(); i++) {
			IPluginModel model = (IPluginModel) models.elementAt(i);
			IPlugin plugin = model.getPlugin();
			if (plugin.getId().equals(id))
				return plugin;
		}
		return null;
	}

	public void fireModelProviderEvent(IModelProviderEvent e) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IModelProviderListener listener = (IModelProviderListener) iter.next();
			listener.modelsChanged(e);
		}
	}

	public IPluginModel[] getModels() {
		return getModels(new NullProgressMonitor());
	}

	public boolean isLoaded() {
		return models != null;
	}

	public IPluginModel[] getModels(IProgressMonitor monitor) {
		if (models == null)
			loadModels(monitor);
		if (models != null) {
			IPluginModel[] result = new IPluginModel[models.size()];
			models.copyInto(result);
			return result;
		}
		return new IPluginModel[0];
	}

	public IFragmentModel[] getFragmentModels(IProgressMonitor monitor) {
		if (fmodels == null)
			loadModels(monitor);
		if (fmodels != null) {
			IFragmentModel[] result = new IFragmentModel[fmodels.size()];
			fmodels.copyInto(result);
			return result;
		}
		return new IFragmentModel[0];
	}

	public IPlugin getPlugin(int i) {
		if (models == null)
			loadModels(new NullProgressMonitor());
		if (models != null) {
			IPluginModel model = (IPluginModel) models.elementAt(i);
			return model.getPlugin();
		}
		return null;
	}

	public IFragment getFragment(int i) {
		if (fmodels == null)
			loadModels(new NullProgressMonitor());
		if (fmodels != null) {
			IFragmentModel fmodel = (IFragmentModel) fmodels.elementAt(i);
			return fmodel.getFragment();
		}
		return null;
	}

	public int getPluginCount() {
		if (models == null)
			loadModels(new NullProgressMonitor());
		return (models != null) ? models.size() : 0;
	}

	public int getFragmentCount() {
		if (fmodels == null)
			loadModels(new NullProgressMonitor());
		return (fmodels != null) ? fmodels.size() : 0;
	}

	private String[] getPluginPaths() {
		return getPluginPaths(null);
	}

	private String[] getPluginPaths(String platformHome) {
		if (platformHome == null) {
			CoreSettings settings = PDECore.getDefault().getSettings();
			platformHome = settings.getString(ICoreConstants.PLATFORM_PATH);
		}
		if (platformHome == null || platformHome.length() == 0) {
			//			Display.getCurrent().beep();
			//			MessageDialog.openError(
			//				PDECore.getActiveWorkbenchShell(),
			//				PDECore.getResourceString(KEY_ERROR_TITLE),
			//				PDECore.getResourceString(KEY_ERROR_NO_HOME));
			return new String[0];
		}
		String[] paths = new String[2];
		paths[0] = platformHome + File.separator + "plugins";
		paths[1] = platformHome + File.separator + "fragments";
		return paths;
	}

	public boolean hasEnabledModels() {
		if (models == null) {
			CoreSettings settings = PDECore.getDefault().getSettings();
			String saved = settings.getString(ICoreConstants.CHECKED_PLUGINS);
			if (saved != null && saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
				return false;
			}
			loadModels(new NullProgressMonitor());
		}
		for (int i = 0; i < models.size(); i++) {
			IPluginModel model = (IPluginModel) models.elementAt(i);
			if (model.isEnabled())
				return true;
		}
		return false;
	}

	private boolean loadModels(IProgressMonitor monitor) {
		initializePlatformPath();
		boolean useOther = getUseOther();
		boolean result;

		if (useOther) {
			result = reload(null, monitor);
		} else {
			reloadFromLive(monitor);
			result = true;
		}
		if (result) {
			initialize();
		}
		Object[] array = models.toArray();
		CoreArraySorter.INSTANCE.sortInPlace(array);

		for (int i = 0; i < array.length; i++) {
			models.set(i, array[i]);
		}
		return result;
	}

	protected static void processPluginDescriptorModel(
		Vector result,
		PluginDescriptorModel descriptorModel,
		IProgressMonitor monitor) {
		ExternalPluginModel model = new ExternalPluginModel();
		String location = descriptorModel.getLocation();
		try {
			URL url = new URL(location);
			String localLocation = url.getFile();
			IPath path = new Path(localLocation).removeTrailingSeparator();
			model.setInstallLocation(path.toOSString());
			model.setEclipseHomeRelativePath(
				createEclipseRelativeHome(model.getInstallLocation(), monitor));
		} catch (MalformedURLException e) {
			model.setInstallLocation(location);
		}
		model.load(descriptorModel);
		if (model.isLoaded()) {
			result.add(model);
			// force creation of the plugin object
			model.getPlugin();
			model.setEnabled(true);
		}
	}

	protected static void processFragmentModel(
		Vector result,
		PluginFragmentModel fragmentModel,
		IProgressMonitor monitor) {
		ExternalFragmentModel model = new ExternalFragmentModel();
		String location = fragmentModel.getLocation();
		try {
			URL url = new URL(location);
			String localLocation = url.getFile();
			IPath path = new Path(localLocation).removeTrailingSeparator();
			model.setInstallLocation(path.toOSString());
			model.setEclipseHomeRelativePath(
				createEclipseRelativeHome(model.getInstallLocation(), monitor));
		} catch (MalformedURLException e) {
			model.setInstallLocation(location);
		}
		model.load(fragmentModel);
		if (model.isLoaded()) {
			result.add(model);
			// force creation of the fragment object
			model.getFragment();
			model.setEnabled(true);
		}
	}

	private void internalProcessPluginDirectories(
		Vector result,
		Vector fresult,
		String[] pluginPaths,
		IProgressMonitor monitor) {
		MultiStatus errors =
			processPluginDirectories(result, fresult, pluginPaths, true, monitor);
		if (errors != null && errors.getChildren().length > 0) {
			ResourcesPlugin.getPlugin().getLog().log(errors);
		}
	}

	public static MultiStatus processPluginDirectories(
		Vector result,
		Vector fresult,
		String[] pluginPaths,
		boolean resolve,
		IProgressMonitor monitor) {
		try {
			MultiStatus errors =
				new MultiStatus(
					PDECore.getPluginId(),
					1,
					PDECore.getResourceString(KEY_SCANNING_PROBLEMS),
					null);
			URL[] urls = new URL[pluginPaths.length];
			for (int i = 0; i < pluginPaths.length; i++) {
				urls[i] = new URL("file:" + pluginPaths[i].replace('\\', '/') + "/");
			}

			//String pattern = PDECore.getResourceString(KEY_PROCESSING_PATH);
			//String message = PDECore.getFormattedMessage(pattern, pluginPath);
			//monitor.subTask(message);
			PluginRegistryModel registryModel =
				Platform.parsePlugins(urls, new Factory(errors));
			IStatus resolveStatus = null;
			if (resolve)
				resolveStatus = registryModel.resolve(true, false);
			PluginDescriptorModel[] pluginDescriptorModels = registryModel.getPlugins();
			PluginFragmentModel[] fragmentModels = registryModel.getFragments();
			for (int i = 0; i < pluginDescriptorModels.length; i++) {
				PluginDescriptorModel pluginDescriptorModel = pluginDescriptorModels[i];
				monitor.subTask(pluginDescriptorModel.getId());
				if (pluginDescriptorModel.getEnabled())
					processPluginDescriptorModel(result, pluginDescriptorModel, monitor);
			}
			for (int i = 0; i < fragmentModels.length; i++) {
				PluginFragmentModel fragmentModel = fragmentModels[i];
				processFragmentModel(fresult, fragmentModel, monitor);
			}
			if (resolve)
				errors.merge(resolveStatus);
			return errors;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	private static void processPluginRegistryModel(
		PluginRegistryModel registryModel,
		Vector result,
		Vector fresult,
		IProgressMonitor monitor) {
		PluginDescriptorModel[] pluginDescriptorModels = registryModel.getPlugins();
		PluginFragmentModel[] fragmentModels = registryModel.getFragments();
		for (int i = 0; i < pluginDescriptorModels.length; i++) {
			PluginDescriptorModel pluginDescriptorModel = pluginDescriptorModels[i];
			monitor.subTask(pluginDescriptorModel.getId());
			if (pluginDescriptorModel.getEnabled())
				processPluginDescriptorModel(result, pluginDescriptorModel, monitor);
		}
		for (int i = 0; i < fragmentModels.length; i++) {
			PluginFragmentModel fragmentModel = fragmentModels[i];
			processFragmentModel(fresult, fragmentModel, monitor);
		}
	}

	public boolean reload(String platformPath, IProgressMonitor monitor) {
		models = new Vector();
		fmodels = new Vector();

		if (monitor == null)
			monitor = new NullProgressMonitor();
		if (platformPath != null)
			setEclipseHome(platformPath, monitor);
		String[] paths = getPluginPaths(platformPath);
		if (paths.length == 0)
			return false;

		internalProcessPluginDirectories(models, fmodels, paths, monitor);

		return true;
	}

	public void reloadFromLive(IProgressMonitor monitor) {
		models = new Vector();
		fmodels = new Vector();

		if (monitor == null)
			monitor = new NullProgressMonitor();

		IPluginRegistry liveRegistry = Platform.getPluginRegistry();
		processPluginRegistryModel(
			(PluginRegistryModel) liveRegistry,
			models,
			fmodels,
			monitor);
	}

	public void removeModelProviderListener(IModelProviderListener listener) {
		listeners.remove(listener);
	}

	public static IPath getEclipseHome(IProgressMonitor monitor) {
		IPath eclipseHome =
			JavaCore.getClasspathVariable(PDECore.ECLIPSE_HOME_VARIABLE);
		CoreSettings settings = PDECore.getDefault().getSettings();
		String platformHome = settings.getString(ICoreConstants.PLATFORM_PATH);
		IPath platformPath = new Path(platformHome);
		if (eclipseHome == null || !eclipseHome.equals(platformPath)) {
			setEclipseHome(platformHome, monitor);
			eclipseHome = platformPath;
		}
		return eclipseHome;
	}

	public static void setEclipseHome(
		final String newValue,
		IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			JavaCore.setClasspathVariable(
				PDECore.ECLIPSE_HOME_VARIABLE,
				new Path(newValue),
				monitor);
			CoreSettings store = PDECore.getDefault().getSettings();
			store.setValue(ICoreConstants.PLATFORM_PATH, newValue);
		} catch (JavaModelException e) {
			PDECore.logException(e);
		} finally {
			monitor.done();
		}
	}
	public static void initializePlatformPath() {
		CoreSettings store = PDECore.getDefault().getSettings();
		boolean useThis = true;
		String mode = store.getString(ICoreConstants.TARGET_MODE);

		if (mode != null && mode.equals(ICoreConstants.VALUE_USE_OTHER))
			useThis = false;
		String path = store.getString(ICoreConstants.PLATFORM_PATH);
		String currentPath = computeDefaultPlatformPath();

		if (path == null
			|| path.length() == 0
			|| (useThis && !currentPath.equals(path))) {
			path = currentPath;
			store.setDefault(ICoreConstants.PLATFORM_PATH, path);
			store.setValue(ICoreConstants.PLATFORM_PATH, path);
		}
	}

	public static boolean getUseOther() {
		CoreSettings store = PDECore.getDefault().getSettings();
		boolean useOther = false;
		String mode = store.getString(ICoreConstants.TARGET_MODE);
		if (mode != null && mode.equals(ICoreConstants.VALUE_USE_OTHER))
			useOther = true;
		return useOther;
	}
	private static String computeDefaultPlatformPath() {
		URL installURL = BootLoader.getInstallURL();
		String file = installURL.getFile();
		IPath ppath = new Path(file).removeTrailingSeparator();
		return getCorrectPath(ppath.toOSString());
	}

	private static String getCorrectPath(String path) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (BootLoader.getOS().equals("win32")) {
				if (i == 0 && c == '/')
					continue;
			}
			// Some VMs may return %20 instead of a space
			if (c == '%' && i + 2 < path.length()) {
				char c1 = path.charAt(i + 1);
				char c2 = path.charAt(i + 2);
				if (c1 == '2' && c2 == '0') {
					i += 2;
					continue;
				}
			}
			buf.append(c);
		}
		return buf.toString();
	}
	public void initialize() {
		CoreSettings store = PDECore.getDefault().getSettings();
		String saved = store.getString(ICoreConstants.CHECKED_PLUGINS);

		if (saved.length() == 0 || saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			initializeDefault(false);
		} else if (saved.equals(ICoreConstants.VALUE_SAVED_ALL)) {
			initializeDefault(true);
		} else {
			Vector savedList = createSavedList(saved);

			IPluginModel[] models = getModels();
			for (int i = 0; i < models.length; i++) {
				IPluginModel model = models[i];
				String id = model.getPlugin().getId();
				model.setEnabled(isChecked(id, savedList));
			}
		}
	}
	private void initializeDefault(boolean enabled) {
		IPluginModel[] models = getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
			model.setEnabled(enabled);
		}
	}
	private boolean isChecked(String name, Vector list) {
		for (int i = 0; i < list.size(); i++) {
			if (name.equals(list.elementAt(i)))
				return false;
		}
		return true;
	}
	private Vector createSavedList(String saved) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(saved);
		while (stok.hasMoreTokens()) {
			result.add(stok.nextToken());
		}
		return result;
	}
}