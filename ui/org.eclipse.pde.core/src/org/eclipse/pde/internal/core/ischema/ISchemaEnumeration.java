package org.eclipse.pde.internal.core.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Objects of this type are carrying one enumeration choice
 * when defining enumeration restrictions for 'string' types.
 * For example, in restriction {"one", "two", "three"},
 * each choice is stored as one object of this type.
 * Choice name can be obtained by inherited 'getName()' method.
 */
public interface ISchemaEnumeration extends ISchemaObject {
}
