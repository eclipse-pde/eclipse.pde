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
package org.eclipse.pde.internal.core.ischema;

import java.net.*;
/**
 * Classes that implement this interface are responsible for
 * holding the schema object, loading it and disposing.
 * Schema objects do not know where they are coming from.
 * Compositors are responsible to provide input streams for
 * loading and output streams for saving (if schema is editable).
 */
public interface ISchemaDescriptor {
/**
 * Returns identifier of the extension point defined in this schema.
 * @return id of the schema extension point
 */
public String getPointId();
/**
 * Returns the schema object. If schema has not been loaded,
 * or has been previously disposed, this method will load it
 * before returning.
 * @return a loaded schema object
 */
ISchema getSchema();
/**
 * Returns the URL of the schema XML file.
 * @return the URL of the schema XML file
 */
public URL getSchemaURL();
}
