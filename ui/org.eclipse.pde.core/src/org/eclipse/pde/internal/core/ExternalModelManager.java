package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
	private static ArrayList eclipseLinks = new ArrayList();
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
		
		String correctVariable = PDECore.ECLIPSE_HOME_VARIABLE;
		int maxMatching = fullPath.matchingFirstSegments(getEclipseHome(monitor));
		
		for (int i = 0; i < eclipseLinks.size(); i++) {
			IPath currentPath = JavaCore.getClasspathVariable(eclipseLinks.get(i).toString());
			if (currentPath != null) {
				int currentMatch = fullPath.matchingFirstSegments(currentPath);
				if (currentMatch > maxMatching) {
					maxMatching = currentMatch;
					correctVariable = eclipseLinks.get(i).toString();
				}
			}
		}
		return new Path(correctVariable).append(fullPath.removeFirstSegments(maxMatching));
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

	public IFragmentModel [] getFragmentsFor(IPluginModel model) {
		IFragmentModel [] candidates = getFragmentModels(null);
		IPlugin plugin = model.getPlugin();
		ArrayList result = new ArrayList();
		for (int i=0; i<candidates.length; i++) {
			IFragment fragment = candidates[i].getFragment();
			if (fragment.getPluginId().equals(plugin.getId()))
				result.add(candidates[i]);
		}
		return (IFragmentModel [])result.toArray(new IFragmentModel[result.size()]);
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
			Preferences preferences = PDECore.getDefault().getPluginPreferences();
			platformHome = preferences.getString(ICoreConstants.PLATFORM_PATH);
		}
		if (platformHome == null || platformHome.length() == 0) {
			//			Display.getCurrent().beep();
			//			MessageDialog.openError(
			//				PDECore.getActiveWorkbenchShell(),
			//				PDECore.getResourceString(KEY_ERROR_TITLE),
			//				PDECore.getResourceString(KEY_ERROR_NO_HOME));
			return new String[0];
		}
		String[] links = getLinks(platformHome);
		
		String [] paths = new String[links.length + 1];
		paths[0] = platformHome + File.separator + "plugins";
		if (links.length > 0) {
			System.arraycopy(links,0,paths,1,links.length);
		}
		
		return paths;
	}
	
	private String[] getLinks(String platformHome) {
		ArrayList result = new ArrayList();
		String prefix = new Path(platformHome).removeLastSegments(1).toString();
		File file = new File(platformHome + Path.SEPARATOR + "links");

		File[] linkFiles = new File[0];
		if (file.exists() && file.isDirectory()) {
			linkFiles = file.listFiles();
		}
		if (linkFiles != null) {
			for (int i = 0; i < linkFiles.length; i++) {
				Properties properties = new Properties();
				try {
					FileInputStream fis = new FileInputStream(linkFiles[i]);
					properties.load(fis);
					fis.close();
					String path = properties.getProperty("path");
					if (path != null) {
						if (!new Path(path).isAbsolute())
							path = prefix + Path.SEPARATOR + path;
						path += Path.SEPARATOR
							+ "eclipse"
							+ Path.SEPARATOR
							+ "plugins";
						if (new File(path).exists()) {
							String variable = PDECore.ECLIPSE_HOME_VARIABLE + "_" + linkFiles[i].getName().toUpperCase().replace('.','_');
							eclipseLinks.add(variable);
							JavaCore.setClasspathVariable(variable,new Path(path).removeLastSegments(1),null);
							result.add(path);
						}
					}
				} catch (IOException e) {
				} catch (JavaModelException e) {
				}
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public boolean hasEnabledModels() {
		if (models == null) {
			Preferences preferences = PDECore.getDefault().getPluginPreferences();
			String saved = preferences.getString(ICoreConstants.CHECKED_PLUGINS);
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
			URL[] urls = new URL[pluginPaths.length];
			for (int i = 0; i < pluginPaths.length; i++) {
				urls[i] = new URL("file:" + pluginPaths[i].replace('\\', '/') + "/");
			}
			TargetPlatformRegistryLoader loader = new TargetPlatformRegistryLoader();
			MultiStatus errors = loader.load(urls, resolve);
			PluginRegistryModel registryModel = loader.getRegistry();

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
		for (int i = 0; i < eclipseLinks.size(); i++) {
			JavaCore.removeClasspathVariable(eclipseLinks.get(i).toString(),null);
		}
		eclipseLinks.clear();
		
		
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
		for (int i = 0; i < eclipseLinks.size(); i++) {
			JavaCore.removeClasspathVariable(eclipseLinks.get(i).toString(),null);
		}
		eclipseLinks.clear();

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
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String platformHome = preferences.getString(ICoreConstants.PLATFORM_PATH);
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
			Preferences preferences = PDECore.getDefault().getPluginPreferences();
			preferences.setValue(ICoreConstants.PLATFORM_PATH, newValue);
			PDECore.getDefault().savePluginPreferences();
		} catch (JavaModelException e) {
			PDECore.logException(e);
		} finally {
			monitor.done();
		}
	}
	public static void initializePlatformPath() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		boolean useThis = true;
		String mode = preferences.getString(ICoreConstants.TARGET_MODE);

		if (mode != null && mode.equals(ICoreConstants.VALUE_USE_OTHER))
			useThis = false;
		String path = preferences.getString(ICoreConstants.PLATFORM_PATH);
		String currentPath = computeDefaultPlatformPath();

		if (path == null
			|| path.length() == 0
			|| (useThis && !currentPath.equals(path))) {
			path = currentPath;
			preferences.setDefault(ICoreConstants.PLATFORM_PATH, path);
			preferences.setValue(ICoreConstants.PLATFORM_PATH, path);
			PDECore.getDefault().savePluginPreferences();
		}
	}

	public static boolean getUseOther() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		boolean useOther = false;
		String mode = preferences.getString(ICoreConstants.TARGET_MODE);
		if (mode != null && mode.equals(ICoreConstants.VALUE_USE_OTHER))
			useOther = true;
		return useOther;
	}
	public static String computeDefaultPlatformPath() {
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
					buf.append(" ");
					continue;
				}
			}
			buf.append(c);
		}
		return buf.toString();
	}
	public void initializeAndStore(boolean selectAll) {
		String toSave = selectAll ? ICoreConstants.VALUE_SAVED_ALL : ICoreConstants.VALUE_SAVED_NONE;
		PDECore.getDefault().getPluginPreferences().setValue(ICoreConstants.CHECKED_PLUGINS, toSave);
		initialize();
	}
	public void initialize() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String saved = preferences.getString(ICoreConstants.CHECKED_PLUGINS);

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
			IFragmentModel [] fmodels = getFragmentModels(null);
			for (int i=0; i<fmodels.length; i++) {
				IFragmentModel model = fmodels[i];
				String id = model.getFragment().getId();
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
		IFragmentModel[] fmodels = getFragmentModels(null);
		for (int i = 0; i < fmodels.length; i++) {
			IFragmentModel fmodel = fmodels[i];
			fmodel.setEnabled(enabled);
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