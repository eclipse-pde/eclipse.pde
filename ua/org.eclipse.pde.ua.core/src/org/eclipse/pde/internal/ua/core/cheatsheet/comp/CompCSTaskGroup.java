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

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;
import org.w3c.dom.Element;

public class CompCSTaskGroup extends CompCSTaskObject implements
		ICompCSTaskGroup {

	private ArrayList fFieldTaskObjects;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSTaskGroup(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#getChildren
	 * ()
	 */
	public List getChildren() {
		ArrayList list = new ArrayList();
		// Add task objects
		if (fFieldTaskObjects.size() > 0) {
			list.addAll(fFieldTaskObjects);
		}
		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#getName()
	 */
	public String getName() {
		return fFieldName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#getType()
	 */
	public int getType() {
		return TYPE_TASKGROUP;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#reset()
	 */
	public void reset() {
		super.reset();

		fFieldTaskObjects = new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * addFieldTaskObject
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public void addFieldTaskObject(ICompCSTaskObject taskObject) {
		fFieldTaskObjects.add(taskObject);
		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.INSERT);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * addFieldTaskObject(int,
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public void addFieldTaskObject(int index, ICompCSTaskObject taskObject) {
		if (index < 0) {
			return;
		}
		if (index >= fFieldTaskObjects.size()) {
			fFieldTaskObjects.add(taskObject);
		} else {
			fFieldTaskObjects.add(index, taskObject);
		}

		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.INSERT);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * getFieldTaskObjectCount()
	 */
	public int getFieldTaskObjectCount() {
		return fFieldTaskObjects.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * getFieldTaskObjects()
	 */
	public ICompCSTaskObject[] getFieldTaskObjects() {
		return (ICompCSTaskObject[]) fFieldTaskObjects
				.toArray(new ICompCSTaskObject[fFieldTaskObjects.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * hasFieldTaskObjects()
	 */
	public boolean hasFieldTaskObjects() {
		if (fFieldTaskObjects.isEmpty()) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * indexOfFieldTaskObject
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public int indexOfFieldTaskObject(ICompCSTaskObject taskObject) {
		return fFieldTaskObjects.indexOf(taskObject);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * isFirstFieldTaskObject
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public boolean isFirstFieldTaskObject(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		if (position == 0) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * isLastFieldTaskObject
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public boolean isLastFieldTaskObject(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		int lastPosition = fFieldTaskObjects.size() - 1;
		if (position == lastPosition) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * removeFieldTaskObject
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public void removeFieldTaskObject(ICompCSTaskObject taskObject) {
		fFieldTaskObjects.remove(taskObject);
		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.REMOVE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * removeFieldTaskObject(int)
	 */
	public void removeFieldTaskObject(int index) {
		if ((index < 0) || (index > (fFieldTaskObjects.size() - 1))) {
			return;
		}
		ICompCSTaskObject taskObject = (ICompCSTaskObject) fFieldTaskObjects
				.remove(index);
		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.REMOVE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseElement
	 * (org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		super.parseElement(element);
		String name = element.getNodeName();
		ICompCSModelFactory factory = getModel().getFactory();

		if (name.equals(ELEMENT_TASK)) {
			// Process task element
			ICompCSTask task = factory.createCompCSTask(this);
			fFieldTaskObjects.add(task);
			task.parse(element);
		} else if (name.equals(ELEMENT_TASKGROUP)) {
			// Process taskGroup element
			ICompCSTaskGroup taskGroup = factory.createCompCSTaskGroup(this);
			fFieldTaskObjects.add(taskGroup);
			taskGroup.parse(element);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#writeElements
	 * (java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		super.writeElements(indent, writer);
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print dependency elements
		Iterator iterator = fFieldTaskObjects.iterator();
		while (iterator.hasNext()) {
			ICompCSTaskObject taskObject = (ICompCSTaskObject) iterator.next();
			taskObject.write(newIndent, writer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_TASKGROUP;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * getNextSibling
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public ICompCSTaskObject getNextSibling(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		int lastIndex = fFieldTaskObjects.size() - 1;
		if ((position == -1) || (position == lastIndex)) {
			// Either the item was not found or the item was found but it is
			// at the last index
			return null;
		}
		return (ICompCSTaskObject) fFieldTaskObjects.get(position + 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * getPreviousSibling
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject)
	 */
	public ICompCSTaskObject getPreviousSibling(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		if ((position == -1) || (position == 0)) {
			// Either the item was not found or the item was found but it is
			// at the first index
			return null;
		}
		return (ICompCSTaskObject) fFieldTaskObjects.get(position - 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup#
	 * moveFieldTaskObject
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject, int)
	 */
	public void moveFieldTaskObject(ICompCSTaskObject taskObject,
			int newRelativeIndex) {
		// Get the current index of the task object
		int currentIndex = indexOfFieldTaskObject(taskObject);
		// Ensure the object is found
		if (currentIndex == -1) {
			return;
		}
		// Calculate the new index
		int newIndex = newRelativeIndex + currentIndex;
		// Validate the new index
		if ((newIndex < 0) || (newIndex >= fFieldTaskObjects.size())) {
			return;
		}
		// Remove the task object
		fFieldTaskObjects.remove(taskObject);
		// Add the task object back at the specified index
		fFieldTaskObjects.add(newIndex, taskObject);
		// Send an insert event
		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.INSERT);
		}
	}

}
