package org.eclipse.pde.internal.base.model.component;

import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.resources.IResource;
/**
 * This model type is designed to hold data loaded from
 * "install.xml" file of an Eclipse component.
 */
public interface IComponentModel extends IModel, IModelChangeProvider {
/**
 * Returns the top-level model object.
 *
 * @return top-level model object of the component model
 */
public IComponent getComponent();
/**
 * Returns the factory that should be used
 * to create new instances of model objects.
 *
 * @return component model factory
 */
IComponentModelFactory getFactory();
/**
 * Returns install location of the install.xml file
 * in case of external files.
 *
 * @return install location for external files,
 * or <samp>null</samp> for models based on
 * workspace resources.
 */
public String getInstallLocation();
/**
 * Tests whether this model is enabled.
 *
 * @return <samp>true</samp> if the model is enabled
 */
public boolean isEnabled();
/**
 * Enables or disables this model.
 *
 * @param enabled the new enable state
 */
public void setEnabled(boolean enabled);
}
