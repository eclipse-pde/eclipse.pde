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
	public void run() {
		final IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final PreferenceDialog dialog = new PreferenceDialog(shell, manager);
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				dialog.open();
			}
		});
	}

}
