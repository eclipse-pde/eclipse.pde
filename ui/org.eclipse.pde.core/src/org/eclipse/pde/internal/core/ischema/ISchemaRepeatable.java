package org.eclipse.pde.internal.core.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Classes that implement this interface store information
 * about objects that carry cardinality information.
 * In DTDs, cardinality is defined using special characters
 * ('?' for "0 to 1", '+' for "1 or more" and '*' for "0 or more".
 * XML Schema allows precise definition of the cardinality
 * by using minimum and maximum of occurences in the
 * instance document. This is one of the reasons why
 * it is not possible to create exact DTD representation
 * of XML Schema grammar.
 */
public interface ISchemaRepeatable {
/**
 * Returns maximal number of occurences of the object in the
 * instance document.
 *
 *@return maximal number of occurences in the document
 */
public int getMaxOccurs();
/**
 * Returns minimal number of occurences of the object in the
 * instance document.
 *
 *@return minimal number of occurences in the document
 */
public int getMinOccurs();
}
