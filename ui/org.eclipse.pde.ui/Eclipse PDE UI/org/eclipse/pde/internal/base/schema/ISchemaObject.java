package org.eclipse.pde.internal.base.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.IAdaptable;
/**
 * Base interface for all objects that belong to the extension point
 * schema model.
 */
public interface ISchemaObject extends IAdaptable, IWritable {
/**
 * Property constant that will be used in the model change event
 * when description field of this object changes.
 */	
public static final String P_DESCRIPTION="description";
/**
 * Property constant that will be used in the model change event
 * when "name" field of this object changes.
 */		
public static final String P_NAME = "name";
/**
 * Returns text associated with this schema object. Typically, it is
 * annotation that will be used to compose the reference HTML documentation.
 * The text may contain HTML tags.
 */	
public String getDescription();
/**
 * Returns the presentation name of this schema object.
 */	
public String getName();
/**
 * Returns the parent of this schema object.
 */
ISchemaObject getParent();
/**
 * Returns the schema object to which this object belongs.
 */	
public ISchema getSchema();
}
