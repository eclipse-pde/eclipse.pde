/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import org.eclipse.core.runtime.CoreException;

/**
 * Generic super-class for all script generator classes.
 */
public abstract class AbstractScriptGenerator implements IXMLConstants {

/**
 * Starting point for script generation. See subclass implementations for
 * individual comments.
 *  * @throws CoreException */
public abstract void generate() throws CoreException;

/**
 * Return a string with the given property name in the format:
 * <pre>${propertyName}</pre>.
 *  * @param propertyName the name of the property * @return String */
protected static String getPropertyFormat(String propertyName) {
	StringBuffer sb = new StringBuffer();
	sb.append(PROPERTY_ASSIGNMENT_PREFIX);
	sb.append(propertyName);
	sb.append(PROPERTY_ASSIGNMENT_SUFFIX);
	return sb.toString();
}

}
