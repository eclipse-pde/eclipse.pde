/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *     Kevin Doyle <kjdoyle@ca.ibm.com> - bug 200727
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 482175
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.pde.internal.runtime.spy.dialogs.SpyDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class SpyHandler extends AbstractHandler {

	private SpyDialog spyDialog = null;


	@Override
	public Object execute(ExecutionEvent event) {
		if (spyDialog != null && spyDialog.getShell() != null && !spyDialog.getShell().isDisposed()) {
			spyDialog.close();
			return null;
		}
		Shell shell = HandlerUtil.getActiveShell(event);
		spyDialog = new SpyDialog(shell, event, shell.getDisplay().getCursorLocation());
		spyDialog.create();
		spyDialog.open();
		return null;
	}

}
