/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.bundle;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({ ImportPackageTestCase.class, ExportPackageTestCase.class, BundleClasspathTestCase.class,
	BundleActivatorTestCase.class, BundleNameTestCase.class, BundleLocalizationTestCase.class,
	LazyStartTestCase.class, RequireBundleTestCase.class, ExecutionEnvironmentTestCase.class,
	BundleSymbolicNameTestCase.class, BundleVendorTestCase.class, BundleVersionTestCase.class,
	FragmentHostTestCase.class })
public class AllBundleModelTests {
}
