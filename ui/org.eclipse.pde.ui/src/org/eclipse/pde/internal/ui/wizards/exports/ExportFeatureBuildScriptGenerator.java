/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.pde.internal.build.Config;
import org.eclipse.pde.internal.build.Policy;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.update.core.IPluginEntry;

public class ExportFeatureBuildScriptGenerator extends FeatureBuildScriptGenerator {
	protected void generateZipDistributionWholeTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, Policy.bind("build.feature.zips", featureIdentifier)); //$NON-NLS-1$
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_FEATURE_BASE, featureTempFolder);
		params.put(PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
		params.put(PROPERTY_OS, feature.getOS() == null ? Config.ANY : feature.getOS());
		params.put(PROPERTY_WS, feature.getWS() == null ? Config.ANY : feature.getWS());
		params.put(PROPERTY_ARCH, feature.getOSArch() == null ? Config.ANY : feature.getOSArch());
		params.put(PROPERTY_NL, feature.getNL() == null ? Config.ANY : feature.getNL());

		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, null, params);
		script.printTargetEnd();
	}
	
	protected void generateZipSourcesTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, featureTempFolder + "/plugins"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printTargetEnd();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator#generateAllPluginsTarget()
	 */
	protected void generateAllPluginsTarget() throws CoreException {
		List plugins = computeElements(false);
		List fragments = computeElements(true);

		String[] sortedPlugins = Utils.computePrerequisiteOrder((PluginModel[]) plugins.toArray(new PluginModel[plugins.size()]), (PluginModel[]) fragments.toArray(new PluginModel[fragments.size()]));
		script.println();
		script.printTargetDeclaration(TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
		Set writtenCalls = new HashSet(plugins.size() + fragments.size());

		for (int i = 0; i < sortedPlugins.length; i++) {
			PluginModel plugin = getSite(false).getPluginRegistry().getPlugin(sortedPlugins[i]);
			// the id is a fragment
			if (plugin == null)
				plugin = getSite(false).getPluginRegistry().getFragment(sortedPlugins[i]);

			// Get the os / ws / arch to pass as a parameter to the plugin
			if (writtenCalls.contains(sortedPlugins[i]))
				continue;

			writtenCalls.add(sortedPlugins[i]);
			IPluginEntry[] entries = Utils.getPluginEntry(feature, sortedPlugins[i]);
			for (int j = 0; j < entries.length; j++) {
				List list = selectConfigs(entries[j]);
				if (list.size() == 0)
					continue;

				Map params = null;
				Config aMatchingConfig = (Config) list.get(0);
				params = new HashMap(3);

				if (!aMatchingConfig.getOs().equals(Config.ANY))
					params.put(PROPERTY_OS, aMatchingConfig.getOs());
				if (!aMatchingConfig.getWs().equals(Config.ANY))
					params.put(PROPERTY_WS, aMatchingConfig.getWs());
				if (!aMatchingConfig.getArch().equals(Config.ANY))
					params.put(PROPERTY_ARCH, aMatchingConfig.getArch());
				params.put(PROPERTY_BUILD_RESULT_FOLDER, PDEPlugin.getDefault().getStateLocation() + "/temp/build_result/" + plugin.getId());
				params.put(PROPERTY_TEMP_FOLDER, PDEPlugin.getDefault().getStateLocation() + "/temp/temp.folder/" + plugin.getId());

				IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(featureRootLocation));
				script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, params);
			}
		}
		script.printTargetEnd();
	}

}

