package org.eclipse.pde.internal.base.schema;

import java.util.*;

/**
 * Objects that implement this interface can
 * have attributes.
 */
public interface ISchemaAttributeProvider {
/**
 * Returns an attribute definition if one with the matching name is found
 * in this provider.
 * @return attribute object or <samp>null</samp> if none with the matching name is found.
 */
public ISchemaAttribute getAttribute(String name);
	public int getAttributeCount();
	public ISchemaAttribute[] getAttributes();
}
