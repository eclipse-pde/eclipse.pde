package org.eclipse.pde.internal.ui.editor;

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
	 * @return
	 */
	public IEditorValidator top(IEditorValidator provider);
}
