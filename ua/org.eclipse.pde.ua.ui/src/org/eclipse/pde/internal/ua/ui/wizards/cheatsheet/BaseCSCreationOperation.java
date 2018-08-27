/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

public abstract class BaseCSCreationOperation extends WorkspaceModifyOperation {

	protected IFile fFile;

	public BaseCSCreationOperation(IFile file) {
		fFile = file;
	}

	public BaseCSCreationOperation(ISchedulingRule rule) {
		super(rule);
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		monitor.beginTask(CSWizardMessages.BaseCSCreationOperation_task, 2);
		createContent();
		monitor.worked(1);
		openFile();
		monitor.done();
	}

	protected abstract void createContent() throws CoreException;

	private void openFile() {
		Display.getCurrent().asyncExec(() -> {
			IWorkbenchWindow window = PDEUserAssistanceUIPlugin.getActiveWorkbenchWindow();
			if (window == null) {
				return;
			}
			IWorkbenchPage page = window.getActivePage();
			if ((page == null) || !fFile.exists()) {
				return;
			}
			IWorkbenchPart focusPart = page.getActivePart();
			if (focusPart instanceof ISetSelectionTarget) {
				ISelection selection = new StructuredSelection(fFile);
				((ISetSelectionTarget) focusPart).selectReveal(selection);
			}
			try {
				IDE.openEditor(page, fFile);
			} catch (PartInitException e) {
				// Ignore
			}
		});
	}

	public static String formatTextBold(String text) {
		// TODO: MP: CompCS:  Create generalized HTML formatter utility
		StringBuilder buffer = new StringBuilder();
		buffer.append("<b>"); //$NON-NLS-1$
		buffer.append(text);
		buffer.append("</b>"); //$NON-NLS-1$
		return buffer.toString();
	}
}
