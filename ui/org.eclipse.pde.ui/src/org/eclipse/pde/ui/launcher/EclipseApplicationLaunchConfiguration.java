/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.internal.launching.launcher.*;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.Version;

/**
 * A launch delegate for launching Eclipse applications
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 *
 * @since 3.2
 * @deprecated use
 *             {@link org.eclipse.pde.launching.EclipseApplicationLaunchConfiguration}
 * @noreference This method is planned for removal. See bug 564563 for details.
 * @noextend This method is planned for removal. See bug 564563 for details.
 * @see org.eclipse.pde.launching.AbstractPDELaunchConfiguration
 */
@Deprecated
public class EclipseApplicationLaunchConfiguration extends AbstractPDELaunchConfiguration {

	static {
		RequirementHelper.registerSameRequirementsAsFor("org.eclipse.pde.ui.RuntimeWorkbench", //$NON-NLS-1$
				IPDELauncherConstants.ECLIPSE_APPLICATION_LAUNCH_CONFIGURATION_TYPE);
	}

	// used to generate the dev classpath entries
	// key is bundle ID, value is a model
	private Map<String, IPluginModelBase> fAllBundles;

	// key is a model, value is startLevel:autoStart
	private Map<IPluginModelBase, String> fModels;

	/**
	 * To avoid duplicating variable substitution (and duplicate prompts)
	 * this variable will store the substituted workspace location.
	 */
	private String fWorkspaceLocation;

	@Override
	public String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		throw new CoreException(Status.error(PDEMessages.PDE_updateManagerNotSupported));
	}

	@Override
	protected File getConfigDir(ILaunchConfiguration config) {
		if (fConfigDir == null) {
			fConfigDir = LaunchConfigurationHelper.getConfigurationArea(config);
		}
		return fConfigDir;
	}

	/**
	 * Clears the workspace prior to launching if the workspace exists and the option to
	 * clear it is turned on.  Also clears the configuration area if that option is chosen.
	 *
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			the progress monitor
	 * @throws CoreException
	 * 			if unable to retrieve launch attribute values or the clear operation was cancelled
	 * @since 3.3
	 */
	@Override
	protected void clear(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		if (fWorkspaceLocation == null) {
			fWorkspaceLocation = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		}

		SubMonitor subMon = SubMonitor.convert(monitor, 50);

		// Clear workspace and prompt, if necessary
		LauncherUtils.clearWorkspace(configuration, fWorkspaceLocation, subMon.split(25));

		subMon.setWorkRemaining(25);
		if (subMon.isCanceled()) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		// clear config area, if necessary
		if (configuration.getAttribute(org.eclipse.pde.launching.IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
			CoreUtility.deleteContent(getConfigDir(configuration), subMon.split(25));

		subMon.done();
	}

	@Override
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		fWorkspaceLocation = null;

		fModels = BundleLauncherHelper.getMergedBundleMap(configuration, false);
		fAllBundles = new HashMap<>(fModels.size());
		Iterator<?> iter = fModels.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			fAllBundles.put(model.getPluginBase().getId(), model);
		}
		validateConfigIni(configuration);
		super.preLaunchCheck(configuration, launch, monitor);
	}

	private void validateConfigIni(ILaunchConfiguration configuration) throws CoreException {
		if (!configuration.getAttribute(org.eclipse.pde.launching.IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true)) {
			String templateLoc = configuration.getAttribute(org.eclipse.pde.launching.IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, ""); //$NON-NLS-1$
			IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
			templateLoc = mgr.performStringSubstitution(templateLoc);

			File templateFile = new File(templateLoc);
			if (!templateFile.exists()) {
				if (!LauncherUtils.generateConfigIni())
					throw new CoreException(Status.CANCEL_STATUS);
				// with the way the launcher works, if a config.ini file is not found one will be generated automatically.
				// This check was to warn the user a config.ini needs to be generated. - bug 161265, comment #7
			}
		}
	}

	@Override
	public String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		String[] vmArgs = super.getVMArguments(configuration);
		IPluginModelBase base = fAllBundles.get(PDECore.PLUGIN_ID);
		if (base != null && VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) >= 0) { //$NON-NLS-1$
			// necessary for PDE to know how to load plugins when target platform = host platform
			// see PluginPathFinder.getPluginPaths() and PluginPathFinder.isDevLaunchMode()
			String[] result = new String[vmArgs.length + 1];
			System.arraycopy(vmArgs, 0, result, 0, vmArgs.length);
			result[vmArgs.length] = "-Declipse.pde.launch=true"; //$NON-NLS-1$
			return result;
		}
		return vmArgs;
	}

}
