package org.eclipse.pde.internal.base.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
/**
 * The reference to a plug-in that is part of this component.
 */
public interface IComponentReference extends IComponentObject, IIdentifiable {
/**
 * The name of the property that will be used to
 * notify about changes in the "version" field.
 */
	public static final String P_VERSION = "version";
/**
 * Returns the version of this reference.
 * Based on this version, plug-in or fragment directory will
 * be packaged into a component as [referenceId]_[version].
 * In case that the version of the original
 * does not match the version in the reference,
 * the reference version will be used.
 *
 * @return the version of this plug-in or fragment reference
 */
public String getVersion();
/**
 * Sets the version of this plug-in's reference.
 * This method may throw a CoreException if
 * the model is not editable.
 *
 * @param version the new version.
 */
public void setVersion(String version) throws CoreException;
}
