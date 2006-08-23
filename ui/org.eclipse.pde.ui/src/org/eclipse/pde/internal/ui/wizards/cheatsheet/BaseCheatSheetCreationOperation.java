/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.cheatsheet;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

/**
 * BaseCheatSheetCreationOperation
 *
 */
public class BaseCheatSheetCreationOperation extends WorkspaceModifyOperation {

	private IFile fFile;
	
	/**
	 * 
	 */
	public BaseCheatSheetCreationOperation(IFile file) {
		fFile = file;
	}

	/**
	 * @param rule
	 */
	public BaseCheatSheetCreationOperation(ISchedulingRule rule) {
		super(rule);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		monitor.beginTask(PDEUIMessages.BaseCheatSheetCreationOperation_0, 2); 
		// TODO: MP: Create file content when cheat sheet editor model is complete
		//createContent();
		monitor.worked(1);
        openFile();
        monitor.done();
	}
	
	/**
	 * 
	 */
	private void openFile() {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = PDEPlugin.getActiveWorkbenchWindow();
				if (window == null) {
					return;
				}
				IWorkbenchPage page = window.getActivePage();
				if ((page == null) || 
						!fFile.exists()) {
					return;
				}
				IWorkbenchPart focusPart = page.getActivePart();
				if (focusPart instanceof ISetSelectionTarget) {
					ISelection selection = new StructuredSelection(fFile);
					((ISetSelectionTarget) focusPart).selectReveal(selection);
				}
				try {
					// TODO: MP: Update when cheat sheet editor is complete
					IDE.openEditor(page, fFile, "org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
				} catch (PartInitException e) {
					// Ignore
				}
			}
		});
	}	
}
