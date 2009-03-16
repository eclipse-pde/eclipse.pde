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

import org.eclipse.pde.api.tools.internal.util.TarException;

import junit.framework.TestCase;

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
		assertNotNull("should be a new object created", exception);
		exception = new TarException("New Tar Exception");
		assertNotNull("should be a new object created", exception);
		exception = new TarException("New Tar Exception", new Exception());
	}
	
}
