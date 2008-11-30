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
package org.eclipse.pde.internal.ua.ui.wizards.ctxhelp;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpContext;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpModel;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpTopic;
import org.eclipse.pde.internal.ua.ui.IConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

/**
 * Operation to create a new context help xml file, add example entries and open it
 * in the context help editor.
 * @since 3.4
 */
public class NewCtxHelpOperation extends WorkspaceModifyOperation {

	private IFile fFile;

	public NewCtxHelpOperation(IFile file) {
		fFile = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		CtxHelpModel model = new CtxHelpModel(CoreUtility.getTextDocument(fFile.getContents()), false);
		model.setUnderlyingResource(fFile);
		initializeModel(model);
		model.save();
		model.dispose();
		openFile();
		monitor.done();
	}

	/**
	 * Initialize the xml with example entries
	 * @param model model for the file
	 */
	private void initializeModel(CtxHelpModel model) {
		CtxHelpContext context = model.getFactory().createContext();
		context.setID(CtxWizardMessages.NewCtxHelpOperation_context);
		model.getCtxHelpRoot().addChild(context);
		CtxHelpTopic topic = model.getFactory().createTopic();
		topic.setLabel(CtxWizardMessages.NewCtxHelpOperation_topic);
		context.addChild(topic);
	}

	/**
	 * Asynchronously opens the created file in the context help editor.
	 */
	protected void openFile() {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow ww = PDEUserAssistanceUIPlugin.getActiveWorkbenchWindow();
				if (ww == null) {
					return;
				}
				IWorkbenchPage page = ww.getActivePage();
				if (page == null || !fFile.exists())
					return;
				IWorkbenchPart focusPart = page.getActivePart();
				if (focusPart instanceof ISetSelectionTarget) {
					ISelection selection = new StructuredSelection(fFile);
					((ISetSelectionTarget) focusPart).selectReveal(selection);
				}
				try {
					IDE.openEditor(page, fFile, IConstants.CONTEXT_HELP_EDITOR_ID);
				} catch (PartInitException e) {
				}
			}
		});
	}

}
