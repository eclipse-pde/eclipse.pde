/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEMasterDetailsBlock;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * SimpleCSPage
 *
 */
public class SimpleCSPage extends CSAbstractPage implements IModelChangedListener {

	public static final String PAGE_ID = "simpleCSPage"; //$NON-NLS-1$
	
	private SimpleCSBlock fBlock;
	
	/**
	 * @param editor
	 */
	public SimpleCSPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.SimpleCSPage_0);
		fBlock = new SimpleCSBlock(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IPDEUIConstants.PLUGIN_DOC_ROOT + "guide/tools/editors/simple_cs_editor/editor.htm"; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		// Bug: Two veritical scrollbars appear when resizing the editor
		// vertically
		// Note: Scrolled form #1 created here
		ScrolledForm form = managedForm.getForm();
		// Set page title
		ISimpleCSModel model = (ISimpleCSModel)getModel();
		// Ensure the model was loaded properly
		if ((model == null) || 
				(model.isLoaded() == false)) {
			Exception e = null;
			if (model instanceof AbstractModel) {
				e = ((AbstractModel)model).getException();
			}
			// Create a formatted error page
			createFormErrorContent(managedForm, 
					PDEUIMessages.SimpleCSPage_msgCheatSheetLoadFailure, 
					PDEUIMessages.SimpleCSPage_msgCheatSheetParsingFailure, 
					e);
			return;
		}
		// Create the register cheat sheet link in the form title area
		createUIFormTitleRegisterCSLink(managedForm, model);		
		// Create the rest of the actions in the form title area
		super.createFormContent(managedForm);		
		// Form image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_CHEATSHEET_OBJ));
		// Form title
		String title = PDETextHelper.translateReadText(model.getSimpleCS()
				.getTitle());
		if (title.length() > 0) {
			form.setText(title);
		} else {
			form.setText(PDEUIMessages.SimpleCSPage_0);
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#dispose()
	 */
	public void dispose() {
		
		ISimpleCSModel simpleCSModel = (ISimpleCSModel)getModel();
		if (simpleCSModel != null) {
			simpleCSModel.removeModelChangedListener(this);
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object[] objects = event.getChangedObjects();
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
			handleModelEventWorldChanged(event);
		}
		// Inform the block
		fBlock.modelChanged(event);
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		
		Object[] objects = event.getChangedObjects();
		ISimpleCSObject object = (ISimpleCSObject) objects[0];		
		if (object == null) {
			// Ignore
			return;
		} else if (object.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
			String newValue = ((ISimpleCS)object).getTitle();
			// Update page title
			getManagedForm().getForm().setText(
					PDETextHelper.translateReadText(newValue));
		}
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
	
}
