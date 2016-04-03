/*******************************************************************************
 * Copyright (c) 2016 Google Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Xenos (Google) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.handlers;

import org.eclipse.core.commands.*;
import org.eclipse.pde.internal.runtime.spy.dialogs.LayoutSpyDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class LayoutSpyHandler extends AbstractHandler {
	private LayoutSpyDialog popupDialog;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (popupDialog != null) {
			popupDialog.close();
		}

		Shell shell = HandlerUtil.getActiveShell(event);
		if (shell != null) {
			popupDialog = new LayoutSpyDialog(shell);
			popupDialog.open();
		}
		return null;
	}

}
