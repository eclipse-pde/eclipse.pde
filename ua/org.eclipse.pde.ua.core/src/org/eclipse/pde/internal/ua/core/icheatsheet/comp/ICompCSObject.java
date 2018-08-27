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

import java.io.Serializable;
import java.util.List;

import org.eclipse.pde.core.IWritable;
import org.w3c.dom.Element;

public interface ICompCSObject extends Serializable, IWritable,
		ICompCSConstants {

	ICompCSModel getModel();

	void setModel(ICompCSModel model);

	ICompCS getCompCS();

	void parse(Element element);

	public void reset();

	/**
	 * To avoid using instanceof all over the place
	 */
	public int getType();

	/**
	 * For the label provider
	 */
	public String getName();

	/**
	 * For the content provider
	 *
	 * @return A empty / non-empty list - never null
	 */
	public List<? extends ICompCSObject> getChildren();

	public ICompCSObject getParent();

	public String getElement();

}
