/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.net.URL;
import java.util.Properties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
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
		assertLogContainsLine(buildFolder.getFile("log.log"), "[eclipse.fetch] Could not retrieve feature.xml and/or build.properties:");
	}
	
	public void testBug171869_Get() throws Exception {
		IFolder buildFolder = newTest("171869");
		Utils.createFolder(buildFolder, "plugins");
		
		//org.eclipse.pde.build.container.feature is special in that the fetch won't try
		//to fetch it, and will just fetch everything it includes.
		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		fetchProperties.put("type", "feature");
		fetchProperties.put("id", "org.eclipse.pde.build.container.feature");
		
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.1.v20070417.jar");
		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.0.20061215.jar");
	}
	
//	public void testBug171869_Get() throws Exception {
//		IFolder buildFolder = newTest("171869");
//		Utils.createFolder(buildFolder, "plugins");
//		
//		//org.eclipse.pde.build.container.feature is special in that the fetch won't try
//		//to fetch it, and will just fetch everything it includes.
//		Properties fetchProperties = new Properties();
//		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
//		fetchProperties.put("type", "feature");
//		fetchProperties.put("id", "org.eclipse.pde.build.container.feature");
//		
//		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
//		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
//		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);
//
//		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.1.v20070417.jar");
//		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.0.20061215.jar");
//	}
}
