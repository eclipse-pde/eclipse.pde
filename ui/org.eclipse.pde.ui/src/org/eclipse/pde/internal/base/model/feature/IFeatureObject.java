package org.eclipse.pde.internal.base.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.internal.base.model.IWritable;
/**
 * A base of all feature model objects.
 */
public interface IFeatureObject extends IWritable, IAdaptable {
/**
 * A property name that will be used to notify
 * about changes in the "label" field.
 */
String P_LABEL = "label";
/**
 * Returns the top-level component model object.
 * @return root feature object
 */
public IFeature getFeature();
/**
 * Returns the label of this feature model object'
 * @return feature object label
 */
String getLabel();
/**
 * Returns the feature model that owns this model object.
 *
 * @return the feature model
 */
IFeatureModel getModel();
/**
 * Returns the parent of this model object.
 *
 * @return the model object parent
 */
public IFeatureObject getParent();
/**
 * Sets the new label of this model object.
 * This method may throw a CoreException
 * if the model is not editable.
 *
 * @param label the new label
 */
void setLabel(String label) throws CoreException;
}
