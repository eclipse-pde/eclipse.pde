/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import org.eclipse.jdt.debug.ui.console.JavaStackTraceConsoleFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.internal.views.log.LogEntry;

/**
 * Action to open a stack trace from a logged error in debug's java stack trace
 * console.
 */
public class ShowErrorInStackTraceConsoleAction implements IViewActionDelegate {

	private String fSelectedStack;
	private JavaStackTraceConsoleFactory fFactory;

	@Override
	public void init(IViewPart view) {
	}

	@Override
	public void run(IAction action) {
		if (fSelectedStack != null) {
			if (fFactory == null) {
				fFactory = new JavaStackTraceConsoleFactory();
			}
			fFactory.openConsole(fSelectedStack);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSelectedStack = null;
		action.setEnabled(false);
		if (selection instanceof IStructuredSelection) {
			Object firstObject = ((IStructuredSelection) selection).getFirstElement();
			if (firstObject instanceof LogEntry) {
				String stack = ((LogEntry) firstObject).getStack();
				if (stack != null && stack.length() > 0) {
					action.setEnabled(true);
					fSelectedStack = stack;
				}
			}
		}
	}

}
