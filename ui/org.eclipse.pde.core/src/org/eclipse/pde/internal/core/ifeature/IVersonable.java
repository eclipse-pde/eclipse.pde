/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.core.runtime.CoreException;

/**
 * @version 	1.0
 * @author
 */
public interface IVersonable extends IIdentifiable {
/**
 * A property that will be carried by the change event
 * if 'version' field of this object is changed.
 */
public static final String P_VERSION = "version";
/**
 * Returns a version of this object.
 * @return the version of this object
 */
public String getVersion();
/**
 * Sets the version of this IVersonable to the provided value.
 * This method will throw CoreException if
 * object is not editable.
 *
 *@param version a new version of this object
 */
void setVersion(String version) throws CoreException;
}
