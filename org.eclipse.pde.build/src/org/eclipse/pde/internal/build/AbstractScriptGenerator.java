package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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

protected String getPropertyFormat(String propertyName) {
	StringBuffer sb = new StringBuffer();
	sb.append(PROPERTY_ASSIGNMENT_PREFIX);
	sb.append(propertyName);
	sb.append(PROPERTY_ASSIGNMENT_SUFFIX);
	return sb.toString();
}

}
