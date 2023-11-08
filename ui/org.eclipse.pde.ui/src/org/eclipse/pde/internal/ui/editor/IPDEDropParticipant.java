/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.editor;

/**
 * IPDEDropParticipant
 *
 */
public interface IPDEDropParticipant {

	/**
	 * @param targetObject The original target object
	 * @param sourceObjects The serialized / deserialized source objects (need to be reconnected)
	 * @param targetLocation ViewerDropAdapter:  LOCATION_ON, LOCATION_BEFORE, LOCATION_AFTER
	 */
	public void doDropCopy(Object targetObject, Object[] sourceObjects, int targetLocation);

	/**
	 * @param targetObject The original target object
	 * @param sourceObjects The serialized / deserialized source objects (need to be reconnected)
	 * @param targetLocation ViewerDropAdapter:  LOCATION_ON, LOCATION_BEFORE, LOCATION_AFTER
	 */
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation);

	/**
	 * @param targetObject The original target object
	 * @param sourceObjects The serialized / deserialized source objects (need to be reconnected)
	 * @param targetLocation ViewerDropAdapter:  LOCATION_ON, LOCATION_BEFORE, LOCATION_AFTER
	 */
	public void doDropLink(Object targetObject, Object[] sourceObjects, int targetLocation);

	/**
	 * @param targetObject The original target object
	 * @param sourceObjects The original source objects
	 * @param targetLocation ViewerDropAdapter:  LOCATION_ON, LOCATION_BEFORE, LOCATION_AFTER
	 */
	public boolean canDropCopy(Object targetObject, Object[] sourceObjects, int targetLocation);

	/**
	 * @param targetObject The original target object
	 * @param sourceObjects The original source objects
	 * @param targetLocation ViewerDropAdapter:  LOCATION_ON, LOCATION_BEFORE, LOCATION_AFTER
	 */
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation);

	/**
	 * @param targetObject The original target object
	 * @param sourceObjects The original source objects
	 * @param targetLocation ViewerDropAdapter:  LOCATION_ON, LOCATION_BEFORE, LOCATION_AFTER
	 */
	public boolean canDropLink(Object targetObject, Object[] sourceObjects, int targetLocation);

}
