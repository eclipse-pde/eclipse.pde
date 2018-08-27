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
package org.eclipse.pde.ui.tests.imports;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({ ImportWithLinksTestCase.class, ImportAsBinaryTestCase.class, ImportAsSourceTestCase.class,

	// Temporarily disabled until git migration is complete and we have access to a
	// stable cvs repo (bug 355873)
	// ImportFromRepoTestCase.class
	// ( removed since it depends on CVS, if it is to be restored in future, we need
	// to add extraIU for this test case see bug 527999 )
	// BundleImporterTests.class
	ImportFeatureProjectsTestCase.class })
public class AllImportTests {
}
