/*
 * Created on Feb 20, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.pde.internal.build.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.build.ant.AntScript;

public class ExportFeatureBuildScriptGenerator extends FeatureBuildScriptGenerator {
	protected void generateZipDistributionWholeTarget(AntScript script) {
		int tab = 1;
		script.println();
		script.printTargetDeclaration(tab++, TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, null);
		script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
		script.printMkdirTask(tab, FEATURE_TEMP_FOLDER);
		Map params = new HashMap(1);
		params.put(PROPERTY_FEATURE_BASE, FEATURE_TEMP_FOLDER);
		params.put(PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
		script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
		params.clear();
		params.put(PROPERTY_ZIPNAME, FEATURE_DESTINATION + "/" + FEATURE_FULL_NAME + ".bin.dist.zip"); //$NON-NLS-1$
		script.printAntCallTask(tab, TARGET_ZIP_FOLDER, null, params);
		script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
		script.printTargetEnd(--tab);
	}

}
