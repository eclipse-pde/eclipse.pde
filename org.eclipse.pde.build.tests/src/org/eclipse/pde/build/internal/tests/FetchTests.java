/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.pde.build.tests.PDETestCase;

public class FetchTests extends PDETestCase {

	public void testBug174194() throws Exception {
		IFolder buildFolder = newTest("174194");
		IFile script = buildFolder.getFile("testbuild.xml");

		try {
			runAntScript(script.getLocation().toOSString(), new String[] {"default"}, buildFolder.getLocation().toOSString(), new Properties());
		} catch (Exception e) {
			assertTrue(e.getMessage().endsWith("Could not retrieve feature.xml or build.properties for feature org.eclipse.rcp."));
		}

		assertResourceFile(buildFolder, "log.log");
		assertLogContainsLine(buildFolder.getFile("log.log"), "[eclipse.fetch] Could not retrieve feature.xml and/or build.properties: cvs exited with error code 1");
	}
}
