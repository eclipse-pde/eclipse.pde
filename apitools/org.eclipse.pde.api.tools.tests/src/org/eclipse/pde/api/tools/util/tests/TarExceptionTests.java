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
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.pde.api.tools.internal.util.TarException;
import org.junit.jupiter.api.Test;

/**
 * Tests creating {@link org.eclipse.pde.api.tools.internal.util.TarException}s
 *
 * @since 1.0.1
 */
public class TarExceptionTests {

	/**
	 * Creates new {@link TarException}s
	 */
	@Test
	public void testCreateTarExceptions() {
		TarException exception = new TarException();
		assertNotNull(exception, "should be a new object created"); //$NON-NLS-1$
		exception = new TarException("New Tar Exception"); //$NON-NLS-1$
		assertNotNull(exception, "should be a new object created"); //$NON-NLS-1$
		exception = new TarException("New Tar Exception", new Exception()); //$NON-NLS-1$
	}

}
