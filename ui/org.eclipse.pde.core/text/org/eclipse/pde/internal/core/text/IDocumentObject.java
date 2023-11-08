/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
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

package org.eclipse.pde.internal.core.text;

import java.util.List;

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

	public void addChildNode(IDocumentElementNode child, boolean fireEvent);

	public void addChildNode(IDocumentElementNode child, int position, boolean fireEvent);

	public IDocumentElementNode clone(IDocumentElementNode node);

	public boolean getBooleanAttributeValue(String name, boolean defaultValue);

	public IDocumentElementNode getChildNode(Class<?> clazz);

	public int getChildNodeCount(Class<?> clazz);

	public List<IDocumentElementNode> getChildNodesList(Class<?> clazz, boolean match);

	public List<IDocumentElementNode> getChildNodesList(Class<?>[] classes, boolean match);

	public IDocumentElementNode getNextSibling(IDocumentElementNode node, Class<?> clazz);

	public IDocumentElementNode getPreviousSibling(IDocumentElementNode node, Class<?> clazz);

	public boolean hasChildNodes(Class<?> clazz);

	public boolean isFirstChildNode(IDocumentElementNode node, Class<?> clazz);

	public boolean isLastChildNode(IDocumentElementNode node, Class<?> clazz);

	public void moveChildNode(IDocumentElementNode node, int newRelativeIndex, boolean fireEvent);

	public IDocumentElementNode removeChildNode(int index, Class<?> clazz);

	public IDocumentElementNode removeChildNode(IDocumentElementNode child, boolean fireEvent);

	public IDocumentElementNode removeChildNode(int index, Class<?> clazz, boolean fireEvent);

	public boolean setBooleanAttributeValue(String name, boolean value);

	public void setChildNode(IDocumentElementNode newNode, Class<?> clazz);

	public void swap(IDocumentElementNode child1, IDocumentElementNode child2, boolean fireEvent);

}
