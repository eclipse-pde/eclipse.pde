package org.eclipse.pde.internal.base.model.plugin;

import org.eclipse.pde.internal.base.model.build.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.resources.IResource;
/**
 * This type of model is created by parsing the plugin.xml file.
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
public interface IPluginModelBase extends IModel, IModelChangeProvider {
/**
 * Insert the method's description here.
 * Creation date: (5/8/2001 7:50:18 PM)
 * @return org.eclipse.pde.internal.base.model.plugin.IPluginBase
 */
IPluginBase createPluginBase();
/**
 * Returns an associated plugin.jars model
 * that works in conjunction with this model.
 *
 * @return the matching plugin.jars model
 */
IBuildModel getBuildModel();
/**
 * Returns a factory object that should be used
 * to create new instances of the model objects.
 */
IPluginModelFactory getFactory();
/**
 * Returns a location of the file that was used
 * to create this model. This property is used
 * only for external models.
 *
 * @return a location of the external model, or
 * <samp>null</samp> if the model is created
 * from a resource.
 */
public String getInstallLocation();
/**
 * Insert the method's description here.
 * Creation date: (5/8/2001 7:50:02 PM)
 * @return org.eclipse.pde.internal.base.model.plugin.IPluginBase
 */
IPluginBase getPluginBase();
/**
 * Returns </samp>true</samp> if this model is currently enabled.
 *
 *@return true if the model is enabled
 */
public boolean isEnabled();
/**
 * Insert the method's description here.
 * Creation date: (5/8/2001 7:57:38 PM)
 * @return boolean
 */
boolean isFragmentModel();
/**
 * Sets the enable state of the model.
 *
 * @param enabled the new enable state
 */
public void setEnabled(boolean enabled);
}
