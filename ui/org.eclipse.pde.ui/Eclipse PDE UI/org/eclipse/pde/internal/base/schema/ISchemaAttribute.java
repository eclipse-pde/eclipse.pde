package org.eclipse.pde.internal.base.schema;

/**
 * Objects that implement this interface hold data about
 * attributes of schema elements.
 */
public interface ISchemaAttribute extends ISchemaObject, IMetaAttribute {
	/**
	 * This attribute can be omitted by the extension element.
	 */
	public static final int OPTIONAL = 0;
	/**
	 * This attribute must be defined in the extension element.
	 */
	public static final int REQUIRED = 1;
	/**
	 * This attribute can be omitted by the extension element, and
	 * if it is, its value will be set to the value defined in the "value"
	 * field.
	 */
	public static final int DEFAULT = 2;
	/**
	 * Table of the 'use' clause choices.
 	 */
	public static final String [] useTable = { "optional", "required", "default" };
/**
 * Returns the type of this attribute. Attributes can only
 * have simple types.
 */
public ISchemaSimpleType getType();
/**
 * Returns the 'use' mode of this attribute (OPTIONAL, REQUIRED or DEFAULT).
 */
public int getUse();
/**
 * Returns the default value of this attribute when 'use' clause is DEFAULT.
 */
public Object getValue();
}
