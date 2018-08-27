/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

public interface IProductModel extends IModel, IModelChangeProvider {

	IProduct getProduct();

	IProductModelFactory getFactory();

	String getInstallLocation();

}
