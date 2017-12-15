/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.target;

import java.io.File;
import junit.framework.TestCase;
import org.eclipse.pde.internal.core.TargetPlatformHelper;

/**
 * Tests TargetPlatformHelper.java convenience methods
 * @since 3.4
 */
public class TargetPlatformHelperTests extends TestCase {

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
				"org.eclipse.osgi12.zip",
				"org.eclipse.osgi13_3.jar",
				"org.eclipse.osgi14_3.4.0.v123_r372.JAR",
				"org.eclipse.osgi15_13_3.4.war",
				"org.eclipse.osgi16.nl_de_3.4.0",
				"org.eclipse.osgi17.ia64_32_3.4.0",
				"org.eclipse.osgi18.x86_64_3.4.0.v20080310.jar",
				"org.eclipse.osgi19.x86_64",
		};

		for (int i = 0; i < bundleStrings.length; i++) {
			if (i <= 7){
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i, TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else if (i <= 11) {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i + "@1:start", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else if (i <= 14) {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i, TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else if (i == 15) {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i + "_13", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else if (i == 16) {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i + ".nl_de", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else if (i == 17) {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i + ".ia64_32", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			} else {
				assertEquals("Bundle path was not stripped properly", "org.eclipse.osgi" + i + ".x86_64", TargetPlatformHelper.stripPathInformation(bundleStrings[i]));
			}
		}

	}


}
