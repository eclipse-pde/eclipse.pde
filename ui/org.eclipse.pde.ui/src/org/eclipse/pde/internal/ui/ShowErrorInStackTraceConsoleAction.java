/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import org.eclipse.jdt.internal.debug.ui.console.JavaStackTraceConsoleFactory;
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fSelectedStack != null) {
			if (fFactory == null) {
				fFactory = new JavaStackTraceConsoleFactory();
			}
			fFactory.openConsole(fSelectedStack);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
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
