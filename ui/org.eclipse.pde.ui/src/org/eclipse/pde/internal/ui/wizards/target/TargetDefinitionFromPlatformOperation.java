package org.eclipse.pde.internal.ui.wizards.target;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.IEnvironmentVariables;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetJRE;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;

public class TargetDefinitionFromPlatformOperation extends BaseTargetDefinitionOperation {

	public TargetDefinitionFromPlatformOperation(IFile file) {
		super(file);
	}
	
	protected void initializeTarget(ITargetModel model) {
		ITarget target = model.getTarget();
		ITargetModelFactory factory = model.getFactory();
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		
		initializeArgumentsInfo(preferences, target, factory);
		initializeEnvironmentInfo(preferences, target, factory);
		initializeImplicitInfo(preferences, target, factory);
		initializeLocationInfo(preferences, target, factory);
		initializeAdditionalLocsInfo(preferences, target, factory);
		initializeJREInfo(target, factory);
		initializePluginContent(preferences, target, factory);
		
	}
		
	protected void initializeArgumentsInfo(Preferences preferences, ITarget target, ITargetModelFactory factory) {
		String progArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		String vmArgs = preferences.getString(ICoreConstants.VM_ARGS);
		if (progArgs.length() + vmArgs.length() > 0) {
			IArgumentsInfo info = factory.createArguments();
			info.setProgramArguments(progArgs);
			info.setVMArguments(vmArgs);
			target.setArguments(info);
		}
	}
	
	protected void initializeEnvironmentInfo(Preferences preferences, ITarget target, ITargetModelFactory factory) {
		IEnvironmentInfo info = factory.createEnvironment();
		info.setOS(preferences.getString(IEnvironmentVariables.OS));
		info.setWS(preferences.getString(IEnvironmentVariables.WS));
		info.setNL(preferences.getString(IEnvironmentVariables.NL));
		info.setArch(preferences.getString(IEnvironmentVariables.ARCH));
		target.setEnvironment(info);
	}
	
	protected void initializeImplicitInfo(Preferences preferences, ITarget target, ITargetModelFactory factory) {
		String value = preferences.getString(ICoreConstants.IMPLICIT_DEPENDENCIES);
		if (value.length() > 0) {
			StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
			ITargetPlugin[] plugins = new ITargetPlugin[tokenizer.countTokens()];
			int i = 0;
			while(tokenizer.hasMoreTokens()) {
				String id = tokenizer.nextToken();
				ITargetPlugin plugin = factory.createPlugin();
				plugin.setId(id);
				plugins[i++] = plugin;
			}
			IImplicitDependenciesInfo info = factory.createImplicitPluginInfo();
			info.addPlugins(plugins);
			target.setImplicitPluginsInfo(info);
		}
	}
	
	protected void initializeLocationInfo(Preferences preferences, ITarget target, ITargetModelFactory factory) {
		ILocationInfo info = factory.createLocation();
		boolean useThis = preferences.getString(ICoreConstants.TARGET_MODE).equals(ICoreConstants.VALUE_USE_THIS);
		info.setDefault(useThis);
		if (!useThis)
			info.setPath(preferences.getString(ICoreConstants.PLATFORM_PATH));
		target.setLocationInfo(info);
	}
	
	protected void initializeAdditionalLocsInfo(Preferences preferences, ITarget target, ITargetModelFactory factory) {
		String additional = preferences.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		StringTokenizer tokenizer = new StringTokenizer(additional, ","); //$NON-NLS-1$
		int size = tokenizer.countTokens();
		if (size > 0) {
			IAdditionalLocation[] locations = new IAdditionalLocation[size];
			int i = 0;
			while (tokenizer.hasMoreTokens()) {
				IAdditionalLocation location = factory.createAdditionalLocation();
				location.setPath(tokenizer.nextToken().trim());
				locations[i++] = location;
			}
			target.addAdditionalDirectories(locations);
		}
	}
	
	protected void initializeJREInfo(ITarget target, ITargetModelFactory factory) {
		ITargetJRE info = factory.createJREInfo();
		info.setDefaultJRE();
		target.setTargetJREInfo(info);
	}
	
	protected void initializePluginContent(Preferences preferences, ITarget target, ITargetModelFactory factory) {
		String value = preferences.getString(ICoreConstants.CHECKED_PLUGINS);
		if (value.length() == 0 || value.equals(ICoreConstants.VALUE_SAVED_NONE))
			return;
		if (value.equals(ICoreConstants.VALUE_SAVED_ALL)) {
			target.setUseAllPlugins(true);
		} else {
			IPluginModelBase [] models = PDECore.getDefault().getModelManager().getExternalModels();
			ArrayList list = new ArrayList(models.length);
			for (int i = 0; i < models.length; i++) {
				if (models[i].isEnabled()) {
					ITargetPlugin plugin = factory.createPlugin();
					String id = models[i].getPluginBase().getId();
					if (id != null)
						plugin.setId(id);
					list.add(plugin);
				}
			}
			if (list.size() > 0)
				target.addPlugins((ITargetPlugin[]) list.toArray(new ITargetPlugin[list.size()]));
		}
			
	}

}
