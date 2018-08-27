/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
 *     Marc-Andre Laperle (Ericsson) - Moved to a different plug-in
 *******************************************************************************/
package org.eclipse.ui.internal.views.log.jdt;

import org.eclipse.core.commands.*;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jdt.debug.ui.console.JavaStackTraceConsoleFactory;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.views.log.LogView;

public class ShowErrorInStackTraceConsoleHandler extends AbstractHandler {
	private JavaStackTraceConsoleFactory fFactory;
	LogView logView = null;

	@Override
	public void setEnabled(Object evaluationContext) {
		super.setEnabled(evaluationContext);

		if (evaluationContext instanceof IEvaluationContext) {
			Object activePart = ((IEvaluationContext) evaluationContext).getVariable(ISources.ACTIVE_PART_NAME);
			if (activePart instanceof LogView) {
				logView = (LogView) activePart;
			}
		}
	}

	@Override
	public boolean isEnabled() {
		if (logView != null) {
			if (logView.getSelectedStack() != null) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart instanceof LogView) {
			LogView logView = (LogView) activePart;
			if (logView.getSelectedStack() != null) {
				if (fFactory == null) {
					fFactory = new JavaStackTraceConsoleFactory();
				}
				fFactory.openConsole(logView.getSelectedStack());
			}
		}
		return null;
	}
}
