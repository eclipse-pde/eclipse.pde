package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;

public class ProductDefinitionWizard extends Wizard {

	private ProductDefinitonWizardPage fMainPage;
	private String fProductId;
	private String fPluginId;
	private String fApplication;
	private IProduct fProduct;

	public ProductDefinitionWizard(IProduct product) {
		fProduct = product;
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_DEFCON_WIZ);
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString("ProductDefinitionWizard.title"));  //$NON-NLS-1$
	}
	
	public void addPages() {
		fMainPage = new ProductDefinitonWizardPage("product"); //$NON-NLS-1$
		addPage(fMainPage);
	}

	public boolean performFinish() {
		try {
			fProductId = fMainPage.getProductId();
			fPluginId = fMainPage.getDefiningPlugin();
			fApplication = fMainPage.getApplication();
			getContainer().run(
					false,
					true,
					new ProductDefinitionOperation(fProduct,
							fPluginId, fProductId, fApplication, 
							getContainer().getShell()));
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getContainer().getShell(), PDEPlugin.getResourceString("ProductDefinitionWizard.error"), e.getTargetException().getMessage()); //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	public String getProductId() {
		return fPluginId + "." + fProductId; //$NON-NLS-1$
	}
	
	public String getApplication() {
		return fApplication;
	}
	

}
