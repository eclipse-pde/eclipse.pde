/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.icheatsheet.comp;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

/**
 * ICompCSModel
 *
 */
public interface ICompCSModel extends IModelChangeProvider, IModel {

	ICompCS getCompCS();

	ICompCSModelFactory getFactory();

}
