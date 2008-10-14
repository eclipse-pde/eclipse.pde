/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;

import junit.framework.TestCase;

import org.eclipse.pde.api.tools.internal.model.TypeStructureCache;

/**
 * Test to write out type structure statistics. Used for performance tuning.
 */
public class BeanCounter extends TestCase {

	public BeanCounter(String name) {
		super(name);
	}
	
	public void testStats() {
		System.out.println(TypeStructureCache.getStats());
	}
	
}
