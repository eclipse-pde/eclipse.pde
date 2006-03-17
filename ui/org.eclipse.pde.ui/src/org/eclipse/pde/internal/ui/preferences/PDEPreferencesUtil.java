/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PDEPreferencesUtil {
	
	public static boolean showPreferencePage(String[] pageIds) {
		final Shell shell = PDEPlugin.getActiveWorkbenchShell();
		final PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell, pageIds[0], pageIds, null);
		return dialog.open() == Window.OK;
	}
	
	public static boolean showPreferencePage(final IPreferenceNode targetNode) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final Shell shell = PDEPlugin.getActiveWorkbenchShell();
		final PreferenceDialog dialog = new PreferenceDialog(shell, manager);
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				if (dialog.open() == Window.OK)
					result[0] = true;
			}
		});
		return result[0];
	}


}
