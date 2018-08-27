/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.ISession;
import org.eclipse.pde.api.tools.internal.provisional.ISessionListener;
import org.eclipse.pde.api.tools.internal.provisional.ISessionManager;

/**
 * Implementation of the ISessionManager. Synchronize all accesses to prevent
 * concurrent modifications.
 */
public class SessionManager implements ISessionManager {

	// use a list so that we can preserve the order
	private List<ISession> sessions = new ArrayList<>();
	private Set<ISessionListener> listeners = new HashSet<>();
	private ISession activeSession;

	@Override
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

	@Override
	public synchronized void removeSession(ISession session) {
		if (sessions.remove(session)) {
			if (session.equals(this.activeSession)) {
				this.activeSession = null;
				fireSessionActivated(null);
			}
			fireSessionRemoved(session);
		}
	}

	@Override
	public synchronized void removeAllSessions() {
		ISession[] allSessions = this.sessions.toArray(new ISession[this.sessions.size()]);
		this.sessions.clear();
		this.activeSession = null;
		fireSessionActivated(null);
		for (ISession session : allSessions) {
			fireSessionRemoved(session);
		}
	}

	@Override
	public synchronized ISession[] getSessions() {
		return sessions.toArray(new ISession[sessions.size()]);
	}

	@Override
	public synchronized void addSessionListener(ISessionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("The given listener cannot be null"); //$NON-NLS-1$
		}
		listeners.add(listener);
	}

	@Override
	public synchronized void removeSessionListener(ISessionListener listener) {
		listeners.remove(listener);
	}

	protected synchronized void fireSessionAdded(ISession session) {
		for (ISessionListener listener : listeners) {
			listener.sessionAdded(session);
		}
	}

	protected synchronized void fireSessionRemoved(ISession session) {
		for (ISessionListener listener : listeners) {
			listener.sessionRemoved(session);
		}
	}

	@Override
	public ISession getActiveSession() {
		return this.activeSession;
	}

	protected synchronized void fireSessionActivated(ISession session) {
		for (ISessionListener listener : listeners) {
			listener.sessionActivated(session);
		}
	}

	@Override
	public void activateSession(ISession session) {
		if (this.sessions.contains(session) && !session.equals(this.activeSession)) {
			this.activeSession = session;
			fireSessionActivated(session);
		}
	}
}
