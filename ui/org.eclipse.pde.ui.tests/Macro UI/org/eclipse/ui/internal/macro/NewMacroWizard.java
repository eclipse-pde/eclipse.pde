/*
 * Created on Nov 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

import java.io.*;
import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
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