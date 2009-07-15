/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * SimpleCSPage
 * 
 */
public class SimpleCSDefinitionPage extends PDEFormPage implements
		IModelChangedListener {

	public static final String PAGE_ID = "simpleCSPage"; //$NON-NLS-1$

	private SimpleCSBlock fBlock;
	private boolean fStale;

	/**
	 * @param editor
	 */
	public SimpleCSDefinitionPage(FormEditor editor) {
		super(editor, PAGE_ID, SimpleMessages.SimpleCSDefinitionPage_title);
		fBlock = new SimpleCSBlock(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.SIMPLE_CS_EDITOR;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.
	 * eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		// Bug: Two veritical scrollbars appear when resizing the editor
		// vertically
		// Note: Scrolled form #1 created here
		ScrolledForm form = managedForm.getForm();
		// Set page title
		ISimpleCSModel model = (ISimpleCSModel) getModel();
		// Ensure the model was loaded properly
		if ((model == null) || (model.isLoaded() == false)) {
			Exception e = null;
			if (model instanceof AbstractModel) {
				e = ((AbstractModel) model).getException();
			}
			// Create a formatted error page
			createFormErrorContent(managedForm,
					SimpleMessages.SimpleCSDefinitionPage_loadFailure,
					SimpleMessages.SimpleCSDefinitionPage_errorMessage, e);
			return;
		}
		// Create the rest of the actions in the form title area
		super.createFormContent(managedForm);
		// Form image
		form.setImage(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider()
				.get(PDEUserAssistanceUIPluginImages.DESC_CHEATSHEET_OBJ));
		// Form title
		String title = PDETextHelper.translateReadText(model.getSimpleCS()
				.getTitle());
		if (title.length() > 0) {
			form.setText(title);
		} else {
			form.setText(SimpleMessages.SimpleCSDefinitionPage_title);
		}
		// Create the masters details block
		// Note: Scrolled form #2 created here
		fBlock.createContent(managedForm);
		// Force the selection in the masters tree section to load the
		// proper details section
		fBlock.getMastersSection().fireSelection();
		// Register this page to be informed of model change events
		model.addModelChangedListener(this);
		// Set context-sensitive help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(),
				IHelpContextIds.SIMPLE_CS_EDITOR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#dispose()
	 */
	public void dispose() {

		ISimpleCSModel simpleCSModel = (ISimpleCSModel) getModel();
		if (simpleCSModel != null) {
			simpleCSModel.removeModelChangedListener(this);
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde
	 * .core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {

		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object[] objects = event.getChangedObjects();
			// Ensure right type
			if ((objects[0] instanceof ISimpleCSObject) == false) {
				return;
			}
			ISimpleCSObject object = (ISimpleCSObject) objects[0];
			if (object == null) {
				// Ignore
			} else if (object.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
				String changeProperty = event.getChangedProperty();
				if ((changeProperty != null)
						&& changeProperty
								.equals(ISimpleCSConstants.ATTRIBUTE_TITLE)) {
					// Has to be a String if the property is a title
					// Update the form page title
					getManagedForm().getForm().setText(
							PDETextHelper.translateReadText((String) event
									.getNewValue()));
				}
			}
		} else if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			fStale = true;
		}
		// Inform the block
		fBlock.modelChanged(event);
	}

	/**
	 * @return
	 */
	public ISelection getSelection() {
		return fBlock.getSelection();
	}

	/**
	 * @return
	 */
	public PDEMasterDetailsBlock getBlock() {
		return fBlock;
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
		IFormPage page = getPDEEditor().findPage(
				SimpleCSInputContext.CONTEXT_ID);
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
		fBlock.getMastersSection().setFormInput(range);

		if (fStale) {
			// If we get a global model change the model's title may have
			// changed
			ScrolledForm form = getManagedForm().getForm();
			ISimpleCSModel model = (ISimpleCSModel) getModel();
			String oldTitle = form.getText();
			String newTitle = model.getSimpleCS().getTitle();
			if (!newTitle.equals(oldTitle)) {
				// Update form page title
				form.setText(PDETextHelper.translateReadText(newTitle));
			}
		}
	}

}
