/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.e4.tools.emf.ui.internal.common.component.PlaceholderEditor;
import org.eclipse.e4.ui.model.application.ui.advanced.MAdvancedFactory;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.junit.Test;

/**
 * Tests {@link PlaceholderEditor#hasReferenceIdMismatch(MPlaceholder)}, which
 * detects placeholders whose {@code elementId} does not match the referenced
 * element's {@code elementId}.
 */
public class PlaceholderEditorTest {

	private static final String ID = "org.eclipse.example.part"; //$NON-NLS-1$
	private static final String OTHER_ID = "org.eclipse.example.Part"; //$NON-NLS-1$

	private static MPlaceholder placeholder(String placeholderId, MPart ref) {
		final MPlaceholder placeholder = MAdvancedFactory.INSTANCE.createPlaceholder();
		placeholder.setElementId(placeholderId);
		placeholder.setRef(ref);
		return placeholder;
	}

	private static MPart part(String partId) {
		final MPart part = MBasicFactory.INSTANCE.createPart();
		part.setElementId(partId);
		return part;
	}

	@Test
	public void testNullPlaceholder() {
		assertFalse(PlaceholderEditor.hasReferenceIdMismatch(null));
	}

	@Test
	public void testNoReference() {
		assertFalse(PlaceholderEditor.hasReferenceIdMismatch(placeholder(ID, null)));
	}

	@Test
	public void testMatchingIds() {
		assertFalse(PlaceholderEditor.hasReferenceIdMismatch(placeholder(ID, part(ID))));
	}

	@Test
	public void testMismatchingIds() {
		assertTrue(PlaceholderEditor.hasReferenceIdMismatch(placeholder(OTHER_ID, part(ID))));
	}

	@Test
	public void testCaseSensitiveMismatch() {
		assertTrue(PlaceholderEditor.hasReferenceIdMismatch(
				placeholder("foo.filterDistribution", part("foo.filterdistribution")))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBothIdsNull() {
		assertFalse(PlaceholderEditor.hasReferenceIdMismatch(placeholder(null, part(null))));
	}

	@Test
	public void testPlaceholderIdNullButReferenceIdSet() {
		assertTrue(PlaceholderEditor.hasReferenceIdMismatch(placeholder(null, part(ID))));
	}

	@Test
	public void testReferenceIdNullButPlaceholderIdSet() {
		assertTrue(PlaceholderEditor.hasReferenceIdMismatch(placeholder(ID, part(null))));
	}
}
