package org.eclipse.pde.internal.core.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Objects that implement this interface are carrying metadata about
 * XML schema attributes. This data is stored as schema attribute
 * annotations.
 */
public interface IMetaAttribute {
	/**
	 * Indicates that the value of the associated attribute is a regular string.
	 */
	public static final int STRING = 0;
	/**
	 * Indicates that the value of the associated attribute is a name of a fully qualified Java class.
	 */
	public static final int JAVA = 1;
	/**
	 * Indicates that the value of the associated attribute is a workspace resource.
	 */
	public static final int RESOURCE = 2;
	/*
	 * non-Javadoc
	 */
	public static final String [] kindTable = { "string", "java", "resource" };

/**
 * Returns optional name of the Java type this type must be based on (only for JAVA kind).
 */
public String getBasedOn();
/**
 * Returns the kind of this attribute's value (STRING, JAVA or RESOURCE).
 */
public int getKind();
}
