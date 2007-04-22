/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetJRE;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;

public class LoadTargetOperation implements IWorkspaceRunnable {

	private ITarget fTarget;
	private Map fRequiredPlugins = new HashMap();
	private List fMissingFeatures = new ArrayList();
	private IPath fPath = null;
	
	public LoadTargetOperation(ITarget target) {
		this(target, (IPath)null);
	}
	
	public LoadTargetOperation(ITarget target, IPath workspaceLoc) {
		fTarget = target;
		fPath = workspaceLoc;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		try {
			Preferences preferences = PDECore.getDefault().getPluginPreferences();
			monitor.beginTask(PDECoreMessages.LoadTargetOperation_mainTaskName, 100);
			loadEnvironmentInfo(preferences, new SubProgressMonitor(monitor, 5));
			loadProgramArgs(preferences, new SubProgressMonitor(monitor,5));
			loadJREInfo(preferences, new SubProgressMonitor(monitor, 15));
			loadImplicitPlugins(preferences, new SubProgressMonitor(monitor, 15));
			loadPlugins(preferences, new SubProgressMonitor(monitor, 60));
			loadAdditionalPreferences(preferences);
			PDECore.getDefault().savePluginPreferences();
		} finally {
			monitor.done();
		}
	}
		
	public Object[] getMissingPlugins() {
		return fRequiredPlugins.values().toArray();
	}
	
	public Object[] getMissingFeatures() {
		return fMissingFeatures.toArray();
	}
	
	protected void loadProgramArgs(Preferences pref, IProgressMonitor monitor) {
		IArgumentsInfo args = fTarget.getArguments();
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_argsTaskName, 2);
		pref.setValue(ICoreConstants.PROGRAM_ARGS, (args != null) ? args.getProgramArguments() : ""); //$NON-NLS-1$
		monitor.worked(1);
		pref.setValue(ICoreConstants.VM_ARGS, (args != null) ? args.getVMArguments() : ""); //$NON-NLS-1$
		monitor.done();
	}
	
	protected void loadEnvironmentInfo(Preferences pref, IProgressMonitor monitor) {
		IEnvironmentInfo env = fTarget.getEnvironment();
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_envTaskName, 1);
		if (env == null) {
			pref.setToDefault(ICoreConstants.ARCH);
			pref.setToDefault(ICoreConstants.NL);
			pref.setToDefault(ICoreConstants.OS);
			pref.setToDefault(ICoreConstants.WS);
		} else {
			pref.setValue(ICoreConstants.ARCH, env.getDisplayArch());
			pref.setValue(ICoreConstants.NL, env.getDisplayNL());
			pref.setValue(ICoreConstants.OS, env.getDisplayOS());
			pref.setValue(ICoreConstants.WS, env.getDisplayWS());
		}
		monitor.done();
	}
	
	protected void loadJREInfo(Preferences pref, IProgressMonitor monitor) {
		ITargetJRE jreInfo = fTarget.getTargetJREInfo();
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_jreTaskName, 1);
		if (jreInfo != null) {
			String jre = jreInfo.getCompatibleJRE();
			IVMInstall install = JavaRuntime.getDefaultVMInstall();
			if (install != null && !jre.equals(install.getName()))
				try {
					JavaRuntime.setDefaultVMInstall(getVMInstall(jre), null);
				} catch (CoreException e) {
				}
		}
		monitor.done();
	}
	
	private IVMInstall getVMInstall(String name) {
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < types.length; i++) {
			IVMInstall[] installs = types[i].getVMInstalls();
			for (int k = 0; k < installs.length; k++) {
				if (installs[i].getName().equals(name))
					return installs[i];
			}
		}
		return JavaRuntime.getDefaultVMInstall();
	}
	
	protected void loadImplicitPlugins(Preferences pref, IProgressMonitor monitor) {
		IImplicitDependenciesInfo info = fTarget.getImplicitPluginsInfo();
		if (info != null) {
			ITargetPlugin[] plugins = info.getPlugins();
			monitor.beginTask(PDECoreMessages.LoadTargetOperation_implicitPluginsTaskName, plugins.length + 1);
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < plugins.length; i++) {
				buffer.append(plugins[i].getId()).append(',');
				monitor.worked(1);
			}
			if (plugins.length > 0)
				buffer.setLength(buffer.length() - 1);
			pref.setValue(ICoreConstants.IMPLICIT_DEPENDENCIES, buffer.toString());
		}
		monitor.done();
	}
	
	protected void loadPlugins(Preferences pref, IProgressMonitor monitor) {
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_loadPluginsTaskName, 100);
		ILocationInfo info = fTarget.getLocationInfo();
		String currentPath = pref.getString(ICoreConstants.PLATFORM_PATH);
		String path;
		if (info == null || info.useDefault()) { 
			path = TargetPlatform.getDefaultLocation();
		} else {
			try {
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				path = manager.performStringSubstitution(info.getPath());
			} catch (CoreException e) {
				return;
			}			
		}
		monitor.worked(10);
		if (!new Path(path).equals(new Path(currentPath)) || !areAdditionalLocationsEqual(pref)) {
			// reload required
			List additional = getAdditionalLocs();
			handleReload(path, additional, pref, new SubProgressMonitor(monitor, 85));
			
			// update preferences (Note: some preferences updated in handleReload())
			pref.setValue(ICoreConstants.PLATFORM_PATH, path);
			String mode =
				new Path(path).equals(new Path(TargetPlatform.getDefaultLocation()))
				? ICoreConstants.VALUE_USE_THIS
						: ICoreConstants.VALUE_USE_OTHER;
			pref.setValue(ICoreConstants.TARGET_MODE, mode);
			
			ListIterator li = additional.listIterator();
			StringBuffer buffer = new StringBuffer();
			while (li.hasNext()) 
				buffer.append(li.next()).append(","); //$NON-NLS-1$
			if (buffer.length() > 0) 
				buffer.setLength(buffer.length() - 1);
			pref.setValue(ICoreConstants.ADDITIONAL_LOCATIONS, buffer.toString());
			
			String newValue = currentPath;
			for (int i = 0; i < 4; i++) {
				String value = pref.getString(ICoreConstants.SAVED_PLATFORM + i);
				pref.setValue(ICoreConstants.SAVED_PLATFORM + i, newValue);
				if (!value.equals(currentPath)) 
					newValue = value;
				else
					break;
			}	
		} else {
			PDECore core = PDECore.getDefault();
			IPluginModelBase[] changed = handlePluginSelection(TargetPlatformHelper.getPDEState(), core.getFeatureModelManager(),
					pref, new SubProgressMonitor(monitor,85));
			if (changed.length > 0) {
				ExternalModelManager pluginManager = core.getModelManager().getExternalModelManager();
				pluginManager.fireModelProviderEvent(
						new ModelProviderEvent(
							pluginManager,
							IModelProviderEvent.MODELS_CHANGED,
							null,
							null,
							changed));
			}
		}
		monitor.done();
	}
	
	protected void loadAdditionalPreferences(Preferences pref) {
		if (fPath == null)
			return;
		String newValue = "${workspace_loc:".concat(fPath.toOSString()).concat("}"); //$NON-NLS-1$ //$NON-NLS-2$
		pref.setValue(ICoreConstants.TARGET_PROFILE, newValue);
	}
	
	private boolean areAdditionalLocationsEqual(Preferences pref) {
		IAdditionalLocation[] addtionalLocs = fTarget.getAdditionalDirectories();
		String value = pref.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		StringTokenizer tokenzier = new StringTokenizer(value);
		if (addtionalLocs.length != tokenzier.countTokens()) 
			return false;
		while (tokenzier.hasMoreTokens()) {
			boolean found = false;
			String location = tokenzier.nextToken();
			for (int i = 0; i < addtionalLocs.length; i++) {
				if (addtionalLocs[i].getPath().equals(location)) {
					found = true;
					break;
				}
			}
			if (!found) 
				return false;
		}
		return true;
	}
	
	private List getAdditionalLocs() {
		ArrayList additional = new ArrayList();
		IAdditionalLocation[] locations = fTarget.getAdditionalDirectories();
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		for (int i = 0; i < locations.length; i++) {
			try {
				additional.add(manager.performStringSubstitution(locations[i].getPath()));
			} catch (CoreException e) {
				additional.add(locations[i]);
			}			
		}
		return additional;
	}
	
	private void handleReload(String targetLocation, List additionalLocations, Preferences pref, IProgressMonitor monitor) {
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_reloadTaskName, 85);
		URL[] paths = getURLs(targetLocation, additionalLocations);
		PDEState state = new PDEState(paths, true, new SubProgressMonitor(monitor, 45));
		
		ExternalFeatureModelManager featureManager = getFeatureManager(targetLocation, additionalLocations);
		IFeatureModel[] models = featureManager.getModels();
		Map features = new HashMap();
		for (int i = 0; i < models.length; i++) 
			features.put(models[i].getFeature().getId(), models[i]);
		monitor.worked(5);
		models = PDECore.getDefault().getFeatureModelManager().getWorkspaceModels();
		for (int i = 0; i < models.length; i++) 
			features.put(models[i].getFeature().getId(), models[i]);
		monitor.worked(5);
		
		handlePluginSelection(state, features, pref, new SubProgressMonitor(monitor,25));
		
		Job job = new TargetPlatformResetJob("Reset Target Platform", state);
		job.schedule();		
		monitor.done();
	}
	
	private URL[] getURLs(String targetLocation, List additionalLocations) {
		int length = additionalLocations.size();
		File[] locations = new File[2 * length + 2];
		ListIterator li = additionalLocations.listIterator();
		while (li.hasNext())  {
			File dir = new File((String)li.next());
			locations[2 *length] = dir;
			locations[2 * length + 1] = new File(dir, "plugins"); //$NON-NLS-1$
			--length;
		}
		File targetDir = new File(targetLocation);
		locations[0] = new File(targetLocation);
		locations[1] = new File(targetDir, "plugins"); //$NON-NLS-1$
		return PluginPathFinder.scanLocations(locations);
	}
	
	private ExternalFeatureModelManager getFeatureManager(String targetLocation, List additionalLocations) {
		StringBuffer buffer = new StringBuffer();
		ListIterator li = additionalLocations.listIterator();
		while (li.hasNext()) 
			buffer.append(li.next()).append(',');
		if (buffer.length() > 0)
			buffer.setLength(buffer.length() - 1);
		ExternalFeatureModelManager featureManager = new ExternalFeatureModelManager();
		featureManager.loadModels(targetLocation, buffer.toString());
		return featureManager;
	}
	
	protected IPluginModelBase[] handlePluginSelection(PDEState state, Map featureMap, Preferences pref, IProgressMonitor monitor) {
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_selectPluginsTaskName, 80);
		Set optionalPlugins = new HashSet();
		getPluginIds(featureMap, null, optionalPlugins, new SubProgressMonitor(monitor, 40));
		return handlePluginSelection(state, optionalPlugins, pref, new SubProgressMonitor(monitor, 40));
	}
	
	protected IPluginModelBase[] handlePluginSelection(PDEState state, FeatureModelManager manager, Preferences pref, IProgressMonitor monitor) {
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_selectPluginsTaskName, 80);
		Set optionalPlugins = new HashSet();
		getPluginIds(null, manager, optionalPlugins, new SubProgressMonitor(monitor, 40));
		return handlePluginSelection(state, optionalPlugins, pref, new SubProgressMonitor(monitor, 40));
	}

	// returns changed Models
	private IPluginModelBase[] handlePluginSelection(PDEState state, Set optionalPlugins, Preferences pref, IProgressMonitor monitor) {
		List changed = new ArrayList();
		boolean useAll = fTarget.useAllPlugins();
		
		IPluginModelBase[] models = state.getTargetModels();
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_enablePluginsTaskName, models.length);
		boolean anyPluginsEnabled = false;
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getBundleDescription().getSymbolicName();
			if (models[i].isEnabled() != (useAll || optionalPlugins.contains(id) || fRequiredPlugins.containsKey(id))) {
				changed.add(models[i]);
				models[i].setEnabled(!models[i].isEnabled());
			}
			fRequiredPlugins.remove(id);
			if (!anyPluginsEnabled)
				anyPluginsEnabled |= models[i].isEnabled();
			monitor.worked(1);
		}
		if (useAll)
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_ALL);
		else if (!anyPluginsEnabled)
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_NONE);
		monitor.done();
		return (IPluginModelBase[])changed.toArray(new IPluginModelBase[changed.size()]);
	}
	
	private void getPluginIds(Map featureMap, FeatureModelManager manager, Set optionalPlugins, IProgressMonitor monitor) {
		ITargetFeature[] targetFeatures = fTarget.getFeatures();
		ITargetPlugin[] plugins = fTarget.getPlugins();
		
		monitor.beginTask(PDECoreMessages.LoadTargetOperation_findPluginsTaskName, targetFeatures.length + plugins.length);
		if (fTarget.useAllPlugins()) {
			monitor.done();
			return;
		}
		boolean useMap = featureMap != null;
		Stack features = new Stack();
		
		for (int i = 0 ; i < targetFeatures.length; i++) {
			IFeatureModel model = (useMap)? (IFeatureModel)featureMap.get(targetFeatures[i].getId()):
				manager.findFeatureModel(targetFeatures[i].getId());
			if (model != null)
				features.push(model);
			else if (!targetFeatures[i].isOptional()) {
				fMissingFeatures.add(targetFeatures[i]);
				break;
			}
			while (!features.isEmpty()) {
				IFeature feature = ((IFeatureModel) features.pop()).getFeature();
				IFeaturePlugin [] featurePlugins = feature.getPlugins();
				for (int j = 0; j < featurePlugins.length; j++) {
					if (targetFeatures[i].isOptional() || featurePlugins[j].isFragment())
						optionalPlugins.add(featurePlugins[j].getId());
					else 
						fRequiredPlugins.put(featurePlugins[j].getId(), featurePlugins[j]);
				}
				IFeatureChild[] children = feature.getIncludedFeatures();
				for (int j = 0; j < children.length; j++) {
					model = (useMap)? (IFeatureModel)featureMap.get(children[j].getId()):
						manager.findFeatureModel(children[j].getId());
					if (model != null)
						features.push(model);
				}
			}
			monitor.worked(1);
		}
		
		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i].isOptional())
				optionalPlugins.add(plugins[i].getId());
			else 
				fRequiredPlugins.put(plugins[i].getId(), plugins[i]);
			monitor.worked(1);
		}
		
		monitor.done();
	}
}
