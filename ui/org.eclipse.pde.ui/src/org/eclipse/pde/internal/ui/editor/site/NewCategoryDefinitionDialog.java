package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.parts.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class NewCategoryDefinitionDialog extends StatusDialog {
	private static final String KEY_TITLE = "NewCategoryDefinitionDialog.title";	
	private static final String KEY_NAME = "NewCategoryDefinitionDialog.name";
	private static final String KEY_LABEL = "NewCategoryDefinitionDialog.label";
	private static final String KEY_DESC = "NewCategoryDefinitionDialog.desc";
	private static final String KEY_EMPTY = "NewCategoryDefinitionDialog.empty";
	private Button okButton;
	private ISiteModel siteModel;
	private ISiteCategoryDefinition categoryDef;
	private Text nameText;
	private Text labelText;
	private Text descText;

	public NewCategoryDefinitionDialog(
		Shell shell,
		ISiteModel siteModel,
		ISiteCategoryDefinition def) {
		super(shell);
		this.siteModel = siteModel;
		this.categoryDef = def;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		okButton =
			createButton(
				parent,
				IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL,
				true);
		createButton(
			parent,
			IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL,
			false);
		dialogChanged();
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_NAME));
		nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		nameText.setLayoutData(gd);

		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_LABEL));
		labelText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		labelText.setLayoutData(gd);

		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_DESC));
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);
		descText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		descText.setLayoutData(gd);

		if (categoryDef != null)
			initializeFields();
		hookListeners();
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));

		//WorkbenchHelp.setHelp(container, IHelpContextIds.SCHEMA_TYPE_RESTRICTION);
		return container;
	}

	private void hookListeners() {
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		nameText.addModifyListener(listener);
		labelText.addModifyListener(listener);
		descText.addModifyListener(listener);
	}

	private void initializeFields() {
		setIfDefined(nameText, categoryDef.getName());
		setIfDefined(labelText, categoryDef.getLabel());
		setIfDefined(
			descText,
			categoryDef.getDescription() != null
				? categoryDef.getDescription().getText()
				: null);
		if (siteModel.isEditable()==false) {
			okButton.setEnabled(false);
			nameText.setEditable(false);
			labelText.setEditable(false);
			descText.setEditable(false);
		}
	}

	private void setIfDefined(Text text, String value) {
		if (value != null)
			text.setText(value);
	}

	private void dialogChanged() {
		IStatus status = null;
		if (nameText.getText().length() == 0
			|| labelText.getText().length() == 0)
			status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEPlugin.getResourceString(KEY_EMPTY),
					null);
		updateStatus(status);
	}

	protected void okPressed() {
		execute();
		super.okPressed();
	}
	private void execute() {
		boolean add = false;
		if (categoryDef == null) {
			add = true;
			categoryDef = siteModel.getFactory().createCategoryDefinition();
		}
		try {
			categoryDef.setName(nameText.getText());
			categoryDef.setLabel(labelText.getText());
			String desc = descText.getText();
			if (desc.length() > 0) {
				ISiteDescription description = categoryDef.getDescription();
				if (description == null)
					description =
						siteModel.getFactory().createDescription(categoryDef);
				description.setText(desc);
				categoryDef.setDescription(description);
			} else {
				categoryDef.setDescription(null);
			}
			if (add) {
				siteModel.getSite().addCategoryDefinitions(
					new ISiteCategoryDefinition[] { categoryDef });
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
