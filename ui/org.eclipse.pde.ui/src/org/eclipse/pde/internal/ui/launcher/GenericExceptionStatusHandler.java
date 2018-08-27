/*******************************************************************************
 * Copyright (c) 2009, 2015 eXXcellent solutions gmbh and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class GenericExceptionStatusHandler implements IStatusHandler {

	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		PDEPlugin.logException(status.getException());
		return null;
	}

}
