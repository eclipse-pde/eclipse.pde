/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import org.eclipse.pde.api.tools.annotations.NoOverride;

/**
 * Test unsupported @NoOverride annotation on fields in a class in the default package
 */
public class test10 {
	@NoOverride
	public Object f1 = null;
	@NoOverride
	protected int f2 = 0;
	@NoOverride
	private static char[] f3 = {};
	@NoOverride
	long f4 = 0L;
}
