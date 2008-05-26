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
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DSPage extends PDEFormPage implements IModelChangedListener {

	public static final String PAGE_ID = Messages.DSPage_pageId;

	private DSBlock fBlock;

	public DSPage(FormEditor editor) {
		super(editor, PAGE_ID, Messages.DSPage_title);

		fBlock = new DSBlock(this);
	}

	public PDEMasterDetailsBlock getBlock() {
		return fBlock;
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object[] objects = event.getChangedObjects();
			// Ensure right type
			if ((objects[0] instanceof IDSObject) == false) {
				return;
			}
			IDSObject object = (IDSObject) objects[0];
			if (object == null) {
				// Ignore
			} else if (object.getType() == IDSConstants.TYPE_ROOT) {
				String changeProperty = event.getChangedProperty();
				if ((changeProperty != null)
						&& changeProperty
								.equals(IDSConstants.ATTRIBUTE_COMPONENT_NAME)) {
					// Has to be a String if the property is a title
					// Update the form page title
					getManagedForm().getForm().setText(
							PDETextHelper.translateReadText((String) event
									.getNewValue()));
				}
			}
		} else if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(event);
		}

		// Inform the block
		fBlock.modelChanged(event);
	}

	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Page will be updated on refresh
		markStale();
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
			createFormErrorContent(managedForm, Messages.DSPage_errorTitle,
					Messages.DSPage_errorMessage, e);
			return;
		}
		// Create the rest of the actions in the form title area
		// super.createFormContent(managedForm);
		// Form title
		String title = PDETextHelper.translateReadText(model.getDSComponent()
				.getAttributeName());
		if (title.length() > 0) {
			form.setText(title);
		} else {
			form.setText(Messages.DSPage_formTitle);
		}

		// decorate the form heading
		managedForm.getToolkit().decorateFormHeading(form.getForm());
		form.setImage(SharedImages.getImage(SharedImages.DESC_DS));

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
		String newTitle = model.getDSComponent().getName();
		if (newTitle.equals(oldTitle) == false) {
			// Update form page title
			form.setText(PDETextHelper.translateReadText(newTitle));
		}
	}

	/**
	 * @return
	 */
	public ISelection getSelection() {
		return fBlock.getSelection();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#setActive(boolean)
	 */
	public void setActive(boolean active) {
		super.setActive(active);

		if (active == false) {
			// Switching away from this page
			return;
		}
		// Switching into this page
		// Get source page
		IFormPage page = getPDEEditor().findPage(DSInputContext.CONTEXT_ID);
		// Ensure we got the source page
		if ((page instanceof PDESourcePage) == false) {
			return;
		}
		PDESourcePage sourcePage = (PDESourcePage) page;
		// Get the source viewer
		ISourceViewer viewer = sourcePage.getViewer();
		// Ensure the viewer is defined
		if (viewer == null) {
			return;
		}
		// Get the styled text
		StyledText text = viewer.getTextWidget();
		// Ensure the styled text is defined
		if (text == null) {
			return;
		}
		// Get the cursor offset
		int offset = text.getCaretOffset();
		// Ensure the offset is defined
		if (offset < 0) {
			return;
		}
		// Get the range the offset is on
		IDocumentRange range = sourcePage.getRangeElement(offset, true);
		// Adapt the range to a node representable in the master tree viewer
		range = sourcePage.adaptRange(range);
		// Ensure the range is defined
		if (range == null) {
			return;
		}
		// Select the node in the master tree viewer if defined
		fBlock.getMasterSection().setFormInput(range);
	}

}
