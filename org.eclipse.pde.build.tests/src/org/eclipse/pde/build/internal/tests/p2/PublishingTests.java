/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests.p2;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.Activator;
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

	public void testPublishBundle_p2infCUs() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_p2infCUs");
		
		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.generateBundle(bundle, "bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("public class A { int i; }"));
		StringBuffer inf = new StringBuffer();
		inf.append("requires.1.namespace=my.awesome.namespace                   \n");
		inf.append("requires.1.name=bundle.cu                                   \n");
		inf.append("requires.1.range=[1.0.0, 1.0.0]                             \n");
		inf.append("requires.1.greedy=true                                      \n");
		inf.append("unit.1.id=bundle.cu                                         \n");
		inf.append("unit.1.version=1.0.0                                        \n");
		inf.append("unit.1.hostRequirements.1.namespace=osgi.bundle             \n");
		inf.append("unit.1.hostRequirements.1.name=bundle                       \n");
		inf.append("unit.1.hostRequirements.1.range=[1.0.0, 1.0.0]              \n");
		inf.append("unit.1.hostRequirements.2.namespace=org.eclipse.equinox.p2.eclipse.type\n");
		inf.append("unit.1.hostRequirements.2.name=bundle                       \n");
		inf.append("unit.1.hostRequirements.2.range=[1.0.0, 2.0.0)              \n");
		inf.append("unit.1.hostRequirements.2.greedy=false                      \n");
		inf.append("unit.1.requires.1.namespace=osgi.bundle                     \n");
		inf.append("unit.1.requires.1.name=bundle                               \n");
		inf.append("unit.1.requires.1.range=[1.0.0, 1.0.0]                      \n");
		inf.append("unit.1.provides.1.namespace=my.awesome.namespace            \n");
		inf.append("unit.1.provides.1.name=bundle.cu                            \n");
		inf.append("unit.1.provides.1.version=1.0.0                             \n");
		inf.append("unit.1.instructions.configure=setStartLevel(startLevel:1);\\\n");
		inf.append("                              markStarted(started: true);   \n");
		inf.append("unit.1.instructions.unconfigure=setStartLevel(startLevel:-1);\\\n");
		inf.append("                                markStarted(started: false);\n");	
		inf.append("unit.1.instructions.install=installBundle(bundle:${artifact});\n");
		inf.append("unit.1.instructions.uninstall=uninstallBundle(bundle:${artifact});\n");
		Utils.writeBuffer(bundle.getFile("META-INF/p2.inf"), inf);
		
		IFile productFile = buildFolder.getFile("foo.product");
		Utils.generateProduct(productFile, "foo", "1.0.0", null, new String[] {"org.eclipse.osgi", "bundle"}, false);
		
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("configs", "*,*,*");
		properties.put("includeLaunchers", "false");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.put("filteredDependencyCheck", "true");
		properties.put("filterP2Base", "true");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);
		assertLogContainsLine(buildFolder.getFile("tmp/eclipse/configuration/config.ini"),"bundle_1.0.0.jar@1\\:start");
	}
	
	protected File copyExecutableFeature(File delta, IFolder executableFeature) throws Exception {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("org.eclipse.equinox.executable");
			}
		};

		File[] features = new File(delta, "features").listFiles(filter);
		Utils.copy(features[0], executableFeature.getLocation().toFile());
		executableFeature.refreshLocal(IResource.DEPTH_INFINITE, null);
		return features[0];
	}

	public void testPublishFeature_ExecutableFeature() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_Executable");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFolder executableFeature = buildFolder.getFolder("features/org.eclipse.equinox.executable");
		File originalExecutable = copyExecutableFeature(delta, executableFeature);

		Properties properties = Utils.loadProperties(executableFeature.getFile("build.properties"));
		properties.remove("custom");
		Utils.storeBuildProperties(executableFeature, properties);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "org.eclipse.equinox.executable");
		properties.put("launcherName", "eclipse");
		try {
			BuildDirector.p2Gathering = true;
			generateScripts(buildFolder, properties);
		} finally {
			BuildDirector.p2Gathering = false;
		}

		String buildXMLPath = executableFeature.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"gather.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		String executable = "org.eclipse.equinox.executable";
		String fileName = originalExecutable.getName();
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
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); /** @nooverride */public void Bar(){}}"));
		Utils.writeBuffer(bundle.getFile("src/b/B.java"), new StringBuffer("package b; /** @noextend */public class B { /** @noreference */public int i = 0; public void Foo(){}}"));

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
		Utils.generateFeature(buildFolder, "F", new String[] {"org.eclipse.cvs"}, new String[] {"a"});
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
		IFile file = buildFolder.getFile("buildRepo/plugins/org.eclipse.team.cvs.ssh_" + iu.getVersion() + ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());

		iu = getIU(repository, "org.eclipse.team.cvs.core");
		assertNotNull(iu);
		file = buildFolder.getFile("buildRepo/plugins/org.eclipse.team.cvs.core_" + iu.getVersion() + ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());

		iu = getIU(repository, "org.eclipse.cvs");
		assertNotNull(iu);
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.cvs_" + iu.getVersion() + ".jar");

		iu = getIU(repository, "org.eclipse.team.cvs.ui");
		assertNotNull(iu);
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.team.cvs.ui_" + iu.getVersion() + ".jar");

		iu = getIU(repository, "org.eclipse.team.cvs.ssh2");
		assertNotNull(iu);
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.team.cvs.ssh2_" + iu.getVersion() + ".jar");

		iu = getIU(repository, "org.eclipse.cvs.feature.jar");
		file = buildFolder.getFile("buildRepo/features/org.eclipse.cvs_" + iu.getVersion() + ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());
	}

	public void testPublish_Source_1() throws Exception {
		IFolder buildFolder = newTest("source_1");

		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); public void Bar(){}}"));
		Utils.writeBuffer(bundle.getFile("src/b/B.java"), new StringBuffer("package b; public class B { public int i = 0; public void Foo(){}}"));
		Utils.generateBundle(bundle, "bundle");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"bundle", "bundle.source"});
		Properties properties = new Properties();
		properties.put("generate.plugin@bundle.source", "bundle");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(buildFolder.getFolder("features/F"), properties);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		Utils.storeBuildProperties(buildFolder, properties);
		try {
			BuildDirector.p2Gathering = true;
			runBuild(buildFolder);
		} finally {
			BuildDirector.p2Gathering = false;
		}

		assertResourceFile(buildFolder, "buildRepo/plugins/bundle.source_1.0.0.jar");
		Set entries = new HashSet();
		entries.add("A.java");
		entries.add("b/B.java");
		assertZipContents(buildFolder, "buildRepo/plugins/bundle.source_1.0.0.jar", entries);
	}

	public void testPublish_Brand_1() throws Exception {
		IFolder buildFolder = newTest("brand_1");
		IFolder rcp = Utils.createFolder(buildFolder, "rcp");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFolder executableFeature = buildFolder.getFolder("features/org.eclipse.equinox.executable");
		copyExecutableFeature(delta, executableFeature);
		Properties properties = Utils.loadProperties(executableFeature.getFile("build.properties"));
		properties.remove("custom");
		Utils.storeBuildProperties(executableFeature, properties);

		IFile product = rcp.getFile("rcp.product");
		StringBuffer branding = new StringBuffer();
		branding.append("<launcher name=\"branded\">           \n");
		branding.append("   <macosx icon=\"mail.icns\" />      \n");
		branding.append("   <win useIco=\"true\">              \n");
		branding.append("      <ico path=\"mail.ico\" />       \n");
		branding.append("      <bmp/>                          \n");
		branding.append("   </win>                             \n");
		branding.append("</launcher>                           \n");
		Utils.generateProduct(product, "org.example.rcp", "1.0.0", null, new String[] {"org.eclipse.osgi"}, false, branding);

		//steal the icons from test 237922
		URL ico = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/resources/237922/rcp/icons/mail.ico"), null);
		IFile icoFile = rcp.getFile("mail.ico");
		icoFile.create(ico.openStream(), IResource.FORCE, null);

		//cheat and spoof a icns file for mac
		Utils.copy(icoFile.getLocation().toFile(), new File(rcp.getLocation().toFile(), "mail.icns"));

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath() + "/plugins");
		properties.put("configs", "win32,win32,x86 & macosx, carbon, ppc");
		Utils.storeBuildProperties(buildFolder, properties);

		try {
			BuildDirector.p2Gathering = true;
			runProductBuild(buildFolder);
		} finally {
			BuildDirector.p2Gathering = false;
		}

		Set entries = new HashSet();
		entries.add("branded.app/Contents/Info.plist");
		entries.add("branded.app/Contents/MacOS/branded.ini");
		entries.add("branded.app/Contents/MacOS/branded");
		entries.add("branded.app/Contents/Resources/mail.icns");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), "org.example.rcp_root.macosx.carbon.ppc_1.0.0", entries);

		entries.clear();
		entries.add("branded.exe");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), "org.example.rcp_root.win32.win32.x86_1.0.0", entries);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "org.example.rcp");
		assertEquals(iu.getId(), "org.example.rcp");
		assertEquals(iu.getVersion().toString(), "1.0.0");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.osgi");

		assertResourceFile(buildFolder, "I.TestBuild/eclipse-macosx.carbon.ppc.zip");
		assertResourceFile(buildFolder, "I.TestBuild/eclipse-win32.win32.x86.zip");
	}
}
