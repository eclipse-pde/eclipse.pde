/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

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
