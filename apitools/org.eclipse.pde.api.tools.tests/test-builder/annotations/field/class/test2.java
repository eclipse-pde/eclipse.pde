/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.annotations.NoReference;

/**
 * Test unsupported @NoReference annotation on final fields in a class in the default package
 */
public class test2 {
	@NoReference
	public final Object f1 = null;
	@NoReference
	protected final int f2 = 0;
	@NoReference
	private final char[] f3 = {};
	@NoReference
	final long f4 = 0L;
}
