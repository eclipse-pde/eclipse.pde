/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved.
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
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.Activator;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.internal.build.P2InfUtils;
import org.eclipse.pde.internal.build.site.*;
import org.osgi.framework.Constants;

public class PublishingTests extends P2TestCase {

	public void testPublishBundle_simple() throws Exception {
		IFolder buildFolder = newTest("PublishBundle_simple");

		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("import b.c.d.B; public class A { B b = new B(); }"));
		Utils.writeBuffer(bundle.getFile("src/b/c/d/B.java"), new StringBuffer("package b.c.d; public class B { int i = 0; }"));
		Utils.writeBuffer(bundle.getFile("about.txt"), new StringBuffer("All about bundle."));
		Utils.writeBuffer(bundle.getFile("META-INF/p2.inf"), new StringBuffer("instructions.install=myRandomAction(foo: bar);"));
		Properties properties = new Properties();
		properties.put("bin.includes", "META-INF/, ., about.txt");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), OSGI);
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
		contents.add("b/");
		contents.add("b/c/");
		contents.add("b/c/d/");
		contents.add("b/c/d/B.class");
		assertZipContents(buildFolder, "buildRepo/plugins/bundle_1.0.0.v1234.jar", contents);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "bundle");
		assertEquals(iu.getId(), "bundle");
		assertEquals(iu.getVersion().toString(), "1.0.0.v1234");
		assertRequires(iu, "osgi.bundle", OSGI);
		assertTouchpoint(iu, "install", "myRandomAction");
	}

	public void testBug277824() throws Exception {
		IFolder buildFolder = newTest("277824 space");

		StringBuffer buffer = new StringBuffer("I am a file");
		Utils.writeBuffer(buildFolder.getFile("file.txt"), buffer);

		Properties rootProperties = new Properties();
		rootProperties.put("root", "absolute:file:" + buildFolder.getFile("file.txt").getLocation().toOSString());
		IFile rootFile = buildFolder.getFile("root.properties");
		Utils.storeProperties(rootFile, rootProperties);

		IFile productFile = buildFolder.getFile("foo.product");
		Utils.generateProduct(productFile, "foo", "1.0.0.qualifier", null, new String[] {OSGI}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("p2.gathering", "true");
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("pluginList", EQUINOX_COMMON);
		properties.put("generatedBuildProperties", rootFile.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		properties.put("forceContextQualifier", "v1234");
		Utils.storeBuildProperties(buildFolder, properties);
		runProductBuild(buildFolder);

		IFolder repo = Utils.createFolder(buildFolder, "buildRepo");
		IMetadataRepository metadata = loadMetadataRepository(repo.getLocationURI());
		IInstallableUnit iu = getIU(metadata, "foo.root.feature.feature.group");
		assertEquals(iu.getVersion().toString(), "1.0.0.v1234");

		getIU(metadata, EQUINOX_COMMON);
		iu = getIU(metadata, "foo");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "foo.root.feature.feature.group");
		assertResourceFile(buildFolder.getFile("tmp/eclipse/file.txt"));
		iu = getIU(metadata, "foo.root.feature.feature.group");
		assertEquals(iu.getProperty("org.eclipse.equinox.p2.name"), "foo Root Files");
	}

	public void testBug277824_2() throws Exception {
		IFolder buildFolder = newTest("277824_2");
		IFolder f = Utils.createFolder(buildFolder, "features/f");

		Utils.generateFeature(buildFolder, "f", null, new String[] {EQUINOX_COMMON});
		Utils.writeBuffer(f.getFile("file.txt"), new StringBuffer("I'm a file!"));
		Properties properties = new Properties();
		properties.put("root", "file:file.txt");
		Utils.storeBuildProperties(f, properties);

		IFile productFile = buildFolder.getFile("foo.product");
		Utils.generateProduct(productFile, "foo", "1.0.0.qualifier", null, new String[] {OSGI}, false);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("p2.gathering", "true");
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("featureList", "f");
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		properties.put("filteredDependencyCheck", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runProductBuild(buildFolder);

		assertResourceFile(buildFolder.getFile("tmp/eclipse/file.txt"));

		IMetadataRepository meta = loadMetadataRepository(buildFolder.getFolder("buildRepo").getLocationURI());
		getIU(meta, EQUINOX_COMMON);
		getIU(meta, "f.feature.group");
	}

	public void testPublishFeature_rootFiles() throws Exception {
		IFolder buildFolder = newTest("PublishFeature_rootFiles");
		IFolder f = Utils.createFolder(buildFolder, "features/f");

		Utils.generateFeature(buildFolder, "f", null, null);
		StringBuffer buffer = new StringBuffer("I am a file");
		Utils.writeBuffer(f.getFile("app/contents/file"), buffer);
		Utils.writeBuffer(f.getFile("app/second"), buffer);
		Properties rootProperties = new Properties();
		//bug 274203
		rootProperties.put("root.folder.contents", "absolute:file:" + f.getFile("app/contents/file").getLocation().toOSString());
		rootProperties.put("root", "file:app/second");
		rootProperties.put("root.permissions.755", "file, second");
		rootProperties.put("root.permissions.766", "contents/file");
		Utils.storeBuildProperties(f, rootProperties);

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f");
		properties.put("p2.gathering", "true");
		properties.put("filteredDependencyCheck", "true");
		generateScripts(buildFolder, properties);

		String buildXMLPath = f.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		IMetadataRepository meta = loadMetadataRepository(buildFolder.getFolder("buildRepo").getLocationURI());
		getIU(meta, "f.feature.group");
		IInstallableUnit iu = getIU(meta, "f_root");
		assertTouchpoint(iu, "install", "chmod(targetDir:${installFolder}, targetFile:second, permissions:755)");
		assertTouchpoint(iu, "install", "chmod(targetDir:${installFolder}, targetFile:contents/file, permissions:766)");
		boolean fail = false;
		try {
			assertTouchpoint(iu, "install", "chmod(targetDir:${installFolder}, targetFile:file, permissions:755)");
			fail = true;
		} catch (AssertionFailedError e) {
		}
		assertFalse(fail);

		Set entries = new HashSet();
		entries.add("contents/file");
		entries.add("second");
		assertZipContents(buildFolder, "buildRepo/binary/f_root_1.0.0", entries);
	}

	public void testPublishFeature_versionReplacement() throws Exception {
		IFolder buildFolder = newTest("PublishFeature_versions");
		IFolder f = Utils.createFolder(buildFolder, "features/F");
		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		IFolder bar = Utils.createFolder(buildFolder, "plugins/bar");

		Utils.generateBundleManifest(bundle, "foo", "1.0.0.qualifier", null);
		Utils.generatePluginBuildProperties(bundle, null);
		Utils.writeBuffer(bundle.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));

		Attributes extra = new Attributes();
		extra.put(new Attributes.Name("Eclipse-BundleShape"), "jar");
		Utils.generateBundleManifest(bar, "bar", "1.0.0.qualifier", extra);
		Utils.generatePluginBuildProperties(bar, null);
		Utils.writeBuffer(bar.getFile("src/bar.java"), new StringBuffer("public class bar { int i; }"));

		//bug 274672, write the feature manually since generateFeature always adds an unpack attribute
		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature id=\"F\" version=\"1.0.0.qualifier\">		\n");
		buffer.append("  <plugin id=\"foo\" version=\"0.0.0\" />			\n");
		buffer.append("  <plugin id=\"bar\" version=\"0.0.0\" />			\n");
		buffer.append("</feature>											\n");
		Utils.writeBuffer(f.getFile("feature.xml"), buffer);
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml");
		Utils.storeBuildProperties(f, properties);

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "F");
		buildProperties.put("p2.gathering", "true");
		buildProperties.put("filteredDependencyCheck", "true");
		buildProperties.put("archivesFormat", "group,group,group-folder");
		buildProperties.put("forceContextQualifier", "12345");
		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		IFile featureXML = buildFolder.getFile("feature.xml");
		assertResourceFile(buildFolder, "tmp/eclipse/features/F_1.0.0.12345.jar");
		Utils.extractFromZip(buildFolder, "tmp/eclipse/features/F_1.0.0.12345.jar", "feature.xml", featureXML);

		IMetadataRepository repo = loadMetadataRepository(buildFolder.getFolder("tmp/eclipse").getLocationURI());
		IInstallableUnit iu = getIU(repo, "foo");
		assertTouchpoint(iu, "zipped", "true");

		iu = getIU(repo, "bar");
		boolean hasZipped = false;
		try {
			assertTouchpoint(iu, "zipped", "true");
			hasZipped = true;
		} catch (AssertionFailedError e) {
		}
		assertFalse(hasZipped);

		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		BuildTimeFeature model = factory.parseBuildFeature(featureXML.getLocationURI().toURL());
		assertEquals(model.getVersion(), "1.0.0.12345");
		assertEquals(model.getPluginEntries()[0].getVersion(), "1.0.0.12345");
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
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), OSGI);
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
		Utils.generateProduct(productFile, "foo", "1.0.0", null, new String[] {OSGI, "bundle"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("configs", "*,*,*");
		properties.put("includeLaunchers", "false");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.put("filteredDependencyCheck", "true");
		properties.put("filterP2Base", "true");
		properties.put("p2.gathering", "true");
		properties.put("p2.director.log", "director.log");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);
		assertLogContainsLine(buildFolder.getFile("tmp/eclipse/configuration/config.ini"), "bundle_1.0.0.jar@1\\:start");
		assertLogContainsLine(buildFolder.getFile("director.log"), "Installing foo 1.0.0");
	}

	protected File findExecutableFeature(File delta) throws Exception {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(EQUINOX_EXECUTABLE);
			}
		};

		File[] features = new File(delta, "features").listFiles(filter);
		assertTrue(features.length > 0);
		Arrays.sort(features, new Comparator() {
			public int compare(Object o1, Object o2) {
				return -1 * ((File) o1).getName().compareTo(((File) o2).getName());
			}
		});
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

		buffer = new StringBuffer();
		buffer.append("provides.0.name=my.provides\n");
		buffer.append("provides.0.namespace=test\n");
		buffer.append("provides.0.version=1.2.3\n");
		Utils.writeBuffer(f.getFile("p2.inf"), buffer);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f");
		properties.put("p2.gathering", "true");
		generateScripts(buildFolder, properties);

		String buildXMLPath = f.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		HashSet contents = new HashSet();
		contents.add("a.txt");
		contents.add("feature.xml");
		contents.add("p2.inf");
		assertZipContents(buildFolder, "buildRepo/features/f_1.0.0.jar", contents, false);
		//p2.inf was not expected in the jar
		assertEquals(contents.size(), 1);
		assertTrue(contents.contains("p2.inf"));

		IMetadataRepository repo = loadMetadataRepository(buildFolder.getFolder("buildRepo").getLocationURI());
		IInstallableUnit iu = getIU(repo, "f.feature.group");
		assertProvides(iu, "test", "my.provides");
	}

	public void testPublishFeature_ExecutableFeature() throws Exception {
		IFolder buildFolder = newTest("PublishFeature_Executable");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		File originalExecutable = findExecutableFeature(delta);

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", EQUINOX_EXECUTABLE);
		properties.put("launcherName", "eclipse");
		properties.put("p2.gathering", "true");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		generateScripts(buildFolder, properties);

		String buildXMLPath = new File(originalExecutable, "build.xml").getAbsolutePath();
		runAntScript(buildXMLPath, new String[] {"publish.bin.parts"}, buildFolder.getLocation().toOSString(), properties);

		String executable = EQUINOX_EXECUTABLE;
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
		entries.add("about_files/");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), executable + "_root.gtk.linux.x86_" + version, entries);

		entries.add("Eclipse.app/");
		entries.add("Eclipse.app/Contents/");
		entries.add("Eclipse.app/Contents/Info.plist");
		entries.add("Eclipse.app/Contents/MacOS/");
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
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), OSGI);
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

		iu = getIU(repository, "org.eclipse.team.cvs.core");
		assertNotNull(iu);
		entries.add("plugins/org.eclipse.team.cvs.core_" + iu.getVersion() + ".jar");
		IFile file = buildFolder.getFile("buildRepo/plugins/org.eclipse.team.cvs.core_" + iu.getVersion() + ".jar");
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

		File executable = findExecutableFeature(delta);
		String executableVersion = executable.getName().substring(executable.getName().indexOf('_') + 1);

		IFile product = rcp.getFile("rcp.product");
		StringBuffer branding = new StringBuffer();
		branding.append("<launcher name=\"branded\">           \n");
		branding.append("   <macosx icon=\"mail.icns\" />      \n");
		branding.append("   <win useIco=\"true\">              \n");
		branding.append("      <ico path=\"mail.ico\" />       \n");
		branding.append("      <bmp/>                          \n");
		branding.append("   </win>                             \n");
		branding.append("</launcher>                           \n");

		//bug 273115 - no version
		Utils.generateProduct(product, "org.example.rcp", null, null, new String[] {OSGI}, false, branding);

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
		//bug 274527 - cocoa.x86_64
		properties.put("configs", "win32,win32,x86 & macosx, carbon, ppc & macosx, cocoa, x86_64");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("branded.app/Contents/Info.plist");
		entries.add("branded.app/Contents/MacOS/branded.ini");
		entries.add("branded.app/Contents/MacOS/branded");
		entries.add("branded.app/Contents/Resources/mail.icns");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), "org.example.rcp_root.carbon.macosx.ppc_" + executableVersion, entries);

		entries.add("branded.app/Contents/Info.plist");
		entries.add("branded.app/Contents/MacOS/branded.ini");
		entries.add("branded.app/Contents/MacOS/branded");
		entries.add("branded.app/Contents/Resources/mail.icns");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), "org.example.rcp_root.cocoa.macosx.x86_64_" + executableVersion, entries);

		entries.clear();
		entries.add("branded.exe");
		assertZipContents(buildFolder.getFolder("buildRepo/binary"), "org.example.rcp_root.win32.win32.x86_" + executableVersion, entries);

		IMetadataRepository repository = loadMetadataRepository("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "org.example.rcp");
		assertEquals(iu.getId(), "org.example.rcp");
		assertEquals(iu.getVersion().toString(), "0.0.0");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", OSGI);

		//bug 218377
		iu = getIU(repository, "org.example.rcp_root.carbon.macosx.ppc");
		assertTouchpoint(iu, "install", "targetFile:branded.app/Contents/MacOS/branded");

		iu = getIU(repository, "org.example.rcp_root.cocoa.macosx.x86_64");
		assertTouchpoint(iu, "install", "targetFile:branded.app/Contents/MacOS/branded");

		assertResourceFile(buildFolder, "I.TestBuild/eclipse-macosx.carbon.ppc.zip");
		assertResourceFile(buildFolder, "I.TestBuild/eclipse-win32.win32.x86.zip");

		iu = getIU(repository, "org.eclipse.equinox.launcher.cocoa.macosx.x86_64");
		entries.clear();
		entries.add("eclipse/plugins/org.eclipse.equinox.launcher.cocoa.macosx.x86_64_" + iu.getVersion() + "/");
		assertZipContents(buildFolder, "I.TestBuild/eclipse-macosx.cocoa.x86_64.zip", entries);

		//bug 295282, bug 282652
		IFile iniFile = buildFolder.getFile("branded.ini");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-macosx.cocoa.x86_64.zip", "eclipse/branded.app/Contents/MacOS/branded.ini", iniFile);
		assertLogContainsLine(iniFile, "../../../plugins/org.eclipse.equinox.launcher");

		IFile wrongFile = buildFolder.getFile("wrong.ini");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-macosx.cocoa.x86_64.zip", "eclipse/Branded.app/Contents/MacOS/branded.ini", wrongFile);
		assertFalse(wrongFile.exists());
	}

	public void testAssemblePackage() throws Exception {
		IFolder buildFolder = newTest("publishAssemblePackage");

		IFolder p = Utils.createFolder(buildFolder, "plugins/p");
		Utils.writeBuffer(p.getFile("src/A.java"), new StringBuffer("import b.B; public class A { B b = new B(); public void Bar(){}}"));
		Utils.writeBuffer(p.getFile("src/b/B.java"), new StringBuffer("package b; public class B { public int i = 0; public void Foo(){}}"));
		Utils.generateBundle(p, "p");

		IFolder f = Utils.createFolder(buildFolder, "features/f");
		Utils.generateFeature(buildFolder, "f", null, new String[] {"p", OSGI});
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
		IInstallableUnit osgi = getIU(repository, OSGI);

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

		StringBuffer buffer = new StringBuffer();
		buffer = new StringBuffer();
		buffer.append("<site>																\n");
		buffer.append("   <feature url=\"features/f_1.0.0.jar\" id=\"f\" version=\"1.0.0\">	\n");
		buffer.append("      <category name=\"new_category_1\"/>							\n");
		buffer.append("   </feature>														\n");
		buffer.append("   <category-def name=\"new_category_1\" label=\"New Category 1\"/>	\n");
		buffer.append("</site>																\n");
		IFile siteXML = buildFolder.getFile("site.xml");
		Utils.writeBuffer(siteXML, buffer);

		String repoLocation = URIUtil.toUnencodedString(buildFolder.getFolder("untarred").getLocationURI());
		buffer = new StringBuffer();
		buffer.append("<project name=\"test\" default=\"noDefault\">			\n");
		buffer.append("   <target name=\"category\">							\n");
		buffer.append("      <eclipse.publish.featuresAndBundles 				\n");
		buffer.append("          artifactrepository=\"" + repoLocation + "\" 	\n");
		buffer.append("          metadatarepository=\"" + repoLocation + "\" 	\n");
		buffer.append("          site=\"" + URIUtil.toUnencodedString(siteXML.getLocationURI()) + "\"	\n");
		buffer.append("          compress=\"true\" />							\n");
		buffer.append("   </target>												\n");
		buffer.append("</project>												\n");
		IFile script = buildFolder.getFile("append.xml");
		Utils.writeBuffer(script, buffer);

		runAntScript(script.getLocation().toOSString(), new String[] {"category"}, buildFolder.getLocation().toOSString(), null);

		assertResourceFile(buildFolder, "untarred/artifacts.jar");
		assertResourceFile(buildFolder, "untarred/content.jar");
		IMetadataRepository repo = loadMetadataRepository(repoLocation);
		getIU(repo, "new_category_1");
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
		String[] bundles = new String[] {"headless", "org.eclipse.core.contenttype", "org.eclipse.core.jobs", "org.eclipse.core.runtime", EQUINOX_APP, EQUINOX_COMMON, EQUINOX_PREFERENCES, EQUINOX_REGISTRY, OSGI};
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

		IFolder binaryFolder = buildFolder.getFolder("/tmp/eclipse/binary");
		assertFalse(binaryFolder.exists());

		IMetadataRepository finalRepo = loadMetadataRepository("file:" + buildFolder.getFolder("finalRepo").getLocation().toOSString());
		getIU(finalRepo, "a.jre.javase");
		IInstallableUnit productIu = getIU(finalRepo, "headless.product");
		assertFalse(productIu.getVersion().toString().equals("1.0.0.qualifier")); //bug 246060, should be a timestamp
		//check up to the date on the timestamp, don't worry about hours/mins
		assertTrue(PublisherHelper.toOSGiVersion(productIu.getVersion()).getQualifier().startsWith(QualifierReplacer.getDateQualifier().substring(0, 8)));
		assertTouchpoint(productIu, "configure", "addRepository(type:0,location:file${#58}//foo/bar);");

		IInstallableUnit iu = getIU(finalRepo, "toolingorg.eclipse.equinox.common");
		assertEquals(iu.getVersion(), productIu.getVersion());

		iu = getIU(finalRepo, "toolingheadless.product_root." + Platform.getWS() + '.' + Platform.getOS() + '.' + Platform.getOSArch());
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
		Utils.writeBuffer(buildFolder.getFile("features/f/p2.inf"), new StringBuffer("properties.1.name=org.eclipse.equinox.p2.type.group\nproperties.1.value=false\n"));
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

		//bug 274703
		IInstallableUnit iu = getIU(repository, "f.feature.group");
		assertFalse(Boolean.valueOf(iu.getProperty("org.eclipse.equinox.p2.type.group")).booleanValue());

		File buildFile = buildFolder.getLocation().toFile();
		assertJarVerifies(new File(buildFile, "tmp/eclipse/plugins/p_1.0.0.jar"), true);
		assertJarVerifies(new File(buildFile, "tmp/eclipse/features/f_1.0.0.jar"), true);

		HashSet entries = new HashSet();
		entries.add("META-INF/PDE_BUIL.SF");
		entries.add("META-INF/PDE_BUIL.DSA");

		iu = getIU(repository, "com.ibm.icu");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/com.ibm.icu_" + iu.getVersion() + ".jar", entries);
		assertJarVerifies(new File(buildFile, "tmp/eclipse/plugins/com.ibm.icu_" + iu.getVersion() + ".jar"));
		iu = getIU(repository, "org.eclipse.core.commands");
		assertNotNull(iu);
		assertZipContents(buildFolder, "tmp/eclipse/plugins/org.eclipse.core.commands_" + iu.getVersion() + ".jar", entries);
		assertJarVerifies(new File(buildFile, "tmp/eclipse/plugins/org.eclipse.core.commands_" + iu.getVersion() + ".jar"));
		iu = getIU(repository, EQUINOX_APP);
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

		Utils.generateFeature(buildFolder, "f", null, new String[] {OSGI + ";os=win32", EQUINOX_COMMON + ";os=linux"});

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
		IInstallableUnit osgi = getIU(repository, OSGI);
		IInstallableUnit common = getIU(repository, EQUINOX_COMMON);

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
		//bug 287537
		Properties props = new Properties();
		props.put("customBuildCallbacks", "true");
		Utils.generatePluginBuildProperties(a, props);
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

		//bug 269122
		customBuffer = new StringBuffer();
		customBuffer.append("<project name=\"custom\" default=\"noDefault\">										\n");
		customBuffer.append("   <import file=\"${eclipse.pdebuild.templates}/headless-build/customAssembly.xml\"/>	\n");
		customBuffer.append("   <target name=\"post.gather.bin.parts\">												\n");
		customBuffer.append("      <echo message=\"post bin parts!\" />												\n");
		customBuffer.append("   </target>																			\n");
		customBuffer.append("</project>																				\n");
		Utils.writeBuffer(build2.getFile("customAssembly.xml"), customBuffer);

		runBuild(build2);

		IFile log = build2.getFile("log.log");
		assertLogContainsLines(log, new String[] {"pre Process Repos!", "post Process Repos!"});
		assertLogContainsLines(log, new String[] {"post bin parts!"});

		//reusing the metadata from part 1
		uri = URIUtil.fromString("file:" + build2.getFolder("I.TestBuild/f-TestBuild-group.group.group.zip").getLocation().toOSString());
		repo = loadMetadataRepository(URIUtil.toJarURI(uri, new Path("")));

		iuB = getIU(repo, "b");
		assertTrue(Boolean.valueOf((String) iuB.getProperties().get("pde.build")).booleanValue());

		repo = null;
		removeMetadataRepository(uri);
	}

	public void testBug267461_2() throws Exception {
		IFolder buildFolder = newTest("267461_2");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "uid.product", "rcp.product", "1.0.0.qualifier", "my.app", null, new String[] {OSGI, SIMPLE_CONFIGURATOR}, false, null);
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
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.application,propValue:my.app);");
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.product,propValue:rcp.product);");
		assertEquals(iu.getVersion().toString(), "1.0.0.I10232");

		iu = getIU(metadata, "toolingorg.eclipse.equinox.simpleconfigurator");
		assertEquals(iu.getVersion().toString(), "1.0.0.I10232");
		assertTouchpoint(iu, "configure", "setStartLevel(startLevel:1);markStarted(started:true);");
		assertFalse(buildFolder.getFile("tmp/eclipse/eclipse.exe").exists());
	}

	public void testBug267972() throws Exception {
		IFolder buildFolder = newTest("267972");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0.qualifier", new String[] {OSGI, EQUINOX_COMMON, "org.eclipse.swt", "org.eclipse.swt.win32.win32.x86", "org.eclipse.swt.gtk.linux.x86"}, false);
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

		assertNull(getIU(metadata, "toolingorg.eclipse.equinox.common", false));

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

		//bug 270894
		buffer = new StringBuffer();
		buffer.append("<site>																					\n");
		buffer.append("   <feature url=\"features/f_1.0.0.qualifier.jar\" id=\"f\" version=\"1.0.0.qualifier\">	\n");
		buffer.append("      <category name=\"new_category_1\"/>												\n");
		buffer.append("   </feature>																			\n");
		buffer.append("   <category-def name=\"new_category_1\" label=\"New Category 1\"/>						\n");
		buffer.append("</site>																					\n");
		IFile siteXML = buildFolder.getFile("site.xml");
		Utils.writeBuffer(siteXML, buffer);

		//bug 272362
		IFile categoryXML = buildFolder.getFile("category.xml");
		String categoryString = buffer.toString();
		categoryString = categoryString.replaceAll("new_category_1", "new_category_2");
		Utils.writeBuffer(categoryXML, new StringBuffer(categoryString));

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("p2.gathering", "true");
		properties.put("filteredDependencyCheck", "true");
		properties.put("archivesFormat", "group,group,group-folder");
		properties.put("p2.category.site", "file:" + siteXML.getLocation().toOSString());
		properties.put("p2.category.definition", "file:" + categoryXML.getLocation().toOSString());
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("license.html");
		assertZipContents(buildFolder, "tmp/eclipse/binary/f_root_1.0.0", entries);

		IFolder repo = buildFolder.getFolder("tmp/eclipse");
		URI repoURI = URIUtil.fromString("file:" + repo.getLocation().toOSString());
		IMetadataRepository metadata = loadMetadataRepository(repoURI);

		IInstallableUnit iu = getIU(metadata, "new_category_1");
		assertTrue(!iu.getVersion().toString().equals("0.0.0"));
		assertNotNull(getIU(metadata, "new_category_2"));

		assertFalse(buildFolder.getFile("tmp/eclipse/features/f_1.0.0.jar").exists());
		assertNull(getIU(metadata, "f.feature.jar", false));
	}

	public void testBug264743_PublishExecutable() throws Exception {
		IFolder buildFolder = newTest("264743").getFolder("build1");

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", EQUINOX_EXECUTABLE);
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
		assertRequires(iu, "org.eclipse.equinox.p2.iu", EQUINOX_LAUNCHER);
		Set entries = new HashSet();
		entries.add("bin/win32/win32/x86/launcher.exe");
		entries.add("bin/carbon/macosx/ppc/Eclipse.app/Contents/MacOS/launcher");
		entries.add("bin/gtk/linux/x86/launcher");
		assertZipContents(buildFolder, "tmp/eclipse/features/org.eclipse.equinox.executable_3.3.200.jar", entries);

		IFolder build2 = Utils.createFolder(buildFolder, "../build2");

		IFile productFile = build2.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0", null, "rcp", new String[] {OSGI, EQUINOX_COMMON}, false, null);

		properties = BuildConfiguration.getBuilderProperties(build2);
		properties.put("configs", "win32,win32,x86 & linux,gtk,x86 & macosx,carbon,ppc");
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

		iu = getIU(metadata, "org.eclipse.equinox.executable_root.carbon.macosx.ppc");
		assertTouchpoint(iu, "configure", "(linkTarget:Eclipse.app/Contents/MacOS/launcher,targetDir:${installFolder},linkName:launcher)");

		//bug 273059, the action will be written out of a map, so there is no order on the parameters
		iu = getIU(metadata, "rcp.product_root.carbon.macosx.ppc");
		assertTouchpoint(iu, "configure", "linkTarget:rcp.app/Contents/MacOS/rcp");
		assertTouchpoint(iu, "configure", "linkName:rcp");

		assertResourceFile(repo, "binary/org.eclipse.equinox.executable_root.win32.win32.x86_3.3.200");
		assertResourceFile(repo, "binary/org.eclipse.equinox.executable_root.gtk.linux.x86_3.3.200");
		assertResourceFile(repo, "binary/org.eclipse.equinox.executable_root.carbon.macosx.ppc_3.3.200");
		assertResourceFile(repo, "binary/rcp.product_root.gtk.linux.x86_1.0.0");
		assertResourceFile(repo, "binary/rcp.product_root.win32.win32.x86_1.0.0");
	}

	public void testBug269972() throws Exception {
		IFolder buildFolder = newTest("269972");
		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		IFolder b = Utils.createFolder(buildFolder, "plugins/b");
		Utils.generateFeature(buildFolder, "f", null, new String[] {"a;unpack=false", "b;unpack=false"});

		Utils.generateBundle(a, "a");
		Utils.writeBuffer(a.getFile("src/A.java"), new StringBuffer("public class A { int i; ds }"));

		Utils.generateBundle(b, "b");
		Utils.writeBuffer(b.getFile("src/B.java"), new StringBuffer("public class B { int i; }"));

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("p2.gathering", "true");
		properties.put("javacFailOnError", "false");
		Utils.storeBuildProperties(buildFolder, properties);

		try {
			runBuild(buildFolder);
		} catch (Exception e) {
			assertTrue(e.getMessage().indexOf("Unable to find: Installable Unit [ id=a version=1.0.0 ]") > -1);
		}

		URI repoURI = URIUtil.fromString("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		IMetadataRepository metadata = loadMetadataRepository(repoURI);
		IQueryResult queryResult = metadata.query(QueryUtil.createIUQuery("a"), null);
		assertTrue(queryResult.isEmpty());
		getIU(metadata, "b");
	}

	public void testBug259792_ReuseIUs() throws Exception {
		IFolder root = newTest("259792");
		IFolder build1 = Utils.createFolder(root, "build1");

		IFolder a = Utils.createFolder(build1, "plugins/a");
		IFolder b = Utils.createFolder(build1, "plugins/b");

		Utils.generateFeature(build1, "f1", new String[] {"f2"}, new String[] {"a"});

		Utils.generateFeature(build1, "f2", null, new String[] {"b"});
		StringBuffer p2Inf = new StringBuffer();
		P2InfUtils.printRequires(p2Inf, null, 1, P2InfUtils.NAMESPACE_IU, "a", "[1.0.0,1.0.0]", null, true);
		Utils.writeBuffer(build1.getFile("features/f2/p2.inf"), p2Inf);
		Utils.writeBuffer(build1.getFile("features/f2/a.txt"), new StringBuffer("boo-urns"));
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml");
		properties.put("root", "file:a.txt");
		Utils.storeBuildProperties(build1.getFolder("features/f2"), properties);

		Utils.generateBundle(a, "a");
		Utils.generateBundle(b, "b");

		properties = BuildConfiguration.getBuilderProperties(build1);
		properties.put("topLevelElementId", "f1");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(build1, properties);
		runBuild(build1);

		URI repoURI = URIUtil.fromString("file:" + build1.getFile("I.TestBuild/f1-TestBuild-group.group.group.zip").getLocation().toOSString());
		IMetadataRepository metadata = loadMetadataRepository(URIUtil.toJarURI(repoURI, null));

		IInstallableUnit iu = getIU(metadata, "f2.feature.group");
		assertRequires(iu, P2InfUtils.NAMESPACE_IU, "a");

		IFolder build2 = Utils.createFolder(root, "build2");

		Utils.generateFeature(build2, "f3", new String[] {"f2"}, null);
		properties = BuildConfiguration.getBuilderProperties(build2);
		properties.put("topLevelElementId", "f3");
		properties.put("p2.gathering", "true");
		properties.put("repoBaseLocation", build1.getFolder("I.TestBuild").getLocation().toOSString());
		properties.put("transformedRepoLocation", build2.getFolder("transformed").getLocation().toOSString());
		Utils.storeBuildProperties(build2, properties);
		runBuild(build2);

		//bug 272219
		assertResourceFile(build2.getFolder("transformed"), "binary/f2_root_1.0.0");

		repoURI = URIUtil.fromString("file:" + build2.getFile("I.TestBuild/f3-TestBuild-group.group.group.zip").getLocation().toOSString());
		metadata = loadMetadataRepository(URIUtil.toJarURI(repoURI, null));
		iu = getIU(metadata, "f2.feature.group");
		assertRequires(iu, P2InfUtils.NAMESPACE_IU, "a");

		getIU(metadata, "a");
		getIU(metadata, "f2_root"); //bug 271848, mirroring from context
		assertResourceFile(build2, "buildRepo/binary/f2_root_1.0.0");

		metadata = null;
		removeMetadataRepository(repoURI);
	}

	public void testPublish_FeatureBasedProduct() throws Exception {
		IFolder buildFolder = newTest("featureBasedProduct");
		IFolder finalRepo = Utils.createFolder(buildFolder, "final");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateFeature(buildFolder, "f", null, new String[] {OSGI, EQUINOX_COMMON});
		Utils.writeBuffer(buildFolder.getFile("features/f/important.txt"), new StringBuffer("boo-urns"));
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml");
		properties.put("root.folder.sub", "file:important.txt"); //bug 272392
		Utils.storeBuildProperties(buildFolder.getFolder("features/f"), properties);

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {"f"}, true);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("configs", "win32,win32,x86");
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("filteredDependencyCheck", "true");
		properties.put("p2.gathering", "true");
		properties.put("skipMirroring", "true");
		properties.put("p2.build.repo", "file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		Utils.storeBuildProperties(buildFolder, properties);

		//bug 269523
		StringBuffer customBuffer = new StringBuffer();
		customBuffer.append("<project name=\"custom\" default=\"noDefault\">										\n");
		customBuffer.append("   <import file=\"${eclipse.pdebuild.templates}/headless-build/customTargets.xml\"/>	\n");
		customBuffer.append("   <target name=\"postBuild\">															\n");
		customBuffer.append("      <p2.mirror destination=\"" + finalRepo.getLocation().toOSString() + "\"			\n");
		customBuffer.append("                 source=\"${p2.build.repo}\" >											\n");
		customBuffer.append("          <slicingOptions platformFilter=\"win32,win32,x86\" 							\n");
		customBuffer.append("                          followStrict=\"true\" /> 									\n");
		customBuffer.append("          <iu id=\"rcp.product\" version=\"1.0.0\" />									\n");
		customBuffer.append("      </p2.mirror>																		\n");
		customBuffer.append("   </target>																			\n");
		customBuffer.append("</project>																				\n");
		Utils.writeBuffer(buildFolder.getFile("customTargets.xml"), customBuffer);

		runProductBuild(buildFolder);

		assertResourceFile(finalRepo, "binary/f_root_1.0.0");
		assertResourceFile(finalRepo, "binary/rcp.product_root.win32.win32.x86_1.0.0");
		assertResourceFile(finalRepo, "features/f_1.0.0.jar");

		HashSet entries = new HashSet();
		entries.add("eclipse/eclipse.exe");
		entries.add("eclipse/features/f_1.0.0/feature.xml");
		entries.add("eclipse/sub/important.txt");
		assertZipContents(buildFolder, "I.TestBuild/eclipse-win32.win32.x86.zip", entries);
	}

	public void testDirectorLogging() throws Exception {
		IFolder buildFolder = newTest("directorLogging");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "uid.product", "rcp.product", "1.0.0.qualifier", "my.app", null, new String[] {OSGI, SIMPLE_CONFIGURATOR}, false, null);
		Properties p2Inf = new Properties(); // bug 268223
		p2Inf.put("requires.1.namespace", "foo");
		p2Inf.put("requires.1.name", "bar");
		p2Inf.put("requires.1.range", "[1.0.0,1.0.0]");
		p2Inf.put("requires.1.greedy", "true");
		Utils.storeProperties(buildFolder.getFile("p2.inf"), p2Inf);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("configs", "win32,win32,x86");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("p2.gathering", "true");
		properties.put("p2.product.qualifier", "I10232");
		properties.put("p2.director.log", "director.log");
		properties.put("filteredDependencyCheck", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		try {
			runProductBuild(buildFolder);
		} catch (Exception e) {
			assertTrue(e.getMessage().indexOf("A problem occured while invoking the director") > -1);
		}

		assertLogContainsLines(buildFolder.getFile("director.log"), new String[] {"Installation failed.", "Missing requirement: rcp.product 1.0.0.I10232 (uid.product 1.0.0.I10232) requires 'foo bar [1.0.0]' but it could not be found"});
	}

	public void testBug272907() throws Exception {
		IFolder buildFolder = newTest("272907");
		IFolder foo = Utils.createFolder(buildFolder, "plugins/foo");
		IFolder f = Utils.createFolder(buildFolder, "features/f");

		Utils.generateBundleManifest(foo, "foo", "1.0.0", null);
		Properties buildProperties = new Properties();
		buildProperties.put("source.src.jar", "src/");
		buildProperties.put("source..", "src2/");
		buildProperties.put("bin.includes", "META-INF/, ., src.jar");
		Utils.storeBuildProperties(foo, buildProperties);
		Utils.writeBuffer(foo.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));
		Utils.writeBuffer(foo.getFile("src2/foob.java"), new StringBuffer("public class foob { int i; }"));

		Utils.generateFeature(buildFolder, "f", null, new String[] {"foo"});
		buildProperties = new Properties();
		buildProperties.put("bin.includes", "feature.xml");
		Utils.storeBuildProperties(f, buildProperties);

		//bug 274702
		StringBuffer buffer = new StringBuffer();
		buffer.append("requires.0.namespace=org.eclipse.equinox.p2.iu	\n");
		buffer.append("requires.0.name=testid0							\n");
		buffer.append("requires.0.range=[1.2.3,1.3)						\n");
		buffer.append("requires.0.greedy=true							\n");
		buffer.append("units.0.id=testid0								\n");
		buffer.append("units.0.version=1.2.3							\n");
		buffer.append("units.0.provides.0.name=testid0					\n");
		buffer.append("units.0.provides.0.namespace=org.eclipse.equinox.p2.iu\n");
		buffer.append("units.0.provides.0.version=1.2.3					\n");
		Utils.writeBuffer(f.getFile("p2.inf"), buffer);

		buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "f");
		buildProperties.put("p2.gathering", "true");
		buildProperties.put("filteredDependencyCheck", "true");
		buildProperties.put("archivesFormat", "group,group,group-folder");
		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("src.jar");
		entries.add("foob.class");
		assertZipContents(buildFolder, "tmp/eclipse/plugins/foo_1.0.0.jar", entries);

		URI repoURI = URIUtil.fromString("file:" + buildFolder.getFolder("tmp/eclipse").getLocation().toOSString());
		IMetadataRepository metadata = loadMetadataRepository(repoURI);
		IInstallableUnit iu = getIU(metadata, "f.feature.group");
		assertRequires(iu, P2InfUtils.NAMESPACE_IU, "testid0");

		iu = getIU(metadata, "testid0");
		assertProvides(iu, P2InfUtils.NAMESPACE_IU, "testid0");
	}

	public void testBug268498() throws Exception {
		IFolder buildFolder = newTest("268498");
		IFolder rcp = Utils.createFolder(buildFolder, "rcp");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile product = rcp.getFile("rcp.product");
		StringBuffer extra = new StringBuffer();
		extra.append("<launcherArgs> 																				\n");
		extra.append("   <programArgsMac>-vm myVm -showsplash org.eclipse.platform</programArgsMac>							\n");
		extra.append("   <vmArgsMac>-XstartOnFirstThread -Dorg.eclipse.swt.internal.carbon.smallFonts</vmArgsMac>	\n");
		extra.append(" </launcherArgs>																				\n");
		extra.append(" <configurations>																				\n");
		extra.append("    <plugin id=\"" + EQUINOX_COMMON + "\" autoStart=\"true\" startLevel=\"2\" />			\n");
		extra.append(" </configurations>																			\n");
		Utils.generateProduct(product, "org.example.rcp", "1.0.0", null, new String[] {OSGI, EQUINOX_COMMON}, false, extra);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("configs", "macosx, cocoa, x86 & macosx, carbon, ppc");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFile ini = buildFolder.getFile("eclipse.ini");
		boolean lowerCase = true;
		if (!Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-macosx.carbon.ppc.zip", "eclipse/eclipse.app/Contents/MacOS/eclipse.ini", ini)) {
			lowerCase = false;
			Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-macosx.carbon.ppc.zip", "eclipse/Eclipse.app/Contents/MacOS/eclipse.ini", ini);
		}

		IFile zip = buildFolder.getFile("I.TestBuild/eclipse-macosx.carbon.ppc.zip");
		String exeString = (lowerCase ? "eclipse/eclipse.app/" : "eclipse/Eclipse.app/") + "Contents/MacOS/eclipse";
		assertZipPermissions(zip, exeString, "-rwxr-xr-x");

		assertLogContainsLines(ini, new String[] {"-vm", "myVm"});
		boolean duplicate = false;
		try {
			assertLogContainsLines(ini, new String[] {"-XstartOnFirstThread", "-XstartOnFirstThread"});
			duplicate = true;
		} catch (Error e) {
			//expected
		}
		assertFalse(duplicate);

		try {
			assertLogContainsLines(ini, new String[] {"-showSplash", "org.eclipse.platform", "-showSplash", "org.eclipse.platform"});
			duplicate = true;
		} catch (Error e) {
			//expected
		}
		assertFalse(duplicate);

		IMetadataRepository repo = loadMetadataRepository(buildFolder.getFolder("buildRepo").getLocationURI());
		IInstallableUnit iu = getIU(repo, "toolingcocoa.macosx.x86org.eclipse.equinox.common");
		assertEquals(iu.getVersion().toString(), "1.0.0");

		IInstallableUnit common = getIU(repo, EQUINOX_COMMON);
		Collection/*<IRequirement>*/required = iu.getRequirements();
		assertEquals(required.size(), 2);
		Iterator it = required.iterator();
		IRequiredCapability req0 = (IRequiredCapability) it.next();
		IRequiredCapability req1 = (IRequiredCapability) it.next();
		if (req0.getName().equals(EQUINOX_COMMON))
			assertEquals(req0.getRange(), new VersionRange(common.getVersion(), true, Version.MAX_VERSION, true));
		else
			assertEquals(req1.getRange(), new VersionRange(common.getVersion(), true, Version.MAX_VERSION, true));
	}

	public void testPublish_P2InfConfigProperty() throws Exception {
		IFolder buildFolder = newTest("infConfig");
		IFolder rcp = Utils.createFolder(buildFolder, "rcp");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile product = rcp.getFile("rcp.product");
		Utils.generateProduct(product, "org.example.rcp", "1.0.0", null, new String[] {OSGI}, false, null);

		StringBuffer inf = new StringBuffer();
		inf.append("requires.1.namespace=org.eclipse.equinox.p2.iu									\n");
		inf.append("requires.1.name=toolingorg.eclipse.configuration.macosx							\n");
		inf.append("requires.1.filter=(osgi.os=macosx)												\n");
		inf.append("requires.1.range=[1.0.0,1.0.0]													\n");
		inf.append("requires.1.greedy=true															\n");
		inf.append("requires.2.namespace=org.eclipse.equinox.p2.iu									\n");
		inf.append("requires.2.name=toolingorg.eclipse.configuration								\n");
		inf.append("requires.2.filter=(!(osgi.os=macosx))											\n");
		inf.append("requires.2.range=[1.0.0,1.0.0]													\n");
		inf.append("requires.2.greedy=true															\n");
		inf.append("units.1.id=toolingorg.eclipse.configuration.macosx								\n");
		inf.append("units.1.version=1.0.0															\n");
		inf.append("units.1.provides.1.namespace=org.eclipse.equinox.p2.iu							\n");
		inf.append("units.1.provides.1.name=toolingorg.eclipse.configuration.macosx					\n");
		inf.append("units.1.provides.1.version=1.0.0												\n");
		inf.append("units.1.filter=(osgi.os=macosx)													\n");
		inf.append("units.1.touchpoint.id=org.eclipse.equinox.p2.osgi								\n");
		inf.append("units.1.touchpoint.version=1.0.0												\n");
		inf.append("units.1.instructions.configure=setProgramProperty(propName:osgi.instance.area.default,propValue:@user.home/Documents/workspace);\n");
		inf.append("units.1.instructions.unconfigure=setProgramProperty(propName:osgi.instance.area.default,propValue:);\n");
		inf.append("units.2.id=toolingorg.eclipse.configuration										\n");
		inf.append("units.2.version=1.0.0															\n");
		inf.append("units.2.provides.1.namespace=org.eclipse.equinox.p2.iu							\n");
		inf.append("units.2.provides.1.name=toolingorg.eclipse.configuration						\n");
		inf.append("units.2.provides.1.version=1.0.0												\n");
		inf.append("units.2.filter=(!(osgi.os=macosx))												\n");
		inf.append("units.2.touchpoint.id=org.eclipse.equinox.p2.osgi								\n");
		inf.append("units.2.touchpoint.version=1.0.0												\n");
		inf.append("units.2.instructions.configure=setProgramProperty(propName:osgi.instance.area.default,propValue:@user.home/workspace);\n");
		inf.append("units.2.instructions.unconfigure=setProgramProperty(propName:osgi.instance.area.default,propValue:);\n");
		Utils.writeBuffer(rcp.getFile("p2.inf"), inf);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("p2.gathering", "true");
		properties.put("configs", "macosx,carbon,ppc & macosx,cocoa,x86 & win32,win32,x86");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFile ini = buildFolder.getFile("config-mac.ini");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-macosx.carbon.ppc.zip", "eclipse/configuration/config.ini", ini);
		assertLogContainsLine(ini, "osgi.instance.area.default=@user.home/Documents/workspace");
		ini = buildFolder.getFile("config-mac2.ini");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-macosx.cocoa.x86.zip", "eclipse/configuration/config.ini", ini);
		assertLogContainsLine(ini, "osgi.instance.area.default=@user.home/Documents/workspace");
		ini = buildFolder.getFile("config-win32.ini");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-win32.win32.x86.zip", "eclipse/configuration/config.ini", ini);
		assertLogContainsLine(ini, "osgi.instance.area.default=@user.home/workspace");
	}

	public void testBug262464_customConfig() throws Exception {
		IFolder buildFolder = newTest("262464");
		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");

		Utils.generateBundle(bundle, "bundle.foo");
		Properties customConfig = new Properties();
		customConfig.put("osgi.bundles", "org.eclipse.equinox.common@2:start, org.eclipse.equinox.app@start");
		customConfig.put("org.eclipse.update.reconcile", "false");
		Utils.storeProperties(bundle.getFile("config.ini"), customConfig);

		IFile product = bundle.getFile("bundle.product");
		StringBuffer extra = new StringBuffer();
		extra.append("<configIni use=\"default\">  \n");
		extra.append("   <win32>/bundle/config.ini</win32>\n");
		extra.append("   <linux>/bundle/config.ini</linux>\n");
		extra.append("   <macosx>/bundle/config.ini</macosx>\n");
		extra.append("</configIni>\n");
		String[] entries = new String[] {EQUINOX_COMMON, OSGI, EQUINOX_APP, EQUINOX_REGISTRY};
		Utils.generateProduct(product, "bun.dle.pro.duct", "bundle.product", "1.0.0", null, null, entries, false, extra);

		String configString = Platform.getOS() + ',' + Platform.getWS() + ',' + Platform.getOSArch();
		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("product", product.getLocation().toOSString());
		buildProperties.put("configs", configString);
		buildProperties.put("includeLaunchers", "false");
		buildProperties.put("p2.gathering", "true");
		buildProperties.put("filteredDependencyCheck", "true");
		buildProperties.put("archivesFormat", configString + "-folder");
		Utils.storeBuildProperties(buildFolder, buildProperties);
		runProductBuild(buildFolder);

		IFolder productFolder = buildFolder.getFolder("features/org.eclipse.pde.build.container.feature/product");
		//check we copied the config.ini file
		assertResourceFile(productFolder.getFile("bundle/config.ini"));

		URI repoURI = URIUtil.fromString("file:" + buildFolder.getFolder("buildRepo").getLocation().toOSString());
		IMetadataRepository metadata = loadMetadataRepository(repoURI);

		IFile eclipseProduct = buildFolder.getFile("tmp/eclipse/.eclipseproduct");
		assertResourceFile(eclipseProduct);
		Properties properties = Utils.loadProperties(eclipseProduct);
		assertEquals(properties.getProperty("name"), "bundle.product");
		assertEquals(properties.getProperty("id"), "bundle.product");
		IFile config = buildFolder.getFile("tmp/eclipse/configuration/config.ini");
		IInstallableUnit iu = getIU(metadata, EQUINOX_COMMON);
		String line = "org.eclipse.equinox.common_" + iu.getVersion() + ".jar@2\\:start";
		assertLogContainsLine(config, line);
		iu = getIU(metadata, EQUINOX_APP);
		line = "org.eclipse.equinox.app_" + iu.getVersion() + ".jar@4\\:start";
		assertLogContainsLine(config, line);
	}

	public void testBug283060() throws Exception {
		IFolder buildFolder = newTest("283060");
		IFolder F = Utils.createFolder(buildFolder, "features/F");
		IFolder rcp = Utils.createFolder(buildFolder, "rcp");

		String[] bundles = new String[] {OSGI, EQUINOX_COMMON, CORE_JOBS, SIMPLE_CONFIGURATOR};
		String[] bundleVersions = Utils.getVersionsNoQualifier(bundles);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature id=\"F\" version=\"1.0.0\">		\n");
		buffer.append("  <requires>											\n");
		for (int i = 0; i < bundleVersions.length; i++) {
			buffer.append("     <import plugin=\"" + bundles[i] + "\" version=\"" + bundleVersions[i] + "\" match=\"equivalent\" />	\n");
		}
		buffer.append("  </requires>											\n");
		buffer.append("</feature>											\n");
		Utils.writeBuffer(F.getFile("feature.xml"), buffer);
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml");
		Utils.writeBuffer(F.getFile("feature.xml"), buffer);

		IFile product = rcp.getFile("rcp.product");
		StringBuffer extra = new StringBuffer();
		extra.append(" <configurations>																					\n");
		extra.append("    <plugin id=\"" + EQUINOX_COMMON + "\" autoStart=\"true\" startLevel=\"2\" />				\n");
		extra.append("    <plugin id=\"org.eclipse.equinox.simpleconfigurator\" autoStart=\"true\" startLevel=\"1\" />	\n");
		extra.append(" </configurations>																				\n");
		Utils.generateProduct(product, "org.example.rcp", "1.0.0", null, new String[] {"F"}, true, extra);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		properties.put("p2.gathering", "true");
		properties.put("p2.context.repos", URIUtil.toUnencodedString(createCompositeFromBase(buildFolder.getFolder("context"))));
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFile bundlesInfo = buildFolder.getFile("tmp/eclipse/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
		assertResourceFile(bundlesInfo);
		assertLogContainsLine(bundlesInfo, ",2,true");

		//bug 283091
		IMetadataRepository meta = loadMetadataRepository(buildFolder.getFolder("buildRepo").getLocationURI());
		assertNull(getIU(meta, EQUINOX_APP, false));
	}

	public void testBug284499() throws Exception {
		IFolder buildFolder = newTest("284499");

		Utils.generateFeature(buildFolder, "f", null, new String[] {OSGI, EQUINOX_COMMON});

		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, "foo", "1.0.0", null, new String[] {"f"}, true);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		assertResourceFile(buildFolder.getFile("tmp/eclipse/.eclipseproduct"));
		IMetadataRepository meta = loadMetadataRepository(buildFolder.getFolder("buildRepo").getLocationURI());
		IInstallableUnit iu = getIU(meta, "foo");
		assertRequires(iu, P2InfUtils.NAMESPACE_IU, "f.feature.group");
		getIU(meta, "f.feature.group");
	}

	public void testBug293048() throws Exception {
		IFolder buildFolder = newTest("293048");
		IFolder A1 = Utils.createFolder(buildFolder, "plugins/A_1.0.0");
		IFolder A2 = Utils.createFolder(buildFolder, "plugins/A_2.0.0");

		Utils.generateBundle(A1, "a", "1.0.0.qualifier");
		Utils.generateBundle(A2, "a", "2.0.0.qualifier");

		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, "foo", "1.0.0", new String[] {"a;version=1.0.0.qualifier", "a;version=2.0.0.qualifier"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IMetadataRepository repo = loadMetadataRepository(buildFolder.getFolder("buildRepo").getLocationURI());
		IInstallableUnit iu = getIU(repo, "foo");
		Collection/*<IRequirement>*/required = iu.getRequirements();
		for (Iterator iterator = required.iterator(); iterator.hasNext();) {
			IRequiredCapability reqCap = (IRequiredCapability) iterator.next();
			if (reqCap.getName().equals("a")) {
				VersionRange range = reqCap.getRange();
				assertTrue(PublisherHelper.toOSGiVersion(range.getMinimum()).getQualifier().startsWith("20"));
				assertTrue(PublisherHelper.toOSGiVersion(range.getMinimum()).getMajor() == 1 || PublisherHelper.toOSGiVersion(range.getMinimum()).getMajor() == 2);
			}
		}
	}

	public void testBug307157() throws Exception {
		IFolder buildFolder = newTest("307157");

		Utils.generateFeature(buildFolder, "F", new String[] {"F1", "F2"}, null);
		Utils.generateFeature(buildFolder, "F1", null, null);
		Utils.generateFeature(buildFolder, "F2", null, null);

		Utils.writeBuffer(buildFolder.getFile("features/F1/file1.txt"), new StringBuffer("for feature 1"));
		Utils.writeBuffer(buildFolder.getFile("features/F2/file2.txt"), new StringBuffer("for feature 2"));

		Properties buildProperties = new Properties();
		buildProperties.put("bin.includes", "feature.xml,file*");
		Utils.storeBuildProperties(buildFolder.getFolder("features/F1"), buildProperties);
		Utils.storeBuildProperties(buildFolder.getFolder("features/F2"), buildProperties);

		buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "F");
		buildProperties.put("p2.gathering", "true");
		buildProperties.put("filteredDependencyCheck", "true");
		buildProperties.put("archivesFormat", "group,group,group-folder");
		buildProperties.put("feature.temp.folder", buildFolder.getFolder("temp").getLocation().toOSString());
		Utils.storeBuildProperties(buildFolder, buildProperties);
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("feature.xml");
		entries.add("file1.txt");
		assertZipContents(buildFolder, "tmp/eclipse/features/F1_1.0.0.jar", entries, true);

		entries.add("feature.xml");
		entries.add("file1.txt");
		entries.add("file2.txt");
		assertZipContents(buildFolder, "tmp/eclipse/features/F2_1.0.0.jar", entries, false);
		assertTrue(entries.contains("file1.txt"));
	}

	public void testBug271373() throws Exception {
		IFolder buildFolder = newTest("271373_publisher");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"A;os=win32,linux;unpack=false"});

		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Eclipse-PlatformFilter"), "(| (osgi.os=win32) (osgi.os=linux))");
		Utils.generateBundleManifest(A, "A", "1.0.0", manifestAdditions);
		Utils.generatePluginBuildProperties(A, null);
		Utils.writeBuffer(A.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));

		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, "foo.product", "1.0.0", new String[] {"F"}, true);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("p2.gathering", "true");
		properties.put("configs", "win32,win32,x86");
		properties.put("baseLocation", "");
		properties.put("includeLaunchers", "false");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		Utils.storeBuildProperties(buildFolder, properties);
		runProductBuild(buildFolder);

		assertResourceFile(buildFolder, "tmp/eclipse/plugins/A_1.0.0.jar");
	}

	public void testBug323286() throws Exception {
		IFolder buildFolder = newTest("323286");
		IFolder A_1 = Utils.createFolder(buildFolder, "plugins/A_1");
		IFolder A_2 = Utils.createFolder(buildFolder, "plugins/A_2");
		IFile productFile = buildFolder.getFile("product.product");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"A;version=1"});

		Utils.generateBundle(A_1, "A", "1");
		Utils.generateBundle(A_2, "A", "2");
		Utils.generateProduct(productFile, "product", "1.0.0", new String[] {"A"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("topLevelElementId", "F");
		properties.put("p2.gathering", "true");
		properties.put("baseLocation", "");
		properties.put("includeLaunchers", "false");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}

	public void testBug322340() throws Exception {
		IFolder buildFolder = newTest("322340");
		IFolder F = Utils.createFolder(buildFolder, "features/F");
		IFolder A = Utils.createFolder(buildFolder, "plugins/A");

		Utils.generateBundle(A, "A", "1");
		Utils.generateFeature(buildFolder, "F", null, new String[] {"A"});
		Utils.writeBuffer(F.getFile("file.txt"), new StringBuffer("I'm a file!"));
		Utils.writeBuffer(F.getFile("meToo.txt"), new StringBuffer("I'm also a file!"));

		Properties properties = new Properties();
		properties.put("root", "file:file.txt, file:meToo.txt");
		properties.put("root.permissions.755", "*.txt");
		Utils.storeBuildProperties(F, properties);

		IFile productFile = buildFolder.getFile("product.product");
		Utils.generateProduct(productFile, "product", "1", new String[] {"F"}, true);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("p2.gathering", "true");
		properties.put("configs", "linux,gtk,x86");
		properties.put("baseLocation", "");
		properties.put("includeLaunchers", "false");
		properties.put("archivesFormat", "linux,gtk,x86-antZip");
		Utils.storeBuildProperties(buildFolder, properties);
		runProductBuild(buildFolder);

		IFile zip = buildFolder.getFile("I.TestBuild/eclipse-linux.gtk.x86.zip");
		assertZipPermissions(zip, "eclipse/meToo.txt", "-rwxr-xr-x");
		assertZipPermissions(zip, "eclipse/file.txt", "-rwxr-xr-x");
	}
}
