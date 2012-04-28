/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * 		IBM Corporation - initial API and implementation
 * 		Pascal Rapicault - Support for bundled macosx application - http://bugs.eclipse.org/57349
 ******************************************************************************/

package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.zip.ZipOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.engine.Phase;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.p2.engine.phases.Install;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.BrandingIron;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;
import org.eclipse.equinox.p2.internal.repository.tools.tasks.IUDescription;
import org.eclipse.equinox.p2.internal.repository.tools.tasks.Repo2RunnableTask;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
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
	private List ius = null;

	public BrandP2Task() {
		application = new Repo2Runnable() {
			protected PhaseSet getPhaseSet() {
				return new PhaseSet(new Phase[] {new Collect(100), new Install(100)}) { /* nothing to override */};
			}

			protected PhaseSet getNativePhase() {
				return null;
			}
		};
	}

	public void execute() {
		if (launcherName == null || launcherName.startsWith(ANT_PREFIX) || config == null)
			return; //TODO error/warning

		if (launcherProvider == null || launcherProvider.startsWith("${")) //$NON-NLS-1$
			launcherProvider = IPDEBuildConstants.FEATURE_EQUINOX_EXECUTABLE;

		IProvisioningAgent agent = (IProvisioningAgent) BundleHelper.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
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
				publishBrandedIU(metadataRepo, artifactRepo, (IInstallableUnit) ius.get(0));
				if ("macosx".equals(config.getOs())) {
					publishBundledMacOS(metadataRepo, artifactRepo, (IInstallableUnit) ius.get(0));
				}
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
		URI destination = new Path(getRootFolder()).toFile().toURI();

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

	protected List prepareIUs() {
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
		InstallableUnitDescription newIUDescription = new MetadataFactory.InstallableUnitDescription();
		newIUDescription.setSingleton(originalIU.isSingleton());
		newIUDescription.setId(id);
		newIUDescription.setVersion(version);
		newIUDescription.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version)});
		newIUDescription.setTouchpointType(originalIU.getTouchpointType());

		//Tweak the filter to take macosx case into account
		if ("macosx".equals(config.getOs())) { //$NON-NLS-1$
			StringBuffer newLDAPFilter = new StringBuffer();
			newLDAPFilter.append("(& (!(macosx-bundled=*))"); //$NON-NLS-1$
			newLDAPFilter.append(createLDAPString());
			newLDAPFilter.append(")"); //$NON-NLS-1$
			newIUDescription.setFilter(InstallableUnit.parseFilter(newLDAPFilter.toString()));
		} else {
			newIUDescription.setFilter(originalIU.getFilter());
		}

		List data = brandTouchpointData(originalIU.getTouchpointData(), false);
		for (int i = 0; i < data.size(); i++) {
			newIUDescription.addTouchpointData((ITouchpointData) data.get(i));
		}

		IArtifactKey key = artifactRepo.createArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, newIUDescription.getId(), newIUDescription.getVersion());
		newIUDescription.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit newIU = MetadataFactory.createInstallableUnit(newIUDescription);
		metadataRepo.addInstallableUnits(Arrays.asList(new IInstallableUnit[] {newIU}));

		publishBrandedArtifact(artifactRepo, key);
	}

	public void publishBundledMacOS(IMetadataRepository metadataRepo, IArtifactRepository artifactRepo, IInstallableUnit originalIU) {
		String nonBrandedId = productId + "_root." + getConfigString(); //$NON-NLS-1$
		String id = productId + "_root." + getConfigString() + "-bundled"; //$NON-NLS-1$ //$NON-NLS-2$
		Version version = Version.parseVersion(productVersion);
		if (version.equals(Version.emptyVersion))
			version = originalIU.getVersion();
		InstallableUnitDescription newIUDescription = new MetadataFactory.InstallableUnitDescription();
		newIUDescription.setSingleton(originalIU.isSingleton());
		newIUDescription.setId(id);
		newIUDescription.setVersion(version);
		newIUDescription.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version), PublisherHelper.createSelfCapability(nonBrandedId, version)});
		newIUDescription.setTouchpointType(originalIU.getTouchpointType());

		//Tweak the filter for macosx-bundled
		StringBuffer newLDAPFilter = new StringBuffer();
		newLDAPFilter.append("(& (macosx-bundled=true)"); //$NON-NLS-1$
		newLDAPFilter.append(createLDAPString());
		newLDAPFilter.append(")"); //$NON-NLS-1$
		newIUDescription.setFilter(InstallableUnit.parseFilter(newLDAPFilter.toString()));

		List data = brandTouchpointData(originalIU.getTouchpointData(), true);
		for (int i = 0; i < data.size(); i++) {
			newIUDescription.addTouchpointData((ITouchpointData) data.get(i));
		}

		//The same artifact is used for the two shapes of MacOS
		IArtifactKey key = artifactRepo.createArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, nonBrandedId, newIUDescription.getVersion());
		newIUDescription.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit newIU = MetadataFactory.createInstallableUnit(newIUDescription);
		metadataRepo.addInstallableUnits(Arrays.asList(new IInstallableUnit[] {newIU}));
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
			FileUtils.zip(output, root, Collections.EMPTY_SET, FileUtils.createRootPathComputer(root));
		} catch (ProvisionException e) {
			throw new BuildException(e.getMessage(), e);
		} catch (IOException e) {
			throw new BuildException(e.getMessage(), e);
		} finally {
			Utils.close(output);
		}
	}

	private static final String CHMOD = "chmod"; //$NON-NLS-1$
	private static final String LN = "ln"; //$NON-NLS-1$
	private static final String LINK_TARGET = "linkTarget"; //$NON-NLS-1$
	private static final String LINK_NAME = "linkName"; //$NON-NLS-1$
	private static final String TARGET_FILE = "targetFile"; //$NON-NLS-1$
	private static final String INSTALL = "install"; //$NON-NLS-1$
	private static final String CONFIGURE = "configure"; //$NON-NLS-1$

	private List/*<ITouchpointData>*/brandTouchpointData(Collection/*<ITouchpointData>*/data, boolean macosxBundled) {
		ArrayList results = new ArrayList(data.size() + 1);
		results.addAll(data);

		boolean haveChmod = false;

		String brandedLauncher = null;
		if (config.getOs().equals("win32")) //$NON-NLS-1$
			brandedLauncher = launcherName + ".exe"; //$NON-NLS-1$
		else if (config.getOs().equals("macosx")) {//$NON-NLS-1$
			if (macosxBundled)
				brandedLauncher = "Contents/MacOS/" + launcherName; //$NON-NLS-1$
			else
				brandedLauncher = launcherName + ".app/Contents/MacOS/" + launcherName; //$NON-NLS-1$
		} else
			brandedLauncher = launcherName;

		for (int i = 0; i < results.size(); i++) {
			ITouchpointData td = (ITouchpointData) results.get(i);
			Map instructions = new HashMap(td.getInstructions());

			String[] phases = new String[] {INSTALL, CONFIGURE};
			for (int phase = 0; phase < phases.length; phase++) {
				ITouchpointInstruction instruction = td.getInstruction(phases[phase]);
				if (instruction == null)
					continue;

				boolean phaseChanged = false;
				String[] actions = Utils.getArrayFromString(instruction.getBody(), ";"); //$NON-NLS-1$
				for (int j = 0; j < actions.length; j++) {
					if (actions[j].startsWith(CHMOD)) {
						Map map = parseAction(actions[j]);
						String targetFile = (String) map.get(TARGET_FILE);
						targetFile = targetFile.replace('\\', '/');
						if (targetFile.equals(brandedLauncher)) {
							haveChmod = true;
							continue; //data has properly branded chmod, nothing to do
						}

						if ((config.getOs().equals("macosx") && (targetFile.endsWith(".app/Contents/MacOS/launcher") || targetFile.endsWith(".app/Contents/MacOS/eclipse"))) || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								(config.getOs().equals("win32") && (targetFile.equals("launcher.exe") || targetFile.equals("eclipse.exe"))) || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								(targetFile.equals("launcher") || targetFile.equals("eclipse"))) //$NON-NLS-1$ //$NON-NLS-2$
						{
							map.put(TARGET_FILE, brandedLauncher);
							actions[j] = CHMOD + toString(map);
							haveChmod = true;
							phaseChanged = true;
							break;
						}
					} else if (actions[j].startsWith(LN) && config.getOs().equals("macosx")) { //$NON-NLS-1$
						//for now only checking links on mac
						Map map = parseAction(actions[j]);
						String linkTarget = (String) map.get(LINK_TARGET);
						if (linkTarget.endsWith(".app/Contents/MacOS/launcher") || linkTarget.endsWith(".app/Contents/MacOS/eclipse")) { //$NON-NLS-1$ //$NON-NLS-2$
							map.put(LINK_TARGET, brandedLauncher);
							map.put(LINK_NAME, launcherName);
							actions[j] = LN + toString(map);
							phaseChanged = true;
						}
					}
				}
				if (phaseChanged) {
					TouchpointInstruction newInstruction = new TouchpointInstruction(toString(actions, ";"), instruction.getImportAttribute()); //$NON-NLS-1$
					instructions.put(phases[phase], newInstruction);
				}
			}

			results.set(i, new TouchpointData(instructions));
		}

		//add a chmod if there wasn't one before
		if (!haveChmod && !config.getOs().equals("win32")) { //$NON-NLS-1$
			String body = null;
			if (macosxBundled) {
				body = "unzip(source:@artifact, target:${installFolder}, path:" + launcherName + ".app);"; //$NON-NLS-1$ //$NON-NLS-2$
				body += " chmod(targetDir:${installFolder}/Contents/MacOS/, targetFile:" + launcherName + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
			} else { 
				body = "chmod(targetDir:${installFolder}, targetFile:" + brandedLauncher + ", permissions:755)"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			TouchpointInstruction newInstruction = new TouchpointInstruction(body, null);
			Map instructions = new HashMap();
			instructions.put(INSTALL, newInstruction);
			results.add(new TouchpointData(instructions));
		}
		return results;
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

	private String toString(Map map) {
		StringBuffer buffer = new StringBuffer();
		buffer.append('(');
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			buffer.append(key);
			buffer.append(':');
			buffer.append(map.get(key));
			if (iterator.hasNext())
				buffer.append(',');
		}
		buffer.append(')');
		return buffer.toString();
	}

	private Map parseAction(String action) {
		Map result = new HashMap();
		int open = action.indexOf('(');
		int close = action.lastIndexOf(')');
		String parameterString = action.substring(open + 1, close);
		String[] parameters = Utils.getArrayFromString(parameterString, ","); //$NON-NLS-1$
		for (int i = 0; i < parameters.length; i++) {
			int colon = parameters[i].indexOf(':');
			result.put(parameters[i].substring(0, colon).trim(), parameters[i].substring(colon + 1).trim());
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
