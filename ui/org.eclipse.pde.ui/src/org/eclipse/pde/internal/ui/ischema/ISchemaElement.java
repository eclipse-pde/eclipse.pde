package org.eclipse.pde.internal.ui.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
/**
 * Classes that implement this interface represent definition
 * of one element in the extension point schema.
 * Elements are defined at the global scope and contain
 * type (typically complex types with compositors) and
 * attribute definitions.
 */
public interface ISchemaElement extends ISchemaObject, ISchemaRepeatable, ISchemaAttributeProvider, IMetaElement {
/**
 * Returns an approximate representation of this element's content
 * model in DTD form. The resulting representation may not
 * be accurate because XML schema is more powerful and
 * provides for grammar definitions that are not possible
 * with DTDs.
 *
 *@return DTD approximation of this element's grammar
 */
String getDTDRepresentation();
/**
 * Returns type object that represents the type defined in this element.
 * The type can be simple (defining an element that can only contain text)
 * or complex (with attributes and/or compositors).
 */	
public ISchemaType getType();
}
