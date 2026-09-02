/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.pde.internal.ui.editor.product.ProductInfoSection;
import org.eclipse.pde.internal.ui.editor.product.ProductInfoSection.ExtensionChange;
import org.junit.Test;

public class ProductInfoSectionTest {

	@Test
	public void testInvalidExtensionDoesNotAbortBatch() {
		IExtensionDelta removed = delta(IExtensionDelta.REMOVED, invalidExtension());
		IExtensionDelta added = delta(IExtensionDelta.ADDED, extension("org.example.app"));

		List<ExtensionChange> changes = ProductInfoSection.toExtensionChanges(new IExtensionDelta[] { removed, added });

		assertEquals(List.of(new ExtensionChange("org.example.app", true)), changes);
	}

	@Test
	public void testKindIsPreserved() {
		IExtensionDelta removed = delta(IExtensionDelta.REMOVED, extension("org.example.a"));
		IExtensionDelta added = delta(IExtensionDelta.ADDED, extension("org.example.b"));

		List<ExtensionChange> changes = ProductInfoSection.toExtensionChanges(new IExtensionDelta[] { removed, added });

		assertEquals(List.of(new ExtensionChange("org.example.a", false), new ExtensionChange("org.example.b", true)),
				changes);
	}

	@Test
	public void testMissingIdsAreSkipped() {
		IExtensionDelta noExtension = delta(IExtensionDelta.ADDED, null);
		IExtensionDelta noId = delta(IExtensionDelta.ADDED, extension(null));

		List<ExtensionChange> changes = ProductInfoSection
				.toExtensionChanges(new IExtensionDelta[] { noExtension, noId });

		assertTrue(changes.isEmpty());
	}

	private static IExtensionDelta delta(int kind, IExtension extension) {
		IExtensionDelta delta = mock(IExtensionDelta.class);
		when(delta.getKind()).thenReturn(kind);
		when(delta.getExtension()).thenReturn(extension);
		return delta;
	}

	private static IExtension extension(String id) {
		IExtension extension = mock(IExtension.class);
		when(extension.getUniqueIdentifier()).thenReturn(id);
		return extension;
	}

	private static IExtension invalidExtension() {
		IExtension extension = mock(IExtension.class);
		when(extension.getUniqueIdentifier()).thenThrow(new InvalidRegistryObjectException());
		return extension;
	}
}
