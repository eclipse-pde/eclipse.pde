/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.ant.*;

/**
 * 
 */
public abstract class AbstractScriptGenerator implements IPDEBuildConstants, IXMLConstants {

/**
 * Starting point for script generation.
 */
public abstract void generate() throws CoreException;

protected static String getPropertyFormat(String propertyName) {
	StringBuffer sb = new StringBuffer();
	sb.append(PROPERTY_ASSIGNMENT_PREFIX);
	sb.append(propertyName);
	sb.append(PROPERTY_ASSIGNMENT_SUFFIX);
	return sb.toString();
}

}
