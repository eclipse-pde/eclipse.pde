/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ISessionListener;
import org.eclipse.pde.api.tools.internal.provisional.ISessionManager;

/**
 * Implementation of the ISessionManager.
 * Synchronize all accesses to prevent concurrent modifications.
 */
public class SessionManager implements ISessionManager {

	// use a list so that we can preserve the order
	private List sessions = new ArrayList();
	private Set listeners = new HashSet();
	private ISession activeSession;

	public synchronized void addSession(ISession session, boolean activate) {
		if (session == null) {
			throw new IllegalArgumentException("The given session cannot be null"); //$NON-NLS-1$
		}
		if (!this.sessions.contains(session)) {
			this.sessions.add(session);
			fireSessionAdded(session);
		}
		if (activate) {
			this.activeSession = session;
			fireSessionActivated(session);
		}
	}

	public synchronized void removeSession(ISession session) {
		if (sessions.remove(session)) {
			if (session.equals(this.activeSession)) {
				this.activeSession = null;
				fireSessionActivated(null);
			}
			fireSessionRemoved(session);
		}
	}

	public synchronized void removeAllSessions() {
		ISession[] allSessions = (ISession[]) this.sessions.toArray(new ISession[this.sessions.size()]);
		this.sessions.clear();
		this.activeSession = null;
		fireSessionActivated(null);
		for (int i = 0; i < allSessions.length; i++) {
			ISession session = allSessions[i];
			fireSessionRemoved(session);
		}
	}

	public synchronized ISession[] getSessions() {
		return (ISession[]) sessions.toArray(new ISession[sessions.size()]);
	}

	public synchronized void addSessionListener(ISessionListener listener) {
		if (listener == null) throw new IllegalArgumentException("The given listener cannot be null"); //$NON-NLS-1$
		listeners.add(listener);
	}

	public synchronized void removeSessionListener(ISessionListener listener) {
		listeners.remove(listener);
	}

	protected synchronized void fireSessionAdded(ISession session) {
		Iterator i = listeners.iterator();
		while (i.hasNext()) {
			((ISessionListener) i.next()).sessionAdded(session);
		}
	}

	protected synchronized void fireSessionRemoved(ISession session) {
		Iterator i = listeners.iterator();
		while (i.hasNext()) {
			((ISessionListener) i.next()).sessionRemoved(session);
		}
	}

	public ISession getActiveSession() {
		return this.activeSession;
	}

	protected synchronized void fireSessionActivated(ISession session) {
		Iterator i = listeners.iterator();
		while (i.hasNext()) {
			((ISessionListener) i.next()).sessionActivated(session);
		}
	}

	public void activateSession(ISession session) {
		if (this.sessions.contains(session) && !session.equals(this.activeSession)) {
			this.activeSession = session;
			fireSessionActivated(session);
		}
	}
}
