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

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * SimpleCSPage
 *
 */
public class SimpleCSPage extends PDEFormPage implements IModelChangedListener {

	public static final String PAGE_ID = "simpleCSPage"; //$NON-NLS-1$
	
	private SimpleCSBlock fBlock;
	
	private IColorManager fColorManager;
	
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public SimpleCSPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.SimpleCSPage_0);
		fBlock = new SimpleCSBlock(this);
		fColorManager = ColorManager.getDefault();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		// TODO: MP: Scrolled form created here
		ScrolledForm form = managedForm.getForm();
		// Set page title
		ISimpleCSModel model = (ISimpleCSModel)getModel();
		// TODO: MP: This probably a very bad idea
		if (!model.isLoaded()) {
			throw new RuntimeException(PDEUIMessages.SimpleCSPage_1);
		}
		
		String title = model.getSimpleCS().getTitle();
		// TODO: MP: Check if model is null?
		if ((title != null) &&
				(title.length() > 0)) {
			// TODO: MP: LOW: Revisit if we need to translate at all
			form.setText(PDETextHelper.translateReadText(title));
		} else {
			// TODO: MP: Set on model?
			form.setText(PDEUIMessages.SimpleCSPage_0);
		}
		// Create the masters details block
		// TODO: MP: Scrolled form created here two
		fBlock.createContent(managedForm);
		// Force the selection in the masters tree section to load the 
		// proper details section
		fBlock.getMastersSection().fireSelection();
		// TODO: MP: Now that we got the selection working, probably should
		// force the tree to expand
		
		// Register this page to be informed of model change events
		model.addModelChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#dispose()
	 */
	public void dispose() {
		
		ISimpleCSModel simpleCSModel = (ISimpleCSModel)getModel();
		if (simpleCSModel != null) {
			// TODO: MP: model change listener
			//schema.removeModelChangedListener(this);
		}
		fColorManager.dispose();
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			// TODO: MP: LOW: Go through all changed objects or just first?
			Object[] objects = event.getChangedObjects();
			for (int i = 0; i < objects.length; i++) {
				ISimpleCSObject object = (ISimpleCSObject)objects[i];
				if (object == null) {
					// Ignore
				} else if (object.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
					String changeProperty = event.getChangedProperty();
					if ((changeProperty != null) && 
							changeProperty.equals(ISimpleCSConstants.ATTRIBUTE_TITLE)) {
						// Has to be a String if the property is a title
						// TODO: MP: LOW: Revisit if we need to translate at all
						getManagedForm().getForm().setText(
								PDETextHelper.translateReadText(
										(String)event.getNewValue()));
					}				 
				}
			}
		}
		// Inform the block
		fBlock.modelChanged(event);
	}
	
}
