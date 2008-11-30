/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple;

import java.util.List;

import org.eclipse.pde.internal.core.text.IDocumentObject;
import org.w3c.dom.Element;

public interface ISimpleCSObject extends IDocumentObject, ISimpleCSConstants {

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
	 * To avoid using instanceof all over the place
	 * 
	 * @return
	 */
	public int getType();

	/**
	 * For the label provider
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * For the content provider
	 * 
	 * @return A empty / non-empty list - never null
	 */
	public List getChildren();

	/**
	 * @return
	 */
	public ISimpleCSObject getParent();

}
