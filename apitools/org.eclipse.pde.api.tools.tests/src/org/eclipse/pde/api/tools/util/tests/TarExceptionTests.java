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
package org.eclipse.pde.api.tools.util.tests;

import junit.framework.TestCase;

import org.eclipse.pde.api.tools.internal.util.TarException;

/**
 * Tests creating {@link org.eclipse.pde.api.tools.internal.util.TarException}s
 *
 * @since 1.0.1
 */
public class TarExceptionTests extends TestCase {

	/**
	 * Creates new {@link TarException}s
	 */
	public void testCreateTarExceptions() {
		TarException exception = new TarException();
		assertNotNull("should be a new object created", exception); //$NON-NLS-1$
		exception = new TarException("New Tar Exception"); //$NON-NLS-1$
		assertNotNull("should be a new object created", exception); //$NON-NLS-1$
		exception = new TarException("New Tar Exception", new Exception()); //$NON-NLS-1$
	}

}
