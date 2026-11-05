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
package org.eclipse.pde.internal.core.schema;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;

/**
 * Inspects the extensions of an extension point based on the schema of that
 * extension point.
 * <p>
 * Elements are looked up by name with {@link ISchema#findElement(String)},
 * which also searches included schemas.
 */
public final class ExtensionInspector {

	private static final String CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$

	private final ISchema schema;

	/**
	 * @param schema the schema of the extension point whose extensions to
	 *               inspect
	 */
	public ExtensionInspector(ISchema schema) {
		this.schema = Objects.requireNonNull(schema);
	}

	/**
	 * Returns the attributes in the elements below the given parent, recursively,
	 * that the schema declares to be of the given kind, such as
	 * {@link IMetaAttribute#JAVA} for attributes holding the name of a Java type.
	 * <p>
	 * A Java type attribute may also be given as nested element named like the
	 * attribute, whose {@code class} attribute then holds the type, which allows
	 * passing parameters, e.g. {@code <class class="..."><parameter .../></class>}.
	 * Some schemas declare such an element, others do not, and both are supported.
	 *
	 * @param parent an extension of the extension point of this schema or one
	 *               of its elements
	 * @param kind   the kind of the attributes to find, one of the kinds defined
	 *               in {@link IMetaAttribute}
	 */
	public Stream<IPluginAttribute> findAttributes(IPluginParent parent, int kind) {
		return Arrays.stream(parent.getChildren()).filter(IPluginElement.class::isInstance)
				.map(IPluginElement.class::cast)
				.flatMap(element -> Stream.of(findDeclared(element, kind), findGivenAsElement(element, parent, kind),
						findAttributes(element, kind)).flatMap(attributes -> attributes));
	}

	private Stream<IPluginAttribute> findDeclared(IPluginElement element, int kind) {
		ISchemaElement schemaElement = schema.findElement(element.getName());
		if (schemaElement == null) {
			return Stream.empty();
		}
		return Arrays.stream(element.getAttributes())
				.filter(attribute -> isAttributeOfKind(schemaElement, attribute.getName(), kind));
	}

	private Stream<IPluginAttribute> findGivenAsElement(IPluginElement element, IPluginParent parent, int kind) {
		if (kind == IMetaAttribute.JAVA && parent instanceof IPluginElement parentElement
				&& isAttributeOfKind(schema.findElement(parentElement.getName()), element.getName(), kind)
				// a declared element whose class attribute is a type is already covered
				&& !isAttributeOfKind(schema.findElement(element.getName()), CLASS_ATTRIBUTE, kind)) {
			return Stream.ofNullable(element.getAttribute(CLASS_ATTRIBUTE));
		}
		return Stream.empty();
	}

	private static boolean isAttributeOfKind(ISchemaElement schemaElement, String attributeName, int kind) {
		if (schemaElement == null) {
			return false;
		}
		ISchemaAttribute attribute = schemaElement.getAttribute(attributeName);
		return attribute != null && attribute.getKind() == kind;
	}

}
