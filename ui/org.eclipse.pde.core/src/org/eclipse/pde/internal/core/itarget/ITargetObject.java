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
package org.eclipse.pde.internal.core.itarget;

import java.io.Serializable;

import org.eclipse.pde.core.IWritable;
import org.w3c.dom.Node;

public interface ITargetObject extends IWritable, Serializable {
	
	ITargetModel getModel();
	
	void setModel(ITargetModel model);
	
	ITarget getTarget();
	
	void parse(Node node);

}
