/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

/**
 * Waits for a resource changed event for a given path in a delta
 *
 * @since 1.0.1
 */
public class ResourceEventWaiter extends AbstractApiEventWaiter implements IResourceChangeListener {

	private IPath path = null;
	private int kind = -1;
	private int flags = -1;
	private int type = -1;

	/**
	 * Constructor
	 * @param path the child path of he delta we are expecting to get an event for
	 * @param kind the kind of the delta
	 * @param type the type of the event
	 * @param flags the flags for the delta
	 */
	public ResourceEventWaiter(IPath path, int type, int kind, int flags) {
		this.path = path;
		this.type = type;
		this.kind = kind;
		this.flags = flags;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public synchronized void resourceChanged(IResourceChangeEvent event) {
		if(accept(event)) {
			setEvent(event);
			this.notifyAll();
			unregister();
		}
	}

	@Override
	protected void unregister() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/**
	 * if this event is the one we are waiting for
	 * @param event the event
	 * @return true if this event is the one we are waiting for
	 */
	protected boolean accept(IResourceChangeEvent event) {
		if(event.getType() == this.type) {
			IResourceDelta delta = event.getDelta();
			if(delta != null) {
				if(delta.getKind() == this.kind && delta.getFlags() == this.flags) {
					return (delta.findMember(this.path) != null);
				}
			}
		}
		return false;
	}
}
