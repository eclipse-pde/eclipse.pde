/*******************************************************************************
 * Copyright (c) 2016, 2020 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Xenos (Google) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tools.layout.spy.internal.dialogs.LayoutSpyDialog;

import jakarta.inject.Named;

public class LayoutSpyHandler {
	private LayoutSpyDialog popupDialog;

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
		if (popupDialog != null) {
			popupDialog.close();
		}
		if (shell != null && !shell.isDisposed()) {
			popupDialog = new LayoutSpyDialog(shell);
			popupDialog.open();
		}
	}

}
