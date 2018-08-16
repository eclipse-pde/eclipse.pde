/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.comparator.tests;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for all of the API tools test
 *
 * @since 1.0.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		FieldDeltaTests.class, InterfaceDeltaTests.class, ClassDeltaTests.class, AnnotationDeltaTests.class,
		EnumDeltaTests.class, MethodDeltaTests.class, MixedTypesDeltaTests.class, BundlesDeltaTests.class,
		RestrictionsDeltaTests.class, ApiScopeDeltaTests.class, Java8DeltaTests.class
})
public class AllDeltaTests {
}
