/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.toc.Toc;
import org.eclipse.pde.internal.core.text.toc.TocModel;
import org.eclipse.pde.internal.core.text.toc.TocObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * TocPage
 *
 */
public class TocPage extends PDEFormPage implements IModelChangedListener {
	public static final String PAGE_ID = "tocPage"; //$NON-NLS-1$

	private TocBlock fBlock;

	/**
	 * @param editor
	 */
	public TocPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.TocPage_title);

		fBlock = new TocBlock(this);
	}

	/**
	 * @return
	 */
	public PDEMasterDetailsBlock getBlock() {
		return fBlock;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		TocModel model = (TocModel)getModel();
		
		// Ensure the model was loaded properly
		if ((model == null) || (model.isLoaded() == false)) {
			createErrorContent(managedForm, model);
			return;
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.TOC_EDITOR);
		// Create the rest of the actions in the form title area
		super.createFormContent(managedForm);
		// Form image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_TOC_OBJ));
		setFormTitle(form, model);
		// Create the master details block
		fBlock.createContent(managedForm);
		// Force the selection in the masters tree section to load the 
		// proper details section
		fBlock.getMasterSection().fireSelection();
		// Register this page to be informed of model change events
		model.addModelChangedListener(this);		
	}

	private void createErrorContent(IManagedForm managedForm, TocModel model) {
		Exception e = null;
		//e = ((AbstractModel)model).getException();

		// Create a formatted error page
		createFormErrorContent(managedForm, 
				PDEUIMessages.TocPage_msgTOCLoadFailure, 
				PDEUIMessages.TocPage_msgTOCParsingFailure, 
				e);
	}

	private void setFormTitle(ScrolledForm form, TocModel model) {
		// Form title
		String title = PDETextHelper.translateReadText(model.getToc()
				.getFieldLabel());
		if (title.length() > 0) {
			form.setText(title);
		} else {
			form.setText(PDEUIMessages.TocPage_title);
		}
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#dispose()
	 */
	public void dispose() {
		
		TocModel tocModel = (TocModel)getModel();
		if (tocModel != null) {
			tocModel.removeModelChangedListener(this);
		}
		super.dispose();
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object[] objects = event.getChangedObjects();
			TocObject object = (TocObject) objects[0];
			if (object == null) {
				// Ignore
			} else if (object.getType() == ITocConstants.TYPE_TOC) {
				String changeProperty = event.getChangedProperty();
				if ((changeProperty != null)
						&& changeProperty
								.equals(ITocConstants.ATTRIBUTE_LABEL)) {
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

	/**
	 * @return
	 */
	public ISelection getSelection() {
		return fBlock.getSelection();
	}
	
	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		
		Object[] objects = event.getChangedObjects();

		if (objects[0] != null && objects[0] instanceof TocObject)
		{	TocObject object = (TocObject)objects[0];
			if (object.getType() == ITocConstants.TYPE_TOC)
			{	String newValue = ((Toc)object).getFieldLabel();

				// Update page title
				getManagedForm().getForm().setText(
						PDETextHelper.translateReadText(newValue));
			}
		}
	}

	public void setActive(boolean active) {
		super.setActive(active);
		if(active)
		{	IFormPage page = getPDEEditor().findPage(TocInputContext.CONTEXT_ID);
			if (page instanceof TocSourcePage && ((TocSourcePage)page).getInputContext().isInSourceMode())
			{	ISourceViewer viewer = ((TocSourcePage)page).getViewer();
				if (viewer == null)
				{	return;
				}

				StyledText text = viewer.getTextWidget();
				if (text == null)
				{	return;
				}

				int offset = text.getCaretOffset();
				if (offset < 0)
				{	return;
				}

				IDocumentRange range = ((TocSourcePage)page).getRangeElement(offset, true);
				if (range instanceof IDocumentAttribute)
				{	range = ((IDocumentAttribute)range).getEnclosingElement();
				}
				else if (range instanceof IDocumentTextNode)
				{	range = ((IDocumentTextNode)range).getEnclosingElement();
				}

				if (range instanceof TocObject)
				{	fBlock.getMasterSection().setSelection(new StructuredSelection(range));
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IPDEUIConstants.PLUGIN_DOC_ROOT + "guide/tools/editors/toc_editor/page_toc.htm"; //$NON-NLS-1$
	}	
}

