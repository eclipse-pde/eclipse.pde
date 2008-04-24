/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Kevin Doyle <kjdoyle@ca.ibm.com> - bug 200727
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.pde.internal.runtime.spy.dialogs.SpyDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @since 3.4
 */
public class SpyHandler extends AbstractHandler {

	private SpyDialog INSTANCE = null;

	public SpyHandler() { // do nothing
	}

	public Object execute(ExecutionEvent event) {
		if (event != null) {
			if (INSTANCE != null && INSTANCE.getShell() != null && !INSTANCE.getShell().isDisposed()) {
				INSTANCE.close();
			}
			Shell shell = HandlerUtil.getActiveShell(event);
			SpyDialog dialog = new SpyDialog(shell, event, shell.getDisplay().getCursorLocation());
			INSTANCE = dialog;
			dialog.create();
			dialog.open();
		}
		return null;
	}

}
