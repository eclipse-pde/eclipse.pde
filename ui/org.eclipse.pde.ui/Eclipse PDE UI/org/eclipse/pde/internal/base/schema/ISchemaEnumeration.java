package org.eclipse.pde.internal.base.schema;

/**
 * Objects of this type are carrying one enumeration choice
 * when defining enumeration restrictions for 'string' types.
 * For example, in restriction {"one", "two", "three"},
 * each choice is stored as one object of this type.
 * Choice name can be obtained by inherited 'getName()' method.
 */
public interface ISchemaEnumeration extends ISchemaObject {
}
