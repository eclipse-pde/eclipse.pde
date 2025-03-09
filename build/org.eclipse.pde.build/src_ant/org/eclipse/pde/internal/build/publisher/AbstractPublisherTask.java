/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

public abstract class AbstractPublisherTask extends Task {
	static final protected String ANT_PREFIX = "${"; //$NON-NLS-1$

	/**
	 * Support nested repository elements that looking something like
	 *    <repo location="file:/foo" metadata="true" artifact="true" />
	 * Both metadata and artifact are optional:
	 *  1) if neither are set, the repo is used for both metadata and artifacts
	 *  2) if only one is true, the repo is that type and not the other 
	 */
	static public class RepoEntry {
		private URI repoLocation;
		private Boolean metadata = null;
		private Boolean artifact = null;

		/**
		 * If not set, default is true if we aren't set as an artifact repo 
		 */
		public boolean isMetadataRepository() {
			if (metadata != null) {
				return metadata.booleanValue();
			}
			return !Boolean.TRUE.equals(artifact);
		}

		/**
		 * If not set, default is true if we aren't set as an metadata repo 
		 */
		public boolean isArtifactRepository() {
			if (artifact != null) {
				return artifact.booleanValue();
			}
			return !Boolean.TRUE.equals(metadata);
		}

		public URI getRepositoryLocation() {
			return repoLocation;
		}

		public void setLocation(String location) {
			try {
				repoLocation = URIUtil.fromString(location);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Repository location (" + location + ") must be a URL."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		public void setMetadata(boolean metadata) {
			this.metadata = Boolean.valueOf(metadata);
		}

		public void setArtifact(boolean artifact) {
			this.artifact = Boolean.valueOf(artifact);
		}
	}

	protected URI metadataLocation;
	protected String metadataRepoName;
	protected URI artifactLocation;
	protected String artifactRepoName;
	protected String baseDirectory;
	protected boolean compress = false;
	protected boolean append = true;
	protected boolean reusePackedFiles = false;
	protected PublisherInfo publisherInfo = null;
	private Properties buildProperties = null;
	protected String overrides = null;
	protected List<URI> contextMetadataRepositories = new ArrayList<>();
	protected List<URI> contextArtifactRepositories = new ArrayList<>();

	protected Properties getBuildProperties() {
		if (buildProperties != null) {
			return buildProperties;
		}

		Properties overrideProperties = null;
		if (overrides != null) {
			File overrideFile = new File(overrides);
			if (overrideFile.exists()) {
				try {
					overrideProperties = AbstractScriptGenerator.readProperties(overrideFile.getParent(), overrideFile.getName(), IStatus.OK);
				} catch (CoreException e) {
					//nothing
				}
			}
		}

		Properties properties = null;
		try {
			properties = AbstractScriptGenerator.readProperties(baseDirectory, IPDEBuildConstants.PROPERTIES_FILE, IStatus.OK);
		} catch (CoreException e) {
			return null;
		}

		buildProperties = new Properties();
		for (Object object : properties.keySet()) {
			String key = (String) object;
			String value = properties.getProperty(key);
			buildProperties.put(key, getProject().replaceProperties(value));
		}

		if (overrideProperties != null) {
			for (Object object : overrideProperties.keySet()) {
				String key = (String) object;
				String value = overrideProperties.getProperty(key);
				buildProperties.put(key, getProject().replaceProperties(value));
			}
		}

		return buildProperties;
	}

	protected BuildPublisherApplication createPublisherApplication() {
		BuildPublisherApplication application = new BuildPublisherApplication();
		application.setMetadataLocation(metadataLocation);
		application.setArtifactLocation(artifactLocation);
		application.setAppend(append);
		application.setCompress(compress);

		URI[] metadata = contextMetadataRepositories.toArray(new URI[contextMetadataRepositories.size()]);
		URI[] artifacts = contextArtifactRepositories.toArray(new URI[contextArtifactRepositories.size()]);
		application.setContextRepositories(metadata, artifacts);

		return application;
	}

	public void setArtifactRepository(String location) {
		try {
			this.artifactLocation = URIUtil.fromString(location);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Artifact repository location (" + location + ") must be a URL."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	public void setArtifactRepositoryName(String value) {
		this.artifactRepoName = value;
	}

	public void setMetadataRepository(String location) {
		try {
			this.metadataLocation = URIUtil.fromString(location);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Metadata repository location (" + location + ") must be a URL."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void setMetadataRepositoryName(String value) {
		this.metadataRepoName = value;
	}

	public void setRepository(String value) {
		setMetadataRepository(value);
		setArtifactRepository(value);
	}

	public void setAppend(String value) {
		this.append = Boolean.valueOf(value).booleanValue();
	}

	public void setCompress(String value) {
		this.compress = Boolean.valueOf(value).booleanValue();
	}

	public void setReusePackedFiles(String reusePackedFiles) {
		this.reusePackedFiles = Boolean.valueOf(reusePackedFiles).booleanValue();
	}

	public void setBaseDirectory(String baseDirectory) {
		if (baseDirectory != null && baseDirectory.length() > 0 && !baseDirectory.startsWith(ANT_PREFIX)) {
			this.baseDirectory = baseDirectory;
		}
	}

	public void setOverrides(String overrides) {
		if (overrides != null && overrides.length() > 0 && !overrides.startsWith(ANT_PREFIX)) {
			this.overrides = overrides;
		}
	}

	protected PublisherInfo getPublisherInfo() {
		if (publisherInfo == null) {
			publisherInfo = new PublisherInfo();
			publisherInfo.setArtifactOptions(IPublisherInfo.A_PUBLISH);
		}
		return publisherInfo;
	}

	// nested <contextRepository/> elements
	public void addConfiguredContextRepository(RepoEntry repo) {
		if (repo.isMetadataRepository()) {
			contextMetadataRepositories.add(repo.getRepositoryLocation());
		}
		if (repo.isArtifactRepository()) {
			contextArtifactRepositories.add(repo.getRepositoryLocation());
		}
	}
}
