/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

public abstract class AbstractPublisherTask extends Task {
	protected URI metadataLocation;
	protected String metadataRepoName;
	protected URI artifactLocation;
	protected String artifactRepoName;
	protected String baseDirectory;
	protected boolean compress = false;
	protected boolean append = true;
	protected boolean reusePackedFiles = false;
	protected PublisherInfo publisherInfo = null;

	protected Properties getBuildProperties() {
		Properties buildProperties = null;
		try {
			buildProperties = AbstractScriptGenerator.readProperties(baseDirectory, IPDEBuildConstants.PROPERTIES_FILE, IStatus.OK);
		} catch (CoreException e) {
			//boo
		}
		return buildProperties;
	}

	protected BuildPublisherApplication createPublisherApplication() {
		BuildPublisherApplication application = new BuildPublisherApplication();
		application.setMetadataLocation(metadataLocation);
		application.setArtifactLocation(artifactLocation);
		application.setAppend(append);
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
		this.baseDirectory = baseDirectory;
	}

	protected PublisherInfo getPublisherInfo() {
		if (publisherInfo == null) {
			publisherInfo = new PublisherInfo();
			publisherInfo.setArtifactOptions(IPublisherInfo.A_PUBLISH);
		}
		return publisherInfo;
	}
}
