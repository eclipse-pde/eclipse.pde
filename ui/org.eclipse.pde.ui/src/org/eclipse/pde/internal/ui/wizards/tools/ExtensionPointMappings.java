/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.HashMap;

public class ExtensionPointMappings {

	private static HashMap fMap = new HashMap();

	private static void initialize() {
		fMap.put("org.eclipse.ui.markerImageProvider", "org.eclipse.ui.ide.markerImageProvider"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.markerHelp", "org.eclipse.ui.ide.markerHelp"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.markerImageProviders", "org.eclipse.ui.ide.markerImageProviders"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.markerResolution", "org.eclipse.ui.ide.markerResolution"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.projectNatureImages", "org.eclipse.ui.ide.projectNatureImages"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.resourceFilters", "org.eclipse.ui.ide.resourceFilters"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.markerUpdaters", "org.eclipse.ui.editors.markerUpdaters"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.documentProviders", "org.eclipse.ui.editors.documentProviders"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.ui.workbench.texteditor.markerAnnotationSpecification", "org.eclipse.ui.editors.markerAnnotationSpecification"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.help.browser", "org.eclipse.help.base.browser"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.help.luceneAnalyzer", "org.eclipse.help.base.luceneAnalyzer"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.help.webapp", "org.eclipse.help.base.webapp"); //$NON-NLS-1$ //$NON-NLS-2$
		fMap.put("org.eclipse.help.support", "org.eclipse.ui.helpSupport"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static boolean isDeprecated(String id) {
		if (fMap.isEmpty())
			initialize();
		return fMap.containsKey(id);
	}

	public static boolean hasMovedFromHelpToBase(String key) {
		return key.equals("org.eclipse.help.browser") //$NON-NLS-1$
				|| key.equals("org.eclipse.help.luceneAnalyzer") //$NON-NLS-1$
				|| key.equals("org.eclipse.help.webapp"); //$NON-NLS-1$
	}

	public static boolean hasMovedFromHelpToUI(String key) {
		return key.equals("org.eclipse.help.support"); //$NON-NLS-1$
	}

	public static String getNewId(String oldId) {
		if (fMap.isEmpty())
			initialize();
		return fMap.containsKey(oldId) ? fMap.get(oldId).toString() : null;
	}

}
