/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DSPage extends PDEFormPage implements IModelChangedListener {

	public static final String PAGE_ID = "dsPage";

	private DSBlock fBlock;

	public DSPage(FormEditor editor) {
		super(editor, PAGE_ID, Messages.DSPage_title);

		fBlock = new DSBlock(this);
	}

	public PDEMasterDetailsBlock getBlock() {
		return fBlock;
	}

	public void modelChanged(IModelChangedEvent event) {

		// Inform the block
		fBlock.modelChanged(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		// Set page title
		IDSModel model = (IDSModel) getModel();
		// Ensure the model was loaded properly
		if ((model == null) || (model.isLoaded() == false)) {
			Exception e = null;
			if (model instanceof AbstractModel) {
				e = ((AbstractModel) model).getException();
			}
			// Create a formatted error page
			createFormErrorContent(
					managedForm,
					"DS Load Failure",
					"An error was encountered while parsing the DS XML file.",
					e);
			return;
		}
		// Create the rest of the actions in the form title area
		// super.createFormContent(managedForm);
		// Form title
		String title = PDETextHelper.translateReadText(model.getDSRoot()
				.getAttributeName());
		if (title.length() > 0) {
			form.setText(title);
		} else {
			form.setText("Definition");
		}
		// Create the masters details block
		fBlock.createContent(managedForm);
		// Force the selection in the masters tree section to load the
		// proper details section
		fBlock.getMasterSection().fireSelection();
		// Register this page to be informed of model change events
		model.addModelChangedListener(this);
		// // Set context-sensitive help
		// PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(),
		// IHelpContextIds.SIMPLE_CS_EDITOR);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#refresh()
	 */
	protected void refresh() {
		super.refresh();
		ScrolledForm form = getManagedForm().getForm();
		IDSModel model = (IDSModel) getModel();
		String oldTitle = form.getText();
		String newTitle = model.getDSRoot().getName();
		if (newTitle.equals(oldTitle) == false) {
			// Update form page title
			form.setText(PDETextHelper.translateReadText(newTitle));
		}
	}

}
