/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

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
