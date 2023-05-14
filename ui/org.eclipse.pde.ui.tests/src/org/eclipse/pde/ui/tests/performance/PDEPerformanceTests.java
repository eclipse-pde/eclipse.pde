/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance;

import org.eclipse.pde.ui.tests.performance.parts.OpenManifestEditorPerfTest;
import org.eclipse.pde.ui.tests.performance.parts.PDEModelManagerPerfTest;
import org.eclipse.pde.ui.tests.performance.parts.SchemaLoaderPerfTest;
import org.eclipse.pde.ui.tests.performance.parts.SchemaTraversePerfTest;
import org.eclipse.pde.ui.tests.performance.parts.TargetPlatformPerfTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	PDEModelManagerPerfTest.class, SchemaLoaderPerfTest.class, SchemaTraversePerfTest.class,
	OpenManifestEditorPerfTest.class, TargetPlatformPerfTest.class
})
public class PDEPerformanceTests {

}
