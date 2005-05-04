/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;

public class ProductDefinitonWizardPage extends WizardPage implements IHyperlinkListener {

	private Text fPluginText;
	private Text fProductText;
	private Set fProductSet;
	private Combo fApplicationCombo;

	private ModifyListener fListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validatePage();
		}
	};
	
	
	public ProductDefinitonWizardPage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.ProductDefinitonWizardPage_title); //$NON-NLS-1$
		setDescription(PDEUIMessages.ProductDefinitonWizardPage_desc); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 20;
		comp.setLayout(layout);
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());	
		createProductGroup(toolkit, comp);		
		createApplicationGroup(toolkit, comp);
		toolkit.dispose();
		setControl(comp);
		setPageComplete(false);
	}
	
	private void createFormText(FormToolkit toolkit, Composite parent, String content, int span) {
		FormText text = toolkit.createFormText(parent, false);
		text.setText(content, true, false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		text.setLayoutData(gd);
		text.setBackground(null);
		text.addHyperlinkListener(this);
	}

	private void createProductGroup(FormToolkit toolkit, Composite comp) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(PDEUIMessages.ProductDefinitonWizardPage_productGroup); //$NON-NLS-1$
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createFormText(toolkit, group, PDEUIMessages.ProductDefinitonWizardPage_productDefinition, 3); //$NON-NLS-1$
		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.ProductDefinitonWizardPage_plugin); //$NON-NLS-1$
		
		fPluginText = new Text(group, SWT.SINGLE|SWT.BORDER);
		fPluginText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPluginText.addModifyListener(fListener);
		
		Button button = new Button(group, SWT.PUSH);
		button.setText(PDEUIMessages.ProductDefinitonWizardPage_browse); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.ProductDefinitonWizardPage_productId); //$NON-NLS-1$
		
		fProductText = new Text(group, SWT.SINGLE|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fProductText.setLayoutData(gd);
		fProductText.addModifyListener(fListener);
		
	}
	
	private void createApplicationGroup(FormToolkit toolkit, Composite comp) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(PDEUIMessages.ProductDefinitonWizardPage_applicationGroup); //$NON-NLS-1$
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createFormText(toolkit, group, PDEUIMessages.ProductDefinitonWizardPage_applicationDefinition, 2); //$NON-NLS-1$

		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.ProductDefinitonWizardPage_application); //$NON-NLS-1$
		
		fApplicationCombo = new Combo(group, SWT.SINGLE|SWT.READ_ONLY);
		fApplicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fApplicationCombo.setItems(TargetPlatform.getApplicationNames());
		if (fApplicationCombo.getItemCount() > 0)
			fApplicationCombo.setText(fApplicationCombo.getItem(0));	
	}
	
	public void setVisible(boolean visible) {
		if (visible) {
			fPluginText.setFocus();
		}
		super.setVisible(visible);
	}

	private void validatePage() {
		String error = null;
		String pluginId = fPluginText.getText().trim();
		if (pluginId.length() == 0) {
			error = PDEUIMessages.ProductDefinitonWizardPage_noPluginId; //$NON-NLS-1$
		} else {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
			if (model == null)
				error = PDEUIMessages.ProductDefinitonWizardPage_noPlugin; //$NON-NLS-1$
			else if (model.getUnderlyingResource() == null)
				error = PDEUIMessages.ProductDefinitonWizardPage_notInWorkspace; //$NON-NLS-1$
		}
		if (error == null)
			error = validateId();
		if (error == null && getProductNameSet().contains(pluginId + "." + fProductText.getText().trim())) { //$NON-NLS-1$
			error = PDEUIMessages.ProductDefinitonWizardPage_productExists; //$NON-NLS-1$
		}
		setErrorMessage(error);
		setPageComplete(error == null);
	}
	
	private String validateId() {
		String id = fProductText.getText().trim();
		if (id.length() == 0)
			return PDEUIMessages.ProductDefinitonWizardPage_noProductID; //$NON-NLS-1$

		for (int i = 0; i<id.length(); i++){
			if (!id.substring(i,i+1).matches("[a-zA-Z0-9_]")) //$NON-NLS-1$
				return PDEUIMessages.ProductDefinitonWizardPage_invalidId; //$NON-NLS-1$
		}
		return null;
	}

	public void linkEntered(HyperlinkEvent e) {
	}

	public void linkExited(HyperlinkEvent e) {
	}

	public void linkActivated(HyperlinkEvent e) {
		String extPoint = Platform.PI_RUNTIME + "." + e.getHref().toString(); //$NON-NLS-1$
		IPluginExtensionPoint point = PDECore.getDefault().findExtensionPoint(extPoint);
		if (point != null)
			new ShowDescriptionAction(point).run();
	}

	private void handleBrowse() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), PDECore.getDefault().getModelManager().getWorkspaceModels(), false);
		if (dialog.open() == PluginSelectionDialog.OK) {
			IPluginModelBase model = (IPluginModelBase)dialog.getFirstResult();
			fPluginText.setText(model.getPluginBase().getId());
		}
	}
	
	private Set getProductNameSet() {
		if (fProductSet == null)
			fProductSet = TargetPlatform.getProductNameSet();
		return fProductSet;
	}
	
	public String getDefiningPlugin() {
		return fPluginText.getText().trim();
	}
	
	public String getProductId() {
		return fProductText.getText().trim();
	}
	
	public String getApplication() {
		return fApplicationCombo.getText();
	}
	
}
