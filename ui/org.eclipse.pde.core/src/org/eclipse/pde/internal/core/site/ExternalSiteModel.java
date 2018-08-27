/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.site;

import org.eclipse.core.runtime.CoreException;

public class ExternalSiteModel extends AbstractSiteModel {

	private static final long serialVersionUID = 1L;

	@Override
	protected void updateTimeStamp() {
	}

	@Override
	public boolean isInSync() {
		return true;
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public void load() throws CoreException {
	}

}
