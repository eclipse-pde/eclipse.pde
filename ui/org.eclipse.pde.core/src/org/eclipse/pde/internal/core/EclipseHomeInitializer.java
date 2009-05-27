/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;

public class EclipseHomeInitializer extends ClasspathVariableInitializer {

	public static final String ECLIPSE_HOME_VARIABLE = "ECLIPSE_HOME"; //$NON-NLS-1$

	/**
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		resetEclipseHomeVariable();
	}

	public static void resetEclipseHomeVariable() {
		try {
			PDEPreferencesManager pref = PDECore.getDefault().getPreferencesManager();
			String platformHome = pref.getString(ICoreConstants.PLATFORM_PATH);
			JavaCore.setClasspathVariable(ECLIPSE_HOME_VARIABLE, new Path(platformHome), null);
		} catch (CoreException e) {
		}
	}
}
