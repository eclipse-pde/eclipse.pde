package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IProvisionerWizard;

public class FileSystemProvisionerWizard extends Wizard implements IProvisionerWizard {
	
	private DirectorySelectionPage fPage = null;
	private File[] fDirs = null;
	
	public FileSystemProvisionerWizard() {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.FileSystemProvisionerWizard_title); 
	}

	public File[] getDirectories() {
		return fDirs;
	}

	public boolean performFinish() {
		fDirs = fPage.getLocations();
		return true;
	}

	public void addPages() {
		fPage = new DirectorySelectionPage("file system"); //$NON-NLS-1$
		addPage(fPage);
		super.addPages();
	}

}
