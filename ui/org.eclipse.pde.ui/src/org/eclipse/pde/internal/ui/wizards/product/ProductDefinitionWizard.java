package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;

public class ProductDefinitionWizard extends Wizard {

	private ProductDefinitonWizardPage fMainPage;

	public ProductDefinitionWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_DEFCON_WIZ);
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString("ProductDefinitionWizard.title"));  //$NON-NLS-1$
	}
	
	public void addPages() {
		fMainPage = new ProductDefinitonWizardPage("product"); //$NON-NLS-1$
		addPage(fMainPage);
	}

	public boolean performFinish() {
		return false;
	}
	

}
