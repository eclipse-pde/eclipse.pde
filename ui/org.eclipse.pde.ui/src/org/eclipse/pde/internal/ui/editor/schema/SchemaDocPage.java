/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.*;

public class SchemaDocPage extends PDEFormPage {
	
	public static final String PAGE_ID = "doc"; //$NON-NLS-1$
	public static final String PAGE_TITLE = "SchemaEditor.DocPage.title"; //$NON-NLS-1$
	private IColorManager colorManager = new ColorManager();
	private DocSection docSection;
	public static final String FORM_TITLE = "SchemaEditor.DocForm.title"; //$NON-NLS-1$
	public static final String KEY_PENDING_TITLE =
		"SchemaEditor.pending.title"; //$NON-NLS-1$
	public static final String KEY_PENDING_MESSAGE =
		"SchemaEditor.pending.docMessage"; //$NON-NLS-1$
	
	public SchemaDocPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEPlugin.getResourceString(PAGE_TITLE));
	}
	
	/**
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#becomesInvisible(IFormPage)
	 */
	public void setActive(boolean active) {
		if (!active)
			getManagedForm().commit(false);
		super.setActive(active);
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.horizontalSpacing=15;
		//layout.setMarginWidth 
		form.getBody().setLayout(layout);

		GridData gd;

		docSection = new DocSection(this, form.getBody(), colorManager);
		gd = new GridData(GridData.FILL_BOTH);
		docSection.getSection().setLayoutData(gd);

		managedForm.addPart(docSection);
		
		WorkbenchHelp.setHelp(form.getBody(), IHelpContextIds.SCHEMA_EDITOR_DOC);
		form.setText(PDEPlugin.getResourceString(FORM_TITLE));		
	}
	
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	public void updateEditorInput(Object obj) {
		docSection.updateEditorInput(obj);
	}	

	public boolean selectReveal(Object object) {
		docSection.checkForPendingChanges();
		return super.selectReveal(object);
	}
	
}
