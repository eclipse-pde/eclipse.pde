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

import java.io.Serializable;
import org.eclipse.pde.core.IWritable;
import org.w3c.dom.Node;

public interface IProductObject extends IWritable, Serializable {

	IProductModel getModel();

	void setModel(IProductModel model);

	IProduct getProduct();

	void parse(Node node);

}
