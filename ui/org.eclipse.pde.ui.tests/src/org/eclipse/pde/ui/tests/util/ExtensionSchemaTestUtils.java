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
package org.eclipse.pde.ui.tests.util;

/**
 * Provides extension point schemas for tests of features that interpret the
 * attributes of extensions according to the schema of their extension point.
 */
public final class ExtensionSchemaTestUtils {

	private ExtensionSchemaTestUtils() {
	}

	/**
	 * Returns the schema of an extension point {@code clients} declared by the
	 * given plug-in. Its {@code client} elements, which may be nested, declare
	 * the attributes {@code class} and {@code factory} to be of the kind java,
	 * {@code icon} to be of the kind resource and {@code label} to be a plain
	 * string. Like in the schemas of org.eclipse.ui, the {@code class}
	 * attribute may also be given as declared nested element {@code class},
	 * which takes parameters, while there is no such declaration for
	 * {@code factory}.
	 */
	public static String createClientsSchema(String pluginId) {
		return """
				<?xml version='1.0' encoding='UTF-8'?>
				<schema targetNamespace="%1$s" xmlns="http://www.w3.org/2001/XMLSchema">
				   <annotation>
				      <appinfo>
				         <meta.schema plugin="%1$s" id="clients" name="Clients"/>
				      </appinfo>
				   </annotation>
				   <element name="extension">
				      <complexType>
				         <sequence>
				            <element ref="client" minOccurs="0" maxOccurs="unbounded"/>
				         </sequence>
				         <attribute name="point" type="string" use="required"/>
				      </complexType>
				   </element>
				   <element name="client">
				      <complexType>
				         <sequence>
				            <element ref="client" minOccurs="0" maxOccurs="unbounded"/>
				            <element ref="class" minOccurs="0" maxOccurs="1"/>
				         </sequence>
				         <attribute name="class" type="string">
				            <annotation>
				               <appinfo>
				                  <meta.attribute kind="java"/>
				               </appinfo>
				            </annotation>
				         </attribute>
				         <attribute name="factory" type="string">
				            <annotation>
				               <appinfo>
				                  <meta.attribute kind="java"/>
				               </appinfo>
				            </annotation>
				         </attribute>
				         <attribute name="icon" type="string">
				            <annotation>
				               <appinfo>
				                  <meta.attribute kind="resource"/>
				               </appinfo>
				            </annotation>
				         </attribute>
				         <attribute name="label" type="string"/>
				      </complexType>
				   </element>
				   <element name="class">
				      <complexType>
				         <sequence>
				            <element ref="parameter" minOccurs="0" maxOccurs="unbounded"/>
				         </sequence>
				         <attribute name="class" type="string">
				            <annotation>
				               <appinfo>
				                  <meta.attribute kind="java"/>
				               </appinfo>
				            </annotation>
				         </attribute>
				      </complexType>
				   </element>
				   <element name="parameter">
				      <complexType>
				         <attribute name="name" type="string" use="required"/>
				         <attribute name="value" type="string" use="required"/>
				      </complexType>
				   </element>
				</schema>
				""".formatted(pluginId);
	}

}
