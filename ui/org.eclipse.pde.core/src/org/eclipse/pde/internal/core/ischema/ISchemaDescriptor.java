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

import java.net.URL;

/**
 * Classes that implement this interface are responsible for holding the schema
 * object, loading it and disposing. Schema objects do not know where they are
 * coming from. Compositors are responsible to provide input streams for loading
 * and output streams for saving (if schema is editable).
 */
public interface ISchemaDescriptor {
	/**
	 * Returns identifier of the extension point defined in this schema.
	 * 
	 * @return id of the schema extension point
	 */
	public String getPointId();

	/**
	 * Returns the schema object. If schema has not been loaded, or has been
	 * previously disposed, this method will load it before returning. If
	 * abbreviated, the light-weight schema will not contain descriptions for
	 * elements or the content of documentation sections.
	 * 
	 * @return a loaded schema object
	 */
	ISchema getSchema(boolean abbreviated);

	/**
	 * Returns the URL of the schema XML file.
	 * 
	 * @return the URL of the schema XML file
	 */
	URL getSchemaURL();

	/**
	 * Tests if the descriptor is created outside the registry.
	 * 
	 * @return <code>true</code> if the descriptor is outside the registry,
	 *         <code>false</code> otherwise.
	 */
	boolean isStandalone();

	long getLastModified();
}
