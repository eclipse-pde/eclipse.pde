/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;

/**
 * DocumentObject
 *
 */
public abstract class DocumentObject extends DocumentElementNode implements IDocumentObject {

	// TODO: MP: TEO: LOW: Integrate with plugin model?
	// TODO: MP: TEO: LOW: Investigate document node to see if any methods to pull down

	private static final long serialVersionUID = 1L;

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

	@Override
	public void setSharedModel(IModel model) {
		fModel = model;
	}

	@Override
	public IModel getSharedModel() {
		return fModel;
	}

	@Override
	public void reset() {
		// TODO: MP: TEO: LOW: Reset parent fields? or super.reset?
		fModel = null;
		fInTheModel = false;
	}

	@Override
	public boolean isInTheModel() {
		return fInTheModel;
	}

	@Override
	public void setInTheModel(boolean inModel) {
		fInTheModel = inModel;
	}

	@Override
	public boolean isEditable() {
		// Convenience method
		return fModel.isEditable();
	}

	protected boolean shouldFireEvent() {
		if (isInTheModel() && isEditable()) {
			return true;
		}
		return false;
	}

	@Override
	protected String getLineDelimiter() {
		if (fModel instanceof IEditingModel) {
			IDocument document = ((IEditingModel) fModel).getDocument();
			return TextUtilities.getDefaultLineDelimiter(document);
		}
		return super.getLineDelimiter();
	}

	@Override
	public void reconnect(IDocumentElementNode parent, IModel model) {
		super.reconnect(parent, model);
		// Transient field:  In The Model
		// Value set to true when added to the parent; however, serialized
		// children's value remains unchanged.  Since, reconnect and add calls
		// are made so close together, set value to true for parent and all
		// children
		fInTheModel = true;
		// Transient field:  Model
		fModel = model;
	}

	/**
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChanged(String property, Object oldValue, Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	/**
	 * @param object
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	private void firePropertyChanged(Object object, String property, Object oldValue, Object newValue) {
		if (fModel.isEditable() && (fModel instanceof IModelChangeProvider provider)) {
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}

	/**
	 * @param child
	 * @param changeType
	 */
	protected void fireStructureChanged(Object child, int changeType) {
		fireStructureChanged(new Object[] {child}, changeType);
	}

	/**
	 * @param children
	 * @param changeType
	 */
	protected void fireStructureChanged(Object[] children, int changeType) {
		if (fModel.isEditable() && (fModel instanceof IModelChangeProvider provider)) {
			IModelChangedEvent event = new ModelChangedEvent(provider, changeType, children, null);
			provider.fireModelChanged(event);
		}
	}

	@Override
	public void addChildNode(IDocumentElementNode child) {
		if (child instanceof IDocumentObject) {
			((IDocumentObject) child).setInTheModel(true);
		}
		super.addChildNode(child);
	}

	@Override
	public void addChildNode(IDocumentElementNode child, int position) {
		// Ensure the position is valid
		// 0 <= position <= number of children
		if ((position < 0) || (position > getChildCount())) {
			return;
		}
		if (child instanceof IDocumentObject) {
			((IDocumentObject) child).setInTheModel(true);
		}
		super.addChildNode(child, position);
	}

	/**
	 * @param child
	 * @param position
	 * @param fireEvent
	 */
	@Override
	public void addChildNode(IDocumentElementNode child, int position, boolean fireEvent) {
		addChildNode(child, position);
		// Fire event
		if (fireEvent && shouldFireEvent()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}
	}

	/**
	 * @param child
	 * @param fireEvent
	 */
	@Override
	public void addChildNode(IDocumentElementNode child, boolean fireEvent) {
		addChildNode(child);
		// Fire event
		if (fireEvent && shouldFireEvent()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public IDocumentElementNode removeChildNode(IDocumentElementNode child) {
		IDocumentElementNode node = super.removeChildNode(child);
		if ((node != null) && (node instanceof IDocumentObject)) {
			((IDocumentObject) node).setInTheModel(false);
		}
		return node;
	}

	@Override
	public IDocumentElementNode removeChildNode(int index) {
		IDocumentElementNode node = super.removeChildNode(index);
		if ((node != null) && (node instanceof IDocumentObject)) {
			((IDocumentObject) node).setInTheModel(false);
		}
		return node;
	}

	@Override
	public IDocumentElementNode removeChildNode(IDocumentElementNode child, boolean fireEvent) {
		IDocumentElementNode node = removeChildNode(child);
		// Fire event
		if (fireEvent && shouldFireEvent()) {
			fireStructureChanged(child, IModelChangedEvent.REMOVE);
		}
		return node;
	}

	@Override
	public IDocumentElementNode removeChildNode(int index, Class<?> clazz, boolean fireEvent) {
		IDocumentElementNode node = removeChildNode(index, clazz);
		// Fire event
		if (fireEvent && shouldFireEvent()) {
			fireStructureChanged(node, IModelChangedEvent.REMOVE);
		}
		return node;
	}

	@Override
	public IDocumentElementNode removeChildNode(int index, Class<?> clazz) {
		// Validate index
		if ((index < 0) || (index >= getChildCount()) || (clazz.isInstance(getChildAt(index)) == false)) {
			// 0 <= index < child element count
			// Cannot remove a node that is not the specified type
			return null;
		}
		// Remove the node
		IDocumentElementNode node = removeChildNode(index);
		return node;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write(true));
	}

	/**
	 * @param newNode
	 * @param clazz
	 */
	@Override
	public void setChildNode(IDocumentElementNode newNode, Class<?> clazz) {
		// Determine whether to fire the event
		boolean fireEvent = shouldFireEvent();
		// Get the old node
		IDocumentElementNode oldNode = getChildNode(clazz);

		if ((newNode == null) && (oldNode == null)) {
			// NEW = NULL, OLD = NULL
			// If the new and old nodes are not defined, nothing to do
			return;
		} else if (newNode == null) {
			// NEW = NULL, OLD = DEF
			// Remove the old node
			removeChildNode(oldNode, fireEvent);
		} else if (oldNode == null) {
			// NEW = DEF, OLD = NULL
			// Add the new node to the end of the list
			addChildNode(newNode, fireEvent);
		} else {
			// NEW = DEF, OLD = DEF
			replaceChildNode(newNode, oldNode, fireEvent);
		}
	}

	/**
	 * @param newNode
	 * @param oldNode
	 * @param fireEvent
	 */
	protected void replaceChildNode(IDocumentElementNode newNode, IDocumentElementNode oldNode, boolean fireEvent) {
		// Get the index of the old node
		int position = indexOf(oldNode);
		// Validate position
		if (position < 0) {
			return;
		}
		// Add the new node to the same position occupied by the old node
		addChildNode(newNode, position, fireEvent);
		// Remove the old node
		removeChildNode(oldNode, fireEvent);
	}

	@Override
	public IDocumentElementNode getChildNode(Class<?> clazz) {
		// Linear search O(n)
		ArrayList<IDocumentElementNode> children = getChildNodesList();
		Iterator<IDocumentElementNode> iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentElementNode node = iterator.next();
			if (clazz.isInstance(node)) {
				return node;
			}
		}
		return null;
	}

	@Override
	public int getChildNodeCount(Class<?> clazz) {
		// Linear search O(n)
		int count = 0;
		ArrayList<IDocumentElementNode> children = getChildNodesList();
		Iterator<IDocumentElementNode> iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentElementNode node = iterator.next();
			if (clazz.isInstance(node)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public ArrayList<IDocumentElementNode> getChildNodesList(Class<?> clazz, boolean match) {
		return getChildNodesList(new Class[] {clazz}, match);
	}

	@Override
	public ArrayList<IDocumentElementNode> getChildNodesList(Class<?>[] classes, boolean match) {
		ArrayList<IDocumentElementNode> filteredChildren = new ArrayList<>();
		ArrayList<IDocumentElementNode> children = getChildNodesList();
		Iterator<IDocumentElementNode> iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentElementNode node = iterator.next();
			for (Class<?> clazz : classes) {
				if (clazz.isInstance(node) == match) {
					filteredChildren.add(node);
					break;
				}
			}
		}
		return filteredChildren;
	}

	@Override
	public IDocumentElementNode getNextSibling(IDocumentElementNode node, Class<?> clazz) {
		int position = indexOf(node);
		int lastIndex = getChildCount() - 1;
		if ((position < 0) || (position >= lastIndex)) {
			// Either the node was not found or the node was found but it is
			// at the last index
			return null;
		}
		// Get the next node of the given type
		for (int i = position + 1; i <= lastIndex; i++) {
			IDocumentElementNode currentNode = getChildAt(i);
			if (clazz.isInstance(currentNode)) {
				return currentNode;
			}
		}
		return null;
	}

	@Override
	public IDocumentElementNode getPreviousSibling(IDocumentElementNode node, Class<?> clazz) {
		int position = indexOf(node);
		if ((position <= 0) || (position >= getChildCount())) {
			// Either the item was not found or the item was found but it is
			// at the first index
			return null;
		}
		// Get the previous node of the given type
		for (int i = position - 1; i >= 0; i--) {
			IDocumentElementNode currentNode = getChildAt(i);
			if (clazz.isInstance(currentNode)) {
				return currentNode;
			}
		}
		return null;
	}

	@Override
	public boolean hasChildNodes(Class<?> clazz) {
		ArrayList<IDocumentElementNode> children = getChildNodesList();
		Iterator<IDocumentElementNode> iterator = children.iterator();
		while (iterator.hasNext()) {
			IDocumentElementNode node = iterator.next();
			if (clazz.isInstance(node)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isFirstChildNode(IDocumentElementNode node, Class<?> clazz) {
		int position = indexOf(node);
		// Check to see if node is found
		if ((position < 0) || (position >= getChildCount())) {
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

	@Override
	public boolean isLastChildNode(IDocumentElementNode node, Class<?> clazz) {
		int position = indexOf(node);
		int lastIndex = getChildCount() - 1;
		// Check to see if node is found
		if ((position < 0) || (position > lastIndex)) {
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

	@Override
	public void swap(IDocumentElementNode child1, IDocumentElementNode child2, boolean fireEvent) {
		super.swap(child1, child2);
		// Fire event
		if (fireEvent && shouldFireEvent()) {
			firePropertyChanged(this, IDocumentElementNode.F_PROPERTY_CHANGE_TYPE_SWAP, child1, child2);
		}
	}

	/**
	 * @param node
	 * @param newRelativeIndex
	 */
	@Override
	public void moveChildNode(IDocumentElementNode node, int newRelativeIndex, boolean fireEvent) {

		// TODO: MP: TEO: MED: Test Problem, if generic not viewable, may appear that node did not move
		// TODO: MP: TEO: MED: Test relative index > 1 or < -1
		// TODO: MP: TEO: MED: BUG: Add item to end, move existing item before it down, teo overwrites new item

		if (newRelativeIndex == 0) {
			// Nothing to do
			return;
		}
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
		if ((newIndex < 0) || (newIndex >= getChildCount())) {
			return;
		}
		// If we are only moving a node up and down one position use a swap
		// operation.  Otherwise, delete the node, clone it and then re-insert
		// the node
		if ((newRelativeIndex == -1) || (newRelativeIndex == 1)) {
			IDocumentElementNode sibling = getChildAt(newIndex);
			// Ensure sibling exists
			if (sibling == null) {
				return;
			}
			swap(node, sibling, fireEvent);
		} else {
			// Remove the node
			removeChildNode(node, fireEvent);
			// Clone the node
			// Needed to create a text edit operation that inserts a new element
			// rather than replacing the old one
			IDocumentElementNode clone = clone(node);
			// Removing the node and moving it to a positive relative index alters
			// the indexing for insertion; however, this pads the new relative
			// index by 1, allowing it to be inserted one position after as
			// desired
			// Add the node back at the specified index
			addChildNode(clone, newIndex, fireEvent);
		}
	}

	@Override
	public IDocumentElementNode clone(IDocumentElementNode node) {
		IDocumentElementNode clone = null;
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			// Serialize

			try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
				out.writeObject(node);
				out.flush();
			}
			byte[] bytes = bout.toByteArray();
			// Deserialize
			try (ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
					ObjectInputStream in = new ObjectInputStream(bin)) {
				clone = (IDocumentElementNode) in.readObject();
			}
			// Reconnect
			clone.reconnect(this, fModel);
		} catch (IOException | ClassNotFoundException e) {
		}

		return clone;
	}

	@Override
	public boolean getBooleanAttributeValue(String name, boolean defaultValue) {
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

	@Override
	public boolean setBooleanAttributeValue(String name, boolean value) {
		String newValue = Boolean.toString(value);
		return setXMLAttribute(name, newValue);
	}

	@Override
	public boolean setXMLAttribute(String name, String newValue) {
		String oldValue = getXMLAttributeValue(name);
		boolean changed = super.setXMLAttribute(name, newValue);
		// Fire an event if in the model
		if (changed && shouldFireEvent()) {
			firePropertyChanged(name, oldValue, newValue);
		}
		return changed;
	}

	@Override
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
			if (node != null) {
				firePropertyChanged(node, IDocumentTextNode.F_PROPERTY_CHANGE_TYPE_PCDATA, oldText, text);
			} else {
				fireStructureChanged(this, IModelChangedEvent.INSERT);
			}
		}
		return changed;
	}

	@Override
	protected Charset getFileEncoding() {
		if ((fModel != null) && (fModel instanceof IEditingModel)) {
			return ((IEditingModel) fModel).getCharset();
		}
		return super.getFileEncoding();
	}

}
