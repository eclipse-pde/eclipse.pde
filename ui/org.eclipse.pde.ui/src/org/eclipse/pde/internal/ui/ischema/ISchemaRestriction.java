package org.eclipse.pde.internal.ui.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;
/**
 * Simple schema types can optionally have a restriction
 * objects that narrows the value space for the type.
 * The restrictions introduce additional requirements
 * for the value to be considered valid for the type.
 * For example, enumeration restriction defines
 * a closed set of values that are legal for the type.
 */
public interface ISchemaRestriction extends ISchemaObject {
/**
 * Returns the simple type to which this restriction applies.
 * @return simple type to which this restriciton applies
 */
public ISchemaSimpleType getBaseType();
/**
 * Returns children of this restriction. Actual types
 * of the children depend on the restriction itself.
 * @return restriction children objects
 */
public Object[] getChildren();
/**
 * Tests if the provided value belongs to
 * the value set defined by this restriction.
 * @return true if the provided value
 * is valid for this restriction
 */
boolean isValueValid(Object value);
/**
 * Associates this restriction with the simple type object.
 *
 * @param baseType type object that owns this restriction
 */
public void setBaseType(ISchemaSimpleType baseType);
}
