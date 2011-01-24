/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

/**
 * This is an interface used only by subclasses of PDELauncherFormPage.
 * Its purpose is to allow code reuse between direct subclasses of
 * PDELauncherFormPage and subclasses of LaunchShortcutOverviewPage.
 */

public interface ILauncherFormPageHelper {
	public void preLaunch();

	public Object getLaunchObject();

	public boolean isOSGi();
}
