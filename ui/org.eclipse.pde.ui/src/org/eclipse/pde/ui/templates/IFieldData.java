package org.eclipse.pde.ui.templates;

/**
 * Provides the values of the fields in the mandatory first
 * page of the template wizard. This interface allows 
 * templates to initialize options that are dependent on 
 * these values and cannot be initialized without context.
 */
public interface IFieldData {
	/**
	 * Returns the chosen name of the plug-in.
	 */
	String getName();
	/**
	 * Returns the chosen version of the plug-in.
	 */
	String getVersion();
	/**
	 * Returns the provider of the plug-in.
	 */
	String getProvider();
	/**
	 * Returns the name of the top-level plug-in class (if this
	 * class is specified) or <samp>null</samp> if not applicable.
	 */
	public String getClassName();
}