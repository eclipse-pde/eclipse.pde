package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.*;

/**
 * @author melhem
 */
public class ExtensionPointMappings {
	
	private static HashMap fMap = new HashMap();
	
	private static void initialize() {
		fMap.put("org.eclipse.ui.markerImageProvider", "org.eclipse.ui.ide.markerImageProvider");
		fMap.put("org.eclipse.ui.markerHelp", "org.eclipse.ui.ide.markerHelp");
		fMap.put("org.eclipse.ui.markerImageProviders", "org.eclipse.ui.ide.markerImageProviders");
		fMap.put("org.eclipse.ui.markerResolution", "org.eclipse.ui.ide.markerResolution");
		fMap.put("org.eclipse.ui.projectNatureImages", "org.eclipse.ui.ide.projectNatureImages");
		fMap.put("org.eclipse.ui.resourceFilters", "org.eclipse.ui.ide.resourceFilters");
		fMap.put("org.eclipse.ui.markerUpdaters", "org.eclipse.ui.editors.markerUpdaters");
		fMap.put("org.eclipse.ui.documentProviders", "org.eclipse.ui.editors.documentProviders");
		fMap.put("org.eclipse.ui.workbench.texteditor.markerAnnotationSpecification", "org.eclipse.ui.editors.markerAnnotationSpecification");
		fMap.put("org.eclipse.help.browser", "org.eclipse.help.base.browser");
		fMap.put("org.eclipse.help.luceneAnalyzer", "org.eclipse.help.base.luceneAnalyzer");
		fMap.put("org.eclipse.help.webapp", "org.eclipse.help.base.webapp");
		fMap.put("org.eclipse.help.support", "org.eclipse.ui.helpSupport");
	}
	
	public static boolean isDeprecated(String id) {
		if (fMap.isEmpty())
			initialize();
		return fMap.containsKey(id);
	}
	
	public static boolean hasMovedFromHelpToBase(String key) {
		return key.equals("org.eclipse.help.browser")
			|| key.equals("org.eclipse.help.luceneAnalyzer")
			|| key.equals("org.eclipse.help.webapp");
	}
	
	public static boolean hasMovedFromHelpToUI(String key) {
		return key.equals("org.eclipse.help.support");
	}
	
	public static String getNewId(String oldId) {
		if (fMap.isEmpty())
			initialize();
		return fMap.containsKey(oldId) ? fMap.get(oldId).toString() : null;
	}

}
