/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.samples;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferenceNode;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ShowTargetPlatformAction extends Action {

	// Bring up Target Platform prefrences page
	@Override
	public void run() {
		final IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final PreferenceDialog dialog = new PreferenceDialog(shell, manager);
		BusyIndicator.showWhile(Display.getCurrent(), () -> {
			dialog.create();
			dialog.setMessage(targetNode.getLabelText());
			dialog.open();
		});
	}

}
