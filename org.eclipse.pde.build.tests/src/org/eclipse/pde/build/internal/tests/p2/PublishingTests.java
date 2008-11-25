/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests.p2;

import java.util.*;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class PublishingTests extends P2TestCase {

	public void testPublishBundle_simple() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_simple");

		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); }"));
		Utils.writeBuffer(bundle.getFile("src/b/B.java"), new StringBuffer("package b; public class B { int i = 0; }"));
		Utils.writeBuffer(bundle.getFile("about.txt"), new StringBuffer("All about bundle."));
		Utils.writeBuffer(bundle.getFile("META-INF/p2.inf"), new StringBuffer("instructions.install=myRandomAction(foo: bar);"));
		Properties properties = new Properties();
		properties.put("bin.includes", "META-INF/, ., about.txt");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.osgi");
		Utils.generateBundleManifest(bundle, "bundle", "1.0.0.qualifier", manifestAdditions);
		Utils.generatePluginBuildProperties(bundle, properties);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle");
		properties.put("forceContextQualifier", "v1234");
		try {
			ModelBuildScriptGenerator.p2Gathering = true;
			generateScripts(buildFolder, properties);
		} finally {
			ModelBuildScriptGenerator.p2Gathering = false;
		}

		String buildXMLPath = bundle.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"build.jars", "gather.bin.parts"}, buildFolder.getLocation().toOSString(), properties);
		
		assertResourceFile(buildFolder, "buildRepo/plugins/bundle_1.0.0.v1234.jar");
		IFile jar = buildFolder.getFile("buildRepo/plugins/bundle_1.0.0.v1234.jar");
		
		ZipFile zip = new ZipFile(jar.getLocation().toFile());
		Enumeration entries = zip.entries();
		ZipEntry entry = (ZipEntry) entries.nextElement();
		assertTrue(entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF"));
		Map headers = new HashMap();
		ManifestElement.parseBundleManifest(zip.getInputStream(entry), headers);
		assertEquals(headers.get(Constants.BUNDLE_VERSION), "1.0.0.v1234");
		zip.close();
		
		HashSet contents = new HashSet();
		contents.add("about.txt");
		contents.add("A.class");
		contents.add("b/B.class");
		assertZipContents(buildFolder, "buildRepo/plugins/bundle_1.0.0.v1234.jar", contents);
		
		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "bundle");
		assertEquals(iu.getId(), "bundle");
		assertEquals(iu.getVersion(), new Version("1.0.0.v1234"));
		assertRequires(iu, "osgi.bundle", "org.eclipse.osgi");
		assertTouchpoint(iu, "install", "myRandomAction");
	}
}
