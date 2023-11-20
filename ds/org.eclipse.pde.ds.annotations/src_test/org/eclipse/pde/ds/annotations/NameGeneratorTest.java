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
		assertEquals("myProperty143", NameGenerator.createPropertyName("myProperty143", null, DSAnnotationVersion.V1_3));
		assertEquals("new", NameGenerator.createPropertyName("$new", null, DSAnnotationVersion.V1_3));
		assertEquals("my$prop", NameGenerator.createPropertyName("my$$prop", null, DSAnnotationVersion.V1_3));
		assertEquals("dot.prop", NameGenerator.createPropertyName("dot_prop", null, DSAnnotationVersion.V1_3));
		assertEquals(".secret", NameGenerator.createPropertyName("_secret", null, DSAnnotationVersion.V1_3));
		assertEquals("another_prop", NameGenerator.createPropertyName("another__prop", null, DSAnnotationVersion.V1_3));
		assertEquals("three_.prop", NameGenerator.createPropertyName("three___prop", null, DSAnnotationVersion.V1_3));
		assertEquals("four._prop", NameGenerator.createPropertyName("four_$__prop", null, DSAnnotationVersion.V1_3));
		assertEquals("five..prop", NameGenerator.createPropertyName("five_$_prop", null, DSAnnotationVersion.V1_3));
	}

	@Test
	void test14() {
		assertEquals("myProperty143", NameGenerator.createPropertyName("myProperty143", null, DSAnnotationVersion.V1_4));
		assertEquals("new", NameGenerator.createPropertyName("$new", null, DSAnnotationVersion.V1_4));
		assertEquals("my$prop", NameGenerator.createPropertyName("my$$prop", null, DSAnnotationVersion.V1_4));
		assertEquals("dot.prop", NameGenerator.createPropertyName("dot_prop", null, DSAnnotationVersion.V1_4));
		assertEquals(".secret", NameGenerator.createPropertyName("_secret", null, DSAnnotationVersion.V1_4));
		assertEquals("another_prop", NameGenerator.createPropertyName("another__prop", null, DSAnnotationVersion.V1_4));
		assertEquals("three_.prop", NameGenerator.createPropertyName("three___prop", null, DSAnnotationVersion.V1_4));
		assertEquals("four._prop", NameGenerator.createPropertyName("four_$__prop", null, DSAnnotationVersion.V1_4));
		assertEquals("five..prop", NameGenerator.createPropertyName("five_$_prop", null, DSAnnotationVersion.V1_4));
		assertEquals("six-prop", NameGenerator.createPropertyName("six$_$prop", null, DSAnnotationVersion.V1_4));
		assertEquals("seven$.prop", NameGenerator.createPropertyName("seven$$_$prop", null, DSAnnotationVersion.V1_4));
		assertEquals("pre.myProperty143",
				NameGenerator.createPropertyName("myProperty143", "pre.", DSAnnotationVersion.V1_4));
		assertEquals("service.ranking", NameGenerator.createClassPropertyName("ServiceRanking", null));
		assertEquals("some_name", NameGenerator.createClassPropertyName("Some_Name", null));
		assertEquals("osgi.property", NameGenerator.createClassPropertyName("OSGiProperty", null));
	}

}
