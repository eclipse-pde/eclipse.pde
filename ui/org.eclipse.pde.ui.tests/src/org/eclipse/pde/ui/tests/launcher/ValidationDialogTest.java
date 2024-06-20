/*******************************************************************************
 * Copyright (c) 2023 ETAS GmbH and others, all rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     ETAS GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.ui.tests.launcher;

import static org.junit.Assert.assertNotNull;

import org.eclipse.jface.window.IShellProvider;
import org.eclipse.pde.internal.ui.launcher.PluginStatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;

public class ValidationDialogTest {
	PluginStatusDialog dialog;

	@Test
	public void editLaunchConfigLink() {
		Display.getDefault().syncExec(() -> {
			IShellProvider shellProvider = PlatformUI.getWorkbench().getModalDialogShellProvider();
			dialog = new PluginStatusDialog(shellProvider.getShell());
			dialog.showLink(true);
			dialog.showCancelButton(true);
			// To make the dialogue close immediately
			dialog.setBlockOnOpen(false);
			dialog.open();
			Control[] children = dialog.buttonBar.getParent().getChildren();
			checkEditConfigurationLink(children);
			dialog.close();
		});

	}

	private void checkEditConfigurationLink(Control[] element) {
		for (Control control : element) {
			if (control instanceof Composite) {
				Control[] children = ((Composite) control).getChildren();
				checkEditConfigurationLink(children);
			} else if ((control instanceof Link)) {
				isEditLaunchConfigurationLinkAvilable(element, control);
				break;
			}
		}
	}

	private void isEditLaunchConfigurationLinkAvilable(Control[] element, Control control) {
		Control editConfigLink = element[0];
		control.notifyListeners(SWT.Selection, new Event());
		assertNotNull(editConfigLink.isVisible());
	}
}