package org.eclipse.pde.internal.core.isite;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.Serializable;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IWritable;
/**
 * A base of all site model objects.
 */
public interface ISiteBuildObject extends IWritable, IAdaptable, Serializable {
/**
 * Returns the top-level site model object.
 * @return root feature object
 */
public ISiteBuild getSiteBuild();
/**
 * Returns the site model that owns this model object.
 *
 * @return the site model
 */
ISiteBuildModel getModel();

boolean isInTheModel();
/**
 * Returns the parent of this model object.
 *
 * @return the model object parent
 */
public ISiteBuildObject getParent();
}
