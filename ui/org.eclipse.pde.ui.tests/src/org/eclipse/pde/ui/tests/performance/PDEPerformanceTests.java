/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance;

import org.eclipse.pde.ui.tests.performance.parts.*;
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
