package org.eclipse.pde.internal.base.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.resource.*;

/**
 * Objects that implement this interface store metadata about extension point
 * schema elements. This metadata is stored as schema element annotation.
 */
public interface IMetaElement {
	public ImageDescriptor getIconDescriptor();
/**
 * Returns a property (attribute) name whose value should
 * be used to load element icon in the UI. For example,
 * if icon property is set to "icon" and the element has
 * an "icon" attribute that represents icon path relative
 * to the plug-in, an attempt will be made to load that icon.
 */
public String getIconProperty();
/**
 * Returns a property (attribute) name whose value should
 * be used to represent this element in the UI. For example,
 * if this value is <samp>null</samp> and the name of the element
 * is "wizard", that will be showing in the UI. However,
 * if label property is set to "name" and the element has
 * a "name" attribute whose value is "Import Wizard",
 * that value will be used in the UI instead of "wizard".
 */
public String getLabelProperty();
}
