/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
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
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.engine.Phase;
import org.eclipse.equinox.internal.provisional.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.provisional.p2.engine.phases.Install;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.internal.repository.tools.Activator;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;
import org.eclipse.equinox.p2.internal.repository.tools.tasks.IUDescription;
import org.eclipse.equinox.p2.internal.repository.tools.tasks.Repo2RunnableTask;
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
		};
	}

	public void execute() {
		if (launcherName == null || launcherName.startsWith(ANT_PREFIX) || config == null)
			return; //TODO error/warning

		if (launcherProvider == null || launcherProvider.startsWith("${")) //$NON-NLS-1$
			launcherProvider = IPDEBuildConstants.FEATURE_EQUINOX_EXECUTABLE;

		IMetadataRepository metadataRepo = loadMetadataRepository();
		IArtifactRepository artifactRepo = loadArtifactRepository();

		try {
			super.setDestination(getRootFolder());

			super.execute();
			if (ius.size() == 1) {
				callBrandingIron();
				publishBrandedIU(metadataRepo, artifactRepo, (IInstallableUnit) ius.get(0));
				FileUtils.deleteAll(new File(getRootFolder()));
			}
		} catch (BuildException e) {
			getProject().log(e.getMessage(), Project.MSG_WARN);
		} finally {
			try {
				cleanupRepositories();
			} catch (ProvisionException e) {
				getProject().log(e.getMessage(), Project.MSG_WARN);
			}
			ius = null;
		}
	}

	private void cleanupRepositories() throws ProvisionException {
		IMetadataRepositoryManager metadataManager = Activator.getMetadataRepositoryManager();
		IArtifactRepositoryManager artifactManager = Activator.getArtifactRepositoryManager();
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

	private IArtifactRepository loadArtifactRepository() throws BuildException {
		if (artifactURI == null)
			throw new BuildException(TaskMessages.error_noArtifactRepo);

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) BundleHelper.getDefault().acquireService(IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new BuildException(TaskMessages.error_artifactRepoManagerService);

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

	private IMetadataRepository loadMetadataRepository() throws BuildException {
		if (metadataURI == null)
			throw new BuildException(TaskMessages.error_noMetadataRepo);

		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) BundleHelper.getDefault().acquireService(IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new BuildException(TaskMessages.error_metadataRepoManagerService);

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
		iron.setRoot(getRootFolder());
		iron.setOS(config.getOs());
		try {
			iron.brand();
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
		newIUDescription.setFilter(originalIU.getFilter());

		ITouchpointData[] data = brandTouchpointData(originalIU.getTouchpointData());
		for (int i = 0; i < data.length; i++) {
			newIUDescription.addTouchpointData(data[i]);
		}

		IArtifactKey key = new ArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, newIUDescription.getId(), newIUDescription.getVersion());
		newIUDescription.setArtifacts(new IArtifactKey[] {key});

		IInstallableUnit newIU = MetadataFactory.createInstallableUnit(newIUDescription);
		metadataRepo.addInstallableUnits(new IInstallableUnit[] {newIU});

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
	private static final String TARGET_FILE = "targetFile"; //$NON-NLS-1$
	private static final String INSTALL = "install"; //$NON-NLS-1$

	private ITouchpointData[] brandTouchpointData(ITouchpointData[] data) {
		boolean haveChmod = false;

		String brandedLauncher = null;
		if (config.getOs().equals("win32")) //$NON-NLS-1$
			brandedLauncher = launcherName + ".exe"; //$NON-NLS-1$
		else if (config.getOs().equals("macosx")) //$NON-NLS-1$
			brandedLauncher = launcherName + ".app/Contents/MacOS/" + launcherName; //$NON-NLS-1$
		else
			brandedLauncher = launcherName;

		for (int i = 0; i < data.length; i++) {
			ITouchpointInstruction instruction = data[i].getInstruction(INSTALL);
			if (instruction == null)
				continue;
			String[] actions = Utils.getArrayFromString(instruction.getBody(), ";"); //$NON-NLS-1$
			for (int j = 0; j < actions.length; j++) {
				if (actions[j].startsWith(CHMOD)) {
					Map map = parseAction(actions[j]);
					String newFile = null;
					String targetFile = (String) map.get(TARGET_FILE);
					targetFile = targetFile.replace('\\', '/');
					if (targetFile.equals(brandedLauncher))
						return data; //data has properly branded chmod, nothing to do

					if ((config.getOs().equals("macosx") && (targetFile.endsWith(".app/Contents/MacOS/launcher") || targetFile.endsWith(".app/Contents/MacOS/eclipse"))) || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							(config.getOs().equals("win32") && (targetFile.equals("launcher.exe") || targetFile.equals("eclipse.exe"))) || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							(targetFile.equals("launcher") || targetFile.equals("eclipse"))) { //$NON-NLS-1$ //$NON-NLS-2$
						newFile = brandedLauncher;
					}
					if (newFile != null) {
						map.put(TARGET_FILE, newFile);
						actions[j] = CHMOD + toString(map);
						haveChmod = true;
						break;
					}
				}
			}
			if (haveChmod) {
				TouchpointInstruction newInstruction = new TouchpointInstruction(toString(actions, ";"), instruction.getImportAttribute()); //$NON-NLS-1$
				Map instructions = new HashMap(data[i].getInstructions());
				instructions.put(INSTALL, newInstruction);
				data[i] = new TouchpointData(instructions);
				return data;
			}
		}

		String body = "chmod(targetDir:${installFolder}, targetFile:" + brandedLauncher + ", permissions:755)"; //$NON-NLS-1$ //$NON-NLS-2$
		TouchpointInstruction newInstruction = new TouchpointInstruction(body, null);
		Map instructions = new HashMap();
		instructions.put(INSTALL, newInstruction);
		ArrayList newData = new ArrayList(data.length + 1);
		newData.addAll(Arrays.asList(data));
		newData.add(new TouchpointData(instructions));
		return (ITouchpointData[]) newData.toArray(new ITouchpointData[newData.size()]);
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
			super.addMetadataSourceRepository(metadataURI);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Metadata repository location (" + location + ") must be a URI."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	public void setArtifactRepository(String location) {
		try {
			this.artifactURI = URIUtil.fromString(location);
			super.addArtifactSourceRepository(artifactURI);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Artifact repository location (" + location + ") must be a URI."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}
}
