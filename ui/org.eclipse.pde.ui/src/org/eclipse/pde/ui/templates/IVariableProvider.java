package org.eclipse.pde.ui.templates;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * The classes that implement this interface are 
 * responsible for providing value of variables
 * when asked. Variables are defined by templates
 * and represent the current value of the template
 * options set by the users.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IVariableProvider {
	/**
	 * Returns the value of the variable with a given name.
	 * @param variable the name of the variable
	 * @return the value of the specified variable
	 * <p> 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public Object getValue(String variable);

}
