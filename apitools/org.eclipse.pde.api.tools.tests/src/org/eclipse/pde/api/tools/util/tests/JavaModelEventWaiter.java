/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

/**
 * This class will cause the current thread to wait for an acceptable java model event
 * to be received.
 * This waiter automatically registers itself as an element changed listener when it is created,
 * and removes itself once an acceptable event has been received; meaning it can only be used to
 * wait for one event.
 * @since 1.0.0
 */
public class JavaModelEventWaiter extends AbstractApiEventWaiter implements IElementChangedListener {

	int fDKind = -1;
	int fDDetails = -1;
	int fElementType = -1;
	String fElementName = null;

	/**
	 * Constructor
	 * @param elementname
	 * @param deltakind
	 * @param deltadetails
	 * @param elementtype
	 */
	public JavaModelEventWaiter(String elementname, int deltakind, int deltadetails, int elementtype) {
		fElementName = elementname;
		fDKind = deltakind;
		fDDetails = deltadetails;
		fElementType = elementtype;
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
	}

	@Override
	public synchronized void elementChanged(ElementChangedEvent event) {
		if(accept(event)) {
			setEvent(event);
			this.notifyAll();
			unregister();
		}
	}

	@Override
	public void unregister() {
		JavaCore.removeElementChangedListener(this);
	}

	/**
	 * Returns if we care about the given event or not
	 * @param event
	 * @return true is we care about the given event, false otherwise
	 */
	protected boolean accept(ElementChangedEvent event) {
		if(event.getSource() instanceof IJavaElementDelta) {
			IJavaElementDelta delta = (IJavaElementDelta) event.getSource();
			IJavaElementDelta[] deltas = delta.getAffectedChildren();
			if(deltas.length == 0) {
				deltas = new IJavaElementDelta[] {delta};
			}
			return processDelta(deltas);
		}
		return false;
	}

	/**
	 * Processes the listing of deltas of interest
	 * @param deltas
	 */
	protected boolean processDelta(IJavaElementDelta[] deltas) {
		IJavaElementDelta delta = null;
		for(int i = 0; i < deltas.length; i++) {
			delta = deltas[i];
			if(delta.getKind() == fDKind) {
				if(fElementType == delta.getElement().getElementType()) {
					if(delta.getElement().getElementName().equals(fElementName) &&
							delta.getFlags() == fDDetails) {
						return true;
					}
				}
				else {
					return processDelta(delta.getAffectedChildren());
				}
			}
			else {
				return processDelta(delta.getAffectedChildren());
			}
		}
		return false;
	}
}
