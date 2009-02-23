/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests.p2;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.zip.ZipOutputStream;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.BuildConfiguration;

public class P2Tests extends P2TestCase {

	public void testP2SimpleProduct() throws Exception {
		IFolder buildFolder = newTest("p2.SimpleProduct");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "/test/test.product");
		properties.put("configs", Platform.getOS() + ',' + Platform.getWS() + ',' + Platform.getOSArch());
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());

		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");

		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		String p2Config = Platform.getWS() + '.' + Platform.getOS() + '.' + Platform.getOSArch();
		String launcherConfig = Platform.getOS().equals("macosx") ? Platform.getWS() + '.' + Platform.getOS() : p2Config;
		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		assertNotNull(repository);

		//some basic existance
		ArrayList ius = new ArrayList();
		ius.add(getIU(repository, "test"));
		ius.add(getIU(repository, "org.eclipse.equinox.launcher"));
		ius.add(getIU(repository, "org.eclipse.osgi"));
		ius.add(getIU(repository, "org.eclipse.core.runtime"));

		//check some start level info
		IInstallableUnit iu = getIU(repository, "tooling" + p2Config + "org.eclipse.core.runtime");
		assertTouchpoint(iu, "configure", "markStarted(started: true);");
		ius.add(iu);

		iu = getIU(repository, "tooling" + p2Config + "org.eclipse.equinox.common");
		assertTouchpoint(iu, "configure", "setStartLevel(startLevel:2);markStarted(started: true);");
		ius.add(iu);

		//product settings
		getIU(repository, "toolingtest.product.ini." + p2Config);

		iu = getIU(repository, "toolingtest.product.config." + p2Config);
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.application, propValue:test.application);");
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.product, propValue:test.product);");
		assertProvides(iu, "toolingtest.product", "test.product.config");

		//some launcher stuff
		iu = getIU(repository, "toolingorg.eclipse.equinox.launcher");
		assertTouchpoint(iu, "configure", "addProgramArg(programArg:-startup);addProgramArg(programArg:@artifact);");
		ius.add(iu);
		iu = getIU(repository, "toolingorg.eclipse.equinox.launcher." + launcherConfig);
		assertTouchpoint(iu, "configure", "addProgramArg(programArg:--launcher.library);addProgramArg(programArg:@artifact);");
		ius.add(iu);

		iu = getIU(repository, "test.product.launcher." + p2Config);
		assertProvides(iu, "toolingtest.product", "test.product.launcher");
		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.equinox.launcher." + launcherConfig);

		//And the main product IU
		iu = getIU(repository, "test.product");
		assertRequires(iu, "toolingtest.product", "test.product.launcher");
		assertRequires(iu, "toolingtest.product", "test.product.ini");
		assertRequires(iu, "toolingtest.product", "test.product.config");
		assertRequires(iu, ius, true);
	}

	public void testBug237096() throws Exception {
		IFolder buildFolder = newTest("237096");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"org.eclipse.osgi;unpack=false", "org.eclipse.core.runtime;unpack=false"});
		Properties featureProperties = new Properties();
		featureProperties.put("root", "rootfiles");
		Utils.storeBuildProperties(buildFolder.getFolder("features/F"), featureProperties);
		IFolder rootFiles = Utils.createFolder(buildFolder.getFolder("features/F"), "rootfiles");
		StringBuffer buffer = new StringBuffer("This is a notice.html");
		Utils.writeBuffer(rootFiles.getFile("notice.html"), buffer);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		properties.put("p2.root.name", "FRoot");
		properties.put("p2.root.version", "1.0.0");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		assertNotNull(repository);

		ArrayList ius = new ArrayList();
		ius.add(getIU(repository, "org.eclipse.osgi"));
		ius.add(getIU(repository, "org.eclipse.core.runtime"));
		ius.add(getIU(repository, "org.eclipse.launcher.ANY.ANY.ANY"));
		ius.add(getIU(repository, "toolingorg.eclipse.launcher.ANY.ANY.ANY"));

		IInstallableUnit iu = getIU(repository, "FRoot");
		assertRequires(iu, ius, true);
	}

	public void testBug242346() throws Exception {
		IFolder buildFolder = newTest("237096");
		IFile productFile = buildFolder.getFile("rcp.product");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {"org.eclipse.osgi", "org.eclipse.equinox.simpleconfigurator"}, false);

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("configs", "win32,win32,x86");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "toolingrcp.product.config.win32.win32.x86");
		//testing relative paths, just check that the value starts with org.eclipse.equinox..., don't bother worrying about dir separator
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:org.eclipse.equinox.simpleconfigurator.configUrl, propValue:file:org.eclipse.equinox.simpleconfigurator");
	}

	public void testBug222962() throws Exception {
		IFolder buildFolder = newTest("222962");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"org.eclipse.osgi;unpack=false", "org.eclipse.core.runtime;unpack=false"});

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		properties.put("p2.compress", "true");

		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		assertResourceFile(buildFolder, "repo/content.jar");
		assertResourceFile(buildFolder, "repo/artifacts.jar");
	}

	public void testBug237662() throws Exception {
		IFolder buildFolder = newTest("237662");
		IFolder repo = Utils.createFolder(buildFolder, "repo");
		IFile productFile = buildFolder.getFile("rcp.product");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {"org.eclipse.osgi", "org.eclipse.core.runtime", "org.eclipse.equinox.simpleconfigurator"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("configs", Platform.getOS() + ',' + Platform.getWS() + ',' + Platform.getOSArch());
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");

		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		String p2Config = Platform.getWS() + '.' + Platform.getOS() + '.' + Platform.getOSArch();
		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "tooling" + p2Config + "org.eclipse.core.runtime");
		assertTouchpoint(iu, "configure", "markStarted(started: true);");
	}

	public void testBug255518() throws Exception {
		IFolder buildFolder = newTest("255518");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {"org.junit4", "org.eclipse.pde.build"}, false);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("configs", Platform.getOS() + ',' + Platform.getWS() + ',' + Platform.getOSArch());

		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");

		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		File plugins = repo.getFolder("plugins").getLocation().toFile();
		File[] bundles = plugins.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("org.junit4_") || name.startsWith("org.eclipse.pde.build");
			}
		});
		assertTrue(bundles.length == 2);
		assertJarVerifies(bundles[0]);
		assertJarVerifies(bundles[1]);
	}

	public void testBug258126() throws Exception {
		IFolder buildFolder = newTest("258126");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFolder repo = Utils.createFolder(buildFolder, "repo");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"org.eclipse.osgi;unpack=false", "org.eclipse.core.runtime;unpack=false"});
		Properties featureProperties = new Properties();
		featureProperties.put("root", "rootfiles");
		Utils.storeBuildProperties(buildFolder.getFolder("features/F"), featureProperties);
		IFolder rootFiles = Utils.createFolder(buildFolder.getFolder("features/F"), "rootfiles");
		Utils.writeBuffer(rootFiles.getFile("eclipse.ini"), new StringBuffer("-foo\n-vmargs\n-Xmx540m\n"));

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {"F"}, true);

		String repoLocation = "file:" + repo.getLocation().toOSString();
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("configs", "win32,win32,x86");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());

		properties.put("archivesFormat", "win32,win32,x86-folder");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		IInstallableUnit iu = getIU(repository, "toolingrcp.product.ini.win32.win32.x86");
		assertTouchpoint(iu, "configure", "addJvmArg(jvmArg:-Xmx540m);");
		assertTouchpoint(iu, "configure", "addProgramArg(programArg:-foo);");
		try {
			assertTouchpoint(iu, "configure", "addProgramArg(programArg:-vmargs);");
			fail("vmargs as program arg");
		} catch (AssertionFailedError e) {
			assertEquals(e.getMessage(), "Action not found:addProgramArg(programArg:-vmargs);");
		}
	}

	public void testBug262421() throws Exception {
		IFolder buildFolder = newTest("262421");

		IFile productFile = buildFolder.getFile("rcp.product");
		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {"org.eclipse.osgi"}, false);

		IFile p2Inf = buildFolder.getFile("p2.inf");
		StringBuffer buffer = new StringBuffer();
		buffer.append("instructions.configure=addRepository(type:0,location:http${#58}//download.eclipse.org/eclipse/updates/3.4);");
		Utils.writeBuffer(p2Inf, buffer);

		IFolder repo = Utils.createFolder(buildFolder, "repo");
		String repoLocation = "file:" + repo.getLocation().toOSString();
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("includeLaunchers", "false");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		IInstallableUnit iu = getIU(repository, "rcp.product");
		assertTouchpoint(iu, "configure", "addRepository");
	}

	public void testBug265526_265524() throws Exception {
		IFolder buildFolder = newTest("265526");
		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		IFolder b = Utils.createFolder(buildFolder, "plugins/b");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"a;unpack=false", "b;unpack=true"});
		Utils.generateBundle(a, "a");
		Utils.writeBuffer(a.getFile("src/a.java"), new StringBuffer("class A {}"));
		Utils.generateBundle(b, "b");
		Utils.writeBuffer(b.getFile("src/b.java"), new StringBuffer("class B {}"));

		IFolder repo = Utils.createFolder(buildFolder, "repo/r1");
		String repoLocation = "file:" + repo.getLocation().toOSString();
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		properties.put("repoBaseLocation", buildFolder.getFolder("repo").getLocation().toOSString());
		properties.put("transformedRepoLocation", buildFolder.getFolder("outRepo").getLocation().toOSString());
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"transformRepos"}, buildFolder.getLocation().toOSString(), properties);

		assertResourceFile(buildFolder, "outRepo/plugins/b_1.0.0/B.class");
		assertResourceFile(buildFolder, "outRepo/plugins/a_1.0.0.jar");
		assertResourceFile(buildFolder, "outRepo/artifacts.xml");
		assertResourceFile(buildFolder, "outRepo/content.xml");

		//part 2, zipped repos
		IFolder zipped = Utils.createFolder(buildFolder, "zipped");
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(new File(zipped.getLocation().toFile(), "zipped repo.zip")));
		File root = buildFolder.getFolder("repo/r1").getLocation().toFile();
		FileUtils.zip(output, root, Collections.EMPTY_SET, FileUtils.createRootPathComputer(root));
		org.eclipse.pde.internal.build.Utils.close(output);

		IFolder outRepo2 = Utils.createFolder(buildFolder, "outRepo2");
		properties.put("repoBaseLocation", zipped.getLocation().toOSString());
		properties.put("transformedRepoLocation", outRepo2.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"transformRepos"}, buildFolder.getLocation().toOSString(), properties);

		assertResourceFile(outRepo2, "plugins/b_1.0.0/B.class");
		assertResourceFile(outRepo2, "plugins/a_1.0.0.jar");
		assertResourceFile(outRepo2, "artifacts.xml");
		assertResourceFile(outRepo2, "content.xml");
	}

	public void testBug265564() throws Exception {
		IFolder buildFolder = newTest("265564");

		IFolder repo = Utils.createFolder(buildFolder, "repo");
		String repoLocation = "file:" + repo.getLocation().toOSString();

		Utils.generateFeature(buildFolder, "F", new String[] {"org.eclipse.cvs"}, null);
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		assertResourceFile(buildFolder, "repo/artifacts.xml");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build.tests"), new Path("/resources/keystore/keystore"), null);
		assertNotNull(resource);
		String keystorePath = FileLocator.toFileURL(resource).getPath();

		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>               \n");
		buffer.append("<project name=\"project\" default=\"default\">           \n");
		buffer.append("    <target name=\"default\">                            \n");
		buffer.append("    	<p2.process.artifacts  repositoryPath=\"" + repoLocation + "\" pack=\"true\">  \n");
		buffer.append("    	   <sign keystore=\"" + keystorePath + "\"         \n");
		buffer.append("    			 keypass=\"keypass\"                        \n");
		buffer.append("    			 storepass=\"storepass\"                    \n");
		buffer.append("    			 alias=\"pde.build\"                        \n");
		buffer.append("    			 unsign=\"true\" />                         \n");
		buffer.append("    	</p2.process.artifacts>                             \n");
		buffer.append("    </target>                                            \n");
		buffer.append("</project>                                               \n");

		final IFile buildXML = buildFolder.getFile("build.xml");
		Utils.writeBuffer(buildXML, buffer);

		runAntScript(buildXML.getLocation().toOSString(), new String[] {"default"}, buildFolder.getLocation().toOSString(), null);

		IFolder repoFolder = buildFolder.getFolder("repo");
		IArtifactRepository repository = loadArtifactRepository(repoLocation);
		final String PACKED_FORMAT = "packed"; //$NON-NLS-1$
		IArtifactKey[] keys = repository.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(keys[i]);
			assertEquals(descriptors.length, 2);

			if (PACKED_FORMAT.equals(descriptors[0].getProperty(IArtifactDescriptor.FORMAT))) {
				assertMD5(repoFolder, descriptors[1]);
			} else if (PACKED_FORMAT.equals(descriptors[1].getProperty(IArtifactDescriptor.FORMAT))) {
				assertMD5(repoFolder, descriptors[0]);
			} else {
				fail("No pack.gz desriptor");
			}

			assertResourceFile(repoFolder, getArtifactLocation(descriptors[0]));
			assertResourceFile(repoFolder, getArtifactLocation(descriptors[1]));
		}
	}

	public void testBug263272() throws Exception {
		IFolder buildFolder = newTest("263272");

		IFolder repo1 = Utils.createFolder(buildFolder, "repo1");
		String repo1Location = "file:" + repo1.getLocation().toOSString();
		IFolder repo2 = Utils.createFolder(buildFolder, "repo2");
		String repo2Location = "file:" + repo2.getLocation().toOSString();
		IFolder finalRepo = Utils.createFolder(buildFolder, "final");
		String finalLocation = "file:" + finalRepo.getLocation().toOSString();

		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		IFolder b = Utils.createFolder(buildFolder, "plugins/b");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"a;unpack=false", "b;unpack=false"});

		Attributes additionalAttributes = new Attributes();
		additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Export-Package"), "a");
		Utils.generateBundleManifest(a, "a", "1.0.0.qualifier", additionalAttributes);
		Utils.generatePluginBuildProperties(a, null);
		StringBuffer code = new StringBuffer();
		code.append("package a;                                   \n");
		code.append("public class A {                             \n");
		code.append("  public void f(Object o) {                  \n");
		code.append("    System.out.println(o.toString());        \n");
		code.append("  }                                          \n");
		code.append("}                                            \n");
		Utils.writeBuffer(a.getFile("src/a/A.java"), code);

		additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Import-Package"), "a");
		Utils.generateBundleManifest(b, "b", "1.0.0", additionalAttributes);
		Utils.generatePluginBuildProperties(b, null);
		code = new StringBuffer();
		code.append("import a.A;                                  \n");
		code.append("class B {                                    \n");
		code.append("  void f() {                                 \n");
		code.append("    A a = new A();                           \n");
		code.append("    a.f(\"foo\");                            \n");
		code.append("  }                                          \n");
		code.append("}                                            \n");
		Utils.writeBuffer(b.getFile("src/b.java"), code);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repo1Location);
		properties.put("p2.artifact.repo", repo1Location);
		properties.put("p2.publish.artifacts", "true");
		properties.put("forceContextQualifier", "v1");
		properties.put("baseLocation", "");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		//now change A and recompile
		code = new StringBuffer();
		code.append("package a;                                   \n");
		code.append("public class A {                             \n");
		code.append("  public void f(Object o) {                  \n");
		code.append("    System.out.println(o.toString());        \n");
		code.append("  }                                          \n");
		code.append("  public void f(String o) {                  \n");
		code.append("    System.out.println(o);                   \n");
		code.append("  }                                          \n");
		code.append("}                                            \n");
		Utils.writeBuffer(a.getFile("src/a/A.java"), code);

		properties.put("p2.metadata.repo", repo2Location);
		properties.put("p2.artifact.repo", repo2Location);
		properties.put("forceContextQualifier", "v2");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		assertResourceFile(buildFolder, "repo1/plugins/a_1.0.0.v1.jar");
		assertResourceFile(buildFolder, "repo1/plugins/b_1.0.0.jar");
		assertResourceFile(buildFolder, "repo2/plugins/a_1.0.0.v2.jar");
		assertResourceFile(buildFolder, "repo2/plugins/b_1.0.0.jar");

		StringBuffer test = new StringBuffer();
		test.append("<project default=\"mirror\">                                                                    \n");
		test.append("  <target name=\"mirror\">                                                                      \n");
		test.append("    <p2.artifact.mirror baseLine=\"${compareAgainst}\"                                          \n");
		test.append("                        source=\"${compareFrom}\"                                               \n");
		test.append("                        destination=\"${newLocation}\"                                          \n");
		test.append("                        comparatorId=\"org.eclipse.equinox.p2.repository.tools.jar.comparator\" \n");
		test.append("    />                                                                                          \n");
		test.append("  </target>                                                                                     \n");
		test.append("</project>                                                                                      \n");
		
		IFile testXML = buildFolder.getFile("test.xml");
		Utils.writeBuffer(testXML, test);

		properties = new Properties();
		properties.put("compareAgainst", repo1Location);
		properties.put("compareFrom", repo2Location);
		properties.put("newLocation", finalLocation);
		runAntScript(testXML.getLocation().toOSString(), new String[] {"mirror"}, buildFolder.getLocation().toOSString(), properties);
		
		assertLogContainsLine(buildFolder.getFile("log.log"), "Mirroring completed with warnings and/or errors.");
	}
}
