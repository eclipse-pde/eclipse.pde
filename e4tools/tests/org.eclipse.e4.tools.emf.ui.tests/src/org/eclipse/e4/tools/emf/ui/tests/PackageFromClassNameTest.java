/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs.NonReferencedActionPage;
import org.junit.jupiter.api.Test;

public class PackageFromClassNameTest {

	@Test
	public void testPackageFromClassName() {
		assertEquals("org.example", NonReferencedActionPage.getPackageFromClassName("org.example.Handler")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("", NonReferencedActionPage.getPackageFromClassName("Handler")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("org.example.Outer", //$NON-NLS-1$
				NonReferencedActionPage.getPackageFromClassName("org.example.Outer.Inner")); //$NON-NLS-1$
	}
}
