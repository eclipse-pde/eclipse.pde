/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;

/**
 * Action for launching the workbench using the 
 * shortcut. This action is suitable for referencing from 
 * welcome and cheat sheet files.
 */
public class RunWorkbenchAction extends Action {
	public void run() {
		final EclipseLaunchShortcut shortcut = new EclipseLaunchShortcut();
		BusyIndicator.showWhile(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay(), new Runnable() {
			public void run() {
				shortcut.launch(new StructuredSelection(), ILaunchManager.RUN_MODE);
				notifyResult(true);
			}
		});
	}
}
