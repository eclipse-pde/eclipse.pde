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
 */
public interface IVariableProvider {
/**
 * Returns the value of the variable with a given name.
 * @param variable the name of the variable
 * @return the value of the specified variable
 */
	public Object getValue(String variable);

}
