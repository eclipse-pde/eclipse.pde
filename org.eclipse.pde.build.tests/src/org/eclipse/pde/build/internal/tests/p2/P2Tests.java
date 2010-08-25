/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests.p2;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.zip.ZipOutputStream;
import junit.framework.AssertionFailedError;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.internal.build.site.QualifierReplacer;

public class P2Tests extends P2TestCase {

	public void testP2SimpleProduct() throws Exception {
		IFolder buildFolder = newTest("p2.SimpleProduct");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		String os = Platform.getOS();
		String ws = Platform.getWS();
		String arch = Platform.getOSArch();

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", "/test/test.product");
		properties.put("configs", os + ',' + ws + ',' + arch);
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

		String p2Config = ws + '.' + os + '.' + arch;
		String launcherConfig = os.equals("macosx") ? ws + '.' + os : p2Config;
		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		assertNotNull(repository);

		//some basic existance
		ArrayList ius = new ArrayList();
		ius.add(getIU(repository, "test"));
		ius.add(getIU(repository, "org.eclipse.equinox.launcher"));
		ius.add(getIU(repository, OSGI));
		ius.add(getIU(repository, CORE_RUNTIME));

		//check some start level info
		IInstallableUnit iu = getIU(repository, "tooling" + p2Config + CORE_RUNTIME);
		assertTouchpoint(iu, "configure", "markStarted(started: true);");
		ius.add(iu);

		iu = getIU(repository, "tooling" + p2Config + EQUINOX_COMMON);
		assertTouchpoint(iu, "configure", "setStartLevel(startLevel:2);markStarted(started: true);");
		ius.add(iu);

		//product settings
		getIU(repository, "toolingtest.product.ini." + p2Config);

		iu = getIU(repository, "toolingtest.product.config." + p2Config);
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.application,propValue:test.application);");
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.product,propValue:test.product);");
		assertProvides(iu, "toolingtest.product", "test.product.config");

		//some launcher stuff
		iu = getIU(repository, "toolingorg.eclipse.equinox.launcher");
		assertTouchpoint(iu, "configure", "addProgramArg(programArg:-startup);addProgramArg(programArg:@artifact);");
		ius.add(iu);
		iu = getIU(repository, "toolingorg.eclipse.equinox.launcher." + launcherConfig);
		assertTouchpoint(iu, "configure", "addProgramArg(programArg:--launcher.library);addProgramArg(programArg:@artifact);");
		ius.add(iu);

		iu = getIU(repository, "test.product.rootfiles." + p2Config);
		assertProvides(iu, "toolingtest.product", "test.product.rootfiles");
		//		assertRequires(iu, "org.eclipse.equinox.p2.iu", "org.eclipse.equinox.launcher." + launcherConfig);

		//And the main product IU
		iu = getIU(repository, "test.product");
		//		assertRequires(iu, "toolingtest.product", "test.product.launcher");
		//		assertRequires(iu, "toolingtest.product", "test.product.ini");
		//		assertRequires(iu, "toolingtest.product", "test.product.config");
		//		assertRequires(iu, ius, true);

		iu = getIU(repository, "toolingtest.product.rootfiles." + p2Config);
		assertTouchpoint(iu, "configure", "setLauncherName(name:test");

		IFolder installFolder = buildFolder.getFolder("install");
		properties.put("p2.director.installPath", installFolder.getLocation().toOSString());
		properties.put("p2.repo", "file:" + buildFolder.getFolder("repo").getLocation().toOSString());
		properties.put("p2.director.iu", "test.product");
		properties.put("os", os);
		properties.put("ws", ws);
		properties.put("arch", arch);
		properties.put("equinoxLauncherJar", FileLocator.getBundleFile(Platform.getBundle("org.eclipse.equinox.launcher")).getAbsolutePath());
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"runDirector"}, buildFolder.getLocation().toOSString(), properties);

		IFile iniFile = os.equals("macosx") ? installFolder.getFile("test.app/Contents/MacOS/test.ini") : installFolder.getFile("test.ini");
		assertLogContainsLine(iniFile, "-startup");
		assertLogContainsLine(iniFile, "--launcher.library");
		assertLogContainsLine(iniFile, "-foo");
	}

	public void testBug237096() throws Exception {
		IFolder buildFolder = newTest("237096");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		Utils.generateFeature(buildFolder, "F", null, new String[] {OSGI + ";unpack=false", CORE_RUNTIME + ";unpack=false"});
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

		IInstallableUnit iu = getIU(repository, "FRoot");
		IInstallableUnit rootIU = getIU(repository, "toolingFRoot.rootfiles");
		ArrayList ius = new ArrayList();
		ius.add(getIU(repository, OSGI));
		ius.add(getIU(repository, CORE_RUNTIME));
		ius.add(rootIU);
		assertRequires(iu, ius, true);

		ius.clear();
		ius.add(getIU(repository, "FRoot.rootfiles.ANY.ANY.ANY"));
		ius.add(getIU(repository, "toolingFRoot.rootfiles.ANY.ANY.ANY"));
		assertRequires(rootIU, ius, true);
	}

	public void testBug242346() throws Exception {
		IFolder buildFolder = newTest("242346");
		IFile productFile = buildFolder.getFile("rcp.product");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		Utils.generateProduct(productFile, "rcp.product", "1.0.0.qualifier", new String[] {OSGI, SIMPLE_CONFIGURATOR}, false);

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
		//		properties.put("p2.product.qualifier", "v1234"); //bug 246060 //commented out for bug 297064
		Utils.storeBuildProperties(buildFolder, properties);
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IMetadataRepository repository = loadMetadataRepository(repoLocation);
		assertNotNull(repository);

		IInstallableUnit iu = getIU(repository, "toolingrcp.product.config.win32.win32.x86");
		ArrayList requiredIUs = new ArrayList();
		IInstallableUnit rootFileCU = getIU(repository, "toolingrcp.product.rootfiles.win32.win32.x86");
		requiredIUs.add(rootFileCU);
		requiredIUs.add(getIU(repository, "rcp.product.rootfiles.win32.win32.x86"));

		assertTouchpoint(rootFileCU, "configure", "setLauncherName");
		iu = getIU(repository, "toolingrcp.product.rootfiles");
		assertRequires(iu, requiredIUs, true);
		requiredIUs.clear();
		requiredIUs.add(iu);

		iu = getIU(repository, "rcp.product");
		assertRequires(iu, requiredIUs, true);
		//check up to the date on the timestamp, don't worry about hours/mins
		assertTrue(PublisherHelper.toOSGiVersion(iu.getVersion()).getQualifier().startsWith(QualifierReplacer.getDateQualifier().substring(0, 8)));

		IFolder installFolder = buildFolder.getFolder("install");
		properties.put("p2.director.installPath", installFolder.getLocation().toOSString());
		properties.put("p2.repo", "file:" + buildFolder.getFolder("repo").getLocation().toOSString());
		properties.put("p2.director.iu", "rcp.product");
		properties.put("os", "win32");
		properties.put("ws", "win32");
		properties.put("arch", "x86");
		properties.put("equinoxLauncherJar", FileLocator.getBundleFile(Platform.getBundle("org.eclipse.equinox.launcher")).getAbsolutePath());
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"runDirector"}, buildFolder.getLocation().toOSString(), properties);

		assertResourceFile(installFolder, "eclipse.exe");
		assertLogContainsLine(installFolder.getFile("configuration/config.ini"), "org.eclipse.equinox.simpleconfigurator.configUrl=file\\:org.eclipse.equinox.simpleconfigurator");
	}

	public void testBug222962_305837() throws Exception {
		IFolder buildFolder = newTest("222962");
		IFolder repo = Utils.createFolder(buildFolder, "repo");

		Utils.generateFeature(buildFolder, "F", null, new String[] {OSGI + ";unpack=false", CORE_RUNTIME + ";unpack=false"});
		Properties featureProperties = new Properties();
		featureProperties.put("bin.includes", "feature.xml");
		Utils.storeProperties(buildFolder.getFile("features/F/build.properties"), featureProperties);

		StringBuffer site = new StringBuffer();
		site.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>               	\n");
		site.append("<site> 										          	\n");
		site.append("    <feature id=\"F\" version=\"0.0.0\" >					\n");
		site.append("       <category name=\"new_category_1\" />				\n");
		site.append("    </feature>												\n");
		site.append("    <category-def name=\"new_category_1\" label=\"Foo!\"/>	\n");
		site.append("</site>        											\n");
		Utils.writeBuffer(buildFolder.getFile("site.xml"), site);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.category.site", URIUtil.toUnencodedString(buildFolder.getFile("site.xml").getLocationURI()));
		properties.put("p2.category.version", "1.2.3.456");
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		properties.put("p2.compress", "true");

		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		assertResourceFile(buildFolder, "repo/content.jar");
		assertResourceFile(buildFolder, "repo/artifacts.jar");

		IMetadataRepository metadata = loadMetadataRepository(repo.getLocationURI());
		IQueryResult result = metadata.query(QueryUtil.createIUQuery("new_category_1"), null);
		IInstallableUnit iu = (IInstallableUnit) result.iterator().next();
		assertNotNull(iu);
		assertEquals(iu.getId(), "new_category_1");
		assertEquals(iu.getVersion().toString(), "1.2.3.456");
	}

	public void testBug237662() throws Exception {
		IFolder buildFolder = newTest("237662");
		IFolder repo = Utils.createFolder(buildFolder, "repo");
		IFile productFile = buildFolder.getFile("rcp.product");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {OSGI, CORE_RUNTIME, SIMPLE_CONFIGURATOR, EQUINOX_PREFERENCES}, false);

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

		IInstallableUnit iu = getIU(repository, "tooling" + p2Config + CORE_RUNTIME);
		assertTouchpoint(iu, "configure", "markStarted(started: true);");

		boolean fail = false;
		try {
			//bug 270524
			getIU(repository, "tooling" + p2Config + EQUINOX_PREFERENCES);
			fail = true;
		} catch (AssertionFailedError e) {
			//expected
		}
		if (fail)
			fail("Unexpected CU found");
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

		Utils.generateFeature(buildFolder, "F", null, new String[] {OSGI + ";unpack=false", CORE_RUNTIME + ";unpack=false"});
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
		Utils.generateProduct(productFile, "rcp.product", "1.0.0", new String[] {OSGI}, false);

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
		assertResourceFile(buildFolder, "outRepo/artifacts.jar");
		assertResourceFile(buildFolder, "outRepo/content.jar");

		//part 2, zipped repos
		IFolder zipped = Utils.createFolder(buildFolder, "zipped");
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(new File(zipped.getLocation().toFile(), "zipped repo.zip")));
		File root = buildFolder.getFolder("repo/r1").getLocation().toFile();
		FileUtils.zip(output, root, Collections.EMPTY_SET, FileUtils.createRootPathComputer(root));
		org.eclipse.pde.internal.build.Utils.close(output);

		//bug 318144
		Utils.writeBuffer(zipped.getFile(".repo/not.a.repo"), new StringBuffer("I am not a repo"));

		IFolder outRepo2 = Utils.createFolder(buildFolder, "outRepo2");
		properties.put("repoBaseLocation", zipped.getLocation().toOSString());
		properties.put("transformedRepoLocation", outRepo2.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"transformRepos"}, buildFolder.getLocation().toOSString(), properties);

		assertResourceFile(outRepo2, "plugins/b_1.0.0/B.class");
		assertResourceFile(outRepo2, "plugins/a_1.0.0.jar");
		assertResourceFile(outRepo2, "artifacts.jar");
		assertResourceFile(outRepo2, "content.jar");
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

		IMetadataRepository metadata = loadMetadataRepository(repoLocation);
		IInstallableUnit iu = getIU(metadata, "org.eclipse.cvs");
		assertNotNull(iu);

		//bug 289866
		StringBuffer buffer = new StringBuffer();
		buffer.append("pack.excludes=plugins/org.eclipse.cvs_" + iu.getVersion() + ".jar\n");
		Utils.writeBuffer(repo.getFile("pack.properties"), buffer);

		assertResourceFile(buildFolder, "repo/artifacts.xml");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build.tests"), new Path("/resources/keystore/keystore"), null);
		assertNotNull(resource);
		String keystorePath = FileLocator.toFileURL(resource).getPath();

		buffer = new StringBuffer();
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
		Map repoProps = repository.getProperties();
		assertEquals(repoProps.get("publishPackFilesAsSiblings"), "true");
		final String PACKED_FORMAT = "packed"; //$NON-NLS-1$
		IQueryResult keys = repository.query(ArtifactKeyQuery.ALL_KEYS, null);
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			IArtifactKey key = (IArtifactKey) iterator.next();
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);

			if (key.getClassifier().equals("osgi.bundle") && key.getId().equals("org.eclipse.cvs")) {
				assertEquals(descriptors.length, 1);
				continue;
			} else
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
		IFolder c = Utils.createFolder(buildFolder, "plugins/c");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"a;unpack=false", "b;unpack=false", "c;unpack=false"});
		Properties featureProperties = new Properties();
		featureProperties.put("bin.includes", "feature.xml");
		Utils.storeProperties(buildFolder.getFile("features/F/build.properties"), featureProperties);

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

		Utils.generateBundleManifest(c, "c", "1.0.0", null);
		Properties properties = new Properties();
		properties.put("bin.includes", "META-INF/, ., build.properties");
		Utils.generatePluginBuildProperties(c, properties);
		Utils.writeBuffer(c.getFile("src/a/A.java"), code);

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

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repo1Location);
		properties.put("p2.artifact.repo", repo1Location);
		properties.put("p2.publish.artifacts", "true");
		properties.put("forceContextQualifier", "v1");
		properties.put("baseLocation", "");
		properties.put("pluginPath", "${buildDirectory}"); //178449
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		//now change A and recompile
		Utils.generateFeature(buildFolder, "F", null, new String[] {"a;unpack=true", "b;optional=true", "c;unpack=false"});
		Utils.storeProperties(buildFolder.getFile("features/F/build.properties"), featureProperties);

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

		//bug 271034 same properties file, touched with new timestamp, perhaps new order
		properties = new Properties();
		properties.put("bin.includes", "META-INF/, ., build.properties");
		Utils.generatePluginBuildProperties(c, properties);

		runBuild(buildFolder);

		assertResourceFile(buildFolder, "repo1/plugins/a_1.0.0.v1.jar");
		assertResourceFile(buildFolder, "repo1/plugins/b_1.0.0.jar");
		assertResourceFile(buildFolder, "repo1/plugins/c_1.0.0.jar");
		assertResourceFile(buildFolder, "repo2/plugins/a_1.0.0.v2.jar");
		assertResourceFile(buildFolder, "repo2/plugins/b_1.0.0.jar");
		assertResourceFile(buildFolder, "repo2/plugins/c_1.0.0.jar");

		StringBuffer test = new StringBuffer();
		test.append("<project default=\"mirror\">                                                                    \n");
		test.append("  <target name=\"mirror\">                                                                      \n");
		test.append("    <p2.artifact.mirror baseLine=\"${compareAgainst}\"                                          \n");
		test.append("                        source=\"${compareFrom}\"                                               \n");
		test.append("                        destination=\"${newLocation}\"                                          \n");
		test.append("                        destinationName=\"testRepoName\"                                        \n");
		test.append("                        comparatorId=\"org.eclipse.equinox.p2.repository.tools.jar.comparator\" \n");
		test.append("                        comparatorLog=\"${basedir}/compare.log\"                                \n");
		test.append("                        ignoreErrors=\"true\"                                                   \n");
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

		IArtifactRepository artifact = loadArtifactRepository(finalLocation);
		assertEquals(artifact.getName(), "testRepoName"); //bug 274094
		assertLogContainsLine(buildFolder.getFile("log.log"), "Messages while mirroring artifact descriptors");
		assertLogContainsLines(buildFolder.getFile("compare.log"), new String[] {"canonical: org.eclipse.update.feature,F,1.0.0", "The entry \"Plugin: a 1.0.0.v2\" is not present in both features", "The entry \"Plugin: b 1.0.0\" has different unpack attribute values"});
		assertLogContainsLines(buildFolder.getFile("compare.log"), new String[] {"canonical: osgi.bundle,b,1.0.0", "The class B.class is different."});
		boolean failed = true;
		try {
			assertLogContainsLine(buildFolder.getFile("compare.log"), "build.properties");
			failed = false;
		} catch (AssertionFailedError e) {
			//expected
		}
		assertTrue(failed);
	}

	public void testBug263272_2() throws Exception {
		IFolder buildFolder = newTest("263272_2");

		IFolder repo1 = Utils.createFolder(buildFolder, "repo1");
		String repo1Location = "file:" + repo1.getLocation().toOSString();
		IFolder repo2 = Utils.createFolder(buildFolder, "repo2");
		String repo2Location = "file:" + repo2.getLocation().toOSString();
		IFolder finalRepo = Utils.createFolder(buildFolder, "final");
		String finalLocation = "file:" + finalRepo.getLocation().toOSString();

		IFolder a = Utils.createFolder(buildFolder, "plugins/a");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"a;unpack=false"});

		Attributes additionalAttributes = new Attributes();
		additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Export-Package"), "a");
		Utils.generateBundleManifest(a, "a", "1.0.0", additionalAttributes);
		Utils.generatePluginBuildProperties(a, null);
		StringBuffer code = new StringBuffer();
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

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repo1Location);
		properties.put("p2.artifact.repo", repo1Location);
		properties.put("p2.publish.artifacts", "true");
		properties.put("baseLocation", "");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		//now change A and recompile
		code = new StringBuffer();
		code.append("package a;                                   \n");
		code.append("public class A {                             \n");
		code.append("  public void f(String o) {                  \n");
		code.append("    System.out.println(o);                   \n");
		code.append("  }                                          \n");
		code.append("  public void f(Object o) {                  \n");
		code.append("    System.out.println(o.toString());        \n");
		code.append("  }                                          \n");
		code.append("}                                            \n");
		Utils.writeBuffer(a.getFile("src/a/A.java"), code);

		properties.put("p2.metadata.repo", repo2Location);
		properties.put("p2.artifact.repo", repo2Location);
		properties.put("forceContextQualifier", "v2");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		assertResourceFile(buildFolder, "repo1/plugins/a_1.0.0.jar");
		assertResourceFile(buildFolder, "repo2/plugins/a_1.0.0.jar");

		StringBuffer test = new StringBuffer();
		test.append("<project default=\"mirror\">                                                                    \n");
		test.append("  <target name=\"mirror\">                                                                      \n");
		test.append("    <p2.artifact.mirror baseLine=\"${compareAgainst}\"                                          \n");
		test.append("                        source=\"${compareFrom}\"                                               \n");
		test.append("                        destination=\"${newLocation}\"                                          \n");
		test.append("                        comparatorId=\"org.eclipse.equinox.p2.repository.tools.jar.comparator\" \n");
		test.append("                        comparatorLog=\"${basedir}/compare.log\"                                \n");
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

		boolean warnings = false;
		try {
			assertLogContainsLine(buildFolder.getFile("log.log"), "Mirroring completed with warnings and/or errors.");
			warnings = true;
		} catch (AssertionFailedError e) {
			//expected
		}
		assertFalse(warnings);
	}

	public void testBug267461() throws Exception {
		IFolder buildFolder = newTest("267461");
		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile productFile = buildFolder.getFile("rcp.product");
		IFolder repo = Utils.createFolder(buildFolder, "repo");
		Utils.generateProduct(productFile, "uid.product", "rcp.product", "1.0.0", "my.app", null, new String[] {OSGI, SIMPLE_CONFIGURATOR}, false, null);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		String repoLocation = "file:" + repo.getLocation().toOSString();
		properties.put("configs", "win32,win32,x86");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("product", productFile.getLocation().toOSString());
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "tooling");
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IMetadataRepository metadata = loadMetadataRepository("file:" + repo.getLocation().toOSString());
		IInstallableUnit iu = getIU(metadata, "uid.product");

		iu = getIU(metadata, "toolinguid.product.config.win32.win32.x86");
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.application,propValue:my.app);");
		assertTouchpoint(iu, "configure", "setProgramProperty(propName:eclipse.product,propValue:rcp.product);");
	}

	public void testBug304736() throws Exception {
		IFolder buildFolder = newTest("304736");

		StringBuffer customBuffer = new StringBuffer();
		customBuffer.append("<project name=\"custom\" default=\"noDefault\">										\n");
		customBuffer.append("   <import file=\"${eclipse.pdebuild.templates}/headless-build/allElements.xml\"/>	\n");
		customBuffer.append("   <target name=\"allElementsDelegator\">												\n");
		customBuffer.append("      <ant antfile=\"${genericTargets}\" target=\"${target}\">							\n");
		customBuffer.append("         <property name=\"type\" value=\"feature\" />									\n");
		customBuffer.append("         <property name=\"id\" value=\"F1\" />											\n");
		customBuffer.append("      </ant>																			\n");
		customBuffer.append("      <ant antfile=\"${genericTargets}\" target=\"${target}\">							\n");
		customBuffer.append("         <property name=\"type\" value=\"feature\" />									\n");
		customBuffer.append("         <property name=\"id\" value=\"F2\" />											\n");
		customBuffer.append("      </ant>																			\n");
		customBuffer.append("   </target>																			\n");
		customBuffer.append("</project>																				\n");
		Utils.writeBuffer(buildFolder.getFile("allElements.xml"), customBuffer);

		Utils.generateFeature(buildFolder, "F1", null, new String[] {"A"});
		Utils.generateFeature(buildFolder, "F2", null, new String[] {"B"});

		IFolder A = Utils.createFolder(buildFolder, "plugins/a");
		IFolder B = Utils.createFolder(buildFolder, "plugins/b");
		Utils.generateBundle(A, "A");
		Utils.generateBundle(B, "B");

		IFolder repo = Utils.createFolder(buildFolder, "repo");
		String repoLocation = "file:" + repo.getLocation().toOSString();
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.flavor", "yummy");
		properties.put("p2.publish.artifacts", "false");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
		//test passes if there was no build failure
	}

	public void testBug271373() throws Exception {
		IFolder buildFolder = newTest("271373_generator");

		Utils.generateFeature(buildFolder, "F", null, new String[] {"A;os=win32,linux;unpack=false"});
		Utils.writeBuffer(buildFolder.getFile("features/F/build.properties"), new StringBuffer("bin.includes=feature.xml\n"));

		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Eclipse-PlatformFilter"), "(| (osgi.os=win32) (osgi.os=linux))");
		Utils.generateBundleManifest(A, "A", "1.0.0", manifestAdditions);
		Utils.generatePluginBuildProperties(A, null);
		Utils.writeBuffer(A.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));

		IFile product = buildFolder.getFile("foo.product");
		Utils.generateProduct(product, "foo.product", "1.0.0", new String[] {"F"}, true);

		IFolder repo = Utils.createFolder(buildFolder, "repo");
		URI repoLocation = repo.getLocationURI();

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("product", product.getLocation().toOSString());
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", URIUtil.toUnencodedString(repoLocation));
		properties.put("p2.artifact.repo", URIUtil.toUnencodedString(repoLocation));
		properties.put("p2.flavor", "gingerbread");
		properties.put("configs", "win32,win32,x86");
		properties.put("baseLocation", "");
		properties.put("includeLaunchers", "false");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runProductBuild(buildFolder);

		IFolder installFolder = buildFolder.getFolder("install");
		properties.put("p2.director.installPath", installFolder.getLocation().toOSString());
		properties.put("p2.repo", URIUtil.toUnencodedString(repoLocation));
		properties.put("p2.director.iu", "foo.product");
		properties.put("os", "win32");
		properties.put("ws", "win32");
		properties.put("arch", "x86");
		properties.put("equinoxLauncherJar", FileLocator.getBundleFile(Platform.getBundle("org.eclipse.equinox.launcher")).getAbsolutePath());
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"runDirector"}, buildFolder.getLocation().toOSString(), properties);

		assertResourceFile(installFolder, "plugins/A_1.0.0.jar");
	}

	public void testMetadataGenerator_BootStrap() throws Exception {
		IFolder testFolder = newTest("metadataGenerator_Bootstrap");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		IFile productFile = testFolder.getFile("Bootstrap.product");

		//Step one, build old fashioned product
		IFolder buildFolder = Utils.createFolder(testFolder, "build");
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("configs", "win32,win32,x86");
		properties.put("archivesFormat", "win32,win32,x86-folder");
		if (!delta.equals(new File((String) properties.get("baseLocation"))))
			properties.put("pluginPath", delta.getAbsolutePath());
		properties.put("product", productFile.getLocation().toOSString());
		Utils.storeBuildProperties(buildFolder, properties);

		runProductBuild(buildFolder);

		IFolder repoFolder = testFolder.getFolder("repository");
		IFolder installFolder = testFolder.getFolder("install");

		//step two, invoke the metadata generator on the product
		StringBuffer scriptBuffer = new StringBuffer();
		scriptBuffer.append("<project name=\"project\" default=\"go\">																\n");
		scriptBuffer.append("   <target name=\"go\">																				\n");
		scriptBuffer.append("		<last id=\"launcher\">																			\n");
		scriptBuffer.append("			<sort>																						\n");
		scriptBuffer.append("				<fileset dir=\"${eclipse.home}/plugins\" includes=\"org.eclipse.equinox.launcher_*\" />	\n");
		scriptBuffer.append("			</sort>																						\n");
		scriptBuffer.append("		</last>																							\n");
		scriptBuffer.append("      <property name=\"launcher\" refid=\"launcher\" />												\n");
		scriptBuffer.append("      <condition property=\"p2.director.devMode\" value=\"-dev &quot;${osgi.dev}&quot;\" else=\"\">	\n");
		scriptBuffer.append("         <isset property=\"osgi.dev\" />																\n");
		scriptBuffer.append("      </condition>																						\n");
		scriptBuffer.append("      <java dir=\"${basedir}\" jar=\"${launcher}\" fork=\"true\">										\n");
		scriptBuffer.append("         <arg line=\"-application org.eclipse.equinox.p2.publisher.EclipseGenerator\" />				\n");
		scriptBuffer.append("         <arg line=\"${p2.director.devMode}\" />														\n");
		scriptBuffer.append("         <sysproperty key=\"osgi.configuration.area\" value=\"${osgi.configuration.area}\" />			\n");
		scriptBuffer.append("         <arg line=\"-metadataRepositoryName BootStrapRepo\" />										\n");
		scriptBuffer.append("         <arg value=\"-metadataRepository\" />															\n");
		scriptBuffer.append("         <arg value=\"" + URIUtil.toUnencodedString(repoFolder.getLocationURI()) + "\" />				\n");
		scriptBuffer.append("         <arg value=\"-artifactRepository\" />															\n");
		scriptBuffer.append("         <arg value=\"" + URIUtil.toUnencodedString(repoFolder.getLocationURI()) + "\" />				\n");
		scriptBuffer.append("         <arg value=\"-source\" />																		\n");
		scriptBuffer.append("         <arg value=\"" + buildFolder.getFolder("tmp/eclipse").getLocation().toOSString() + "\" />		\n");
		scriptBuffer.append("         <arg line=\"-root bootstrap -rootVersion 1.2.0.12345\" />										\n");
		scriptBuffer.append("         <arg line=\"-flavor tooling -publishArtifacts -append\" />									\n");
		//scriptBuffer.append(" <jvmarg line=\"-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000\" /> 	\n");
		scriptBuffer.append("      </java>																							\n");

		//step three call the director
		scriptBuffer.append("      <java dir=\"${basedir}\" jar=\"${launcher}\" fork=\"true\">				\n");
		scriptBuffer.append("         <arg line=\"${p2.director.devMode}\" />														\n");
		scriptBuffer.append("         <sysproperty key=\"osgi.configuration.area\" value=\"${osgi.configuration.area}\" />			\n");
		scriptBuffer.append("         <arg line=\"-application org.eclipse.equinox.p2.director\" />									\n");
		scriptBuffer.append("         <arg value=\"-metadataRepository\" />															\n");
		scriptBuffer.append("         <arg value=\"" + URIUtil.toUnencodedString(repoFolder.getLocationURI()) + "\" />				\n");
		scriptBuffer.append("         <arg value=\"-artifactRepository\" />															\n");
		scriptBuffer.append("         <arg value=\"" + URIUtil.toUnencodedString(repoFolder.getLocationURI()) + "\" />				\n");
		scriptBuffer.append("         <arg line=\"-installIU bootstrap/1.2.0.12345 -profile bootProfile\" />						\n");
		scriptBuffer.append("         <arg line=\"-profileProperties org.eclipse.update.install.features=true\" />					\n");
		scriptBuffer.append("         <arg line=\"-p2.os win32 -p2.ws win32 -p2.arch x86\" />										\n");
		scriptBuffer.append("         <arg value=\"-destination\" />																\n");
		scriptBuffer.append("         <arg value=\"" + installFolder.getLocation().toOSString() + "\" />							\n");
		//scriptBuffer.append(" <jvmarg value=\"-Declipse.p2.data.area=" + installFolder.getLocation().toOSString() + "/p2\" />		\n");
		//scriptBuffer.append(" <jvmarg line=\"-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000\" /> 	\n");
		scriptBuffer.append("      </java>																							\n");
		scriptBuffer.append("   </target>																							\n");
		scriptBuffer.append("</project>																								\n");
		IFile script = buildFolder.getFile("build.xml");
		Utils.writeBuffer(script, scriptBuffer);

		runAntScript(script.getLocation().toOSString(), new String[] {"go"}, testFolder.getLocation().toOSString(), null);

		IMetadataRepository metaRepo = loadMetadataRepository(repoFolder.getLocationURI());
		assertEquals(metaRepo.getName(), "BootStrapRepo");
		IInstallableUnit iu = getIU(metaRepo, "bootstrap");
		assertEquals(iu.getVersion().toString(), "1.2.0.12345");
		assertResourceFile(installFolder, "eclipse.exe");
	}
}
