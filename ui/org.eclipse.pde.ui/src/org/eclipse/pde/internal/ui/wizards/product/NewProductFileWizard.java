package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.wizards.newresource.*;


public class NewProductFileWizard extends BasicNewResourceWizard {
	
	private NewProductFileWizadPage fMainPage;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new NewProductFileWizadPage("product", getSelection());
		fMainPage.setTitle("Product Configuration File");
		addPage(fMainPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
        IFile file = fMainPage.createNewFile();
        ILaunchConfiguration config = fMainPage.getSelectedLaunchConfiguration();
		try {
			IRunnableWithProgress op = new NewProductCreationOperation(file, config);
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewResourceWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle("New Product Configuration File");
		setNeedsProgressMonitor(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewResourceWizard#initializeDefaultPageImageDescriptor()
	 */
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_DEFCON_WIZ);
	}

}
