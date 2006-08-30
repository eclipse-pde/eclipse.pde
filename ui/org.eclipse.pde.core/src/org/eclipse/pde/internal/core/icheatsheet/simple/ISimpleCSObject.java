/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.icheatsheet.simple;

import java.io.Serializable;

import org.eclipse.pde.core.IWritable;
import org.w3c.dom.Element;

/**
 * ISimpleCSObject
 *
 */
public interface ISimpleCSObject extends Serializable, IWritable,
		ISimpleCSConstants {

	/**
	 * @return
	 */
	ISimpleCSModel getModel();
	
	/**
	 * @param model
	 */
	void setModel(ISimpleCSModel model);
	
	/**
	 * @return
	 */
	ISimpleCS getSimpleCS();
	
	/**
	 * @param node
	 */
	void parse(Element element);	

	/**
	 * 
	 */
	public void reset();
	
	/**
	 * @return
	 */
	public int getType();
}
