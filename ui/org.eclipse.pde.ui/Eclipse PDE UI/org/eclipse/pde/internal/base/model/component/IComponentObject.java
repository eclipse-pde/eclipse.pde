package org.eclipse.pde.internal.base.model.component;

import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.internal.base.model.IWritable;
/**
 * A base of all component model objects.
 */
public interface IComponentObject extends IWritable, IAdaptable {
/**
 * A property name that will be used to notify
 * about changes in the "label" field.
 */
String P_LABEL = "label";
/**
 * Returns the top-level component model object.
 * @return root component object
 */
public IComponent getComponent();
/**
 * Returns the label of this component model object'
 * @return component object label
 */
String getLabel();
/**
 * Returns the component model that owns this model object.
 *
 * @return the component model
 */
IComponentModel getModel();
/**
 * Returns the parent of this model object.
 *
 * @return the model object parent
 */
public IComponentObject getParent();
/**
 * Sets the new label of this model object.
 * This method may throw a CoreException
 * if the model is not editable.
 *
 * @param label the new label
 */
void setLabel(String label) throws CoreException;
}
