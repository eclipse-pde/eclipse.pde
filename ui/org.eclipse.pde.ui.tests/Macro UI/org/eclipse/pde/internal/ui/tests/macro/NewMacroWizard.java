/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class NewMacroWizard extends Wizard {
	private String contents;
	private NewMacroPage page;
	
	class NewMacroPage extends WizardNewFileCreationPage {
		public NewMacroPage(IStructuredSelection ssel) {
			super("newFile", ssel);
			setTitle("Macro script name");
			setDescription("Select the target location and the name of the new script (extension *.emc).");
		} 
		public InputStream getInitialContents() {
			InputStream is=null;
			try {
				is = new ByteArrayInputStream(contents.getBytes("UTF8"));
			}
			catch (UnsupportedEncodingException e) {
			}
			return is;
		}
	}

	public NewMacroWizard(String contents) {
		this.contents = contents;
		setWindowTitle("Macro Recorder");
	}
	
	public void addPages() {
		ISelectionService sservice = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = sservice.getSelection();
		IStructuredSelection ssel;
		if (!(selection instanceof IStructuredSelection))
			ssel = new StructuredSelection();
		else
			ssel = (IStructuredSelection)selection;
			
		page = new NewMacroPage(ssel);
		addPage(page);
	}
	public boolean performFinish() {
		IFile file = page.createNewFile();
		return file!=null;
	}
}