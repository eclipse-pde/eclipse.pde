/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import junit.framework.AssertionFailedError;
import org.apache.tools.ant.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.build.internal.tests.ant.AntUtils;
import org.eclipse.pde.build.internal.tests.ant.TestBrandTask;
import org.eclipse.pde.build.tests.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.swt.tools.IconExe;
import org.osgi.framework.Version;

/**
 * NOTE:  To run some of these tests, you must have the delta pack installed in your target.  Any 
 * test that calls {@link Utils#findDeltaPack()} will fail without the delta pack being available
 */
public class ProductTests extends PDETestCase {

	public void testBug192127() throws Exception {
		IFolder buildFolder = newTest("192127");
		IFolder containerFeature = Utils.createFolder(buildFolder, "features/org.eclipse.pde.build.container.feature");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		// Exporting from the UI gives the container feature some /Eclipse.App root files
		Utils.generateFeature(buildFolder, "org.eclipse.pde.build.container.feature", null, null, "/rcp/rcp.product", true, true, null);
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

	public void test218878() throws Exception {
		//platform specific config.ini files
		//files copied from resources folder
		IFolder buildFolder = newTest("218878");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "acme.product");
		properties.put("configs", "win32,win32,x86 & linux, gtk, x86");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("eclipse/pablo.exe");
		entries.add("eclipse/configuration/config.ini");

		assertZipContents(buildFolder, "I.TestBuild/eclipse-win32.win32.x86.zip", entries, false);

		IFile win32Config = buildFolder.getFile("win32.config.ini");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-win32.win32.x86.zip", "eclipse/configuration/config.ini", win32Config);
		Properties props = Utils.loadProperties(win32Config);
		assertEquals("win32", props.getProperty("os"));

		IFile linuxConfig = buildFolder.getFile("linux.config.ini");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-linux.gtk.x86.zip", "eclipse/configuration/config.ini", linuxConfig);
		props = Utils.loadProperties(linuxConfig);
		assertEquals("linux", props.getProperty("os"));
	}

	public void test234032() throws Exception {
		IFolder buildFolder = newTest("234032");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "test.product");
		properties.put("configs", "macosx,carbon,ppc");
		properties.put("archivesFormat", "macosx,carbon,ppc-folder");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFile iniFile = buildFolder.getFile("tmp/eclipse/test.app/Contents/MacOS/test.ini");
		assertLogContainsLine(iniFile, "-Dfoo=bar");
		//bug 313940
		assertLogContainsLine(iniFile, "-Dschemes1=archive zip jar");
		assertLogContainsLine(iniFile, "-Dschemes2=archive zip jar");
	}

	public void test237922() throws Exception {
		IFolder buildFolder = newTest("237922");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateFeature(buildFolder, "F", null, new String[] {"rcp"});
		Properties featureProperties = new Properties();
		featureProperties.put("root", "file:myFile.txt");
		featureProperties.put("bin.includes", "feature.xml");
		Utils.storeBuildProperties(buildFolder.getFolder("features/F"), featureProperties);
		Utils.writeBuffer(buildFolder.getFile("features/F/myFile.txt"), new StringBuffer("Please sir, may I have another?\n"));

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "F");
		properties.put("product", "/rcp/rcp.product");
		properties.put("configs", "win32,win32,x86");

		generateScripts(buildFolder, properties);

		IFile assembleScript = buildFolder.getFile("assemble.F.win32.win32.x86.xml");

		Map alternateTasks = new HashMap();
		alternateTasks.put("eclipse.brand", "org.eclipse.pde.build.internal.tests.ant.TestBrandTask");
		Project antProject = assertValidAntScript(assembleScript, alternateTasks);
		Target main = (Target) antProject.getTargets().get("main");
		assertNotNull(main);
		TestBrandTask brand = (TestBrandTask) AntUtils.getFirstChildByName(main, "eclipse.brand");
		assertNotNull(brand);

		assertTrue(brand.icons.indexOf("mail.ico") > 0);

		//bug 178928
		Target gather = (Target) antProject.getTargets().get("gather.bin.parts");
		Task[] subTasks = gather.getTasks();
		assertEquals(subTasks.length, 2);
	}

	public void test186224() throws Exception {
		IFolder buildFolder = newTest("186224");

		Utils.generateProduct(buildFolder.getFile("features/foo/foo.product"), null, "1.0.0", null, new String[] {"org.eclipse.osgi"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "/foo/foo.product");
		properties.put("configs", "win32,win32,x86");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/productBuild/productBuild.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"generateFeature", "generate"}, buildFolder.getLocation().toOSString(), properties);
	}

	public void test315792() throws Exception {
		IFolder buildFolder = newTest("315792");

		Utils.generateProduct(buildFolder.getFile("features/foo/foo.product"), null, "1.0.0", null, new String[] {"org.eclipse.osgi"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "/foo/foo.product");
		properties.put("configs", Platform.getOS() + ',' + Platform.getWS() + ',' + Platform.getOSArch());
		properties.put("verify", "true");
		properties.put("baseLocation", buildFolder.getFolder("base").getLocation().toOSString());
		properties.put("pluginPath", Platform.getInstallLocation().getURL().getPath());

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/productBuild/productBuild.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"generateFeature"}, buildFolder.getLocation().toOSString(), properties);
		assertLogContainsLine(buildFolder.getFile("log.log"), "[eclipse.generateFeature] Incorrect directory entry");
	}

	public void test237747() throws Exception {
		IFolder buildFolder = newTest("237747");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFolder fooFolder = Utils.createFolder(buildFolder, "plugins/foo");
		Utils.generateBundle(fooFolder, "foo");
		Utils.generateProduct(buildFolder.getFile("plugins/foo/foo.product"), null, "1.0.0", null, new String[] {"org.eclipse.osgi"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "/foo/foo.product");
		properties.put("configs", "win32,win32,x86_64 & aix, gtk, ppc64");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		assertResourceFile(buildFolder, "I.TestBuild/eclipse-aix.gtk.ppc64.zip");
		assertResourceFile(buildFolder, "I.TestBuild/eclipse-win32.win32.x86_64.zip");
	}

	public void testBug238001() throws Exception {
		IFolder buildFolder = newTest("238001");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("org.eclipse.equinox.executable");
			}
		};
		File[] files = new File(delta, "features").listFiles(filter);

		File win32Exe = new File(files[0], "bin/win32/win32/x86/launcher.exe");
		assertTrue(win32Exe.exists());
		File win64Exe = new File(files[0], "bin/win32/win32/x86_64/launcher.exe");
		assertTrue(win64Exe.exists());

		IFile win32File = buildFolder.getFile("win32.exe");
		win32File.create(new BufferedInputStream(new FileInputStream(win32Exe)), IResource.FORCE, null);
		IFile win64File = buildFolder.getFile("win64.exe");
		win64File.create(new BufferedInputStream(new FileInputStream(win64Exe)), IResource.FORCE, null);

		//steal the icons from test 237922
		URL ico = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/resources/237922/rcp/icons/mail.ico"), null);
		IFile icoFile = buildFolder.getFile("mail.ico");
		icoFile.create(ico.openStream(), IResource.FORCE, null);

		//IconExe prints to stderr, redirect that to a file
		PrintStream oldErr = System.err;
		PrintStream newErr = new PrintStream(new FileOutputStream(buildFolder.getLocation().toOSString() + "/out.out"));
		System.setErr(newErr);
		IconExe.main(new String[] {win32File.getLocation().toOSString(), icoFile.getLocation().toOSString()});
		IconExe.main(new String[] {win64File.getLocation().toOSString(), icoFile.getLocation().toOSString()});
		System.setErr(oldErr);
		newErr.close();

		assertEquals(new File(buildFolder.getLocation().toOSString(), "out.out").length(), 0);
	}

	public void testBug249410() throws Exception {
		IFolder buildFolder = newTest("249410");
		IFile product = buildFolder.getFile("foo.product");
		Utils.generateFeature(buildFolder, "f", null, new String[] {"a", "b", "c", "d"});
		Utils.generateProduct(product, null, "1.0.0", null, new String[] {"f"}, true);

		AssembleScriptGenerator.setConfigInfo("win32,win32,x86 & linux,gtk,x86");
		Config win32 = new Config("win32,win32,x86");
		Config linux = new Config("linux, gtk, x86");
		AssemblyInformation assembly = new AssemblyInformation();
		StateObjectFactory factory = Platform.getPlatformAdmin().getFactory();

		BundleDescription a = factory.createBundleDescription(1, "a", Version.emptyVersion, null, null, null, null, null, true, true, true, null, null, null, null);
		BundleDescription b = factory.createBundleDescription(2, "b", Version.emptyVersion, null, null, null, null, null, true, true, true, null, null, null, null);
		assembly.addPlugin(win32, a);
		assembly.addPlugin(linux, a);
		assembly.addPlugin(win32, b);
		assembly.addPlugin(linux, b);
		assembly.addPlugin(linux, factory.createBundleDescription(3, "c", Version.emptyVersion, null, null, null, null, null, true, true, true, "(& (osgi.ws=gtk) (osgi.os=linux) (osgi.arch=x86))", null, null, null));
		assembly.addPlugin(win32, factory.createBundleDescription(4, "d", Version.emptyVersion, null, null, null, null, null, true, true, true, "(& (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86))", null, null, null));

		ProductGenerator generator = new ProductGenerator();
		generator.setAssemblyInfo(assembly);
		generator.setWorkingDirectory(buildFolder.getLocation().toOSString());
		generator.setRoot(buildFolder.getLocation().toOSString() + "/");
		generator.setProduct(product.getLocation().toOSString());
		generator.generate();

		Properties win32Config = Utils.loadProperties(buildFolder.getFile("productRootFiles/win32.win32.x86/configuration/config.ini"));
		Properties linuxConfig = Utils.loadProperties(buildFolder.getFile("productRootFiles/linux.gtk.x86/configuration/config.ini"));

		String bundlesList = win32Config.getProperty("osgi.bundles");
		assertTrue(bundlesList.indexOf('a') > -1);
		assertTrue(bundlesList.indexOf('b') > -1);
		assertTrue(bundlesList.indexOf('d') > -1);

		bundlesList = linuxConfig.getProperty("osgi.bundles");
		assertTrue(bundlesList.indexOf('a') > -1);
		assertTrue(bundlesList.indexOf('b') > -1);
		assertTrue(bundlesList.indexOf('c') > -1);
	}

	public void testBug252246() throws Exception {
		IFolder buildFolder = newTest("252246");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, null, "1.0.0", null, new String[] {"A", "org.eclipse.equinox.simpleconfigurator", "org.eclipse.swt", "org.eclipse.swt.gtk.linux.x86"}, false);

		IFolder A1 = Utils.createFolder(buildFolder, "plugins/A1");
		IFolder A2 = Utils.createFolder(buildFolder, "plugins/A2");
		IFolder simple = Utils.createFolder(buildFolder, "plugins/simple");

		Utils.generateBundleManifest(A1, "A", "1.0.0.v1", null);
		Utils.generateBundleManifest(A2, "A", "1.0.0.v2", null);
		Utils.generateBundleManifest(simple, "org.eclipse.equinox.simpleconfigurator", "1.0.0", null);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86 & linux,gtk,x86");
		//properties.put("archivesFormat", "win32,win32,x86-folder");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFolder tmp = Utils.createFolder(buildFolder, "tmp");
		FileUtils.unzipFile(buildFolder.getFile("I.TestBuild/eclipse-win32.win32.x86.zip").getLocation().toFile(), tmp.getLocation().toFile());

		File file = buildFolder.getFolder("tmp/eclipse/plugins").getLocation().toFile();
		String[] a = file.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("A_1.0.0.v");
			}
		});
		assertTrue(a.length == 1);
		String bundleString = a[0].substring(0, a[0].length() - 4); //trim .jar

		//bug 218355
		IFile info = buildFolder.getFile("tmp/eclipse/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
		assertLogContainsLine(info, bundleString);
		boolean swtNotThere = true;
		try {
			assertLogContainsLine(info, "org.eclipse.swt.gtk.linux.x86");
			swtNotThere = false;
		} catch (AssertionFailedError e) {
			//good
		}
		assertTrue(swtNotThere);

		IFile gtkInfo = buildFolder.getFile("gtk.info");
		Utils.extractFromZip(buildFolder, "I.TestBuild/eclipse-linux.gtk.x86.zip", "eclipse/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info", gtkInfo);
		assertLogContainsLine(gtkInfo, "org.eclipse.swt.gtk.linux.x86");
	}

	public void testBug271373() throws Exception {
		IFolder buildFolder = newTest("271373");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"A;os=win32,linux;unpack=false"});

		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Eclipse-PlatformFilter"), "(| (osgi.os=win32) (osgi.os=linux))");
		Utils.generateBundleManifest(A, "A", "1.0.0", manifestAdditions);
		Utils.generatePluginBuildProperties(A, null);
		Utils.writeBuffer(A.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));

		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, null, "1.0.0", new String[] {"F"}, true);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("configs", "win32,win32,x86");
		properties.put("baseLocation", "");
		properties.put("includeLaunchers", "false");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		Utils.storeBuildProperties(buildFolder, properties);
		runProductBuild(buildFolder);

		assertResourceFile(buildFolder, "tmp/eclipse/plugins/A_1.0.0.jar");
	}

	public void testBug265438() throws Exception {
		IFolder buildFolder = newTest("265438");

		IFile product = buildFolder.getFile("foo.product");
		StringBuffer buffer = new StringBuffer();
		buffer.append("<product name=\"foo\" version=\"1.0.0\" useFeatures=\"false\">         \n");
		buffer.append("   <plugins>                                                           \n");
		buffer.append("      <plugin id=\"A\" version=\"1.0.0.v1\"  />                        \n");
		buffer.append("   </plugins>                                                          \n");
		buffer.append("   <configurations>                                                    \n");
		buffer.append("     <plugin id=\"A\" autoStart=\"true\" startLevel=\"0\" />           \n"); //bug 274901
		buffer.append("   </configurations>                                                   \n");
		buffer.append("</product>                                                             \n");
		Utils.writeBuffer(product, buffer);

		IFolder A1 = Utils.createFolder(buildFolder, "plugins/A1");
		IFolder A2 = Utils.createFolder(buildFolder, "plugins/A2");

		Utils.generateBundleManifest(A1, "A", "1.0.0.v1", null);
		Utils.generateBundleManifest(A2, "A", "1.0.0.v2", null);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("baseLocation", "");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		assertResourceFile(buildFolder, "tmp/eclipse/plugins/A_1.0.0.v1.jar");
		assertLogContainsLine(buildFolder.getFile("tmp/eclipse/configuration/config.ini"), "osgi.bundles=A@start");
	}

	public void testBug246060() throws Exception {
		IFolder buildFolder = newTest("246060");

		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, "foo", null, new String[] {"A", "id", "version"}, false);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<project name=\"project\" default=\"default\"> \n");
		buffer.append("   <target name=\"default\" >                  \n");
		buffer.append("      <eclipse.idReplacer productFilePath=\"" + product.getLocation().toOSString() + "\"\n");
		buffer.append("            selfVersion=\"1.2.3.va\"           \n");
		buffer.append("            pluginIds=\"A:0.0.0,1.2.3,id:0.0.0,2.3.4,version:0.0.0,1.2.1\" /> \n");
		buffer.append("	  </target>                                   \n");
		buffer.append("</project>                                     \n");

		IFile buildXML = buildFolder.getFile("build.xml");
		Utils.writeBuffer(buildXML, buffer);

		runAntScript(buildXML.getLocation().toOSString(), new String[] {"default"}, buildFolder.getLocation().toOSString(), null);

		ProductFile productFile = new ProductFile(product.getLocation().toOSString(), null);
		assertEquals(productFile.getVersion(), "1.2.3.va");

		Iterator i = productFile.getProductEntries().iterator();
		assertEquals(((FeatureEntry) i.next()).getVersion(), "1.2.3");
		assertEquals(((FeatureEntry) i.next()).getVersion(), "2.3.4");
		assertEquals(((FeatureEntry) i.next()).getVersion(), "1.2.1");
	}

	public void testBug262324() throws Exception {
		IFolder buildFolder = newTest("262324");

		IFile product = buildFolder.getFile("foo.product");
		StringBuffer buffer = new StringBuffer();
		buffer.append("<product name=\"foo\" useFeatures=\"false\">         \n");
		buffer.append("   <plugins>                                         \n");
		buffer.append("      <plugin id=\"org.eclipse.equinox.util\"/>      \n");
		buffer.append("      <plugin id=\"org.eclipse.osgi\"/>              \n");
		buffer.append("   </plugins>                                        \n");
		buffer.append("</product>                                           \n");
		Utils.writeBuffer(product, buffer);

		Utils.generateFeature(buildFolder, "container", null, new String[] {"org.eclipse.osgi", "org.eclipse.equinox.util"});
		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "container");
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86");
		generateScripts(buildFolder, properties);
	}

	public void testBug266056() throws Exception {
		IFolder buildFolder = newTest("266056");

		IFile product = buildFolder.getFile("foo.product");

		StringBuffer extra = new StringBuffer();
		extra.append("<configurations>                                                                                            \n");
		extra.append("  <plugin id=\"org.eclipse.equinox.app\" autoStart=\"false\" startLevel=\"0\" />                            \n");
		extra.append("  <plugin id=\"org.eclipse.equinox.common\" autoStart=\"true\" startLevel=\"1\" />                          \n");
		extra.append("  <property name=\"org.eclipse.update.reconcile\" value=\"false\" />                                        \n");
		extra.append("  <property name=\"osgi.bundles.defaultStartLevel\" value=\"3\" />                                          \n");
		extra.append("</configurations>                                                                                           \n");
		String[] bundles = new String[] {"org.eclipse.core.runtime", "org.eclipse.equinox.simpleconfigurator", "org.eclipse.equinox.app", "org.eclipse.equinox.common"};
		Utils.generateProduct(product, "foo.product", "1.0.0", null, bundles, false, extra);

		Utils.generateFeature(buildFolder, "container", null, bundles);
		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "container");
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86");
		generateScripts(buildFolder, properties);

		IFile config = buildFolder.getFile("features/container/productRootFiles/win32.win32.x86/configuration/config.ini");
		IFile info = buildFolder.getFile("features/container/productRootFiles/win32.win32.x86/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");

		Properties versions = Utils.loadProperties(buildFolder.getFile("finalPluginsVersions.properties"));
		assertLogContainsLine(config, "org.eclipse.update.reconcile=false");
		assertLogContainsLine(config, "osgi.bundles.defaultStartLevel=3");
		assertLogContainsLine(config, "osgi.bundles=org.eclipse.equinox.simpleconfigurator@1:start");
		assertLogContainsLine(info, "org.eclipse.core.runtime_" + versions.get("org.eclipse.core.runtime") + ",3,false");
		assertLogContainsLine(info, "org.eclipse.equinox.app_" + versions.get("org.eclipse.equinox.app") + ",3,false"); //bug 274901
		assertLogContainsLine(info, "org.eclipse.equinox.common_" + versions.get("org.eclipse.equinox.common") + ",1,true");
	}

	public void testBug266056_2() throws Exception {
		IFolder buildFolder = newTest("266056_2");

		IFile product = buildFolder.getFile("foo.product");

		StringBuffer extra = new StringBuffer();
		extra.append("<configurations>                                                                                \n");
		extra.append("  <plugin id=\"org.eclipse.equinox.common\" autoStart=\"true\" startLevel=\"-1\" />              \n");
		extra.append("  <property name=\"org.eclipse.update.reconcile\" value=\"false\" />                            \n");
		extra.append("</configurations>                                                                               \n");
		String[] bundles = new String[] {"org.eclipse.core.runtime", "org.eclipse.update.configurator", "org.eclipse.equinox.common"};
		Utils.generateProduct(product, "foo.product", "1.0.0", null, bundles, false, extra);

		Utils.generateFeature(buildFolder, "container", null, bundles);
		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "container");
		properties.put("product", product.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("configs", "win32,win32,x86");
		generateScripts(buildFolder, properties);

		IFile config = buildFolder.getFile("features/container/productRootFiles/win32.win32.x86/configuration/config.ini");

		assertLogContainsLine(config, "org.eclipse.update.reconcile=false");
		assertLogContainsLine(config, "osgi.bundles=org.eclipse.core.runtime,org.eclipse.update.configurator,org.eclipse.equinox.common@start");

	}

	public void testBug269540() throws Exception {
		IFolder buildFolder = newTest("269540");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFolder a = Utils.createFolder(buildFolder, "plugins/A");
		Utils.generateBundle(a, "A");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Eclipse-PlatformFilter"), "(& (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86))");
		Utils.generateBundleManifest(a, "A", "1.0.0", manifestAdditions);
		Utils.generatePluginBuildProperties(a, null);
		Utils.writeBuffer(a.getFile("src/a.java"), new StringBuffer("class A {}"));

		StringBuffer buffer = new StringBuffer();
		buffer.append("<launcher name=\"rcp\">                    \n");
		buffer.append("   <win useIco=\"true\">                   \n");
		buffer.append("      <ico path=\"/A/mail.ico\"/>          \n");
		buffer.append("   </win>                                  \n");
		buffer.append("</launcher>                                \n");
		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, "rcp", "1.0.0", null, "rcp", new String[] {"A"}, false, buffer);

		//steal the icons from test 237922
		URL ico = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/resources/237922/rcp/icons/mail.ico"), null);
		IFile icoFile = a.getFile("mail.ico");
		icoFile.create(ico.openStream(), IResource.FORCE, null);

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("product", product.getLocation().toOSString());
		buildProperties.put("filteredDependencies", "true");
		buildProperties.put("pluginPath", delta.getAbsolutePath());
		buildProperties.put("configs", "win32, win32, x86");
		//buildProperties.put("archivesFormat", "win32,win32,x86 - folder");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runProductBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("eclipse/plugins/A_1.0.0.jar");
		entries.add("eclipse/rcp.exe");
		assertZipContents(buildFolder, "I.TestBuild/eclipse-win32.win32.x86.zip", entries);
	}
}
