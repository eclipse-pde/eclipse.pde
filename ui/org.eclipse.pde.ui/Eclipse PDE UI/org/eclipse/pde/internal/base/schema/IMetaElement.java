package org.eclipse.pde.internal.base.schema;

import org.eclipse.jface.resource.*;

/**
 * Objects that implement this interface store metadata about extension point
 * schema elements. This metadata is stored as schema element annotation.
 */
public interface IMetaElement {
	public ImageDescriptor getIconDescriptor();
/**
 * Returns optional name of the icon that should be used to
 * represent this element in the UI, or <samp>null</samp> if default
 * icon should be used.
 */
public String getIconName();
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
