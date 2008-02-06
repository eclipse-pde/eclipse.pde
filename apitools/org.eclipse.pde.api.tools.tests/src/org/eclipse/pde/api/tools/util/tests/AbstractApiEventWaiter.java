/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

/**
 * Abstract class for Api tooling event waiters to extend
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
				wait(DEFAULT_TIMEOUT);
			}
			catch(InterruptedException e) {
				System.err.println("Thread interrupted waiting for element changed event");
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
