package org.eclipse.pde.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.resources.IResource;
/**
 * This type of model is created by parsing the fragment.xml file.
 * If the file is a workspace resource, it will be
 * available as the underlying resource of the model.
 * The model may be read-only or editable.
 * It will also make a reference to the plugin.jars
 * model when created. The reference will be of the
 * same type as the model itself: if the model is
 * editable, it will attempt to obtain an exclusive
 * editable copy of plugin.jars model.
 * <p>
 * The plug-in model can be disabled. Disabling the
 * model will not change its data. Users of the
 * model will have to decide if the disabled state
 * if of any importance to them or not.
 * <p>
 * The model is capable of notifying listeners
 * about changes. An attempt to change a read-only
 * model will result in a CoreException.
 */
public interface IFragmentModel extends IPluginModelBase {
	/**
	 * Returns a top-level model object.
	 *
	 * @return a root model instance
	 */
	public IFragment getFragment();
}