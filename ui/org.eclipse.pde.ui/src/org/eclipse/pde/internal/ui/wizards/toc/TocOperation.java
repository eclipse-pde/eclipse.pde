/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.toc;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.core.toc.Toc;
import org.eclipse.pde.internal.core.toc.TocModelFactory;
import org.eclipse.pde.internal.core.toc.TocTopic;
import org.eclipse.pde.internal.core.toc.TocWorkspaceModel;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
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

public class TocOperation extends WorkspaceModifyOperation {
	
	private IFile fFile;
	private String fTocName;

	public TocOperation(IFile file, String tocName){
		fFile = file;
		fTocName = tocName;
	}

	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
        TocWorkspaceModel model = new TocWorkspaceModel(fFile, false);
        initializeToc(model.getToc());
        model.save();
        model.dispose();
        openFile();
        monitor.done();
	}
	
	private void initializeToc(Toc toc) {
		// Create Topic
		TocTopic topic = createTopic(toc);

		// Bind the created topic to this TOC
		toc.addChild(topic);
		
		// Set the initial TOC name 
		toc.setFieldLabel(fTocName);
	}

	private TocTopic createTopic(Toc toc) {
		TocModelFactory factory = toc.getModel().getFactory();
		TocTopic topic = factory.createTocTopic(toc);
		
		topic.setFieldLabel(PDEUIMessages.TocPage_TocTopic);
		
		return topic;
	}

	protected void openFile() {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
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
					IDE.openEditor(page, fFile, IPDEUIConstants.TABLE_OF_CONTENTS_EDITOR_ID);
				} catch (PartInitException e) {
				}
			}
		});	
	}

}
