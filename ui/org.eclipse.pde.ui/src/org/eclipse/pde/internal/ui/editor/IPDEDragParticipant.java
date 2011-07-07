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
