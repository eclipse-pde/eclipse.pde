/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class NewCategoryDefinitionDialog extends BaseNewDialog {
	private static final String KEY_TITLE = "NewCategoryDefinitionDialog.title";	 //$NON-NLS-1$
	private static final String KEY_NAME = "NewCategoryDefinitionDialog.name"; //$NON-NLS-1$
	private static final String KEY_LABEL = "NewCategoryDefinitionDialog.label"; //$NON-NLS-1$
	private static final String KEY_DESC = "NewCategoryDefinitionDialog.desc"; //$NON-NLS-1$
	private static final String KEY_EMPTY = "NewCategoryDefinitionDialog.empty"; //$NON-NLS-1$
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
		gd.heightHint = 100;
		gd.widthHint = 225;
		descText.setLayoutData(gd);
		if (getCategoryDefinition()==null) {
			presetFields();
		}
	}
	
	private void presetFields() {
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
		boolean edit = getCategoryDefinition()!=null;
		IStatus status = null;
		String name = nameText.getText();
		if (name.length() == 0
			|| labelText.getText().length() == 0)
			status = getEmptyErrorStatus();
		else {
			if (!edit && alreadyExists(name))
				status = createErrorStatus(PDEPlugin.getResourceString("NewCategoryDefinitionDialog.alreadyExists"));  //$NON-NLS-1$
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
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
