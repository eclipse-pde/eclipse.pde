/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests.p2;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.Activator;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.internal.build.site.QualifierReplacer;
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
		properties.put("p2.gathering", "true");
		generateScripts(buildFolder, properties);

		String buildXMLPath = bundle.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"build.jars", "publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

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

	public void testPublishBundle_customCallbacks() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_callbacks");

		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); }"));
		Utils.writeBuffer(bundle.getFile("src/b/B.java"), new StringBuffer("package b; public class B { int i = 0; }"));
		Utils.writeBuffer(bundle.getFile("META-INF/p2.inf"), new StringBuffer("instructions.install=myRandomAction(foo: bar);"));
		Properties properties = new Properties();
		properties.put("bin.includes", "META-INF/, .");
		properties.put("customBuildCallbacks", "customBuildCallbacks.xml");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.osgi");
		Utils.generateBundleManifest(bundle, "bundle", "1.0.0.qualifier", manifestAdditions);
		Utils.generatePluginBuildProperties(bundle, properties);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<project name=\"customGather\" default=\"noDefault\">			\n");
		buffer.append("   <target name=\"pre.gather.bin.parts\">						\n");
		buffer.append("      <concat destfile=\"${target.folder}/a.txt\">				\n");
		buffer.append("        Mary had a little lamb.									\n");
		buffer.append("      </concat>													\n");
		buffer.append("   </target>														\n");
		buffer.append("   <target name=\"post.gather.bin.parts\">						\n");
		buffer.append("      <copy file=\"${build.result.folder}/@dot/A.class\"			\n");
		buffer.append("            tofile=\"${target.folder}/b.txt\" />					\n");
		buffer.append("   </target>														\n");
		buffer.append("</project>														\n");
		Utils.writeBuffer(bundle.getFile("customBuildCallbacks.xml"), buffer);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle");
		properties.put("forceContextQualifier", "v1234");
		properties.put("p2.gathering", "true");
		generateScripts(buildFolder, properties);

		String buildXMLPath = bundle.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"build.jars", "publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		HashSet contents = new HashSet();
		contents.add("a.txt");
		contents.add("b.txt");
		assertZipContents(buildFolder, "buildRepo/plugins/bundle_1.0.0.v1234.jar", contents);
	}

	public void testPublishBundle_p2infCUs() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_p2infCUs");

		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.generateBundle(bundle, "bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("public class A { int i; }"));
		StringBuffer inf = new StringBuffer();
		inf.append("requires.1.namespace=my.awesome.namespace                    \n");
		inf.append("requires.1.name=bundle.cu                                    \n");
		inf.append("requires.1.range=[1.0.0, 1.0.0]                              \n");
		inf.append("requires.1.greedy=true                                       \n");
		inf.append("units.1.id=bundle.cu                                         \n");
		inf.append("units.1.version=1.0.0                                        \n");
		inf.append("units.1.hostRequirements.1.namespace=osgi.bundle             \n");
		inf.append("units.1.hostRequirements.1.name=bundle                       \n");
		inf.append("units.1.hostRequirements.1.range=[1.0.0, 1.0.0]              \n");
		inf.append("units.1.hostRequirements.2.namespace=org.eclipse.equinox.p2.eclipse.type\n");
		inf.append("units.1.hostRequirements.2.name=bundle                       \n");
		inf.append("units.1.hostRequirements.2.range=[1.0.0, 2.0.0)              \n");
		inf.append("units.1.hostRequirements.2.greedy=false                      \n");
		inf.append("units.1.requires.1.namespace=osgi.bundle                     \n");
		inf.append("units.1.requires.1.name=bundle                               \n");
		inf.append("units.1.requires.1.range=[1.0.0, 1.0.0]                      \n");
		inf.append("units.1.provides.1.namespace=my.awesome.namespace            \n");
		inf.append("units.1.provides.1.name=bundle.cu                            \n");
		inf.append("units.1.provides.1.version=1.0.0                             \n");
		inf.append("units.1.instructions.configure=setStartLevel(startLevel:1);\\\n");
		inf.append("                              markStarted(started: true);    \n");
		inf.append("units.1.instructions.unconfigure=setStartLevel(startLevel:-1);\\\n");
		inf.append("                                markStarted(started: false); \n");
		inf.append("units.1.instructions.install=installBundle(bundle:${artifact});\n");
		inf.append("units.1.instructions.uninstall=uninstallBundle(bundle:${artifact});\n");
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
		assertLogContainsLine(buildFolder.getFile("tmp/eclipse/configuration/config.ini"), "bundle_1.0.0.jar@1\\:start");
	}

	protected File findExecutableFeature(File delta) throws Exception {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("org.eclipse.equinox.executable");
			}
		};

		File[] features = new File(delta, "features").listFiles(filter);
		assertTrue(features.length > 0);
		return features[0];
	}

	public void testPublishFeature_customCallbacks() throws Exception {
		IFolder buildFolder = newTest("PublishFeature_custom");
		IFolder f = buildFolder.getFolder("features/f");

		Utils.generateFeature(buildFolder, "f", null, null);
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml");
		properties.put("customBuildCallbacks", "customBuildCallbacks.xml");
		Utils.storeBuildProperties(f, properties);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<project name=\"customGather\" default=\"noDefault\">			\n");
		buffer.append("   <target name=\"pre.gather.bin.parts\">						\n");
		buffer.append("      <concat destfile=\"${feature.directory}/a.txt\">			\n");
		buffer.append("        Mary had a little lamb.									\n");
		buffer.append("      </concat>													\n");
		buffer.append("   </target>														\n");
		buffer.append("</project>														\n");
		Utils.writeBuffer(f.getFile("customBuildCallbacks.xml"), buffer);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f");
		properties.put("p2.gathering", "true");
		generateScripts(buildFolder, properties);

		String buildXMLPath = f.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		HashSet contents = new HashSet();
		contents.add("a.txt");
		contents.add("feature.xml");
		assertZipContents(buildFolder, "buildRepo/features/f_1.0.0.jar", contents);
	}

	public void testPublishFeature_ExecutableFeature() throws Exception {
		IFolder buildFolder = newTest("PublishFeature_Executable");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		File originalExecutable = findExecutableFeature(delta);

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "org.eclipse.equinox.executable");
		properties.put("launcherName", "eclipse");
		properties.put("p2.gathering", "true");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		generateScripts(buildFolder, properties);

		String buildXMLPath = new File(originalExecutable, "build.xml").getAbsolutePath();
		runAntScript(buildXMLPath, new String[] {"publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		String executable = "org.eclipse.equinox.executable";
		String fileName = originalExecutable.getName();
		String version = fileName.substring(fileName.indexOf('_') + 1);
		Set entries = new HashSet();
		entries.add("launcher");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), executable + "_root.motif.aix.ppc_" + version, entries);

		entries.add("about.html");
		entries.add("libcairo-swt.so");
		entries.add("about_files/about_cairo.html");
		entries.add("about_files/mpl-v11.txt");
		entries.add("about_files/pixman-licenses.txt");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), executable + "_root.gtk.linux.x86_" + version, entries);

		entries.add("Eclipse.app/Contents/Info.plist");
		entries.add("Eclipse.app/Contents/MacOS/eclipse.ini");
		entries.add("Eclipse.app/Contents/MacOS/launcher");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), executable + "_root.carbon.macosx.ppc_" + version, entries);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "org.eclipse.equinox.executable_root.gtk.linux.ppc");
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
		properties.put("p2.gathering", "true");
		generateScripts(buildFolder, properties);

		String buildXMLPath = bundle.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"build.jars", "publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

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
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		HashSet entries = new HashSet();

		IInstallableUnit iu = getIU(repository, "a");
		assertEquals(iu.getVersion().toString(), "1.0.0");

		iu = getIU(repository, "org.eclipse.team.cvs.ssh");
		assertNotNull(iu);
		entries.add("plugins/org.eclipse.team.cvs.ssh_" + iu.getVersion() + ".jar");
		IFile file = buildFolder.getFile("buildRepo/plugins/org.eclipse.team.cvs.ssh_" + iu.getVersion() + ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());

		iu = getIU(repository, "org.eclipse.team.cvs.core");
		assertNotNull(iu);
		entries.add("plugins/org.eclipse.team.cvs.core_" + iu.getVersion() + ".jar");
		file = buildFolder.getFile("buildRepo/plugins/org.eclipse.team.cvs.core_" + iu.getVersion() + ".jar");
		assertTrue(file.exists());
		assertJarVerifies(file.getLocation().toFile());

		iu = getIU(repository, "org.eclipse.cvs");
		assertNotNull(iu);
		entries.add("plugins/org.eclipse.cvs_" + iu.getVersion() + ".jar");
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.cvs_" + iu.getVersion() + ".jar");

		iu = getIU(repository, "org.eclipse.team.cvs.ui");
		assertNotNull(iu);
		entries.add("plugins/org.eclipse.team.cvs.ui_" + iu.getVersion() + ".jar");
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.team.cvs.ui_" + iu.getVersion() + ".jar");

		iu = getIU(repository, "org.eclipse.team.cvs.ssh2");
		assertNotNull(iu);
		entries.add("plugins/org.eclipse.team.cvs.ssh2_" + iu.getVersion() + ".jar");
		assertResourceFile(buildFolder, "buildRepo/plugins/org.eclipse.team.cvs.ssh2_" + iu.getVersion() + ".jar");

		iu = getIU(repository, "org.eclipse.cvs.feature.jar");
		file = buildFolder.getFile("buildRepo/features/org.eclipse.cvs_" + iu.getVersion() + ".jar");
		assertTrue(file.exists());
		entries.add("features/org.eclipse.cvs_" + iu.getVersion() + ".jar");
		assertJarVerifies(file.getLocation().toFile());

		entries.add("artifacts.xml");
		entries.add("content.xml");
		assertZipContents(buildFolder, "I.TestBuild/F-TestBuild-group.group.group.zip", entries);
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
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

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

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("configs", "win32,win32,x86 & macosx, carbon, ppc");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("branded.app/Contents/Info.plist");
		entries.add("branded.app/Contents/MacOS/branded.ini");
		entries.add("branded.app/Contents/MacOS/branded");
		entries.add("branded.app/Contents/Resources/mail.icns");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), "org.example.rcp_root.carbon.macosx.ppc_1.0.0", entries);

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

	public void testAssemblePackage() throws Exception {
		IFolder buildFolder = newTest("publishAssemblePackage");

		IFolder p = Utils.createFolder(buildFolder, "plugins/p");
		Utils.writeBuffer(p.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); public void Bar(){}}"));
		Utils.writeBuffer(p.getFile("src/b/B.java"), new StringBuffer("package b; public class B { public int i = 0; public void Foo(){}}"));
		Utils.generateBundle(p, "p");

		IFolder f = Utils.createFolder(buildFolder, "features/f");
		Utils.generateFeature(buildFolder, "f", null, new String[] {"p", "org.eclipse.osgi"});
		Utils.writeBuffer(f.getFile("about.html"), new StringBuffer("about!\n"));
		Utils.writeBuffer(f.getFile("rootFiles/license.html"), new StringBuffer("license"));
		Properties properties = new Properties();
		properties.put("bin.includes", "about.html, feature.xml");
		properties.put("root", "rootFiles");
		Utils.storeBuildProperties(f, properties);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		if (Platform.getOS().equals("linux"))
			properties.put("archivesFormat", "group,group,group-tar");
		else
			properties.put("archivesFormat", "group,group,group-antTar");
		properties.put("p2.metadata.repo.name", "MyMeta");
		properties.put("p2.metadata.repo.name", "MyArtifact");
		properties.put("p2.compress", "true");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		IInstallableUnit osgi = getIU(repository, "org.eclipse.osgi");

		IFile tar = buildFolder.getFile("I.TestBuild/f-TestBuild-group.group.group.tar.gz");
		assertResourceFile(tar);
		File untarred = new File(buildFolder.getLocation().toFile(), "untarred");
		FileUtils.unzipFile(tar.getLocation().toFile(), untarred);

		assertResourceFile(buildFolder, "untarred/plugins/org.eclipse.osgi_" + osgi.getVersion() + ".jar");
		assertResourceFile(buildFolder, "untarred/binary/f_root_1.0.0");
		assertResourceFile(buildFolder, "untarred/plugins/p_1.0.0.jar");
		assertResourceFile(buildFolder, "untarred/features/f_1.0.0.jar");
		assertResourceFile(buildFolder, "untarred/artifacts.jar");
		assertResourceFile(buildFolder, "untarred/content.jar");

		HashSet entries = new HashSet();
		entries.add("license.html");
		assertZipContents(buildFolder, "untarred/binary/f_root_1.0.0", entries);
	}

	public void testPublishAndRunSimpleProduct() throws Exception {
		IFolder buildFolder = newTest("PublishAndRunSimpleProduct");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		//headless rcp hello world
		IFolder headless = Utils.createFolder(buildFolder, "plugins/headless");
		StringBuffer buffer = new StringBuffer();
		buffer.append("package headless;													\n");
		buffer.append("import org.eclipse.equinox.app.IApplication;								\n");
		buffer.append("import org.eclipse.equinox.app.IApplicationContext;						\n");
		buffer.append("public class Application implements IApplication {						\n");
		buffer.append("   public Object start(IApplicationContext context) throws Exception {	\n");
		buffer.append("      System.out.println(\"Hello RCP World!\");							\n");
		buffer.append("      return IApplication.EXIT_OK;										\n");
		buffer.append("   }																		\n");
		buffer.append("   public void stop() {													\n");
		buffer.append("   }																		\n");
		buffer.append("}																		\n");
		Utils.writeBuffer(headless.getFile("src/headless/Application.java"), buffer);

		buffer = new StringBuffer();
		buffer.append("<plugin>																			\n");
		buffer.append("   <extension id=\"application\" point=\"org.eclipse.core.runtime.applications\">\n");
		buffer.append("      <application>																\n");
		buffer.append("         <run class=\"headless.Application\"/>									\n");
		buffer.append("      </application>																\n");
		buffer.append("   </extension>																	\n");
		buffer.append("   <extension id=\"product\" point=\"org.eclipse.core.runtime.products\">		\n");
		buffer.append("      <product application=\"headless.application\" name=\"Headless Name\"/>		\n");
		buffer.append("   </extension>		                                                            \n");
		buffer.append("</plugin>																		\n");
		Utils.writeBuffer(headless.getFile("plugin.xml"), buffer);

		Attributes additionalAttributes = new Attributes();
		additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Require-Bundle"), "org.eclipse.core.runtime");
		additionalAttributes.put(new Attributes.Name("Bundle-ActivationPolicy"), "lazy");
		Utils.generateBundleManifest(headless, "headless;singleton:=true", "1.0.0", additionalAttributes);
		Properties properties = new Properties();
		properties.put("bin.includes", "META-INF/, ., plugin.xml");
		Utils.generatePluginBuildProperties(headless, properties);

		IFile productFile = buildFolder.getFile("headless.product");
		String[] bundles = new String[] {"headless", "org.eclipse.core.contenttype", "org.eclipse.core.jobs", "org.eclipse.core.runtime", "org.eclipse.equinox.app", "org.eclipse.equinox.common", "org.eclipse.equinox.preferences", "org.eclipse.equinox.registry", "org.eclipse.osgi"};
		Utils.generateProduct(productFile, "headless.product", "1.0.0.qualifier", "headless.application", "headless", bundles, false, null);
		Properties p2Inf = new Properties(); // bug 268223
		p2Inf.put("instructions.configure", "addRepository(type:0,location:file${#58}//foo/bar);");
		Utils.storeProperties(buildFolder.getFile("p2.inf"), p2Inf);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		String config = Platform.getOS() + ',' + Platform.getWS() + ',' + Platform.getOSArch();
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("configs", config);
		properties.put("archivesFormat", config + "-folder");
		properties.put("filteredDependencyCheck", "true");
		properties.put("p2.gathering", "true");
		properties.put("p2.metadata.repo", "file:" + buildFolder.getFolder("finalRepo").getLocation().toOSString());
		properties.put("p2.artifact.repo", "file:" + buildFolder.getFolder("finalRepo").getLocation().toOSString());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFile configFile = buildFolder.getFile("/tmp/eclipse/configuration/config.ini");
		assertLogContainsLine(configFile, "eclipse.application=headless.application");
		assertLogContainsLine(configFile, "eclipse.product=headless.product");

		if (Platform.getOS().equals("macosx")) {
			IFile iniFile = buildFolder.getFile("/tmp/eclipse/headless.app/Contents/MacOS/headless.ini");
			assertLogContainsLines(iniFile, new String[] {"-startup", "plugins/org.eclipse.equinox.launcher_"});
			assertLogContainsLines(iniFile, new String[] {"--launcher.library", "plugins/org.eclipse.equinox.launcher."});
		} else {
			IFile iniFile = buildFolder.getFile("/tmp/eclipse/headless.ini");
			assertLogContainsLines(iniFile, new String[] {"-startup", "plugins/org.eclipse.equinox.launcher_"});
			assertLogContainsLines(iniFile, new String[] {"--launcher.library", "plugins/org.eclipse.equinox.launcher."});
		}
		IMetadataRepository finalRepo = loadMetadataRepository("file:" + buildFolder.getFolder("finalRepo").getLocation().toOSString());
		getIU(finalRepo, "a.jre.javase");
		IInstallableUnit productIu = getIU(finalRepo, "headless.product");
		assertFalse(productIu.getVersion().toString().equals("1.0.0.qualifier")); //bug 246060, should be a timestamp
		//check up to the date on the timestamp, don't worry about hours/mins
		assertTrue(productIu.getVersion().getQualifier().startsWith(QualifierReplacer.getDateQualifier().substring(0, 8)));
		assertTouchpoint(productIu, "configure", "addRepository(type:0,location:file${#58}//foo/bar);");

		getIU(finalRepo, "toolingorg.eclipse.equinox.common");

		IInstallableUnit iu = getIU(finalRepo, "toolingheadless.product_root." + Platform.getWS() + '.' + Platform.getOS() + '.' + Platform.getOSArch());
		assertTouchpoint(iu, "configure", "setLauncherName(name:headless");
		assertEquals(iu.getVersion(), productIu.getVersion());
	}

	public void testBug265726() throws Exception {
		IFolder buildFolder = newTest("265726");
		if (Platform.getOS().equals("win32") && buildFolder.getLocation().toOSString().length() > 70) {
			System.out.println("Skipping PublishingTests.testBug265726() because of path length issues.\n");
			return;
		}

		IFolder p = Utils.createFolder(buildFolder, "plugins/p");
		Utils.writeBuffer(p.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); public void Bar(){}}"));
		Utils.writeBuffer(p.getFile("src/b/B.java"), new StringBuffer("package b; public class B { public int i = 0; public void Foo(){}}"));
		Utils.generateBundle(p, "p");

		IFolder f = Utils.createFolder(buildFolder, "features/f");
		Utils.generateFeature(buildFolder, "f", new String[] {"org.eclipse.rcp"}, new String[] {"p"});
		Utils.writeBuffer(f.getFile("about.html"), new StringBuffer("about!\n"));
		Properties properties = new Properties();
		properties.put("bin.includes", "about.html, feature.xml");
		Utils.storeBuildProperties(f, properties);

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build.tests"), new org.eclipse.core.runtime.Path("/resources/keystore/keystore"), null);
		assertNotNull(resource);
		String keystorePath = FileLocator.toFileURL(resource).getPath();

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("archivesFormat", "group,group,group-folder");
		properties.put("p2.gathering", "true");
		properties.put("signJars", "true");
		properties.put("sign.alias", "pde.build");
		properties.put("sign.keystore", keystorePath);
		properties.put("sign.storepass", "storepass");
		properties.put("sign.keypass", "keypass");
		properties.put("jarProcessor.unsign", "true");
		properties.put("filteredDependencyCheck", "true");

		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		//bug 270887
		IFolder repo = Utils.createFolder(buildFolder, "buildRepo");
		URI repoURI = URIUtil.fromString("file:" + repo.getLocation().toOSString());
		assertManagerDoesntContain(repoURI);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("tmp/eclipse").getLocation().toOSString());
		assertNotNull(repository);

		File buildFile = buildFolder.getLocation().toFile();
		assertJarVerifies(new File(buildFile, "tmp/eclipse/plugins/p_1.0.0.jar"), true);
		assertJarVerifies(new File(buildFile, "tmp/eclipse/features/f_1.0.0.jar"), true);

		HashSet entries = new HashSet();
		entries.add("META-INF/PDE_BUIL.SF");
		entries.add("META-INF/PDE_BUIL.DSA");

		IInstallableUnit iu = getIU(repository, "com.ibm.icu");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/com.ibm.icu_" + iu.getVersion() + ".jar", entries);
		assertJarVerifies(new File(buildFile, "tmp/eclipse/plugins/com.ibm.icu_" + iu.getVersion() + ".jar"));
		iu = getIU(repository, "org.eclipse.core.commands");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/org.eclipse.core.commands_" + iu.getVersion() + ".jar", entries);
		assertJarVerifies(new File(buildFile, "tmp/eclipse/plugins/org.eclipse.core.commands_" + iu.getVersion() + ".jar"));
		iu = getIU(repository, "org.eclipse.equinox.app");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/org.eclipse.equinox.app_" + iu.getVersion() + ".jar", entries);
		iu = getIU(repository, "org.eclipse.help");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/org.eclipse.help_" + iu.getVersion() + ".jar", entries);
		iu = getIU(repository, "org.eclipse.swt");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/org.eclipse.swt_" + iu.getVersion() + ".jar", entries);
		iu = getIU(repository, "org.eclipse.ui");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/org.eclipse.ui_" + iu.getVersion() + ".jar", entries);
	}

	public void testMultiConfig() throws Exception {
		IFolder buildFolder = newTest("multiConfig");

		Utils.generateFeature(buildFolder, "f", null, new String[] {"org.eclipse.osgi;os=win32", "org.eclipse.equinox.common;os=linux"});

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("configs", "win32,win32,x86 & linux,gtk,x86");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		IFile tar = buildFolder.getFile("I.TestBuild/f-TestBuild-group.group.group.zip");
		assertResourceFile(tar);
		File untarred = new File(buildFolder.getLocation().toFile(), "unzipped");
		FileUtils.unzipFile(tar.getLocation().toFile(), untarred);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("unzipped").getLocation().toOSString());
		IInstallableUnit osgi = getIU(repository, "org.eclipse.osgi");
		IInstallableUnit common = getIU(repository, "org.eclipse.equinox.common");

		assertResourceFile(buildFolder, "unzipped/plugins/org.eclipse.osgi_" + osgi.getVersion() + ".jar");
		assertResourceFile(buildFolder, "unzipped/plugins/org.eclipse.equinox.common_" + common.getVersion() + ".jar");
		assertResourceFile(buildFolder, "unzipped/artifacts.xml");
		assertResourceFile(buildFolder, "unzipped/content.xml");
	}

	public void testShape_267506() throws Exception {
		IFolder buildFolder = newTest("publishShape");
		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		IFolder b = Utils.createFolder(buildFolder, "plugins/b");
		Utils.generateFeature(buildFolder, "f", null, new String[] {"a;unpack=true", "b;unpack=false"});

		Utils.generateBundle(a, "a");
		Utils.writeBuffer(a.getFile("src/A.java"), new StringBuffer("public class A { int i; }"));

		Properties includes = new Properties();
		includes.put("bin.includes", "META-INF/MANIFEST.MF, .");
		Utils.generateBundleManifest(b, "b", "1.0.0", null);
		Utils.generatePluginBuildProperties(b, includes);
		Utils.writeBuffer(b.getFile("src/B.java"), new StringBuffer("public class B { int i; }"));
		StringBuffer p2Inf = new StringBuffer();
		p2Inf.append("properties.1.name=pde.build\n"); //$NON-NLS-1$
		p2Inf.append("properties.1.value=true\n"); //$NON-NLS-1$
		Utils.writeBuffer(b.getFile("META-INF/p2.inf"), p2Inf);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		URI uri = URIUtil.fromString("file:" + buildFolder.getFolder("I.TestBuild/f-TestBuild-group.group.group.zip").getLocation().toOSString());
		IMetadataRepository repo = loadMetadataRepository(URIUtil.toJarURI(uri, new Path("")));
		IInstallableUnit iuA = getIU(repo, "a");
		assertTouchpoint(iuA, "zipped", "true");

		IInstallableUnit iuB = getIU(repo, "b");
		assertTrue(Boolean.valueOf((String) iuB.getProperties().get("pde.build")).booleanValue());

		/*
		 * Part 2. Use the above zipped repo as input to a build to test reusing IUs (bug 259792) 
		 */
		IFolder build2 = Utils.createFolder(buildFolder, "build2");
		Utils.generateFeature(build2, "f", null, new String[] {"a;unpack=false", "b;unpack=false"});

		properties = BuildConfiguration.getBuilderProperties(build2);
		properties.put("topLevelElementId", "f");
		properties.put("p2.gathering", "true");
		properties.put("repoBaseLocation", buildFolder.getFolder("I.TestBuild").getLocation().toOSString());
		//space here tries bug 267509, bug 267219
		properties.put("transformedRepoLocation", build2.getFolder("trans formed").getLocation().toOSString());
		Utils.storeBuildProperties(build2, properties);

		//bug 269122
		StringBuffer customBuffer = new StringBuffer();
		customBuffer.append("<project name=\"custom\" default=\"noDefault\">										\n");
		customBuffer.append("   <import file=\"${eclipse.pdebuild.templates}/headless-build/customTargets.xml\"/>	\n");
		customBuffer.append("   <target name=\"preProcessRepos\">													\n");
		customBuffer.append("      <echo message=\"pre Process Repos!\" />											\n");
		customBuffer.append("   </target>																			\n");
		customBuffer.append("   <target name=\"postProcessRepos\">													\n");
		customBuffer.append("      <echo message=\"post Process Repos!\" />											\n");
		customBuffer.append("   </target>																			\n");
		customBuffer.append("</project>																				\n");
		Utils.writeBuffer(build2.getFile("customTargets.xml"), customBuffer);

		runBuild(build2);

		assertLogContainsLines(build2.getFile("log.log"), new String[] {"pre Process Repos!", "post Process Repos!"});

		//reusing the metadata from part 1
		uri = URIUtil.fromString("file:" + build2.getFolder("I.TestBuild/f-TestBuild-group.group.group.zip").getLocation().toOSString());
		repo = loadMetadataRepository(URIUtil.toJarURI(uri, new Path("")));

		iuB = getIU(repo, "b");
		assertTrue(Boolean.valueOf((String) iuB.getProperties().get("pde.build")).booleanValue());
	}

	public void testBug267461_2() throws Exception {
		IFolder buildFolder = newTest("267461_2");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "uid.product", "rcp.product", "1.0.0.qualifier", "my.app", null, new String[] {"org.eclipse.osgi", "org.eclipse.equinox.simpleconfigurator"}, false, null);
		Properties p2Inf = new Properties(); // bug 268223
		p2Inf.put("org.eclipse.pde.build.append.launchers", "false");
		Utils.storeProperties(buildFolder.getFile("p2.inf"), p2Inf);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("configs", "win32,win32,x86");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("p2.gathering", "true");
		properties.put("p2.product.qualifier", "I10232"); //bug 246060
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		assertLogContainsLine(buildFolder.getFile("tmp/eclipse/configuration/config.ini"), "eclipse.product=rcp.product");
		assertLogContainsLine(buildFolder.getFile("tmp/eclipse/configuration/config.ini"), "eclipse.application=my.app");

		IFolder repo = Utils.createFolder(buildFolder, "buildRepo");
		IMetadataRepository metadata = loadMetadataRepository("file:" + repo.getLocation().toOSString());
		IInstallableUnit iu = getIU(metadata, "uid.product");
		assertEquals(iu.getVersion().toString(), "1.0.0.I10232");

		iu = getIU(metadata, "toolinguid.product.config.win32.win32.x86");
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.application, propValue:my.app);");
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.product, propValue:rcp.product);");

		iu = getIU(metadata, "toolingorg.eclipse.equinox.simpleconfigurator");
		assertTouchpoint(iu, "configure", "setStartLevel(startLevel:1);markStarted(started:true);");
		assertFalse(buildFolder.getFile("tmp/eclipse/eclipse.exe").exists());
	}

	public void testBug267972() throws Exception {
		IFolder buildFolder = newTest("267972");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0.qualifier", new String[] {"org.eclipse.osgi", "org.eclipse.equinox.common", "org.eclipse.swt", "org.eclipse.swt.win32.win32.x86", "org.eclipse.swt.gtk.linux.x86"}, false);
		Properties p2Inf = new Properties(); // bug 268223
		p2Inf.put("org.eclipse.pde.build.append.startlevels", "false");
		Utils.storeProperties(buildFolder.getFile("p2.inf"), p2Inf);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("configs", "win32,win32,x86");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("filteredDependencyCheck", "true");
		properties.put("includeLaunchers", "false"); //bug 268119
		properties.put("p2.gathering", "true");
		properties.put("forceContextQualifier", "v1234"); //bug 246060
		properties.put("skipDirector", "true"); //bug 271141
		properties.put("skipMirroring", "true");
		properties.put("p2.metadata.repo", "file:" + buildFolder.getFolder("finalRepo").getLocation().toOSString());
		properties.put("p2.artifact.repo", "file:" + buildFolder.getFolder("finalRepo").getLocation().toOSString());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFolder repo = Utils.createFolder(buildFolder, "buildRepo");
		URI repoURI = URIUtil.fromString("file:" + repo.getLocation().toOSString());
		assertManagerDoesntContain(repoURI); //bug 268867
		IMetadataRepository metadata = loadMetadataRepository(repoURI);
		IInstallableUnit iu = getIU(metadata, "rcp.product");
		assertEquals(iu.getVersion().toString(), "1.0.0.v1234");

		iu = null;
		try {
			//don't want to find this
			iu = getIU(metadata, "toolingorg.eclipse.equinox.common");
		} catch (AssertionFailedError e) {
		}
		assertNull(iu);

		//bug 271141
		assertFalse(buildFolder.getFile("I.TestBuild/eclipse-win32.win32.x86.zip").exists());
		assertFalse(buildFolder.getFolder("finalRepo").exists());
	}

	public void testBug266488() throws Exception {
		IFolder buildFolder = newTest("266488");
		IFolder bundle = Utils.createFolder(buildFolder, "plugins/e");
		IFolder f = Utils.createFolder(buildFolder, "features/f");
		IFolder e = Utils.createFolder(buildFolder, "features/e");

		Utils.generateFeature(buildFolder, "f", new String[] {"e", "e.source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@e.source", "e");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(f, properties);

		Utils.generateFeature(buildFolder, "e", null, new String[] {"e"});
		Utils.writeBuffer(e.getFile("sourceTemplatePlugin/license.html"), new StringBuffer("important stuff!\n"));

		Utils.generateBundle(bundle, "e");
		properties = new Properties();
		properties.put("bin.includes", "META-INF/, .");
		Utils.storeBuildProperties(bundle, properties);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("p2.gathering", "true");
		properties.put("archivesFormat", "group,group,group-folder");
		properties.put("skipMirroring", "true"); //bug 271114
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		assertFalse(buildFolder.getFolder("tmp/eclipse").exists());

		properties.remove("skipMirroring");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		assertResourceFile(buildFolder.getFile("tmp/eclipse/plugins/e.source_1.0.0.jar"));
		assertResourceFile(buildFolder.getFile("tmp/eclipse/plugins/e_1.0.0.jar"));
		assertResourceFile(buildFolder.getFile("tmp/eclipse/features/e.source_1.0.0.jar"));

		Set entries = new HashSet();
		entries.add("license.html");
		assertZipContents(buildFolder, "tmp/eclipse/plugins/e.source_1.0.0.jar", entries);
	}

	public void testPublishFeature_Bug270882() throws Exception {
		IFolder buildFolder = newTest("PublishFeature_Bug270882");

		IFolder f = Utils.createFolder(buildFolder, "features/f");

		IFile licenseFile = f.getFile("license.html");
		Utils.writeBuffer(licenseFile, new StringBuffer("important stuff!\n"));

		Utils.generateFeature(buildFolder, "f", null, null);
		Properties properties = new Properties();
		properties.put("root", "absolute:file:" + licenseFile.getLocation().toOSString());
		Utils.storeBuildProperties(f, properties);

		//bug 270894
		StringBuffer buffer = new StringBuffer();
		buffer.append("<site>																					\n");
		buffer.append("   <feature url=\"features/f_1.0.0.qualifier.jar\" id=\"f\" version=\"1.0.0.qualifier\">	\n");
		buffer.append("      <category name=\"new_category_1\"/>												\n");
		buffer.append("   </feature>																			\n");
		buffer.append("   <category-def name=\"new_category_1\" label=\"New Category 1\"/>						\n");
		buffer.append("</site>																					\n");
		IFile siteXML = buildFolder.getFile("site.xml");
		Utils.writeBuffer(siteXML, buffer);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("p2.gathering", "true");
		properties.put("filteredDependencyCheck", "true");
		properties.put("archivesFormat", "group,group,group-folder");
		properties.put("p2.category.site", "file:" + siteXML.getLocation().toOSString());
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("license.html");
		assertZipContents(buildFolder, "tmp/eclipse/binary/f_root_1.0.0", entries);

		IFolder repo = buildFolder.getFolder("tmp/eclipse");
		URI repoURI = URIUtil.fromString("file:" + repo.getLocation().toOSString());
		IMetadataRepository metadata = loadMetadataRepository(repoURI);

		assertNotNull(getIU(metadata, "new_category_1"));
	}

	public void testBug264743_PublishExecutable() throws Exception {
		IFolder buildFolder = newTest("264743").getFolder("build1");

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "org.eclipse.equinox.executable");
		properties.put("p2.gathering", "true");
		properties.put("filteredDependencyCheck", "true");
		properties.put("archivesFormat", "group,group,group-folder");
		properties.put("feature.temp.folder", buildFolder.getFolder("ftemp").getLocation().toOSString());
		properties.put("elementPath", "${buildDirectory}/features/ee");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		IFolder repo = buildFolder.getFolder("tmp/eclipse");
		URI repoURI = URIUtil.fromString("file:" + repo.getLocation().toOSString());
		IMetadataRepository metadata = loadMetadataRepository(repoURI);
		IInstallableUnit iu = getIU(metadata, "equinox.executable");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.equinox.executable.feature.jar");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.equinox.launcher");
		Set entries = new HashSet();
		entries.add("bin/win32/win32/x86/launcher.exe");
		entries.add("bin/carbon/macosx/ppc/Eclipse.app/Contents/MacOS/launcher");
		entries.add("bin/gtk/linux/x86/launcher");
		assertZipContents(buildFolder, "tmp/eclipse/features/org.eclipse.equinox.executable_3.3.200.jar", entries);

		IFolder build2 = Utils.createFolder(buildFolder, "../build2");

		IFile productFile = build2.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {"org.eclipse.osgi", "org.eclipse.equinox.common"}, false);

		properties = BuildConfiguration.getBuilderProperties(build2);
		properties.put("configs", "win32,win32,x86 & linux,gtk,x86");
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("filteredDependencyCheck", "true");
		properties.put("p2.gathering", "true");
		properties.put("skipDirector", "true");
		properties.put("skipMirroring", "true");
		//this tests bug 269361
		properties.put("repoBaseLocation", buildFolder.getFolder("tmp").getLocation().toOSString());
		properties.put("transformedRepoLocation", build2.getFolder("transformed").getLocation().toOSString());

		Utils.storeBuildProperties(build2, properties);

		runProductBuild(build2);

		repo = build2.getFolder("buildRepo");
		repoURI = URIUtil.fromString("file:" + repo.getLocation().toOSString());
		metadata = loadMetadataRepository(repoURI);
		iu = getIU(metadata, "org.eclipse.equinox.executable.feature.group");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.equinox.executable_root.win32.win32.x86");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.equinox.executable_root.gtk.linux.x86");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.equinox.executable_root.carbon.macosx.ppc");

		assertResourceFile(repo, "binary/org.eclipse.equinox.executable_root.win32.win32.x86_3.3.200");
		assertResourceFile(repo, "binary/org.eclipse.equinox.executable_root.gtk.linux.x86_3.3.200");
		assertResourceFile(repo, "binary/org.eclipse.equinox.executable_root.carbon.macosx.ppc_3.3.200");
		assertResourceFile(repo, "binary/rcp.product_root.gtk.linux.x86_1.0.0");
		assertResourceFile(repo, "binary/rcp.product_root.win32.win32.x86_1.0.0");
	}
}
