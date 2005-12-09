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
		String message = validator.getMessage();
		if (message != null)
			form.setMessage(message, validator.getSeverity());
	}

	public IEditorValidator top(IEditorValidator callingValidator) {
		if (fStack.isEmpty()) return null;
		Form form = getForm();
		if (form == null) return null;
		IEditorValidator top = getTopValidator();
		if (top == null)
			form.setMessage(null);
		else
			form.setMessage(top.getMessage(), top.getSeverity());
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
}
