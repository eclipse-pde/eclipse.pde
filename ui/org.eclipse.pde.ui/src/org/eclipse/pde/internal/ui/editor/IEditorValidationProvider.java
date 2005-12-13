package org.eclipse.pde.internal.ui.editor;

/**
 * Classes that implement this interface will have the ability
 * to contriubte the an editors validator.
 *
 */
public interface IEditorValidationProvider {
	/**
	 * Set a validator assiciated with this object
	 * @param validator
	 */
	public void setValidator(IEditorValidator validator);
	/**
	 * Return the validator currently associated with this object
	 * @return
	 */
	public IEditorValidator getValidator() ;
	/**
	 * Validate this object.  This method will have to be called
	 * by the object itself, preferrably during the modification of
	 * that object's field.
	 *
	 */
	public void validate();
	/**
	 * Returns the input value of the object
	 * @return value
	 */
	public String getProviderValue();
	/**
	 * Returns the details on the contents of this provider.
	 * eg. label
	 * @return details/information
	 */
	public String getProviderDescription();
}
