package org.eclipse.pde.internal.base.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.resources.IResource;
/**
 * This model is created from the "plugin.jars" file
 * that defines what source folders in the plug-in are
 * to be used to build require plug-in Jars.
 * <p>
 * If this model is editable, isEditable() will return
 * true and the model instance will implement IEditable
 * interface. The model is capable of providing
 * change notification for the registered listeners.
 */
public interface IJarsModel extends IModel, IModelChangeProvider {
/**
 * Returns the factory that should be used
 * to create new instance of model objects.
 * @return the plugin.jars model factory
 */
IJarsModelFactory getFactory();
/**
 * Returns the location of the file
 * used to create the model.
 *
 * @return the location of the plugin.jars file
 * or <samp>null</samp> if the file
 * is in a workspace.
 */
public String getInstallLocation();
/**
 * Returns the top-level model object of this model.
 *
 * @return a plugin.jars top-level model object
 */
IJars getJars();
/**
 * @return boolean
 */
boolean isFragment();
}
