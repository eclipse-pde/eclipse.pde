/*******************************************************************************
 * Copyright (c) 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.pde.internal.core.util.VMUtil;
import org.junit.Test;

public class VMUtilTest {

	@Test
	public void testParseJavaVersion() {
		assertEquals(3, VMUtil.parseJavaRelease("1.3"));
		assertEquals(4, VMUtil.parseJavaRelease("1.4.1"));
		assertEquals(8, VMUtil.parseJavaRelease("1.8.0_502"));

		assertEquals(9, VMUtil.parseJavaRelease("9.0.1"));
		assertEquals(25, VMUtil.parseJavaRelease("25.1"));
		assertEquals(26, VMUtil.parseJavaRelease("26"));
	}

}
