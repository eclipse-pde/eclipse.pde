/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests.p2;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.internal.build.builder.*;
import org.osgi.framework.Constants;

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
			BuildDirector.p2Gathering = true;
			generateScripts(buildFolder, properties);
		} finally {
			BuildDirector.p2Gathering = false;
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
		assertEquals(iu.getVersion().toString(), "1.0.0.v1234");
		assertRequires(iu, "osgi.bundle", "org.eclipse.osgi");
		assertTouchpoint(iu, "install", "myRandomAction");
	}

	public void testPublishFeature_ExecutableFeature() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_Executable");
		IFolder executableFeature = buildFolder.getFolder("features/org.eclipse.equinox.executable");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("org.eclipse.equinox.executable");
			}
		};

		File[] features = new File(delta, "features").listFiles(filter);
		Utils.copy(features[0], executableFeature.getLocation().toFile());
		executableFeature.refreshLocal(IResource.DEPTH_INFINITE, null);

		Properties properties = Utils.loadProperties(executableFeature.getFile("build.properties"));
		properties.remove("custom");
		//temporary
		properties.put("root.win32.win32.x86", "file:bin/win32/win32/x86/launcher.exe");
		properties.put("root.win32.win32.x86_64", "file:bin/win32/win32/x86_64/launcher.exe");
		properties.put("root.win32.win32.ia64", "file:contributed/win32/win32/ia64/launcher.exe");
		properties.put("root.win32.wpf.x86", "file:bin/wpf/win32/x86/launcher.exe");
		Utils.storeBuildProperties(executableFeature, properties);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "org.eclipse.equinox.executable");
		try {
			BuildDirector.p2Gathering = true;
			generateScripts(buildFolder, properties);
		} finally {
			BuildDirector.p2Gathering = false;
		}

		String buildXMLPath = executableFeature.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"gather.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		String executable = "org.eclipse.equinox.executable";
		String fileName = features[0].getName();
		String version = fileName.substring(fileName.indexOf('_') + 1);
		Set entries = new HashSet();
		entries.add("launcher");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), executable + "_root.aix.motif.ppc_" + version, entries);

		entries.add("about.html");
		entries.add("libcairo-swt.so");
		entries.add("about_files/about_cairo.html");
		entries.add("about_files/mpl-v11.txt");
		entries.add("about_files/pixman-licenses.txt");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), executable + "_root.linux.gtk.x86_" + version, entries);

		entries.add("Eclipse.app/Contents/Info.plist");
		entries.add("Eclipse.app/Contents/MacOS/eclipse.ini");
		entries.add("Eclipse.app/Contents/MacOS/launcher");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), executable + "_root.macosx.carbon.ppc_" + version, entries);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "org.eclipse.equinox.executable_root.linux.gtk.ppc");
		assertEquals(iu.getVersion().toString(), version);
		assertTouchpoint(iu, "install", "chmod(targetDir:${installFolder}, targetFile:libcairo-swt.so, permissions:755);");
	}

	public void testPublishBundle_APITooling() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_APITooling");

		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); public void Bar(){}}"));
		Utils.writeBuffer(bundle.getFile("src/b/B.java"), new StringBuffer("package b; public class B { int i = 0; public void Foo(){}}"));

		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> 						\n");
		buffer.append("<projectDescription> 											\n");
		buffer.append("  <name>org.eclipse.pde.build</name> 							\n");
		buffer.append("  <natures> 														\n");
		buffer.append("    <nature>org.eclipse.jdt.core.javanature</nature> 			\n");
		buffer.append("    <nature>org.eclipse.pde.PluginNature</nature> 				\n");
		buffer.append("    <nature>org.eclipse.pde.api.tools.apiAnalysisNature</nature> \n");
		buffer.append("  </natures> 													\n");
		buffer.append("</projectDescription> 											\n");
		Utils.writeBuffer(bundle.getFile(".project"), buffer);

		Properties properties = new Properties();
		properties.put("bin.includes", "META-INF/, .");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.osgi");
		manifestAdditions.put(new Attributes.Name("Export-Package"), "b");
		Utils.generateBundleManifest(bundle, "bundle", "1.0.0.qualifier", manifestAdditions);
		Utils.generatePluginBuildProperties(bundle, properties);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle");
		properties.put("forceContextQualifier", "v1234");
		properties.put("generateAPIDescription", "true");

		try {
			BuildDirector.p2Gathering = true;
			generateScripts(buildFolder, properties);
		} finally {
			BuildDirector.p2Gathering = false;
		}

		String buildXMLPath = bundle.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"build.jars", "gather.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		assertResourceFile(buildFolder, "buildRepo/plugins/bundle_1.0.0.v1234.jar");
		HashSet contents = new HashSet();
		contents.add("A.class");
		contents.add("b/B.class");
		contents.add(".api_description");
		assertZipContents(buildFolder, "buildRepo/plugins/bundle_1.0.0.v1234.jar", contents);
	}
	
	public void testPublish_Packaging_1() throws Exception {
		IFolder buildFolder = newTest("packaging_1");
		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		Utils.generateFeature(buildFolder, "F", new String [] { "org.eclipse.cvs" }, new String [] { "a" } );
		Utils.generateBundle(a, "a");
		Utils.writeBuffer(a.getFile("src/A.java"), new StringBuffer("public class A { int i; }"));
		
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		Utils.storeBuildProperties(buildFolder, properties);
		
		try {
			BuildDirector.p2Gathering = true;
			runBuild(buildFolder);
		} finally {
			BuildDirector.p2Gathering = false;
		}
		
		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		
		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "a");
		assertEquals(iu.getVersion().toString(), "1.0.0");

		iu = getIU(repository, "org.eclipse.team.cvs.ssh");
		assertNotNull(iu);
		IFile file = buildFolder.getFile("buildRepo/plugins/org.eclipse.team.cvs.ssh_" + iu.getVersion()+ ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());
	
		iu = getIU(repository, "org.eclipse.team.cvs.core");
		assertNotNull(iu);
		file = buildFolder.getFile("buildRepo/plugins/org.eclipse.team.cvs.core_" + iu.getVersion()+ ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());
		
		iu = getIU(repository, "org.eclipse.cvs");
		assertNotNull(iu);
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.cvs_" + iu.getVersion()+ ".jar");
		
		iu = getIU(repository, "org.eclipse.team.cvs.ui");
		assertNotNull(iu);
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.team.cvs.ui_" + iu.getVersion()+ ".jar");
		
		iu = getIU(repository, "org.eclipse.team.cvs.ssh2");
		assertNotNull(iu);
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.team.cvs.ssh2_" + iu.getVersion()+ ".jar");
		
		iu = getIU(repository, "org.eclipse.cvs.feature.jar");
		file = buildFolder.getFile("buildRepo/features/org.eclipse.cvs_" + iu.getVersion() + ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());
	}
}
