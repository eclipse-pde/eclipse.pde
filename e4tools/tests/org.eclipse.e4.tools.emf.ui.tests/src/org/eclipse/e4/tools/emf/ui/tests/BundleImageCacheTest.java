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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.BundleImageCache;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;

public class BundleImageCacheTest {

	private static final String MISSING = "/icons/does_not_exist.png"; //$NON-NLS-1$

	private static BundleImageCache newCache() {
		return new BundleImageCache(Display.getDefault(), BundleImageCache.class.getClassLoader());
	}

	@Test
	public void testDisposeKeepsPlaceholderOfOtherCache() {
		final BundleImageCache cache1 = newCache();
		final BundleImageCache cache2 = newCache();
		try {
			final Image placeholder1 = cache1.create(MISSING);
			final Image placeholder2 = cache2.create(MISSING);
			assertNotSame(placeholder1, placeholder2);

			cache2.dispose();

			assertTrue(placeholder2.isDisposed());
			assertFalse(placeholder1.isDisposed());
		} finally {
			cache1.dispose();
			cache2.dispose();
		}
	}

	@Test
	public void testDisposeReleasesPlaceholder() {
		final BundleImageCache cache = newCache();
		final Image placeholder = cache.create(MISSING);

		cache.dispose();

		assertTrue(placeholder.isDisposed());
	}
}
