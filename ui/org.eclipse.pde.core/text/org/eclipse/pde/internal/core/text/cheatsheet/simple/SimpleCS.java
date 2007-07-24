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

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.text.IDocumentNode;

/**
 * SimpleCS
 *
 */
public class SimpleCS extends SimpleCSObject implements ISimpleCS {

	private static final long serialVersionUID = 1L;

	// TODO: MP: TEO: Allow generics and take them into account for calculations
	
	/**
	 * 
	 */
	public SimpleCS(ISimpleCSModel model) {
		super(model);
		reset();
		
		setXMLTagName(ELEMENT_CHEATSHEET); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentObject#reset()
	 */
	public void reset() {
		super.reset();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#addItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void addItem(ISimpleCSItem item) {
		addChildNode((IDocumentNode)item);
		if (shouldFireEvent()) {
			fireStructureChanged(item, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#addItem(int, org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void addItem(int index, ISimpleCSItem item) {
		addChildNode((IDocumentNode)item, index);
		if (shouldFireEvent()) {
			fireStructureChanged(item, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getIntro()
	 */
	public ISimpleCSIntro getIntro() {
		// TODO: MP: TEO: Enhance performance
		// Linear search O(n)
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			if (node instanceof ISimpleCSIntro) {
				return (ISimpleCSIntro)node;
			}
		}
	
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getItemCount()
	 */
	public int getItemCount() {
		// TODO: MP: TEO: Enhance performance
		// Linear search O(n)
		int count = 0;
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			if (node instanceof ISimpleCSItem) {
				// TODO: TEO: Test with elements not items or intros
				count++;
			}
		}		
		return count;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getItems()
	 */
	public ISimpleCSItem[] getItems() {
		ArrayList items = new ArrayList();
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			if (node instanceof ISimpleCSItem) {
				items.add(node);
			}
		}		
		return (ISimpleCSItem[])items.toArray(new ISimpleCSItem[items.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getNextSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public ISimpleCSItem getNextSibling(ISimpleCSItem item) {

		int position = indexOf((IDocumentNode)item);
		int lastIndex = getChildCount() - 1;
		if ((position == -1) ||
				(position == lastIndex)) {
			// Either the item was not found or the item was found but it is 
			// at the last index
			return null;
		}
		// Ensure the next node is an item
		IDocumentNode nextSiblingNode = getChildAt(position + 1);
		if (nextSiblingNode instanceof ISimpleCSItem) {
			return (ISimpleCSItem)nextSiblingNode;
		}
		// Must be the intro
		return null;	
		// TODO: MP: TEO: Test model throws errors when foreign nodes found
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getPreviousSibling(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public ISimpleCSItem getPreviousSibling(ISimpleCSItem item) {

		int position = indexOf((IDocumentNode)item);
		if ((position == -1) ||
				(position == 0)) {
			// Either the item was not found or the item was found but it is 
			// at the first index
			return null;
		}
		// Ensure the previous node is an item
		IDocumentNode previousSiblingNode = getChildAt(position - 1);
		if (previousSiblingNode instanceof ISimpleCSItem) {
			return (ISimpleCSItem)previousSiblingNode;
		}
		// Must be an intro
		return null;			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getTitle()
	 */
	public String getTitle() {
		return getXMLAttributeValue(ATTRIBUTE_TITLE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#hasItems()
	 */
	public boolean hasItems() {
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			if (node instanceof ISimpleCSItem) {
				return true;
			}
		}		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#indexOfItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public int indexOfItem(ISimpleCSItem item) {
		return indexOf((IDocumentNode)item);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#isFirstItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public boolean isFirstItem(ISimpleCSItem item) {
		int position = indexOfItem(item);
		if (position == -1) {
			return false;
		} 
		
		for (int i = 0; i < getChildCount(); i++) {
			if ((getChildAt(i) instanceof ISimpleCSItem) == false) {
				// Intro node
				continue;
			} else if (i == position) {
				// Item node at position
				return true;
			} else {
				// Item node not at position
				return false;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#isLastItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public boolean isLastItem(ISimpleCSItem item) {
		int position = indexOfItem(item);
		if (position == -1) {
			return false;
		} 
		int lastPosition = getChildCount() - 1;
		for (int i = lastPosition; i >= 0; i--) {
			if ((getChildAt(i) instanceof ISimpleCSItem) == false) {
				// Intro node
				continue;
			} else if (i == position) {
				// Item node at position
				return true;
			} else {
				// Item node not at position
				return false;
			}
		}
		return false;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#moveItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem, int)
	 */
	public void moveItem(ISimpleCSItem item, int newRelativeIndex) {

		// Get the current index of the task object
		int currentIndex = indexOfItem(item);
		// Ensure the object is found
		if (currentIndex == -1) {
			return;
		}
		// Calculate the new index
		int newIndex = newRelativeIndex + currentIndex;
		// Validate the new index
		if ((newIndex < 0) ||
				(newIndex >= getChildCount()) ||
				((getChildAt(newIndex) instanceof ISimpleCSItem) == false)) {
			// 0 <= newIndex < child element count
			// Cannot move an item into a place occupied by an Intro node
			// TODO: MP: TEO: Test
			return;
		}
		// Remove the task object
		removeChildNode((IDocumentNode)item);
		// Add the task object back at the specified index
		addChildNode((IDocumentNode)item, newIndex);
		// Send an insert event
		if (shouldFireEvent()) {
			fireStructureChanged(item, IModelChangedEvent.INSERT);
		}			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#removeItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem)
	 */
	public void removeItem(ISimpleCSItem item) {
		removeChildNode((IDocumentNode)item);
		
		if (shouldFireEvent()) {
			fireStructureChanged(item, IModelChangedEvent.REMOVE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#removeItem(int)
	 */
	public void removeItem(int index) {

		if ((index < 0) ||
				(index > (getChildCount() - 1)) ||
				((getChildAt(index) instanceof ISimpleCSItem) == false)) {
			// 0 <= index < child element count
			// Cannot remove an intro node with this method			
			return;
		}		
		
		ISimpleCSItem item = (ISimpleCSItem)removeChildNode(index);
		
		if (shouldFireEvent()) {
			fireStructureChanged(item, IModelChangedEvent.REMOVE);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#setIntro(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro)
	 */
	public void setIntro(ISimpleCSIntro intro) {
		// Get the old intro
		Object oldIntro = getIntro();
		// Remove the old intro if it is defined
		if (oldIntro != null) {
			removeChildNode((IDocumentNode)oldIntro);
		}			
		// If the new intro is not defined, nothing to do
		if (intro == null) {
			// No intro
			return;
		}
		// Add new intro
		// Add the new intro as the first child
		addChildNode((IDocumentNode)intro, 0);
		
		if (shouldFireEvent()) {
			fireStructureChanged(intro, oldIntro);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		String oldValue = getXMLAttributeValue(ATTRIBUTE_TITLE);
		setXMLAttribute(ATTRIBUTE_TITLE, title);
		// Fire an event if in the model
		if (shouldFireEvent()) {
			firePropertyChanged(ATTRIBUTE_TITLE, oldValue, title);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		return getTitle();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return getChildNodesList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_CHEAT_SHEET;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#write(boolean)
	 */
	public String write(boolean indent) {
		// Print XML decl
		// TODO: MP: TEO: Extract into super class method?
		StringBuffer buffer = new StringBuffer(XMLPrintHandler.XML_HEAD);
		buffer.append(ATTRIBUTE_VALUE_ENCODING);
		buffer.append(XMLPrintHandler.XML_DBL_QUOTES);
		buffer.append(XMLPrintHandler.XML_HEAD_END_TAG);
		buffer.append(getLineDelimiter());
		
		return super.write(indent);
	}
	
}
