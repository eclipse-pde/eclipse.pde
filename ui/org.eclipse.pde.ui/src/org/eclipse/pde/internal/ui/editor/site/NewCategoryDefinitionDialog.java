package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class NewCategoryDefinitionDialog extends BaseDialog {
	private static final String KEY_TITLE = "NewCategoryDefinitionDialog.title";	
	private static final String KEY_NAME = "NewCategoryDefinitionDialog.name";
	private static final String KEY_LABEL = "NewCategoryDefinitionDialog.label";
	private static final String KEY_DESC = "NewCategoryDefinitionDialog.desc";
	private static final String KEY_EMPTY = "NewCategoryDefinitionDialog.empty";
	private static final String SETTINGS_SECTION = "NewCategoryDefinitionDialog";
	private static final String S_NAME = "name";
	private static final String S_LABEL = "label";
	private static final String S_DESC = "desc";
	private Text nameText;
	private Text labelText;
	private Text descText;

	public NewCategoryDefinitionDialog(
		Shell shell,
		ISiteModel siteModel,
		ISiteCategoryDefinition def) {
		super(shell, siteModel, def);
	}

	protected void createEntries(Composite container) {
		GridData gd;
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
		if (getCategoryDefinition()==null) {
			presetFields();
		}
	}
	
	private void presetFields() {
		IDialogSettings settings = getDialogSettings(SETTINGS_SECTION);
		setIfDefined(nameText, settings.get(S_NAME));
		setIfDefined(labelText, settings.get(S_LABEL));
		setIfDefined(descText, settings.get(S_DESC));
	}
	
	private ISiteCategoryDefinition getCategoryDefinition() {
		return (ISiteCategoryDefinition)getSiteObject();
	}
	
	protected String getDialogTitle() {
		return PDEPlugin.getResourceString(KEY_TITLE);
	}
	
	protected String getHelpId() {
		return IHelpContextIds.NEW_CATEGORY_DEF_DIALOG;
	}
	
	protected String getEmptyErrorMessage() {
		return PDEPlugin.getResourceString(KEY_EMPTY);
	}

	protected void hookListeners(ModifyListener modifyListener) {
		nameText.addModifyListener(modifyListener);
		labelText.addModifyListener(modifyListener);
		descText.addModifyListener(modifyListener);
	}

	protected void initializeFields() {
		super.initializeFields();
		ISiteCategoryDefinition categoryDef = getCategoryDefinition();
		setIfDefined(nameText, categoryDef.getName());
		setIfDefined(labelText, categoryDef.getLabel());
		setIfDefined(
			descText,
			categoryDef.getDescription() != null
				? categoryDef.getDescription().getText()
				: null);
	}

	protected void dialogChanged() {
		IStatus status = null;
		String name = nameText.getText();
		if (name.length() == 0
			|| labelText.getText().length() == 0)
			status = getEmptyErrorStatus();
		else {
			if (alreadyExists(name))
				status = createErrorStatus("This category already exists."); 
		}
		if (status==null)
			status = getOKStatus();
		updateStatus(status);
	}
	
	private boolean alreadyExists(String name) {
		ISiteCategoryDefinition [] defs = getSiteModel().getSite().getCategoryDefinitions();
		for (int i=0; i<defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			String dname = def.getName();
			if (dname!=null && dname.equals(name))
				return true;
		}
		return false;
	}

	protected void execute() {
		boolean add = false;
		ISiteCategoryDefinition categoryDef = getCategoryDefinition();
		ISiteModel siteModel = getSiteModel();
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
				IDialogSettings settings = getDialogSettings(SETTINGS_SECTION);
				settings.put(S_NAME, categoryDef.getName());
				settings.put(S_LABEL, categoryDef.getLabel());
				settings.put(S_DESC, desc);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
