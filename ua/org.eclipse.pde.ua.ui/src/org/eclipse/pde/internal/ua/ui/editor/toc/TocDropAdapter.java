/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;

/**
 * TocDropAdapter - implements drop behaviour for the TOC Tree Section.
 * It extends ViewerDropAdapter for advanced feedback behaviour, but
 * mostly overrides the specified drop behaviour.
 */
public class TocDropAdapter extends ViewerDropAdapter {
	private TocTreeSection fSection;

	/**
	 * Constant describing the position of the cursor relative 
	 * to the target object.  This means the mouse is positioned
	 * slightly after the target, but not after its children if it is
	 * expanded.
	 * @see #getCurrentLocation()
	 */
	public static final int LOCATION_JUST_AFTER = 5;

	public TocDropAdapter(TreeViewer tocTree, TocTreeSection section) {
		super(tocTree);
		fSection = section;
	}

	/**
	 * Returns the position of the given event's coordinates relative to its target.
	 * The position is determined to be before, after, or on the item, based on
	 * some threshold value.
	 *
	 * @param event the event
	 * @return one of the <code>LOCATION_* </code>constants defined in this class
	 */
	protected int determineLocation(DropTargetEvent event) {
		if (!(event.item instanceof Item)) {
			return LOCATION_NONE;
		}
		Item item = (Item) event.item;
		Point coordinates = new Point(event.x, event.y);
		coordinates = getViewer().getControl().toControl(coordinates);
		if (item != null) {
			Rectangle bounds = getBounds(item);
			if (bounds == null) {
				return LOCATION_NONE;
			}
			if ((coordinates.y - bounds.y) < 5) {
				return LOCATION_BEFORE;
			}
			if ((bounds.y + bounds.height - coordinates.y) < 5) {
				if ((bounds.y - coordinates.y) < 5) {
					return LOCATION_JUST_AFTER;
				}
				return LOCATION_AFTER;
			}
		}
		return LOCATION_ON;
	}

	/* (non-Javadoc)
	 * A new drag has entered the widget. Do file validation if necessary,
	 * and then set the Drag and Drop mode.
	 */
	public void dragEnter(DropTargetEvent event) {
		validateFileDrop(event);
		setDNDMode(event);
	}

	/* (non-Javadoc)
	 * Override the dragOver behaviour to directly supply event feedback
	 * but do nothing else.
	 */
	public void dragOver(DropTargetEvent event) {
		int currentLocation = determineLocation(event);
		switch (currentLocation) {
			case LOCATION_BEFORE :
				event.feedback = DND.FEEDBACK_INSERT_BEFORE;
				break;
			case LOCATION_AFTER :
			case LOCATION_JUST_AFTER :
				event.feedback = DND.FEEDBACK_INSERT_AFTER;
				break;
			case LOCATION_ON :
			default :
				event.feedback = DND.FEEDBACK_SELECT;
				break;
		}

		event.feedback |= DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
	}

	/* (non-Javadoc)
	 * The Drag and Drop operation changed. Change the operation to a valid one
	 * if necessary.
	 */
	public void dragOperationChanged(DropTargetEvent event) {
		validateFileDrop(event);
		setDNDMode(event);
	}

	/**
	 * Set the Drag and Drop mode depending on the dragged items and event
	 * details. Files can only be copied, not linked or moved.
	 * Model data objects can have any operation occur.
	 * 
	 * All other objects cannot be dropped.
	 * 
	 * @param event The drop event to change.
	 */
	private void setDNDMode(DropTargetEvent event) {
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) { //If a file is being dragged
			if (event.detail == DND.DROP_DEFAULT) { //If no modifier key is pressed
				//set the operation to DROP_COPY if available
				//DROP_NONE otherwise
				event.detail = (event.operations & DND.DROP_COPY);
			} else { //If a modifier key is pressed for a file and the operation isn't a copy,
				//disallow it
				event.detail &= DND.DROP_COPY;
			}
		}
		//The only other transfer type allowed is a Model Data Transfer
		else if (!ModelDataTransfer.getInstance().isSupportedType(event.currentDataType)) { //disallow drag if the transfer is not Model Data or Files
			event.detail = DND.DROP_NONE;
		}
	}

	/**
	 * Ensure that, if files are being dropped, they have valid
	 * file extensions for the TOC Editor (HTML pages and XML documents).
	 * 
	 * Invalidate the drop if this condition is not met.
	 * 
	 * @param event The drop event containing the transfer.
	 */
	private void validateFileDrop(DropTargetEvent event) {
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
			IBaseModel model = fSection.getPage().getModel();
			String[] fileNames = (String[]) FileTransfer.getInstance().nativeToJava(event.currentDataType);
			for (int i = 0; i < fileNames.length; i++) {
				IPath path = new Path(fileNames[i]);

				// Make sure that the file is in the workspace
				if (ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path) == null) {
					event.detail = DND.DROP_NONE;
					return;
				}

				if(!HelpEditorUtil.hasValidPageExtension(path)
					&& !HelpEditorUtil.isTOCFile(path))
				{	event.detail = DND.DROP_NONE;
					return;
				}

				// Make sure that the user isn't dropping a TOC into itself
				if(HelpEditorUtil.isCurrentResource(path, model))
				{	event.detail = DND.DROP_NONE;
					return;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * Override the drop behaviour in order to directly manage the drop event
	 */
	public void drop(DropTargetEvent event) {
		Object target = determineTarget(event);
		int location = determineLocation(event);
		if (!fSection.performDrop(target, event.data, location)) {
			event.detail = DND.DROP_NONE;
		}
	}

	/* (non-Javadoc)
	 * Override the behaviour of ViewerDropAdapter#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
	 * 
	 * @see org.eclipse.swt.dnd.DropTargetAdapter#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	public void dragLeave(DropTargetEvent event) { //NO-OP
	}

	/* (non-Javadoc)
	 * Override the behaviour of ViewerDropAdapter#dropAccept(org.eclipse.swt.dnd.DropTargetEvent)
	 * 
	 * @see org.eclipse.swt.dnd.DropTargetAdapter#dropAccept(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	public void dropAccept(DropTargetEvent event) { //NO-OP
	}

	//These methods are never called because much of ViewerDropAdapter's
	//behaviour is overridden, but they must be implemented.

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	public boolean performDrop(Object data) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return false;
	}

}
