package org.eclipse.pde.internal.base.schema;

import org.eclipse.pde.internal.base.model.IWritable;
/**
 * Classes that implement this interface define
 * simple types. Simple types do not have compositors.
 * They consist of the base type whose name is
 * available by calling 'getName()' and an optional
 * restriction that can narrow the value space for the type.
 */
public interface ISchemaSimpleType extends ISchemaType, IWritable {
/**
 * Returns the restriction that narrows the value space of
 * this type.
 *
 * @return restriction for this simple type, or <samp>null</samp> if there
 * is no restriction.
 */
ISchemaRestriction getRestriction();
}
