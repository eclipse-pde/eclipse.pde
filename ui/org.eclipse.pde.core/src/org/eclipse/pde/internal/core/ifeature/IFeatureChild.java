package org.eclipse.pde.internal.core.ifeature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeatureChild extends IFeatureObject, IIdentifiable {
	String P_VERSION = "version";
	String getVersion();
	void setVersion(String version) throws CoreException;
}