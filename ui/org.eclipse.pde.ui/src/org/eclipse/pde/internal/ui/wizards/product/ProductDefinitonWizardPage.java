package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;

public class ProductDefinitonWizardPage extends WizardPage {

	private Text fPluginText;
	private Text fProductText;

	private ModifyListener fListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			validatePage();
		}
	};

	public ProductDefinitonWizardPage(String pageName) {
		super(pageName);
		setTitle(PDEPlugin.getResourceString("ProductDefinitonWizardPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("ProductDefinitonWizardPage.desc")); //$NON-NLS-1$
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
	}

	private void createProductGroup(FormToolkit toolkit, Composite comp) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ProductDefinitonWizardPage.productGroup")); //$NON-NLS-1$
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createFormText(toolkit, group, PDEPlugin.getResourceString("ProductDefinitonWizardPage.productDefinition"), 3); //$NON-NLS-1$
		Label label = new Label(group, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ProductDefinitonWizardPage.plugin")); //$NON-NLS-1$
		
		fPluginText = new Text(group, SWT.SINGLE|SWT.BORDER);
		fPluginText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPluginText.addModifyListener(fListener);
		
		Button button = new Button(group, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ProductDefinitonWizardPage.browse")); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(button);
		
		label = new Label(group, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ProductDefinitonWizardPage.productId")); //$NON-NLS-1$
		
		fProductText = new Text(group, SWT.SINGLE|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fProductText.setLayoutData(gd);
		fProductText.addModifyListener(fListener);
		
	}
	
	private void createApplicationGroup(FormToolkit toolkit, Composite comp) {
		Group group = new Group(comp, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ProductDefinitonWizardPage.applicationGroup")); //$NON-NLS-1$
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createFormText(toolkit, group, PDEPlugin.getResourceString("ProductDefinitonWizardPage.applicationDefinition"), 2); //$NON-NLS-1$

		Label label = new Label(group, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ProductDefinitonWizardPage.application")); //$NON-NLS-1$
		
		Combo combo = new Combo(group, SWT.SINGLE|SWT.READ_ONLY);
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.setItems(TargetPlatform.getApplicationNames());
		if (combo.getItemCount() > 0)
			combo.setText(combo.getItem(0));	
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
			error = PDEPlugin.getResourceString("ProductDefinitonWizardPage.noPluginId"); //$NON-NLS-1$
		} else {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
			if (model == null)
				error = PDEPlugin.getResourceString("ProductDefinitonWizardPage.noPlugin"); //$NON-NLS-1$
			else if (model.getUnderlyingResource() == null)
				error = PDEPlugin.getResourceString("ProductDefinitonWizardPage.notInWorkspace"); //$NON-NLS-1$
		}
		if (error == null) {
			error = validateId();
		}
		setErrorMessage(error);
		setPageComplete(error == null);
	}
	
	private String validateId() {
		String id = fProductText.getText().trim();
		if (id.length() == 0)
			return PDEPlugin.getResourceString("ProductDefinitonWizardPage.noProductID"); //$NON-NLS-1$

		for (int i = 0; i<id.length(); i++){
			if (!id.substring(i,i+1).matches("[a-zA-Z0-9_]")) //$NON-NLS-1$
				return PDEPlugin.getResourceString("ProductDefinitonWizardPage.invalidId"); //$NON-NLS-1$
		}
		return null;
	}


	

}
