/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.context;

import org.eclipse.core.resources.IFile;

public interface IInputContextListener {
	/**
	 * Informs the listener that a new context has been added.
	 * This should result in a new source tab.
	 * @param context
	 */
	void contextAdded(InputContext context);

	/**
	 * Informs the listener that the context has been removed.
	 * This should result in removing the source tab.
	 * @param context
	 */
	void contextRemoved(InputContext context);

	/**
	 * Informs the listener that a monitored file has
	 * been added.
	 * @param monitoredFile the file we were monitoring
	 */
	void monitoredFileAdded(IFile monitoredFile);

	/**
	 * Informs the listener that a monitored file has
	 * been removed.
	 * @param monitoredFile
	 * @return <code>true</code> if it is OK to remove
	 * the associated context, <code>false</code> otherwise.
	 */
	boolean monitoredFileRemoved(IFile monitoredFile);
}
