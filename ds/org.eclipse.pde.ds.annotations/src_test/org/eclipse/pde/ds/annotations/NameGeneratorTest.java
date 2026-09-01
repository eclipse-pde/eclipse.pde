/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.pde.ds.internal.annotations.DSAnnotationVersion;
import org.eclipse.pde.ds.internal.annotations.NameGenerator;
import org.junit.jupiter.api.Test;

class NameGeneratorTest {

	@Test
	void test13() {
		assertEquals(NameGenerator.createPropertyName("myProperty143", null, DSAnnotationVersion.V1_3), "myProperty143");
		assertEquals(NameGenerator.createPropertyName("$new", null, DSAnnotationVersion.V1_3), "new");
		assertEquals(NameGenerator.createPropertyName("my$$prop", null, DSAnnotationVersion.V1_3), "my$prop");
		assertEquals(NameGenerator.createPropertyName("dot_prop", null, DSAnnotationVersion.V1_3), "dot.prop");
		assertEquals(NameGenerator.createPropertyName("_secret", null, DSAnnotationVersion.V1_3), ".secret");
		assertEquals(NameGenerator.createPropertyName("another__prop", null, DSAnnotationVersion.V1_3), "another_prop");
		assertEquals(NameGenerator.createPropertyName("three___prop", null, DSAnnotationVersion.V1_3), "three_.prop");
		assertEquals(NameGenerator.createPropertyName("four_$__prop", null, DSAnnotationVersion.V1_3), "four._prop");
		assertEquals(NameGenerator.createPropertyName("five_$_prop", null, DSAnnotationVersion.V1_3), "five..prop");
	}

	@Test
	void test14() {
		assertEquals(NameGenerator.createPropertyName("myProperty143", null, DSAnnotationVersion.V1_4), "myProperty143");
		assertEquals(NameGenerator.createPropertyName("$new", null, DSAnnotationVersion.V1_4), "new");
		assertEquals(NameGenerator.createPropertyName("my$$prop", null, DSAnnotationVersion.V1_4), "my$prop");
		assertEquals(NameGenerator.createPropertyName("dot_prop", null, DSAnnotationVersion.V1_4), "dot.prop");
		assertEquals(NameGenerator.createPropertyName("_secret", null, DSAnnotationVersion.V1_4), ".secret");
		assertEquals(NameGenerator.createPropertyName("another__prop", null, DSAnnotationVersion.V1_4), "another_prop");
		assertEquals(NameGenerator.createPropertyName("three___prop", null, DSAnnotationVersion.V1_4), "three_.prop");
		assertEquals(NameGenerator.createPropertyName("four_$__prop", null, DSAnnotationVersion.V1_4), "four._prop");
		assertEquals(NameGenerator.createPropertyName("five_$_prop", null, DSAnnotationVersion.V1_4), "five..prop");
		assertEquals(NameGenerator.createPropertyName("six$_$prop", null, DSAnnotationVersion.V1_4), "six-prop");
		assertEquals(NameGenerator.createPropertyName("seven$$_$prop", null, DSAnnotationVersion.V1_4), "seven$.prop");
		assertEquals(NameGenerator.createPropertyName("myProperty143", "pre.", DSAnnotationVersion.V1_4), "pre.myProperty143");
		assertEquals(NameGenerator.createClassPropertyName("ServiceRanking", null), "service.ranking");
		assertEquals(NameGenerator.createClassPropertyName("Some_Name", null), "some_name");
		assertEquals(NameGenerator.createClassPropertyName("OSGiProperty", null), "osgi.property");
	}

}
