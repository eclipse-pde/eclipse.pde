/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

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
		setWindowTitle(PDEUIMessages.ProductDefinitionWizard_title);
	}

	@Override
	public void addPages() {
		fMainPage = new ProductDefinitonWizardPage("product", fProduct); //$NON-NLS-1$
		addPage(fMainPage);
	}

	@Override
	public boolean performFinish() {
		try {
			fProductId = fMainPage.getProductId();
			fPluginId = fMainPage.getDefiningPlugin();
			fApplication = fMainPage.getApplication();
			String newProductName = fMainPage.getProductName();
			if (newProductName != null)
				fProduct.setName(newProductName);
			getContainer().run(false, true, new ProductDefinitionOperation(fProduct, fPluginId, fProductId, fApplication, getContainer().getShell()));
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getContainer().getShell(), PDEUIMessages.ProductDefinitionWizard_error, e.getTargetException().getMessage());
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
