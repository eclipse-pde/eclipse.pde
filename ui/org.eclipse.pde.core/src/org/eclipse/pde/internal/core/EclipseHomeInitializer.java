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
package org.eclipse.pde.internal.core;


import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

public class EclipseHomeInitializer extends ClasspathVariableInitializer {

	
	/**
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		resetEclipseHomeVariables();
	}

	public static void resetEclipseHomeVariables() {
		try {
			Preferences pref = PDECore.getDefault().getPluginPreferences();
			String platformHome = pref.getString(ICoreConstants.PLATFORM_PATH);
			JavaCore.setClasspathVariable(
				PDECore.ECLIPSE_HOME_VARIABLE,
				new Path(platformHome),
				null);
		} catch (CoreException e) {
		}
	}
}
