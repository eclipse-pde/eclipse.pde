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
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.pde.internal.build.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.build.Policy;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.ant.AntScript;

public class ExportFeatureBuildScriptGenerator extends FeatureBuildScriptGenerator {
	protected void generateZipDistributionWholeTarget(AntScript script) {
		int tab = 1;
		script.println();
		script.printTargetDeclaration(tab++, TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, Policy.bind("build.feature.zips",featureID)); //$NON-NLS-1$
		script.printMkdirTask(tab, FEATURE_TEMP_FOLDER);
		Map params = new HashMap(1);
		params.put(PROPERTY_FEATURE_BASE, FEATURE_TEMP_FOLDER);
		params.put(PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
		script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
		script.printTargetEnd(--tab);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.FeatureBuildScriptGenerator#generateZipSourcesTarget(org.eclipse.pde.internal.build.ant.AntScript)
	 */
	protected void generateZipSourcesTarget(AntScript script) {
			int tab = 1;
			script.println();
			script.printTargetDeclaration(tab++, TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
			Map params = new HashMap(1);
			params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
			params.put(PROPERTY_DESTINATION_TEMP_FOLDER, FEATURE_TEMP_FOLDER + "/" + "plugins"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
			script.printTargetEnd(--tab);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.FeatureBuildScriptGenerator#generateAllPluginsTarget(org.eclipse.pde.internal.build.ant.AntScript)
	 */
	protected void generateAllPluginsTarget(AntScript script) throws CoreException {
		int tab = 1;
		List plugins = computeElements(false);
		List fragments = computeElements(true);
	
		String[] sortedPlugins = Utils.computePrerequisiteOrder((PluginModel[]) plugins.toArray(new PluginModel[plugins.size()]), (PluginModel[]) fragments.toArray(new PluginModel[fragments.size()]));
		script.println();
		script.printTargetDeclaration(tab++, TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
			for (int i = 0; i < sortedPlugins.length; i++) {
				PluginModel plugin = getRegistry().getPlugin(sortedPlugins[i]);
				if (plugin==null)
					plugin = getRegistry().getFragment(sortedPlugins[i]);
				
				IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(getFeatureRootLocation()));
				Map params = new HashMap(1);
				params.put(PROPERTY_BUILD_RESULT_FOLDER, FEATURE_TEMP_FOLDER + "/build_result/" + plugin.getPluginId());
				script.printAntTask(tab, buildScriptName, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, params);
			}
		script.printTargetEnd(--tab);
	}

}

