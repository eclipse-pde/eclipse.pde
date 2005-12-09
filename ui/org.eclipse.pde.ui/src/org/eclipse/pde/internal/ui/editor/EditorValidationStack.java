package org.eclipse.pde.internal.ui.editor;

import java.util.Stack;

import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.Form;

public class EditorValidationStack implements IEditorValidationStack {

	private PDEFormEditor fEditor;
	private Stack fStack = new Stack();
	
	public EditorValidationStack(PDEFormEditor editor) {
		fEditor = editor;
	}
	
	public void push(IEditorValidator validator) {
		if (validator == null)
			return;
		if (!fStack.contains(validator))
			fStack.push(validator);
		Form form = getForm();
		if (form == null)
			return;
		String message = validator.getMessage(true);
		if (message != null)
			form.setMessage(message, validator.getSeverity());
	}

	public IEditorValidator top(IEditorValidator callingValidator, IFormPage page) {
		Form form = getForm();
		if (form == null) return null;
		IEditorValidator top = getTopValidator();
		if (top == null || fStack.isEmpty())
			form.setMessage(null);
		else {
			boolean samePage = page == null || (
					page.equals(top.getSection().getPage())); 
			form.setMessage(top.getMessage(!samePage), top.getSeverity());
		}
		return top;
	}

	private IEditorValidator getTopValidator() {
		IEditorValidator currTop = (IEditorValidator)fStack.peek();
		while (currTop.inputValidates()) {
			fStack.pop();
			if (fStack.isEmpty())
				return null;
			currTop = (IEditorValidator)fStack.peek();
		}
		return currTop;
	}
	
	private Form getForm() {
		IFormPage page = fEditor.getActivePageInstance();
		if (page == null)
			return null;
		return page.getManagedForm().getForm().getForm();
	}
	
	public boolean isEmpty() {
		return fStack.isEmpty();
	}
}
