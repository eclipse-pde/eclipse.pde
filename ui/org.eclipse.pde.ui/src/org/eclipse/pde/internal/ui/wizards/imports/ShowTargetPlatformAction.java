/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferenceNode;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Action suitable for calling from cheat sheets and
 * other places.
 */
public class ShowTargetPlatformAction extends Action {
	public void run() {
		IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
		boolean result = showPreferencePage(targetNode);
		notifyResult(result);
	}
	private boolean showPreferencePage(final IPreferenceNode targetNode) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final PreferenceDialog dialog =
			new PreferenceDialog(shell, manager);
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				if (dialog.open() == PreferenceDialog.OK)
					result[0] = true;
			}
		});
		return result[0];
	}
}
