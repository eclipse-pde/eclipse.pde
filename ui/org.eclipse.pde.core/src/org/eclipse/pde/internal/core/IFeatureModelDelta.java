/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public interface IFeatureModelDelta {

	public static final int ADDED = 1;

	public static final int REMOVED = 2;

	public static final int CHANGED = 4;

	public abstract int getKind();

	public abstract IFeatureModel[] getAdded();

	public abstract IFeatureModel[] getRemoved();

	public abstract IFeatureModel[] getChanged();

}