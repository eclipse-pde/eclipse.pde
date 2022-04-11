/*******************************************************************************
 * Copyright (c) 2008, 2023 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     IBM Corporation - Bug 265935: Product editor opens on the wrong page
 *     Hannes Wellmann - Bug 325614 - Support mixed products (features and bundles)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.Map;

import org.eclipse.pde.internal.core.iproduct.IProduct.ProductType;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DependenciesPage extends PDEFormPage {

	static final String PLUGIN_ID = "plugin-dependencies"; //$NON-NLS-1$
	static final String FEATURE_ID = "feature-dependencies"; //$NON-NLS-1$
	static final String MIXED_ID = "mixed-dependencies"; //$NON-NLS-1$

	static final Map<ProductType, String> TYPE_2_ID = Map.of( //
			ProductType.BUNDLES, PLUGIN_ID, ProductType.FEATURES, FEATURE_ID, ProductType.MIXED, MIXED_ID);

	private ProductType fProductType;
	private PluginSection fPluginSection = null;

	public DependenciesPage(FormEditor editor, ProductType productType) {
		super(editor, TYPE_2_ID.get(productType), PDEUIMessages.Product_DependenciesPage_title);
		fProductType = productType;
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.CONFIGURATION_PAGE;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ));
		form.setText(PDEUIMessages.Product_DependenciesPage_title);
		fillBody(managedForm);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.CONFIGURATION_PAGE);
	}

	private void fillBody(IManagedForm managedForm) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 1));
		fPluginSection = null;
		IFormPart section = switch (fProductType)
			{
			case BUNDLES -> fPluginSection = new PluginSection(this, body);
			case FEATURES -> new FeatureSection(this, body);
			case MIXED -> new MixedSection(this, body);
			};
		managedForm.addPart(section);
	}

	public boolean includeOptionalDependencies() {
		return fPluginSection != null && fPluginSection.includeOptionalDependencies();
	}
}
