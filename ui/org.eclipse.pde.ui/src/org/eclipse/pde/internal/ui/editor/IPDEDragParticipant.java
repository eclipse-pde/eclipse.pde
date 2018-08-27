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

public interface IPDEDragParticipant {

	/**
	 * @param sourceObjects The original source objects
	 */
	public boolean canDragCopy(Object[] sourceObjects);

	/**
	 * @param sourceObjects The original source objects
	 */
	public boolean canDragMove(Object[] sourceObjects);

	/**
	 * @param sourceObjects The original source objects
	 */
	public boolean canDragLink(Object[] sourceObjects);

	/**
	 * @param sourceObjects The serialized / deserialized source objects (need to be reconnected)
	 */
	public void doDragRemove(Object[] sourceObjects);

	public int getSupportedDNDOperations();

}
