/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ DSComponentTestCase.class, DSServiceTestCase.class, DSReferenceTestCase.class,
		DSProvideTestCase.class, DSPropertyTestCase.class, DSPropertiesTestCase.class, DSImplementationTestCase.class,
		DSObjectTestCase.class, DSv10tov11TestCase.class })
public class AllDSModelTests {
}
