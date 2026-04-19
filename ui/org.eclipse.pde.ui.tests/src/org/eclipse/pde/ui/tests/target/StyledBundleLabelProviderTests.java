/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider;
import org.junit.Test;

/**
 * Tests for {@link StyledBundleLabelProvider}, in particular the optional
 * inline source-location suffix that is appended to {@link TargetBundle}
 * labels when a target context is set.
 */
public class StyledBundleLabelProviderTests extends AbstractTargetTest {

	private static final String SEPARATOR = " - "; //$NON-NLS-1$

	/**
	 * Without a target context the label must not carry any source-location
	 * suffix.
	 */
	@Test
	public void testBundleLabelWithoutTargetContextOmitsSource() throws Exception {
		Path dirPath = extractAbcdePlugins().resolve("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		TargetBundle[] bundles = definition.getAllBundles();
		assertNotNull("Expected resolved bundles", bundles);
		assertTrue("Expected at least one bundle", bundles.length > 0);

		StyledBundleLabelProvider provider = new StyledBundleLabelProvider(true, false);
		try {
			for (TargetBundle bundle : bundles) {
				String text = provider.getText(bundle);
				assertFalse("Label must not contain source suffix without target context: " + text,
						text.contains(SEPARATOR + dirPath.toString()));
			}
		} finally {
			provider.dispose();
		}
	}

	/**
	 * With a target context set, every bundle label must be suffixed with its
	 * originating location so duplicates can be distinguished.
	 */
	@Test
	public void testBundleLabelWithTargetContextAppendsSource() throws Exception {
		Path dirPath = extractAbcdePlugins().resolve("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		TargetBundle[] bundles = definition.getAllBundles();
		assertNotNull("Expected resolved bundles", bundles);
		assertTrue("Expected at least one bundle", bundles.length > 0);

		StyledBundleLabelProvider provider = new StyledBundleLabelProvider(true, false);
		try {
			provider.setTargetContext(definition);
			for (TargetBundle bundle : bundles) {
				String text = provider.getText(bundle);
				assertTrue("Label should end with source location suffix but was: " + text,
						text.endsWith(SEPARATOR + dirPath.toString()));
			}
		} finally {
			provider.dispose();
		}
	}

	/**
	 * When a bundle exists in two different locations, each resolved
	 * {@link TargetBundle} must carry its owning location's path so the user
	 * can tell duplicates apart — the scenario that motivated this feature.
	 */
	@Test
	public void testBundleLabelDistinguishesDuplicatesFromDifferentLocations() throws Exception {
		Path abcdePath = extractAbcdePlugins().resolve("plugins");
		Path multiPath = extractMultiVersionPlugins();

		ITargetDefinition definition = getNewTarget();
		ITargetLocation abcdeLocation = getTargetService().newDirectoryLocation(abcdePath.toString());
		ITargetLocation multiLocation = getTargetService().newDirectoryLocation(multiPath.toString());
		definition.setTargetLocations(new ITargetLocation[] { abcdeLocation, multiLocation });
		definition.resolve(null);

		TargetBundle[] abcdeBundles = abcdeLocation.getBundles();
		TargetBundle[] multiBundles = multiLocation.getBundles();
		assertNotNull("Expected abcde bundles", abcdeBundles);
		assertNotNull("Expected multi-version bundles", multiBundles);
		assertTrue("Expected abcde location to contribute bundles", abcdeBundles.length > 0);
		assertTrue("Expected multi-version location to contribute bundles", multiBundles.length > 0);

		StyledBundleLabelProvider provider = new StyledBundleLabelProvider(true, false);
		try {
			provider.setTargetContext(definition);
			for (TargetBundle bundle : abcdeBundles) {
				String text = provider.getText(bundle);
				assertTrue("abcde bundle label should carry abcde path but was: " + text,
						text.endsWith(SEPARATOR + abcdePath.toString()));
			}
			for (TargetBundle bundle : multiBundles) {
				String text = provider.getText(bundle);
				assertTrue("multi-version bundle label should carry multi path but was: " + text,
						text.endsWith(SEPARATOR + multiPath.toString()));
			}
		} finally {
			provider.dispose();
		}
	}

	/**
	 * Clearing the target context restores the unsuffixed label, so the
	 * provider can be reused across editors.
	 */
	@Test
	public void testClearingTargetContextRemovesSource() throws Exception {
		Path dirPath = extractAbcdePlugins().resolve("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		TargetBundle bundle = definition.getAllBundles()[0];
		StyledBundleLabelProvider provider = new StyledBundleLabelProvider(true, false);
		try {
			provider.setTargetContext(definition);
			String withContext = provider.getText(bundle);
			assertTrue("Expected source suffix when context is set: " + withContext,
					withContext.endsWith(SEPARATOR + dirPath.toString()));

			provider.setTargetContext(null);
			String withoutContext = provider.getText(bundle);
			assertFalse("Expected no source suffix after context cleared: " + withoutContext,
					withoutContext.contains(SEPARATOR + dirPath.toString()));
		} finally {
			provider.dispose();
		}
	}

	/**
	 * The styled string for a resolved bundle without target context should
	 * still start with the bundle's symbolic name — a sanity check that the
	 * new code does not disturb the base label.
	 */
	@Test
	public void testBundleLabelStartsWithSymbolicName() throws Exception {
		Path dirPath = extractAbcdePlugins().resolve("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		TargetBundle[] bundles = definition.getAllBundles();
		assertTrue("Expected resolved abcde bundles", bundles.length > 0);

		StyledBundleLabelProvider provider = new StyledBundleLabelProvider(true, false);
		try {
			TargetBundle a = Arrays.stream(bundles)
					.filter(b -> "bundle.a".equals(b.getBundleInfo().getSymbolicName())).findFirst()
					.orElse(bundles[0]);
			String text = provider.getText(a);
			assertEquals(a.getBundleInfo().getSymbolicName(), text.split(" ")[0]);
		} finally {
			provider.dispose();
		}
	}
}
