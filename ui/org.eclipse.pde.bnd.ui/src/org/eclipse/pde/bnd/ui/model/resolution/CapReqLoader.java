/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.resolution;

import java.io.Closeable;

import org.eclipse.core.runtime.IProgressMonitor;

public interface CapReqLoader extends Closeable {

	String getShortLabel();

	String getLongLabel();
	CapReq loadCapReq(IProgressMonitor monitor) throws Exception;

}
