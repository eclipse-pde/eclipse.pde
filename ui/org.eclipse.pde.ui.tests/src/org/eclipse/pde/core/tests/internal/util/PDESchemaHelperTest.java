/*******************************************************************************
 * Copyright (c) 2019 AixpertSoft GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      AixpertSoft GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.core.iproduct.IConfigurationProperty;
import org.eclipse.pde.internal.core.product.ProductModel;
import org.eclipse.pde.internal.core.product.ProductModelFactory;
import org.eclipse.pde.internal.core.util.PDESchemaHelper;
import org.junit.Test;

public class PDESchemaHelperTest {

	private ProductModelFactory fProductModelFactory;

	Set<IConfigurationProperty> fConfigurationProperties = new HashSet<>();

	public PDESchemaHelperTest() {
		initConfigurationProperties();
	}

	private void initConfigurationProperties() {
		ProductModel productModel = new ProductModel();
		fProductModelFactory = new ProductModelFactory(productModel);

		// create a single property for win32 / all architectures
		IConfigurationProperty property = fProductModelFactory.createConfigurationProperty();
		property.setName("org.osgi.instance.area");
		property.setValue("$APPDATA$/Eclipse");
		property.setOs(Platform.OS_WIN32);
		property.setArch(PDESchemaHelper.ALL_ARCH);
		fConfigurationProperties.add(property);
		IConfigurationProperty property2 = fProductModelFactory.createConfigurationProperty();
		property2.setName("osgi.configuration.area");
		property2.setValue("/usr/local/share/Eclipse");
		property2.setOs(Platform.OS_LINUX);
		property2.setArch(Platform.ARCH_X86);
		fConfigurationProperties.add(property2);
		IConfigurationProperty property3 = fProductModelFactory.createConfigurationProperty();
		property3.setName("p1");
		property3.setValue("v1");
		property3.setOs(PDESchemaHelper.ALL_OS);
		property3.setArch(Platform.ARCH_X86);
		fConfigurationProperties.add(property3);

	}

	@Test
	public void testContainsMatchingProperty() {
		// exact same property test
		boolean containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"org.osgi.instance.area", Platform.OS_WIN32, PDESchemaHelper.ALL_ARCH);
		assertTrue(containsMatchingProperty);

		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"osgi.configuration.area", Platform.OS_LINUX, Platform.ARCH_X86);
		assertTrue(containsMatchingProperty);

		// specific architecture
		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"org.osgi.instance.area", Platform.OS_WIN32, Platform.ARCH_X86);
		assertTrue(containsMatchingProperty);

		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"org.osgi.instance.area", Platform.OS_WIN32, Platform.ARCH_X86_64);
		assertTrue(containsMatchingProperty);

		// for all OS
		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"org.osgi.instance.area", PDESchemaHelper.ALL_OS, PDESchemaHelper.ALL_ARCH);
		assertTrue(containsMatchingProperty);

		// for all os but specific arch
		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"org.osgi.instance.area", PDESchemaHelper.ALL_OS, Platform.ARCH_X86);
		assertTrue(containsMatchingProperty);

		// for different OS
		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"org.osgi.instance.area", Platform.OS_LINUX, PDESchemaHelper.ALL_ARCH);
		assertFalse(containsMatchingProperty);

		// different property
		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties,
				"differnt.property.name", Platform.OS_WIN32, PDESchemaHelper.ALL_ARCH);
		assertFalse(containsMatchingProperty);

		// all os but different architecture
		containsMatchingProperty = PDESchemaHelper.containsMatchingProperty(fConfigurationProperties, "p1",
				PDESchemaHelper.ALL_OS, Platform.ARCH_X86_64);
		assertFalse(containsMatchingProperty);

	}
}
