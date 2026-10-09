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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.tools.emf.ui.common.IClassContributionProvider.ContributionData;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.ContributionDataFile;
import org.junit.jupiter.api.Test;

public class ContributionDataFileTest {

	private static ContributionData data() {
		return new ContributionData("org.example", null, "png", "icons/sample.png"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testWrappersOfSameDataAreEqual() {
		final ContributionData data = data();
		final ContributionDataFile file1 = new ContributionDataFile(data);
		final ContributionDataFile file2 = new ContributionDataFile(data);

		assertEquals(file1, file2);
		assertEquals(file1.hashCode(), file2.hashCode());
		assertNotEquals(file1, new ContributionDataFile(data()));
	}

	@Test
	public void testWrapperFindsCachedValue() {
		final ContributionData data = data();
		final Map<IFile, Object> cache = new HashMap<>();
		final Object value = new Object();
		cache.put(new ContributionDataFile(data), value);

		assertSame(value, cache.get(new ContributionDataFile(data)));
	}
}
