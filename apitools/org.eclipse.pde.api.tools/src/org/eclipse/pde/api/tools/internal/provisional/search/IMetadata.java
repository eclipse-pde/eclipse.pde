/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.provisional.search;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;

/**
 * Object used to hold API use scan metadata that can be written out by an
 * {@link IApiSearchReporter}
 *
 * @since 1.0.1
 */
public interface IMetadata {

	/**
	 * Writes the current snapshot of metadata out to the given file
	 *
	 * @param file the file to write to
	 * @throws IOException
	 * @throws CoreException
	 */
	public void serializeToFile(File file) throws IOException, CoreException;
}
