/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;

/**
 * Classes that implement this interface are registered with
 * the macro manager to handle cases where the user interrupted
 * the recording process to insert a named index. Upon playback,
 * index handler will be called to handle the named index
 * when reached in the script.
 *
 * @since 3.1
 */

public interface IIndexHandler {
/**
 * Evaluates the state of the platform at the provided script
 * index. Error status returned from the method will cause
 * the script manager to stop the script execution and
 * throw a <code>CoreException</code>.
 * 
 * @param shell the active shell when the index was reached
 * @param indexId the unique identifier of the index
 * inserted in the script
 * @return <code>Status.OK_STATUS</code> if the script can proceed, or 
 * an error status otherwise.
 */
	IStatus processIndex(Shell shell, String indexId);
}