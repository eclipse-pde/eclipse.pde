/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;

/**
 * TocDragAdapter implements the drag behaviour for the TOC tree
 * section.
 */
public class TocDragAdapter implements DragSourceListener {
	//The TOC Tree Section being dragged from
	private TocTreeSection fSection;
	//The dragged items
	private ArrayList<TocObject> fDraggedItems;

	/**
	 * Constructs a new Drag Adapter with the specified selection
	 * provider and TocTreeSection
	 *
	 * @param section The section that will handle removal
	 */
	public TocDragAdapter(TocTreeSection section) {
		fSection = section;
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		if (event.doit) { //The event should only be enabled if there is a selection to drag
			event.doit = !fSection.getSelection().isEmpty();
		}
	}

	@Override
	public void dragSetData(DragSourceEvent event) {
		//Check if the drag is still enabled
		if (event.doit) {
			IStructuredSelection sel = (IStructuredSelection) fSection.getSelection();

			if (TextTransfer.getInstance().isSupportedType(event.dataType)) { //If the expected data is text, then write out the selection
				//into its XML representation

				StringWriter sw = new StringWriter();
				PrintWriter writer = new PrintWriter(sw);

				//Write the XML representation of each selected object
				for (Iterator<?> iter = sel.iterator(); iter.hasNext();) {
					Object obj = iter.next();
					if (obj instanceof TocObject) {
						((TocObject) obj).write("", writer); //$NON-NLS-1$
					}
				}

				//Set the event's drag object to be this String
				event.data = sw.toString();
				//Set the array of dragged items to null,
				//since we are dragging a String
				fDraggedItems = null;
			} else if (ModelDataTransfer.getInstance().isSupportedType(event.dataType)) {
				//If we are dragging items from the model
				fDraggedItems = getSelectedObjects(sel);
				TocObject[] selectedObjects = fDraggedItems.toArray(new TocObject[fDraggedItems.size()]);
				if (selectedObjects.length == 0) { //disable the drag if there are no items selected
					event.doit = false;
				} else { //set the event's drag object to the selection
					event.data = selectedObjects;
				}
			}
		}
	}

	/**
	 * @param selection The selection to place in the ArrayList
	 * @return an ArrayList containing all removable TocObjects in the selection
	 */
	private ArrayList<TocObject> getSelectedObjects(IStructuredSelection selection) {
		ArrayList<TocObject> objects = new ArrayList<>();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof TocObject && ((TocObject) obj).canBeRemoved()) { //If the object is a removable TocObject, add it
				objects.add((TocObject) obj);
			} else { //If the object is not a removable TocObject,
				//we don't want to permit the drag, so return an empty list
				return new ArrayList<>();
			}
		}

		return objects;
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.detail == DND.DROP_MOVE && fDraggedItems != null) {
			fSection.handleDrag(fDraggedItems);
		}

		fDraggedItems = null;
	}

	public ArrayList<TocObject> getDraggedElements() {
		return fDraggedItems;
	}
}
