package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.base.model.*;

/**
 * Classes that implement this interface model the extension
 * element found in the plug-in manifest.
 */
public interface IPluginExtension extends IPluginParent, IIdentifiable {
/**
 * A name of the property that will be used to
 * notify about the "point" change
 */
	public static final String P_POINT = "point";
/**
 * Returns the full Id of the extension point that this extension
 * is plugged into.
 */
String getPoint();
/**
 * Returns the schema object that corresponds to
 * the extension point that this extension plugs into.
 * @return a matching extension point schema object
 */
public ISchema getSchema();
/**
 * Sets the value of the extension point Id
 * This method will throw a CoreException if
 * this model is not editable.
 * @param point the new extension point Id
 */
void setPoint(String point) throws CoreException;
}
