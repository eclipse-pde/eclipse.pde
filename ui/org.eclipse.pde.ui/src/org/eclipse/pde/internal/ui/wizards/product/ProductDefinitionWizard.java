package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;

public class ProductDefinitionWizard extends Wizard {

	private ProductDefinitonWizardPage fMainPage;
	private String fProductId;
	private String fPluginId;
	private String fApplication;

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
		try {
			fProductId = fMainPage.getProductId();
			fPluginId = fMainPage.getDefiningPlugin();
			fApplication = fMainPage.getApplication();
			getContainer().run(false, true, new ProductDefinitionOperation());
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	public String getProductId() {
		return fPluginId + "." + fProductId;
	}
	
	public String getApplication() {
		return fApplication;
	}
	

}
