/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpModel;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * The main page for the context help editor. Contains a tree displaying the
 * structure of the xml and a details section. UI elements are handled by
 * CtxHelpBlock.
 * 
 * @since 3.4
 * @see CtxHelpEditor
 * @see CtxHelpBlock
 */
public class CtxHelpPage extends PDEFormPage implements IModelChangedListener {
	public static final String PAGE_ID = "ctxHelpPage"; //$NON-NLS-1$

	private CtxHelpBlock fBlock;

	public CtxHelpPage(FormEditor editor) {
		super(editor, PAGE_ID, CtxHelpMessages.CtxHelpPage_title);
		fBlock = new CtxHelpBlock(this);
	}

	/**
	 * @return the CtxHelpBlock containing ui elements
	 */
	public PDEMasterDetailsBlock getBlock() {
		return fBlock;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.
	 * eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		CtxHelpModel model = (CtxHelpModel) getModel();

		// Ensure the model was loaded properly
		if ((model == null) || (model.isLoaded() == false)) {
			createErrorContent(managedForm);
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.CTX_HELP_EDITOR);

		// Create the rest of the actions in the form title area
		super.createFormContent(managedForm);
		// Form image
		form.setImage(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider().get(PDEUserAssistanceUIPluginImages.DESC_CTXHELP_CONTEXT_OBJ));
		form.setText(CtxHelpMessages.CtxHelpPage_formText);
		// Create the master details block
		fBlock.createContent(managedForm);
		// Force the selection in the masters tree section to load the
		// proper details section
		fBlock.getMasterSection().fireSelection();
		// Register this page to be informed of model change events
		model.addModelChangedListener(this);
	}

	private void createErrorContent(IManagedForm managedForm) {
		// Add error meesage to the form
		ScrolledForm form = managedForm.getForm();
		form.setMessage(CtxHelpMessages.CtxHelpPage_errMsg, IMessageProvider.ERROR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#dispose()
	 */
	public void dispose() {
		CtxHelpModel model = (CtxHelpModel) getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
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
		fBlock.modelChanged(event);
	}

	/**
	 * @return the current selection of the ui block
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
		if (active) {
			CtxHelpModel model = (CtxHelpModel) getModel();
			if ((model == null) || (model.isLoaded() == false)) {
				createErrorContent(getManagedForm());
			} else {
				// Clear the error message
				getManagedForm().getForm().setMessage("", IMessageProvider.NONE);
			}

			IFormPage page = getPDEEditor().findPage(CtxHelpInputContext.CONTEXT_ID);
			if (page instanceof CtxHelpSourcePage && ((CtxHelpSourcePage) page).getInputContext().isInSourceMode()) {
				ISourceViewer viewer = ((CtxHelpSourcePage) page).getViewer();
				if (viewer == null) {
					return;
				}

				StyledText text = viewer.getTextWidget();
				if (text == null) {
					return;
				}

				int offset = text.getCaretOffset();
				if (offset < 0) {
					return;
				}

				IDocumentRange range = ((CtxHelpSourcePage) page).getRangeElement(offset, true);
				if (range instanceof IDocumentAttributeNode) {
					range = ((IDocumentAttributeNode) range).getEnclosingElement();
				} else if (range instanceof IDocumentTextNode) {
					range = ((IDocumentTextNode) range).getEnclosingElement();
				}

				if (range instanceof CtxHelpObject) {
					fBlock.getMasterSection().setSelection(new StructuredSelection(range));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.CTX_HELP_EDITOR;
	}
}
