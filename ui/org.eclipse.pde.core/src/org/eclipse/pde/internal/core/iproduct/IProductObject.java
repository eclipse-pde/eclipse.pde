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
package org.eclipse.pde.internal.core.iproduct;

import java.io.*;

import org.eclipse.pde.core.*;
import org.w3c.dom.*;


public interface IProductObject extends IWritable, Serializable{
	
	IProductModel getModel();
	
	void setModel(IProductModel model);
	
	IProduct getProduct();
	
	boolean isValid();
	
	void parse(Node node);
	
}
