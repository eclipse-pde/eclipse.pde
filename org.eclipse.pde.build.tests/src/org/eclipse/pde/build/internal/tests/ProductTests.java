/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;

public class ProductTests extends PDETestCase {

	public void testBug192127() throws Exception {
		IFolder buildFolder = newTest("192127");
		IFolder containerFeature = Utils.createFolder(buildFolder, "features/org.eclipse.pde.build.container.feature");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		// Exporting from the UI gives the container feature some /Eclipse.App root files
		Utils.generateFeature(buildFolder, "org.eclipse.pde.build.container.feature", null, null, "/rcp/rcp.product", true, true);
		Properties featureProperties = new Properties();
		featureProperties.put("root", "/temp/");
		Utils.storeBuildProperties(containerFeature, featureProperties);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "/rcp/rcp.product");
		properties.put("configs", "macosx,carbon,x86");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/productBuild/allElements.xml"), null);
		properties.put("allElementsFile", FileLocator.toFileURL(resource).getPath());		
		Utils.storeBuildProperties(buildFolder, properties);
		
		runBuild(buildFolder);
		
		Set entries = new HashSet();
		entries.add("eclipse/.eclipseproduct");
		entries.add("eclipse/configuration/config.ini");
		entries.add("eclipse/rcp.app/Contents/Info.plist");
		entries.add("eclipse/rcp.app/Contents/MacOS/rcp");
		entries.add("eclipse/rcp.app/Contents/MacOS/rcp.ini");

		entries.add("eclipse/Eclipse.app/");

		//bug 206788 names the archive .zip
		assertZipContents(buildFolder, "I.TestBuild/eclipse-macosx.carbon.x86.zip", entries, false);
		assertTrue(entries.contains("eclipse/Eclipse.app/"));
		assertTrue(entries.size() == 1);
	}
}
