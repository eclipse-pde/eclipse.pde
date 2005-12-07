package org.eclipse.pde.internal.ui.editor;

/**
 * IEditorValidator is used to validate fields in an editor
 */
public interface IEditorValidator {

	/**
	 * Indicates whether this validator is currently enabled.
	 * @return true if this validator is enabled.
	 */
	public boolean isEnabled();
	/**
	 * Sets the enabled state of this validator
	 * @param enable
	 */
	public void setEnabled(boolean enable);
	/**
	 * Validates the editor's field and updates the error
	 * stack if neccesary.
	 * @return true if this form object has a valid entry.
	 */
	public boolean validate(boolean revalidate);
	/**
	 * Validates the an object versus it's model.
	 * Message and Severity must be set/unset during this method.
	 * @return true if this form object has a valid entry.
	 */
	public boolean inputValidates();
	/**
	 * Returns a message to the user indicating the problem
	 * with the validation;
	 * @return the message
	 */
	public String getMessage();
	public void setMessage(String message);
	/**
	 * Returns the severity (if any) of the form object's entry's
	 * validation result.
	 * @return severity
	 */
	public int getSeverity();
	public void setSeverity(int severity);
	/**
	 * Returns the section in which the validator was instantiated
	 * @return section
	 */
	public PDESection getSection();
}
