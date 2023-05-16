/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 * 		IBM Corporation - initial API and implementation
 * 		Pascal Rapicault - Support for bundled macosx application - http://bugs.eclipse.org/431116
 ******************************************************************************/

package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.engine.Phase;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.p2.engine.phases.Install;
import org.eclipse.equinox.internal.p2.metadata.TouchpointData;
import org.eclipse.equinox.internal.p2.metadata.TouchpointInstruction;
import org.eclipse.equinox.internal.p2.publisher.eclipse.BrandingIron;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;
import org.eclipse.equinox.p2.internal.repository.tools.tasks.IUDescription;
import org.eclipse.equinox.p2.internal.repository.tools.tasks.Repo2RunnableTask;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.tasks.Config;
import org.eclipse.pde.internal.build.tasks.TaskMessages;

public class BrandP2Task extends Repo2RunnableTask {
	private Config config = null;
	private String launcherName = null;
	private String tempFolder = null;
	private String iconsList = null;
	private String launcherProvider = null;
	private String productId = null;
	private String productVersion = Version.emptyVersion.toString();
	private URI metadataURI = null;
	private URI artifactURI = null;
	private boolean removeMetadataRepo = true;
	private boolean removeArtifactRepo = true;
	private List<IInstallableUnit> ius = null;

	public BrandP2Task() {
		application = new Repo2Runnable() {
			@Override
			protected PhaseSet getPhaseSet() {
				return new PhaseSet(new Phase[] {new Collect(100), new Install(100)});
			}

			@Override
			protected PhaseSet getNativePhase() {
				return null;
			}
		};
	}

	@Override
	public void execute() {
		if (launcherName == null || launcherName.startsWith(ANT_PREFIX) || config == null)
			return; //TODO error/warning

		if (launcherProvider == null || launcherProvider.startsWith("${")) //$NON-NLS-1$
			launcherProvider = IPDEBuildConstants.FEATURE_EQUINOX_EXECUTABLE;

		IProvisioningAgent agent = BundleHelper.getDefault().acquireService(IProvisioningAgent.class);
		if (agent == null)
			throw new BuildException(TaskMessages.error_agentService);
		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (metadataManager == null)
			throw new BuildException(TaskMessages.error_metadataRepoManagerService);
		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (artifactManager == null)
			throw new BuildException(TaskMessages.error_artifactRepoManagerService);

		IMetadataRepository metadataRepo = loadMetadataRepository(metadataManager);
		IArtifactRepository artifactRepo = loadArtifactRepository(artifactManager);

		try {
			super.setDestination(getRootFolder());

			super.execute();
			if (ius.size() == 1) {
				callBrandingIron();
				publishBrandedIU(metadataRepo, artifactRepo, ius.get(0));
				FileUtils.deleteAll(new File(getRootFolder()));
			}
		} catch (BuildException e) {
			getProject().log(e.getMessage(), Project.MSG_WARN);
		} finally {
			cleanupRepositories(metadataManager, artifactManager);
			ius = null;
		}
	}

	private void cleanupRepositories(IMetadataRepositoryManager metadataManager, IArtifactRepositoryManager artifactManager) {
		URI destination = IPath.fromOSString(getRootFolder()).toFile().toURI();

		if (metadataManager != null) {
			if (removeMetadataRepo)
				metadataManager.removeRepository(metadataURI);
			metadataManager.removeRepository(destination);
		}

		if (artifactManager != null) {
			if (removeArtifactRepo)
				artifactManager.removeRepository(artifactURI);
			artifactManager.removeRepository(destination);
		}
	}

	private IArtifactRepository loadArtifactRepository(IArtifactRepositoryManager manager) throws BuildException {
		if (artifactURI == null)
			throw new BuildException(TaskMessages.error_noArtifactRepo);

		removeArtifactRepo = !manager.contains(artifactURI);

		IArtifactRepository repo = null;
		try {
			repo = manager.loadRepository(artifactURI, null);
		} catch (ProvisionException e) {
			throw new BuildException(NLS.bind(TaskMessages.error_loadRepository, artifactURI.toString()));
		}

		if (!repo.isModifiable())
			throw new BuildException(NLS.bind(TaskMessages.error_unmodifiableRepository, artifactURI.toString()));

		return repo;
	}

	private IMetadataRepository loadMetadataRepository(IMetadataRepositoryManager manager) throws BuildException {
		if (metadataURI == null)
			throw new BuildException(TaskMessages.error_noMetadataRepo);

		removeMetadataRepo = !manager.contains(metadataURI);

		IMetadataRepository repo = null;
		try {
			repo = manager.loadRepository(metadataURI, null);
		} catch (ProvisionException e) {
			throw new BuildException(NLS.bind(TaskMessages.error_loadRepository, metadataURI.toString()));
		}

		if (!repo.isModifiable())
			throw new BuildException(NLS.bind(TaskMessages.error_unmodifiableRepository, metadataURI.toString()));
		return repo;
	}

	protected String getProviderIUName() {
		return launcherProvider + "_root." + getConfigString(); //$NON-NLS-1$
	}

	@Override
	protected List<IInstallableUnit> prepareIUs() {
		String iuName = getProviderIUName();
		IUDescription task = (IUDescription) super.createIu();
		task.setId(iuName);

		ius = super.prepareIUs();

		return ius;
	}

	protected void callBrandingIron() {
		if (!new File(getRootFolder()).exists())
			return;

		BrandingIron iron = new BrandingIron();
		iron.setName(launcherName);
		iron.setIcons(iconsList);
		iron.setOS(config.getOs());
		try {
			iron.brand(ExecutablesDescriptor.createDescriptor(config.getOs(), launcherName, new File(getRootFolder())));
		} catch (Exception e) {
			getProject().log(TaskMessages.error_branding, e, Project.MSG_WARN);
		}
	}

	protected String getRootFolder() {
		return tempFolder + "/p2.branding/" + getProviderIUName(); //$NON-NLS-1$
	}

	private String getConfigString() {
		return config.getWs() + '.' + config.getOs() + '.' + config.getArch();
	}

	protected void publishBrandedIU(IMetadataRepository metadataRepo, IArtifactRepository artifactRepo, IInstallableUnit originalIU) {
		String id = productId + "_root." + getConfigString(); //$NON-NLS-1$
		Version version = Version.parseVersion(productVersion);
		if (version.equals(Version.emptyVersion))
			version = originalIU.getVersion();
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription newIUDescription = new MetadataFactory.InstallableUnitDescription();
		newIUDescription.setSingleton(originalIU.isSingleton());
		newIUDescription.setId(id);
		newIUDescription.setVersion(version);
		newIUDescription.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, id, version)});
		newIUDescription.setTouchpointType(originalIU.getTouchpointType());
		newIUDescription.setFilter(originalIU.getFilter());

		List<ITouchpointData> data = brandTouchpointData(originalIU.getTouchpointData());
		for (ITouchpointData element : data) {
			newIUDescription.addTouchpointData(element);
		}

		IArtifactKey key = artifactRepo.createArtifactKey("binary", newIUDescription.getId(), newIUDescription.getVersion()); //$NON-NLS-1$
		newIUDescription.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit newIU = MetadataFactory.createInstallableUnit(newIUDescription);
		metadataRepo.addInstallableUnits(Arrays.asList(new IInstallableUnit[] {newIU}));

		publishBrandedArtifact(artifactRepo, key);
	}

	protected String createLDAPString() {
		String filter = "(& "; //$NON-NLS-1$
		filter += "(osgi.ws=" + config.getWs() + ')'; //$NON-NLS-1$
		filter += "(osgi.os=" + config.getOs() + ')'; //$NON-NLS-1$
		filter += "(osgi.arch=" + config.getArch() + ')'; //$NON-NLS-1$
		filter += ')';
		return filter;
	}

	private void publishBrandedArtifact(IArtifactRepository artifactRepo, IArtifactKey key) {
		ArtifactDescriptor descriptor = new ArtifactDescriptor(key);
		ZipOutputStream output = null;
		try {
			output = new ZipOutputStream(artifactRepo.getOutputStream(descriptor));
			File root = new File(getRootFolder());
			new File(root, "content.xml").delete(); //$NON-NLS-1$
			new File(root, "artifacts.xml").delete(); //$NON-NLS-1$
			new File(root, "content.jar").delete(); //$NON-NLS-1$
			new File(root, "artifacts.jar").delete(); //$NON-NLS-1$
			FileUtils.zip(output, root, Collections.<File> emptySet(), FileUtils.createRootPathComputer(root));
		} catch (ProvisionException | IOException e) {
			throw new BuildException(e.getMessage(), e);
		} finally {
			Utils.close(output);
		}
	}

	private static final String CHMOD = "chmod"; //$NON-NLS-1$
	private static final String TARGET_FILE = "targetFile"; //$NON-NLS-1$
	private static final String INSTALL = "install"; //$NON-NLS-1$
	private static final String UNINSTALL = "uninstall"; //$NON-NLS-1$
	private static final String CONFIGURE = "configure"; //$NON-NLS-1$

	private List<ITouchpointData> brandTouchpointData(Collection<ITouchpointData> data) {
		if (config.getOs().equals("macosx")) //$NON-NLS-1$
			return brandMacTouchpointData();
		ArrayList<ITouchpointData> results = new ArrayList<>(data.size() + 1);
		results.addAll(data);

		boolean haveChmod = false;

		String brandedLauncher = null;
		if (config.getOs().equals("win32")) //$NON-NLS-1$
			brandedLauncher = launcherName + ".exe"; //$NON-NLS-1$
		else
			brandedLauncher = launcherName;

		for (int i = 0; i < results.size(); i++) {
			ITouchpointData td = results.get(i);
			Map<String, ITouchpointInstruction> instructions = new HashMap<>(td.getInstructions());

			String[] phases = new String[] {INSTALL, CONFIGURE};
			for (String element : phases) {
				ITouchpointInstruction instruction = td.getInstruction(element);
				if (instruction == null)
					continue;

				boolean phaseChanged = false;
				String[] actions = Utils.getArrayFromString(instruction.getBody(), ";"); //$NON-NLS-1$
				for (int j = 0; j < actions.length; j++) {
					if (actions[j].startsWith(CHMOD)) {
						Map<String, String> map = parseAction(actions[j]);
						String targetFile = map.get(TARGET_FILE);
						targetFile = targetFile.replace('\\', '/');
						if (targetFile.equals(brandedLauncher)) {
							haveChmod = true;
							continue; //data has properly branded chmod, nothing to do
						}

						if ((config.getOs().equals("win32") && (targetFile.equals("launcher.exe") || targetFile.equals("eclipse.exe"))) || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								(targetFile.equals("launcher") || targetFile.equals("eclipse"))) //$NON-NLS-1$ //$NON-NLS-2$
						{
							map.put(TARGET_FILE, brandedLauncher);
							actions[j] = CHMOD + toString(map);
							haveChmod = true;
							phaseChanged = true;
							break;
						}
					}
				}
				if (phaseChanged) {
					TouchpointInstruction newInstruction = new TouchpointInstruction(toString(actions, ";"), instruction.getImportAttribute()); //$NON-NLS-1$
					instructions.put(element, newInstruction);
				}
			}

			results.set(i, new TouchpointData(instructions));
		}

		//add a chmod if there wasn't one before
		if (!haveChmod && !config.getOs().equals("win32")) { //$NON-NLS-1$
			String body = "chmod(targetDir:${installFolder}, targetFile:" + brandedLauncher + ", permissions:755)"; //$NON-NLS-1$ //$NON-NLS-2$
			TouchpointInstruction newInstruction = new TouchpointInstruction(body, null);
			Map<String, ITouchpointInstruction> instructions = new HashMap<>();
			instructions.put(INSTALL, newInstruction);
			results.add(new TouchpointData(instructions));
		}
		return results;
	}

	private List<ITouchpointData> brandMacTouchpointData() {
		Map<String, ITouchpointInstruction> instructions = new HashMap<>(3);
		instructions.put(INSTALL, getMacInstallInstruction());
		instructions.put(UNINSTALL, getMacUninstallInstruction());
		List<ITouchpointData> result = new ArrayList<>(2);
		result.add(new TouchpointData(instructions));
		return result;
	}

	private ITouchpointInstruction getMacUninstallInstruction() {
		return new TouchpointInstruction("cleanupzip(source:@artifact, target:${installFolder}/../);", null); //$NON-NLS-1$
	}

	private ITouchpointInstruction getMacInstallInstruction() {
		String body = "unzip(source:@artifact, target:${installFolder}/../);"; //$NON-NLS-1$
		body += " chmod(targetDir:${installFolder}/../MacOS/, targetFile:" + launcherName + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
		return new TouchpointInstruction(body, null);
	}

	private String toString(String[] elements, String separator) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			buffer.append(elements[i]);
			if (i < elements.length - 1)
				buffer.append(separator);
		}
		return buffer.toString();
	}

	private String toString(Map<String, String> map) {
		StringBuffer buffer = new StringBuffer();
		buffer.append('(');
		for (Iterator<String> iterator = map.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			buffer.append(key);
			buffer.append(':');
			buffer.append(map.get(key));
			if (iterator.hasNext())
				buffer.append(',');
		}
		buffer.append(')');
		return buffer.toString();
	}

	private Map<String, String> parseAction(String action) {
		Map<String, String> result = new HashMap<>();
		int open = action.indexOf('(');
		int close = action.lastIndexOf(')');
		String parameterString = action.substring(open + 1, close);
		String[] parameters = Utils.getArrayFromString(parameterString, ","); //$NON-NLS-1$
		for (String parameter : parameters) {
			int colon = parameter.indexOf(':');
			result.put(parameter.substring(0, colon).trim(), parameter.substring(colon + 1).trim());
		}
		return result;
	}

	public void setConfig(String config) {
		if (config == null || config.startsWith(ANT_PREFIX))
			return;

		String[] elements = Utils.getArrayFromStringWithBlank(config, "."); //$NON-NLS-1$
		if (elements.length != 3)
			throw new BuildException(NLS.bind(TaskMessages.error_invalidConfig, config));

		this.config = new Config(elements);
	}

	public void setLauncherName(String launcherName) {
		this.launcherName = launcherName;
	}

	public void setLauncherProvider(String launcherProvider) {
		this.launcherProvider = launcherProvider;
	}

	public void setIconsList(String iconsList) {
		this.iconsList = iconsList;
	}

	public void setTempDirectory(String temp) {
		this.tempFolder = temp;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public void setProductVersion(String productVersion) {
		if (productVersion != null && !productVersion.startsWith(ANT_PREFIX))
			this.productVersion = productVersion;
	}

	public void setRepository(String location) {
		setMetadataRepository(location);
		setArtifactRepository(location);
	}

	public void setMetadataRepository(String location) {
		try {
			this.metadataURI = URIUtil.fromString(location);
			super.addMetadataSourceRepository(metadataURI, false);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Metadata repository location (" + location + ") must be a URI."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	public void setArtifactRepository(String location) {
		try {
			this.artifactURI = URIUtil.fromString(location);
			super.addArtifactSourceRepository(artifactURI, false);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Artifact repository location (" + location + ") must be a URI."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}
}
