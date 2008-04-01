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

import java.io.File;
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
				"org.eclipse.osgi0",
				"org.eclipse.osgi1.jar",
				"org.eclipse.osgi2_3.4.0.v20080310.jar",
				"file:plugins" + File.separatorChar + "org.eclipse.osgi3_3.4.0.v20080310.jar",
				"file" + File.separatorChar + ":plugins" + File.separatorChar + "" + File.separatorChar + "org.eclipse.osgi4_3.4.0.v20080310.jar",
				"reference:file:plugins" + File.separatorChar + "org.eclipse.osgi5_3.4.0.v20080310.jar",
				"osgi.framework=file" + File.separatorChar + ":plugins" + File.separatorChar + "" + File.separatorChar + "org.eclipse.osgi6_3.4.0.v20080310.jar",
				"platform:" + File.separatorChar + "file" + File.separatorChar + ":plugins" + File.separatorChar + "" + File.separatorChar + "org.eclipse.osgi7_3.4.0.v20080310.jar",
				"org.eclipse.osgi8@1:start",
				"org.eclipse.osgi9.jar@1" + File.separatorChar + ":start",
				"platform" + File.separatorChar + ":file" + File.separatorChar + ":C" + File.separatorChar + ":/Eclipse/eclipse-SDK-N20080312-2000-win32/eclipse/plugins/org.eclipse.osgi10_0.1.0.N20080312-2000.jar@1" + File.separatorChar + ":start",
				"reference" + File.separatorChar + ":file" + File.separatorChar + ":C" + File.separatorChar + ":/Eclipse/eclipse-SDK-N20080312-2000-win32/eclipse/plugins/org.eclipse.osgi11_0.1.0.N20080312-2000.jar@1" + File.separatorChar + ":start",
		};
		
		for (int i = 0; i < bundleStrings.length; i++) {
			if (i <= 7){ 
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i, TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i + "@1:start", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			}
		}

	}

	
}
