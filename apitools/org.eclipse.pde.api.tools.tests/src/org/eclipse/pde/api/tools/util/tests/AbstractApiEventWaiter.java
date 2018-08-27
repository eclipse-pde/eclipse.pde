/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.util.tests;

/**
 * Abstract class for API Tools event waiters to extend
 * @since 1.0.0
 */
public abstract class AbstractApiEventWaiter {

	final int DEFAULT_TIMEOUT = 15000;
	private Object fEvent = null;

	/**
	 * Begins waiting for an acceptable event
	 * @return the source of the event or <code>null</code> if the waiter times out
	 * or is interrupted
	 */
	public synchronized Object waitForEvent() {
		if(fEvent == null) {
			try {
				this.wait(DEFAULT_TIMEOUT);
			}
			catch(InterruptedException e) {
				System.err.println("Thread interrupted waiting for element changed event"); //$NON-NLS-1$
			}
		}
		unregister();
		if(fEvent == null) {
			return null;
		}
		return fEvent;
	}

	/**
	 * Sets the current received event
	 * @param event
	 */
	protected void setEvent(Object event) {
		fEvent = event;
	}

	/**
	 * Disconnects the waiter from whatever it is waiting on
	 */
	protected abstract void unregister();

}
