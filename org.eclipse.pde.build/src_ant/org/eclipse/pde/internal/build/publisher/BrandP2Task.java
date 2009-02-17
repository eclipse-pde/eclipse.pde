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
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
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
	private String productVersion = null;
	private URI metadataURI = null;
	private URI artifactURI = null;
	private List ius = null;

	public BrandP2Task() {
		application = new Repo2Runnable() {
			protected PhaseSet getPhaseSet() {
				return new PhaseSet(new Phase[] {new Collect(100), new Install(100)}) { /* nothing to override */};
			}
		};
	}

	public void execute() {
		if (launcherName == null || launcherName.startsWith("${") || config == null) //$NON-NLS-1$
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
		} finally {
			ius = null;
		}
	}

	private IArtifactRepository loadArtifactRepository() throws BuildException {
		if (artifactURI == null)
			throw new BuildException(TaskMessages.error_noArtifactRepo);

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) BundleHelper.getDefault().acquireService(IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new BuildException(TaskMessages.error_artifactRepoManagerService);

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
		return launcherProvider + "_root." + config.toString("."); //$NON-NLS-1$ //$NON-NLS-2$
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

	protected void publishBrandedIU(IMetadataRepository metadataRepo, IArtifactRepository artifactRepo, IInstallableUnit originalIU) {
		String id = productId + "_root." + config.toString("."); //$NON-NLS-1$ //$NON-NLS-2$
		Version version = new Version(productVersion);
		if (version.equals(Version.emptyVersion))
			version = originalIU.getVersion();
		InstallableUnitDescription newIUDescription = new MetadataFactory.InstallableUnitDescription();
		newIUDescription.setSingleton(originalIU.isSingleton());
		newIUDescription.setId(id);
		newIUDescription.setVersion(version);
		newIUDescription.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version)});
		newIUDescription.setTouchpointType(originalIU.getTouchpointType());
		newIUDescription.setFilter(originalIU.getFilter());

		ITouchpointData[] data = originalIU.getTouchpointData();
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
			FileUtils.zip(output, root, Collections.EMPTY_SET, FileUtils.createRootPathComputer(root));
		} catch (ProvisionException e) {
			throw new BuildException(e.getMessage(), e);
		} catch (IOException e) {
			throw new BuildException(e.getMessage(), e);
		} finally {
			Utils.close(output);
		}
	}

	public void setConfig(String config) {
		if (config == null || config.startsWith("${")) //$NON-NLS-1$
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
