package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;

import java.net.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import org.eclipse.swt.widgets.*;

import java.util.*;
import java.io.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.preferences.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.model.plugin.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import java.lang.reflect.InvocationTargetException;

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

	public static IPath getEclipseHome(IProgressMonitor monitor) {
		IPath eclipseHome =
			JavaCore.getClasspathVariable(PDEPlugin.ECLIPSE_HOME_VARIABLE);
		if (eclipseHome == null) {
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			String newValue =
				store.getString(TargetPlatformPreferencePage.PROP_PLATFORM_PATH);
			if (newValue == null || newValue.length() == 0)
				return null;
			setEclipseHome(newValue, monitor);
		}
		return eclipseHome;
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
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			platformHome = store.getString(TargetPlatformPreferencePage.PROP_PLATFORM_PATH);
		}
		if (platformHome == null || platformHome.length() == 0) {
			Display.getCurrent().beep();
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString(KEY_ERROR_TITLE),
				PDEPlugin.getResourceString(KEY_ERROR_NO_HOME));
			return new String[0];
		}
		String[] paths = new String[2];
		paths[0] = platformHome + File.separator + "plugins";
		paths[1] = platformHome + File.separator + "fragments";
		return paths;
	}

	public boolean hasEnabledModels() {
		if (models == null) {
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
		//long startTime = System.currentTimeMillis();
		boolean result = reload(null, monitor);
		if (result) {
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			ExternalPluginsBlock.initialize(this, store);
		}
		//long stopTime = System.currentTimeMillis();
		Object[] array = models.toArray();
		ArraySorter.INSTANCE.sortInPlace(array);

		for (int i = 0; i < array.length; i++) {
			models.set(i, array[i]);
		}
		//long sortTime = System.currentTimeMillis();
		/*
		System.out.println("Load time: "+(stopTime - startTime));
		System.out.println("Sort time: "+(sortTime - stopTime));
		System.out.println("Total: "+(sortTime - startTime));
		*/
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
					PDEPlugin.getPluginId(),
					1,
					PDEPlugin.getResourceString(KEY_SCANNING_PROBLEMS),
					null);
			URL[] urls = new URL[pluginPaths.length];
			for (int i = 0; i < pluginPaths.length; i++) {
				urls[i] = new URL("file:" + pluginPaths[i].replace('\\', '/') + "/");
			}

			//String pattern = PDEPlugin.getResourceString(KEY_PROCESSING_PATH);
			//String message = PDEPlugin.getFormattedMessage(pattern, pluginPath);
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
			for (int i=0; i<fragmentModels.length; i++) {
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

	public void removeModelProviderListener(IModelProviderListener listener) {
		listeners.remove(listener);
	}

	public static void setEclipseHome(
		final String newValue,
		IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			JavaCore.setClasspathVariable(
				PDEPlugin.ECLIPSE_HOME_VARIABLE,
				new Path(newValue),
				monitor);
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		} finally {
			monitor.done();
		}
	}
}