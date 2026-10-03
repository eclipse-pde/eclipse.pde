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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.e4.tools.emf.ui.internal.imp.RegistryStruct;
import org.eclipse.e4.tools.emf.ui.internal.imp.RegistryUtil;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.junit.jupiter.api.Test;

public class RegistryUtilTest {

	private static IConfigurationElement element(String contributor, Map<String, String> attributes) {
		final IContributor c = (IContributor) Proxy.newProxyInstance(RegistryUtilTest.class.getClassLoader(),
				new Class<?>[] { IContributor.class }, (proxy, method, args) -> contributor);
		return (IConfigurationElement) Proxy.newProxyInstance(RegistryUtilTest.class.getClassLoader(),
				new Class<?>[] { IConfigurationElement.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getAttribute" -> attributes.get(args[0]); //$NON-NLS-1$
				case "getContributor" -> c; //$NON-NLS-1$
				default -> null;
				});
	}

	@Test
	public void testPartStructFollowsHint() {
		final RegistryStruct views = RegistryUtil.getStruct(MPart.class, RegistryUtil.HINT_VIEW);
		assertEquals("org.eclipse.ui.views", views.getExtensionPoint()); //$NON-NLS-1$
		assertEquals("view", views.getExtensionPointName()); //$NON-NLS-1$

		final RegistryStruct editors = RegistryUtil.getStruct(MPart.class, RegistryUtil.HINT_EDITOR);
		assertEquals("org.eclipse.ui.editors", editors.getExtensionPoint()); //$NON-NLS-1$
		assertEquals("editor", editors.getExtensionPointName()); //$NON-NLS-1$
	}

	@Test
	public void testPartDescriptorStructFollowsHint() {
		assertEquals("org.eclipse.ui.editors", //$NON-NLS-1$
				RegistryUtil.getStruct(MPartDescriptor.class, RegistryUtil.HINT_EDITOR).getExtensionPoint());
		assertEquals("org.eclipse.ui.views", //$NON-NLS-1$
				RegistryUtil.getStruct(MPartDescriptor.class, new String(RegistryUtil.HINT_VIEW)).getExtensionPoint());
		assertNull(RegistryUtil.getStruct(MPartDescriptor.class, "unknown")); //$NON-NLS-1$
	}

	@Test
	public void testViewPartDescriptorHasContributionUri() {
		final IConfigurationElement view = element("org.example", //$NON-NLS-1$
				Map.of("id", "org.example.view", "name", "Example", "class", "org.example.ExampleView")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

		final MApplicationElement[] result = RegistryUtil.getModelElements(MPartDescriptor.class,
				RegistryUtil.HINT_VIEW, null, view);

		assertEquals(1, result.length);
		final MPartDescriptor descriptor = (MPartDescriptor) result[0];
		assertEquals("org.example.view", descriptor.getElementId()); //$NON-NLS-1$
		assertEquals("bundleclass://org.example/org.example.ExampleView", descriptor.getContributionURI()); //$NON-NLS-1$
	}
}
