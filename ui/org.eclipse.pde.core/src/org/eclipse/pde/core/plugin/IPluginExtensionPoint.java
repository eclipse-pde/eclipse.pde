package org.eclipse.pde.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
/**
 * Classes that implement this interface model an extension point
 * element specified in the plug-in manifest.
 * <p>
 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IPluginExtensionPoint extends IPluginObject, IIdentifiable {
	/**
	 * A property name that will be used to notify
	 * about changes to the schema value.
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public static final String P_SCHEMA = "schema";
	/**
	 * Returns the full extension point Id that
	 * is composed as "pluginId.pointId". This full
	 * Id will be used by extensions to reference this
	 * extension point.
	 *
	 * @return a full extension point Id
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	String getFullId();
	/**
	 * Returns the name of the extension point XML schema
	 * that defines this extension point.
	 *
	 * @return XML extension point schema file name
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	String getSchema();
	/**
	 * Sets the plug-in relative name of
	 * the extension point schema file that
	 * describes this extension point.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void setSchema(String schema) throws CoreException;
}