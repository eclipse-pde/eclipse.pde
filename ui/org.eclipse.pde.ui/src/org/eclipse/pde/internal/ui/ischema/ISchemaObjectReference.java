package org.eclipse.pde.internal.ui.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Classes that implement this interface store a reference
 * to a schema object defined elsewhere.
 */
public interface ISchemaObjectReference {
/**
 * Returns a name of this reference.
 * @return reference object name
 */
public String getName();
/**
 * Returns a schema object that is referenced by this object.
 * @return referenced schema object
 */
public ISchemaObject getReferencedObject();
/**
 * Returns a real Java class of the referenced object.
 * @return Java class of the referenced object.
 */
public Class getReferencedObjectClass();
/**
 * Associates this reference with a schema object.
 * @param referencedObject associates this reference with the object it references
 */
public void setReferencedObject(ISchemaObject referencedObject);
}
