/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;
import org.w3c.dom.Element;

public class CompCSTaskGroup extends CompCSTaskObject implements
		ICompCSTaskGroup {

	private List<ICompCSTaskObject> fFieldTaskObjects;

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

	@Override
	public List<ICompCSTaskObject> getChildren() {
		List<ICompCSTaskObject> list = new ArrayList<>();
		// Add task objects
		if (fFieldTaskObjects.size() > 0) {
			list.addAll(fFieldTaskObjects);
		}
		return list;
	}

	@Override
	public String getName() {
		return fFieldName;
	}

	@Override
	public int getType() {
		return TYPE_TASKGROUP;
	}

	@Override
	public void reset() {
		super.reset();

		fFieldTaskObjects = new ArrayList<>();
	}

	@Override
	public void addFieldTaskObject(ICompCSTaskObject taskObject) {
		fFieldTaskObjects.add(taskObject);
		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.INSERT);
		}
	}

	@Override
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

	@Override
	public int getFieldTaskObjectCount() {
		return fFieldTaskObjects.size();
	}

	@Override
	public ICompCSTaskObject[] getFieldTaskObjects() {
		return fFieldTaskObjects
				.toArray(new ICompCSTaskObject[fFieldTaskObjects.size()]);
	}

	@Override
	public boolean hasFieldTaskObjects() {
		if (fFieldTaskObjects.isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public int indexOfFieldTaskObject(ICompCSTaskObject taskObject) {
		return fFieldTaskObjects.indexOf(taskObject);
	}

	@Override
	public boolean isFirstFieldTaskObject(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		if (position == 0) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isLastFieldTaskObject(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		int lastPosition = fFieldTaskObjects.size() - 1;
		if (position == lastPosition) {
			return true;
		}
		return false;
	}

	@Override
	public void removeFieldTaskObject(ICompCSTaskObject taskObject) {
		fFieldTaskObjects.remove(taskObject);
		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public void removeFieldTaskObject(int index) {
		if ((index < 0) || (index > (fFieldTaskObjects.size() - 1))) {
			return;
		}
		ICompCSTaskObject taskObject = fFieldTaskObjects
				.remove(index);
		if (isEditable()) {
			fireStructureChanged(taskObject, IModelChangedEvent.REMOVE);
		}
	}

	@Override
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

	@Override
	protected void writeElements(String indent, PrintWriter writer) {
		super.writeElements(indent, writer);
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print dependency elements
		Iterator<ICompCSTaskObject> iterator = fFieldTaskObjects.iterator();
		while (iterator.hasNext()) {
			ICompCSTaskObject taskObject = iterator.next();
			taskObject.write(newIndent, writer);
		}
	}

	@Override
	public String getElement() {
		return ELEMENT_TASKGROUP;
	}

	@Override
	public ICompCSTaskObject getNextSibling(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		int lastIndex = fFieldTaskObjects.size() - 1;
		if ((position == -1) || (position == lastIndex)) {
			// Either the item was not found or the item was found but it is
			// at the last index
			return null;
		}
		return fFieldTaskObjects.get(position + 1);
	}

	@Override
	public ICompCSTaskObject getPreviousSibling(ICompCSTaskObject taskObject) {
		int position = fFieldTaskObjects.indexOf(taskObject);
		if ((position == -1) || (position == 0)) {
			// Either the item was not found or the item was found but it is
			// at the first index
			return null;
		}
		return fFieldTaskObjects.get(position - 1);
	}

	@Override
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
