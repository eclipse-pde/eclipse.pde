package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class TargetProfileWizardPage extends WizardNewFileCreationPage {
	
	private static String EXTENSION = ".target"; //$NON-NLS-1$
	
	public TargetProfileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.TargetProfileWizardPage_title);
		setDescription(PDEUIMessages.TargetProfileWizardPage_description);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		setFileName(EXTENSION); 
	}
	
    protected void createAdvancedControls(Composite parent) {
    }
    
    protected boolean validatePage() {
		if (!getFileName().trim().endsWith(EXTENSION)) { 
			setErrorMessage(PDEUIMessages.TargetProfileWizardPage_error); 
			return false;
		}
		if (getFileName().trim().length() <= EXTENSION.length()) {
			return false;
		}
		return super.validatePage();
    }
    
    protected void createLinkTarget() {
    }
        
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
    
}
