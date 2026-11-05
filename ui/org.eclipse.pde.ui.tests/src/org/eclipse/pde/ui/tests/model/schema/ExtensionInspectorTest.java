/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.schema;

import static org.eclipse.pde.ui.tests.util.ExtensionSchemaTestUtils.createClientsSchema;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.schema.ExtensionInspector;
import org.eclipse.pde.internal.core.schema.Schema;
import org.junit.Test;

/**
 * Tests that the attributes of an extension are found by the kind that the
 * schema of its extension point declares for them.
 */
public class ExtensionInspectorTest {

	@Test
	public void testFindsAttributesOfJavaKind() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<client class="p.Type" icon="icons/type.svg" label="p.Label"/>
				""");

		assertEquals(List.of("p.Type"), findValues(extension, IMetaAttribute.JAVA));
	}

	@Test
	public void testFindsAttributesOfResourceKind() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<client class="p.Type" icon="icons/type.svg" label="p.Label"/>
				""");

		assertEquals(List.of("icons/type.svg"), findValues(extension, IMetaAttribute.RESOURCE));
	}

	@Test
	public void testFindsAttributesOfNestedElements() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<client class="p.Outer">
				   <client class="p.Inner"/>
				</client>
				""");

		assertEquals(List.of("p.Outer", "p.Inner"), findValues(extension, IMetaAttribute.JAVA));
	}

	@Test
	public void testFindsJavaAttributeGivenAsUndeclaredElement() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<client>
				   <factory class="p.Type">
				      <parameter name="key" value="p.Value"/>
				   </factory>
				</client>
				""");

		assertEquals(List.of("p.Type"), findValues(extension, IMetaAttribute.JAVA));
	}

	@Test
	public void testFindsJavaAttributeGivenAsDeclaredElementOnce() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<client>
				   <class class="p.Type">
				      <parameter name="key" value="p.Value"/>
				   </class>
				</client>
				""");

		assertEquals("The attribute must be found once although the element both is declared and represents a "
				+ "java attribute of its parent", List.of("p.Type"), findValues(extension, IMetaAttribute.JAVA));
	}

	@Test
	public void testIgnoresJavaAttributeGivenAsElementWithoutType() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<client>
				   <class>
				      <parameter name="key" value="p.Value"/>
				   </class>
				   <factory>
				      <parameter name="key" value="p.Value"/>
				   </factory>
				</client>
				""");

		assertEquals("An element giving a java attribute of its parent but no type, e.g. while being edited, must be "
				+ "ignored", List.of(), findValues(extension, IMetaAttribute.JAVA));
	}

	@Test
	public void testDoesNotFindResourceAttributeGivenAsElement() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<client>
				   <icon class="icons/type.svg"/>
				</client>
				""");

		assertEquals(List.of(), findValues(extension, IMetaAttribute.RESOURCE));
	}

	@Test
	public void testDoesNotFindAttributesOfUndeclaredElements() throws CoreException {
		IPluginExtension extension = loadExtension("""
				<unknown class="p.Type"/>
				""");

		assertEquals(List.of(), findValues(extension, IMetaAttribute.JAVA));
	}

	private static List<String> findValues(IPluginExtension extension, int kind) {
		Schema schema = new Schema("test", "clients", "Clients", false);
		schema.load(new ByteArrayInputStream(createClientsSchema("test").getBytes(StandardCharsets.UTF_8)));
		return new ExtensionInspector(schema).findAttributes(extension, kind).map(IPluginAttribute::getValue).toList();
	}

	private static IPluginExtension loadExtension(String content) throws CoreException {
		String pluginXml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<?eclipse version="3.4"?>
				<plugin>
				   <extension point="test.clients">
				      %s
				   </extension>
				</plugin>
				""".formatted(content.strip());
		ExternalPluginModel model = new ExternalPluginModel();
		model.load(new ByteArrayInputStream(pluginXml.getBytes(StandardCharsets.UTF_8)), false);
		return model.getPluginBase().getExtensions()[0];
	}

}
