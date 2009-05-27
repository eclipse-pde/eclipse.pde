/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

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
