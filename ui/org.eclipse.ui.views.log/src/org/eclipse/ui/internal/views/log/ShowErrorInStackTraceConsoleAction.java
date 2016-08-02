/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import org.eclipse.jdt.debug.ui.console.JavaStackTraceConsoleFactory;
import org.eclipse.jface.action.Action;

public class ShowErrorInStackTraceConsoleAction extends Action {
	private JavaStackTraceConsoleFactory fFactory;
	LogView logView = null;

	public ShowErrorInStackTraceConsoleAction(LogView lView, String text) {
		super(text);
		logView = lView;
	}
	@Override
	public void run() {
		if (logView != null) {
			if (logView.getSelectedStack() != null) {
				if (fFactory == null) {
					fFactory = new JavaStackTraceConsoleFactory();
				}
				fFactory.openConsole(logView.getSelectedStack());
			}
		}
	}
}
