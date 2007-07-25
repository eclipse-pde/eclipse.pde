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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode;

/**
 * DocumentObject
 *
 */
public abstract class DocumentObject extends PluginDocumentNode implements
		IDocumentObject {

	// TODO: MP: TEO: Consider renaming class
	
	// TODO: MP: TEO: Integrate with plugin model?
	
	// TODO: MP: TEO: Investigate document node to see if any methods to pull down
	
	private transient IModel fModel;
	
	private transient boolean fInTheModel;
	
	/**
	 * @param model
	 * @param tagName
	 */
	public DocumentObject(IModel model, String tagName) {
		super();
		
		fModel = model;
		fInTheModel = false;
		setXMLTagName(tagName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#setSharedModel(org.eclipse.pde.core.IModel)
	 */
	public void setSharedModel(IModel model) {
		fModel = model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#getSharedModel()
	 */
	public IModel getSharedModel() {
		return fModel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#reset()
	 */
	public void reset() {
		// TODO: MP: TEO: reset parent fields? or super.reset?
		fModel = null;
		fInTheModel = false;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return fInTheModel;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
		fInTheModel = inModel;
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#isEditable()
	 */
	public boolean isEditable() {
		// Convenience method
		return fModel.isEditable();
	}
	
	/**
	 * @return
	 */
	protected boolean shouldFireEvent() {
		if (isInTheModel() && isEditable()) {
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#getLineDelimiter()
	 */
	protected String getLineDelimiter() {
		if (fModel instanceof IEditingModel) {
			IDocument document = ((IEditingModel)fModel).getDocument();
			return TextUtilities.getDefaultLineDelimiter(document);
		}
		return super.getLineDelimiter();
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, org.eclipse.pde.internal.core.ischema.ISchema, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void reconnect(IDocumentNode parent, IModel model) {
		super.reconnect(parent, model);
		// Transient field:  In The Model
		// Value set to true when added to the parent
		// TODO: MP: TEO: Need to set recursively to true on add for children? Only affects parent?
		fInTheModel = false;
		// Transient field:  Model
		fModel = model;
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return write(false);
	}	
	
	/**
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChanged(String property, Object oldValue,
			Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	/**
	 * @param object
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	private void firePropertyChanged(Object object, String property,
		Object oldValue, Object newValue) {
		if (fModel.isEditable() && 
				(fModel instanceof IModelChangeProvider)) {
			IModelChangeProvider provider = (IModelChangeProvider)fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}	
	
	/**
	 * @param newValue
	 * @param oldValue
	 */
	protected void fireStructureChanged(Object newValue,
			Object oldValue) {

		int changeType = -1;
		Object object = null;
		if (newValue == null) {
			changeType = IModelChangedEvent.REMOVE;
			object = oldValue;
		} else {
			changeType = IModelChangedEvent.INSERT;
			object = newValue;
		}
		fireStructureChanged(object, changeType);
	}	
	
	/**
	 * @param child
	 * @param changeType
	 */
	protected void fireStructureChanged(Object child, int changeType) {
		fireStructureChanged(new Object[] { child }, changeType);
	}	
	
	/**
	 * @param children
	 * @param changeType
	 */
	protected void fireStructureChanged(Object[] children, int changeType) {
		if (fModel.isEditable() && 
				(fModel instanceof IModelChangeProvider)) {
			IModelChangeProvider provider = (IModelChangeProvider)fModel;
			IModelChangedEvent event = new ModelChangedEvent(provider, changeType,
					children, null);
			provider.fireModelChanged(event);
		}
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#addChildNode(org.eclipse.pde.internal.core.text.IDocumentNode, int)
	 */
	public void addChildNode(IDocumentNode child, int position) {
		super.addChildNode(child, position);
		// Fire event
		if (shouldFireEvent()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#addChildNode(org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void addChildNode(IDocumentNode child) {
		super.addChildNode(child);
		// Fire event
		if (shouldFireEvent()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#removeChildNode(org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public IDocumentNode removeChildNode(IDocumentNode child) {
		IDocumentNode node = super.removeChildNode(child);
		// Fire event
		if (shouldFireEvent()) {
			fireStructureChanged(node, IModelChangedEvent.REMOVE);
		}	
		return node;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#removeChildNode(int)
	 */
	public IDocumentNode removeChildNode(int index, Class clazz) {
		// Validate index
		if ((index < 0) ||
				(index >= getChildCount()) ||
				(clazz.isInstance(getChildAt(index)) == false)) {
			// 0 <= index < child element count
			// Cannot remove a node that is not the specified type		
			return null;
		}
		// Remove the node
		IDocumentNode node = super.removeChildNode(index);
		// Fire event
		if (shouldFireEvent()) {
			fireStructureChanged(node, IModelChangedEvent.REMOVE);
		}	
		return node;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write(true));
	}	

	/**
	 * @param newNode
	 * @param oldNode
	 */
	protected void setChildNode(IDocumentNode newNode, Class clazz) {
		// Get the old node
		IDocumentNode oldNode = getChildNode(clazz);
		
		if ((newNode == null) &&
				(oldNode == null)) {
			// NEW = NULL, OLD = NULL
			// If the new and old nodes are not defined, nothing to do
			return;
		} else if (newNode == null) {
			// NEW = NULL, OLD = DEF
			// Remove the old node
			removeChildNode((IDocumentNode)oldNode);
		} else if (oldNode == null) {
			// NEW = DEF, OLD = NULL
			// Add the new node as the first child
			addChildNode((IDocumentNode)newNode, 0);
		} else {
			// NEW = DEF, OLD = DEF
			// Remove the old node
			removeChildNode((IDocumentNode)oldNode);
			// Add the new node as the first child
			addChildNode((IDocumentNode)newNode, 0);
		}
		
		if (shouldFireEvent()) {
			fireStructureChanged(newNode, oldNode);
		}
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	protected IDocumentNode getChildNode(Class clazz) {
		// Linear search O(n)
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			if (clazz.isInstance(node)) {
				return node;
			}
		}
		return null;		
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	protected int getChildNodeCount(Class clazz) {
		// Linear search O(n)
		int count = 0;
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			if (clazz.isInstance(node)) {
				count++;
			}
		}		
		return count;		
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	protected IDocumentNode[] getChildNodes(Class clazz, boolean match) {
		ArrayList filteredChildren = getChildNodesList(clazz, match);
		return (IDocumentNode[])filteredChildren.toArray(new IDocumentNode[filteredChildren.size()]);	
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	protected ArrayList getChildNodesList(Class clazz, boolean match) {
		return getChildNodesList(new Class[]{ clazz }, match);
	}
	
	/**
	 * @param clazz
	 * @return
	 */
	protected IDocumentNode[] getChildNodes(Class[] classes, boolean match) {
		ArrayList filteredChildren = getChildNodesList(classes, match);
		return (IDocumentNode[])filteredChildren.toArray(new IDocumentNode[filteredChildren.size()]);		
	}	
	
	/**
	 * @param classes
	 * @return
	 */
	protected ArrayList getChildNodesList(Class[] classes, boolean match) {
		ArrayList filteredChildren = new ArrayList();
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			for (int i = 0; i < classes.length; i++) {
				Class clazz = classes[i];
				if (clazz.isInstance(node) == match) {
					filteredChildren.add(node);
					break;
				}				
			}
		}		
		return filteredChildren;
	}
	
	/**
	 * @param node
	 * @param clazz
	 * @return
	 */
	protected IDocumentNode getNextSibling(IDocumentNode node, Class clazz) {
		int position = indexOf(node);
		int lastIndex = getChildCount() - 1;
		if ((position < 0) ||
				(position >= lastIndex)) {
			// Either the node was not found or the node was found but it is 
			// at the last index
			return null;
		}
		// Get the next node of the given type
		for (int i = position + 1; i <= lastIndex; i++) {
			IDocumentNode currentNode = getChildAt(i);
			if (clazz.isInstance(currentNode)) {
				return currentNode;
			}
		}
		return null;			
	}
	
	/**
	 * @param node
	 * @param clazz
	 * @return
	 */
	protected IDocumentNode getPreviousSibling(IDocumentNode node, Class clazz) {
		int position = indexOf(node);
		if ((position <= 0) ||
				(position >= getChildCount())) {
			// Either the item was not found or the item was found but it is 
			// at the first index
			return null;
		}
		// Get the previous node of the given type
		for (int i = position - 1; i >= 0; i--) {
			IDocumentNode currentNode = getChildAt(i);
			if (clazz.isInstance(currentNode)) {
				return currentNode;
			}
		}
		return null;		
	}	
	
	/**
	 * @param clazz
	 * @return
	 */
	protected boolean hasChildNodes(Class clazz) {
		ArrayList children = getChildNodesList();
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentNode node = (IDocumentNode)iterator.next();
			if (clazz.isInstance(node)) {
				return true;
			}
		}		
		return false;		
	}
	
	/**
	 * @param node
	 * @param clazz
	 * @return
	 */
	protected boolean isFirstChildNode(IDocumentNode node, Class clazz) {
		int position = indexOf(node);
		// Check to see if node is found
		if ((position < 0) ||
				(position >= getChildCount())) {
			// Node not found
			return false;
		} else if (position == 0) {
			// Node found in the first position
			return true;
		}
		// Check to see if there is any node before the specified node of the
		// same type
		// Assertion: Position > 0
		for (int i = 0; i < position; i++) {
			if (clazz.isInstance(getChildAt(i))) {
				// Another node of the same type found before the specified node
				return false;
			}
		}
		// All nodes before the specified node were of a different type
		return true;
	}
	
	/**
	 * @param node
	 * @param clazz
	 * @return
	 */
	protected boolean isLastChildNode(IDocumentNode node, Class clazz) {
		int position = indexOf(node);
		int lastIndex = getChildCount() - 1;
		// Check to see if node is found
		if ((position < 0) ||
				(position > lastIndex)) {
			// Node not found
			return false;
		} else if (position == lastIndex) {
			// Node found in the last position
			return true;
		}
		// Check to see if there is any node after the specified node of the
		// same type
		// Assertion: Position < lastIndex
		for (int i = position + 1; i <= lastIndex; i++) {
			if (clazz.isInstance(getChildAt(i))) {
				// Another node of the same type found after the specified node
				return false;
			}
		}
		// All nodes after the specified node were of a different type
		return true;		
	}
	
	/**
	 * @param node
	 * @param newRelativeIndex
	 */
	protected void moveChildNode(IDocumentNode node, int newRelativeIndex) {
		// TODO: MP: TEO: Problem, if generic not viewable, may appear that node did not move
		// Get the current index of the node
		int currentIndex = indexOf(node);
		// Ensure the node exists
		if (currentIndex == -1) {
			return;
		}
		// Calculate the new index
		int newIndex = newRelativeIndex + currentIndex;
		// Validate the new index
		// 0 <= newIndex < child element count
		if ((newIndex < 0) ||
				(newIndex >= getChildCount())) {
			return;
		}
		// Remove the node
		removeChildNode(node);
		// Removing the node and moving it to a positive relative index alters
		// the indexing for insertion; however, this pads the new relative
		// index by 1, allowing it to be inserted one position after as 
		// desired
		// Add the node back at the specified index
		addChildNode(node, newIndex);
	}
	
	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected boolean getBooleanAttributeValue(String name, boolean defaultValue) {
		String value = getXMLAttributeValue(name);
		if (value == null) {
			return defaultValue;
		} else if (value.equalsIgnoreCase(ATTRIBUTE_VALUE_TRUE)) {
			return true;
		} else if (value.equalsIgnoreCase(ATTRIBUTE_VALUE_FALSE)) {
			return false;
		}
		return defaultValue;
	}
	
	/**
	 * @param name
	 * @param value
	 * @return
	 */
	protected boolean setBooleanAttributeValue(String name, boolean value) {
		String newValue = Boolean.valueOf(value).toString();
		return setXMLAttribute(name, newValue);
	}
	
	/**
	 * @param name
	 * @param newValue
	 */
	public boolean setXMLAttribute(String name, String newValue) {
		String oldValue = getXMLAttributeValue(name);
		boolean changed = super.setXMLAttribute(name, newValue);
		// Fire an event if in the model
		if (changed && shouldFireEvent()) {
			firePropertyChanged(name, oldValue, newValue);
		}
		return changed;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#setXMLContent(java.lang.String)
	 */
	public boolean setXMLContent(String text) {
		String oldText = null; 
		// Get old text node
		IDocumentTextNode node = getTextNode();
		if (node == null) {
			// Text does not exist
			oldText = ""; //$NON-NLS-1$
		} else {
			// Text exists
			oldText = node.getText();
		}		
		boolean changed = super.setXMLContent(text);
		
		// Fire an event 
		if (changed && shouldFireEvent()) {
			// TODO: MP: TEO: Create constant
			firePropertyChanged(node, "TEXT", oldText, text); //$NON-NLS-1$
		}
		return changed;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#getFileEncoding()
	 */
	protected String getFileEncoding() {
		if ((fModel != null) &&
				(fModel instanceof IEditingModel)) {
			return ((IEditingModel)fModel).getCharset();
		}
		return super.getFileEncoding();
	}	
	
}
