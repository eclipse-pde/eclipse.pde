package org.eclipse.pde.internal.ui.editor;

import org.eclipse.ui.forms.editor.IFormPage;

/**
 * A stack used to keep track of validating fields in a form.
 * Validations are pushed to the top so that the most recently discovered
 * has highest priority.
 * @author janeklb
 *
 */
public interface IEditorValidationStack {
	
	/**
	 * Push a validation provider on top of the stack.
	 * @param provider
	 */
	public void push(IEditorValidator provider);
	
	/**
	 * Get the next false-validating provider.
	 * @param provider the provider which is currently being validated
	 * @param page the page from which this is getting called
	 * @return
	 */
	public IEditorValidator top(IEditorValidator provider, IFormPage page);
	
	/**
	 * Returns if the validation stack is empty;
	 * @return
	 */
	public boolean isEmpty();
}
