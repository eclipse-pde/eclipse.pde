package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.wizards.newresource.*;


public class NewProductFileWizard extends BasicNewResourceWizard {
	
	private ProductFileWizadPage fMainPage;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new ProductFileWizadPage("product", getSelection());
		fMainPage.setTitle("Product Configuration File");
		addPage(fMainPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			getContainer().run(false, true, getOperation());
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	private IRunnableWithProgress getOperation() {
        IFile file = fMainPage.createNewFile();
		int option = fMainPage.getInitializationOption();
		if (option == ProductFileWizadPage.USE_LAUNCH_CONFIG)
			return new ProductFromConfigOperation(file, fMainPage.getSelectedLaunchConfiguration());
		if (option == ProductFileWizadPage.USE_PRODUCT)
			return new ProductFromExtensionOperation(file, fMainPage.getSelectedProduct());
		return new BaseProductCreationOperation(file);
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
