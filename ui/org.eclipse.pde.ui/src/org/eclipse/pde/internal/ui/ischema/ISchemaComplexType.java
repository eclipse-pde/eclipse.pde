package org.eclipse.pde.internal.ui.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.IWritable;
/**
 * Objects of this type are holding information about complex types defined
 * inside schema elements.
 */
public interface ISchemaComplexType extends ISchemaType, ISchemaAttributeProvider, IWritable {
/**
 * A complex type can have one root compositor.
 @ return root complex type compositor
 */
public ISchemaCompositor getCompositor();
/**
 * Returns whether the content of the element that owns this type
 * can mix child elements and text.
 *@return true if element can mix text and other elements
 */
public boolean isMixed();
}
