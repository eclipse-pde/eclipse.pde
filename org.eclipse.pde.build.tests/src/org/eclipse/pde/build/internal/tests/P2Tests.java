package org.eclipse.pde.build.internal.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.pde.build.internal.tests.p2.P2TestCase;
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
		String launcherConfig = Platform.getOS().equals("macosx") ? Platform.getWS() + '.' + Platform.getOS()  : p2Config;
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

}
