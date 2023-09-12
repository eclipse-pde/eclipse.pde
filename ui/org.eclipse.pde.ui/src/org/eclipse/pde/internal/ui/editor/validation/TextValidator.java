/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.validation;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;

public abstract class TextValidator extends AbstractControlValidator {

	private ModifyListener fModifyListener;

	private final boolean fAutoValidate;

	private String fCurrentText;

	public TextValidator(IManagedForm managedForm, Text control, IProject project, boolean autoValidate) {
		super(managedForm, control, project);
		// Turn on / off auto-validation
		// If auto-validation is on, validation is triggered by modify text
		// events.  Otherwise, manual calls to validate must be made.
		fAutoValidate = autoValidate;
		// Initialize the text validator
		intialize();
	}

	private void intialize() {
		// Save the current contents of the Text field
		fCurrentText = getText().getText();
		// No listeners required if auto-validation is off
		if (fAutoValidate == false) {
			return;
		}
		// Create the listeners for auto-validation
		createListeners();
		// Add the listeners if the validator is enabled
		if (getEnabled()) {
			addListeners();
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		// Nothing to do here if enablement is not being changed
		if (getEnabled() == enabled) {
			return;
		}
		// Update validator enablement
		super.setEnabled(enabled);
		// No listeners required if auto-validation is off
		if (fAutoValidate == false) {
			return;
		}
		// Add listeners if enabled; otherwise, remove them
		if (getEnabled()) {
			addListeners();
		} else {
			removeListeners();
		}
	}

	protected void createListeners() {
		fModifyListener = this::handleModifyTextEvent;
	}

	protected void handleModifyTextEvent(ModifyEvent e) {
		// Validation is not required if the current text contents is the
		// same as the new text contents
		String newText = getText().getText();
		if (newText.equals(fCurrentText)) {
			return;
		}
		// Save the current contents of the Text field
		fCurrentText = newText;
		// Perform auto-validation
		validate();
	}

	protected void addListeners() {
		getText().addModifyListener(fModifyListener);
	}

	protected void removeListeners() {
		getText().removeModifyListener(fModifyListener);
	}

	protected Text getText() {
		return (Text) getControl();
	}

	@Override
	protected boolean autoEnable() {
		// Enable validator if the text field is editable
		if (getText().getEditable() == false) {
			return false;
		}
		return super.autoEnable();
	}
}
