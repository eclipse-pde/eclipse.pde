/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;

/**
 * IDocumentObject
 *
 */
public interface IDocumentObject extends IDocumentElementNode, IWritable {

	public IModel getSharedModel();

	public void setSharedModel(IModel model);	
	
	public void reset();
	
	public boolean isInTheModel();
	
	public void setInTheModel(boolean inModel);
	
	public boolean isEditable();
	
}
