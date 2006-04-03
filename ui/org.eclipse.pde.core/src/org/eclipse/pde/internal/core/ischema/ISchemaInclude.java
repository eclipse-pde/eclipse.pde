/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

import org.eclipse.core.runtime.CoreException;

/**
 * Classes that implement this interface represent a schema that is included in
 * another schema.
 */
public interface ISchemaInclude extends ISchemaObject {
	/**
	 * Model property of the schema location.
	 */
	String P_LOCATION = "location"; //$NON-NLS-1$

	String getLocation();

	void setLocation(String location) throws CoreException;

	ISchema getIncludedSchema();

	void dispose();
}
