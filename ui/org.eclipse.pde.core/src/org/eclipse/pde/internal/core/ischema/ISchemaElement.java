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
 * Classes that implement this interface represent definition
 * of one element in the extension point schema.
 * Elements are defined at the global scope and contain
 * type (typically complex types with compositors) and
 * attribute definitions.
 */
public interface ISchemaElement extends ISchemaObject, ISchemaRepeatable, ISchemaAttributeProvider, IMetaElement, Comparable {
	/**
	 * Returns an approximate representation of this element's content
	 * model in DTD form. The resulting representation may not
	 * be accurate because XML schema is more powerful and
	 * provides for grammar definitions that are not possible
	 * with DTDs.
	 * 
	 * param addLinks if true, the representation will contain
	 * HTML tags for quick access to referenced elements.
	 *
	 *@return DTD approximation of this element's grammar
	 */
	String getDTDRepresentation(boolean addLinks);

	/**
	 * Returns type object that represents the type defined in this element.
	 * The type can be simple (defining an element that can only contain text)
	 * or complex (with attributes and/or compositors).
	 */
	public ISchemaType getType();

	/**
	 * Returns the names of the element's attributes. Placed here instead of ISchemaAttributeProvider
	 * so that SchemaComplexType does not need to implement needlessly.
	 */
	public String[] getAttributeNames();
}
