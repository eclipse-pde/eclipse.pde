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
package org.eclipse.pde.api.tools.internal.provisional;

/**
 * The session manager holds a list of currently available sessions.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISessionManager {

	/**
	 * Adds the given session to this session manager. If the session is already
	 * part of this session manager, the method has no effect.
	 * 
	 * @param session the new session
	 * @param activate if <code>true</code> the session will also be activated, <code>false</code> otherwise
	 * @throws IllegalArgumentException if the given session is null
	 */
	public void addSession(ISession session, boolean activate);

	/**
	 * Removes the given session. If the session is not in included in this
	 * session manager, this method has no effect.
	 * 
	 * @param session the given session to remove
	 */
	public void removeSession(ISession session);

	/**
	 * Removes all available sessions.
	 */
	public void removeAllSessions();

	/**
	 * Returns all available sessions registered with this session manager.
	 * 
	 * @return list of available sessions
	 */
	public ISession[] getSessions();

	/**
	 * Adds the given session listener unless it has been added before.
	 * 
	 * @param listener the given session listener to add
	 * @throws IllegalArgumentException if the given listener is null
	 */
	public void addSessionListener(ISessionListener listener);

	/**
	 * Removes the given session listener. If the listener has not been added
	 * before this method has no effect.
	 * 
	 * @param listener the given session listener to remove
	 */
	public void removeSessionListener(ISessionListener listener);

	/**
	 * Returns the active session or <code>null</code> if there is no session.
	 * 
	 * @return active session or <code>null</null>
	 */
	public ISession getActiveSession();
	/**
	 * Activates the given session. If the session is not in included in this
	 * session manager this method has no effect.
	 * 
	 * @param session the given session or <code>null</code> to remove the active session.
	 */
	public void activateSession(ISession session);
}
