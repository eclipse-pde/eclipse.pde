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
 * Listener interface for changes of the session manager.
 * 
 * @see ISessionManager#addSessionListener(ISessionListener)
 * @see ISessionManager#removeSessionListener(ISessionListener)
 */
public interface ISessionListener {

	/**
	 * Called when a session has been added.
	 * 
	 * @param addedSession the given added session
	 */  
	public void sessionAdded(ISession addedSession);

	/**
	 * Called when a session has been removed.
	 * 
	 * @param removedSession the given removed session
	 */
	public void sessionRemoved(ISession removedSession);
	
	/**
	 * Called when a new session has been activated or the last session has been
	 * removed. In this case <code>null</code> is passed as a parameter.
	 * 
	 * @param session the given activated session or <code>null</code>
	 */
	public void sessionActivated(ISession session);
}
