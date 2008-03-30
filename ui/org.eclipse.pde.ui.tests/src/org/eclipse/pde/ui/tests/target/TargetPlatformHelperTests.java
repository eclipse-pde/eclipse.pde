/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import junit.framework.*;
import org.eclipse.pde.internal.core.TargetPlatformHelper;

/**
 * Tests TargetPlatformHelper.java convenience methods
 * @since 3.4
 */
public class TargetPlatformHelperTests extends TestCase {

	public static Test suite() {
		return new TestSuite(TargetPlatformHelperTests.class);
	}
	
	public void testStripPathInformation(){
		String[] bundleStrings = new String[]{
				"org.eclipse.osgi",
				"org.eclipse.osgi.jar",
				"org.eclipse.osgi_3.4.0.v20080310.jar",
				"file:plugins\\org.eclipse.osgi_3.4.0.v20080310.jar",
				"file\\:plugins\\\\org.eclipse.osgi_3.4.0.v20080310.jar",
				"reference:file:plugins\\org.eclipse.osgi_3.4.0.v20080310.jar",
				"osgi.framework=file\\:plugins\\\\org.eclipse.osgi_3.4.0.v20080310.jar",
				"platform:\\file\\:plugins\\\\org.eclipse.osgi_3.4.0.v20080310.jar",
				"org.eclipse.osgi@1:start",
				"org.eclipse.osgi.jar@1\\:start",
				"platform\\:file\\:C\\:/Eclipse/eclipse-SDK-N20080312-2000-win32/eclipse/plugins/org.eclipse.osgi_0.1.0.N20080312-2000.jar@1\\:start",
				"reference\\:file\\:C\\:/Eclipse/eclipse-SDK-N20080312-2000-win32/eclipse/plugins/org.eclipse.osgi_0.1.0.N20080312-2000.jar@1\\:start",
		};
		
		for (int i = 0; i < bundleStrings.length; i++) {
			System.out.println(TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			if (i <= 7){ 
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi@1:start", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			}
		}

	}

	
}
