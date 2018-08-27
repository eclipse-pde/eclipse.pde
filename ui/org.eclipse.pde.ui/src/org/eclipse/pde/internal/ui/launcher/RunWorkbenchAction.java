/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
	@Override
	public void run() {
		final EclipseLaunchShortcut shortcut = new EclipseLaunchShortcut();
		BusyIndicator.showWhile(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay(), () -> {
			shortcut.launch(new StructuredSelection(), ILaunchManager.RUN_MODE);
			notifyResult(true);
		});
	}
}
