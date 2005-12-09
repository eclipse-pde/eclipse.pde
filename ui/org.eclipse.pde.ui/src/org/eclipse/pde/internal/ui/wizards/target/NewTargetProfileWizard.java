package org.eclipse.pde.internal.ui.wizards.target;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewTargetProfileWizard extends BasicNewResourceWizard {
	
	TargetProfileWizardPage fPage;
	
	public void addPages() {
		fPage = new TargetProfileWizardPage("profile", getSelection()); //$NON-NLS-1$
		addPage(fPage);
	}

	public boolean performFinish() {
		try {
			getContainer().run(false, true, new TargetProfileOperation(fPage.createNewFile()));
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(PDEUIMessages.NewTargetProfileWizard_title); 
		setNeedsProgressMonitor(true);
	}
	
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

}
