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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tools.layout.spy.internal.dialogs.LayoutSpyDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class LayoutSpyHandler extends AbstractHandler {
	private LayoutSpyDialog popupDialog;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (popupDialog != null) {
			popupDialog.close();
		}

		Shell shell = HandlerUtil.getActiveShell(event);
		if (shell == null || shell.isDisposed()) {
			// The active shell may have been disposed since the user initiated the command,
			// such as when it was started via Find Actions dialog whose shell is disposed
			// as a side effect of launching this handler
			IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
			if (activeWorkbenchWindow != null) {
				shell = activeWorkbenchWindow.getShell();
			}
		}
		if (shell != null && !shell.isDisposed()) {
			popupDialog = new LayoutSpyDialog(shell);
			popupDialog.open();
		}
		return null;
	}

}
