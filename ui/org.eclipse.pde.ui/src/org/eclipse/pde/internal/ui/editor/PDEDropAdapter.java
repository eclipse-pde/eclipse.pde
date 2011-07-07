/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;

public class PDEDropAdapter extends ViewerDropAdapter {

	private IPDEDropParticipant fDropParticipant;

	private IPDESourceParticipant fSourceParticipant;

	private int fLastValidOperation;

	public PDEDropAdapter(Viewer viewer, IPDEDropParticipant dropParticipant, IPDESourceParticipant sourceParticipant) {
		super(viewer);
		fDropParticipant = dropParticipant;
		fSourceParticipant = sourceParticipant;
		resetLastValidOperation();
	}

	protected void resetLastValidOperation() {
		fLastValidOperation = DND.DROP_NONE;
	}

	protected int getLastValidOperation(int currentOperation) {
		if (currentOperation != DND.DROP_NONE) {
			fLastValidOperation = currentOperation;
		}
		return fLastValidOperation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	public boolean performDrop(Object data) {
		// Clear the last valid operation for the next drop event
		resetLastValidOperation();
		// Get the original target object to drop source objects on
		Object targetObject = getCurrentTarget();
		// Get the drop location relative to the target object
		int targetLocation = getCurrentLocation();
		// Get the serialized / deserialized source objects to drop
		Object[] sourceObjects = null;
		if (data instanceof Object[]) {
			sourceObjects = (Object[]) data;
		} else {
			sourceObjects = new Object[] {data};
		}
		// Get the current operation
		int operation = getCurrentOperation();
		// Drop the source objects on the target
		// object given the specified operation
		if (operation == DND.DROP_COPY) {
			fDropParticipant.doDropCopy(targetObject, sourceObjects, targetLocation);
		} else if (operation == DND.DROP_MOVE) {
			fDropParticipant.doDropMove(targetObject, sourceObjects, targetLocation);
		} else if (operation == DND.DROP_LINK) {
			fDropParticipant.doDropLink(targetObject, sourceObjects, targetLocation);
		} else if (operation == DND.DROP_DEFAULT) {
			fDropParticipant.doDropMove(targetObject, sourceObjects, targetLocation);
		} else {
			return false;
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	public boolean validateDrop(Object targetObject, int operation, TransferData transferType) {
		// Current operation listed is not set until after the drop is validated
		// i.e. This method call
		// Replace the current operation with the last valid operation
		operation = getLastValidOperation(operation);
		// Get the original target object to drop source objects on
		targetObject = getCurrentTarget();
		// Get the drop location relative to the target object
		int targetLocation = getCurrentLocation();
		// Ensure we have a model transfer operation
		if (ModelDataTransfer.getInstance().isSupportedType(transferType) == false) {
			return false;
		}
		// Ensure the location is defined
		if (targetLocation == LOCATION_NONE) {
			return false;
		}
		// Get the original source objects
		Object[] sourceObjects = fSourceParticipant.getSourceObjects();
		// Ensure we have source objects
		if (sourceObjects == null) {
			return false;
		}
		if (sourceObjects.length == 0) {
			return false;
		}
		// Ensure the target object is defined
		if (targetObject == null) {
			return false;
		}
		// Determine whether the source objects can be dropped on the target
		// object given the specified operation
		if (operation == DND.DROP_COPY) {
			return validateDropCopy(targetObject, sourceObjects, targetLocation);
		} else if (operation == DND.DROP_MOVE) {
			return validateDropMove(targetObject, sourceObjects, targetLocation);
		} else if (operation == DND.DROP_LINK) {
			return validateDropLink(targetObject, sourceObjects, targetLocation);
		} else if (operation == DND.DROP_DEFAULT) {
			return validateDropDefault(targetObject, sourceObjects, targetLocation);
		}
		return false;
	}

	protected boolean validateDropCopy(Object targetObject, Object[] sourceObjects, int targetLocation) {
		return fDropParticipant.canDropCopy(targetObject, sourceObjects, targetLocation);
	}

	protected boolean validateDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Source objects have not been serialized yet.
		// As a result we can compare whether a source and target object is
		// equal
		// Ensure the target is valid for a move operation and not redundant
		// Meaning there is no effect of the move
		for (int i = 0; i < sourceObjects.length; i++) {
			if (targetObject.equals(sourceObjects[i])) {
				// No source objects are allowed to be dropped on themselves for
				// move operations
				return false;
			}
		}
		return fDropParticipant.canDropMove(targetObject, sourceObjects, targetLocation);
	}

	protected boolean validateDropLink(Object targetObject, Object[] sourceObjects, int targetLocation) {
		return fDropParticipant.canDropLink(targetObject, sourceObjects, targetLocation);
	}

	protected boolean validateDropDefault(Object targetObject, Object[] sourceObjects, int targetLocation) {
		return validateDropMove(targetObject, sourceObjects, targetLocation);
	}

}
