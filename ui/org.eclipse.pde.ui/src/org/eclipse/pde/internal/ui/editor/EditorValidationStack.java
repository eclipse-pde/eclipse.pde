/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.Stack;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.Form;

public class EditorValidationStack implements IEditorValidationStack {

	private PDEFormEditor fEditor;
	private Stack fStack = new Stack();
	
	public EditorValidationStack(PDEFormEditor editor) {
		fEditor = editor;
	}
	
	public void push(IEditorValidator validator) {
		Form form = getForm(validator);
		if (form != null && validator.markedInvalid()) {
			if (!fStack.contains(validator))
				fStack.push(validator);
			form.setMessage(validator.getMessage(), validator.getSeverity());
		}
	}

	public void top() {
		IEditorValidator top = getTopValidator();
		Form form = getForm(top);
		if (form == null)
			return;
		if (top == null)
			form.setMessage(null);
		else
			form.setMessage(top.getMessage(), top.getSeverity());
	}

	private IEditorValidator getTopValidator() {
		if (fStack.isEmpty()) return null;
		IEditorValidator currTop = (IEditorValidator)fStack.peek();
		while (!currTop.markedInvalid()) {
			fStack.pop();
			if (fStack.isEmpty())
				return null;
			currTop = (IEditorValidator)fStack.peek();
		}
		return currTop;
	}
	
	private Form getForm(IEditorValidator validator) {
		IFormPage page = fEditor.getActivePageInstance();
		if (validator != null && page == null)
			page = validator.getSection().getPage();
		if (page == null)
			return null;
		IManagedForm mform = page.getManagedForm();
		return mform == null ? null : mform.getForm().getForm();
	}
	
	public boolean isEmpty() {
		return fStack.isEmpty();
	}
}
