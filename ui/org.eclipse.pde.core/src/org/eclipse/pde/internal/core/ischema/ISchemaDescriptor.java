package org.eclipse.pde.internal.core.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
