package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
/**
 * This factory should be used to create
 * instances of the plug-in model objects.
 */
public interface IPluginModelFactory {
/**
 * Creates a new attribute instance for the
 * provided element.
 *
 * @param element the parent element
 * @return the new attribute instance
 */
IPluginAttribute createAttribute(IPluginElement element);
/**
 * Creates a new element instance for the
 * provided parent.
 *
 * @param parent the parent element
 * @return the new element instance
 */
IPluginElement createElement(IPluginObject parent);
/**
 * Creates a new extension instance.
 * @return the new extension instance
 */
IPluginExtension createExtension();
/**
 * Creates a new extension point instance
 *
 * @return a new extension point 
 */
IPluginExtensionPoint createExtensionPoint();
/**
 * Creates a new plug-in import
 * @return a new plug-in import instance
 */
IPluginImport createImport();
/**
 * Creates a new library instance
 *
 *@return a new library instance
 */
IPluginLibrary createLibrary();
}
