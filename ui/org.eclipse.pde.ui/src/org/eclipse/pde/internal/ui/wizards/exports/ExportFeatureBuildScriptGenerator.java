/*
 * Created on Feb 28, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.pde.internal.build.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.build.Policy;
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

}

