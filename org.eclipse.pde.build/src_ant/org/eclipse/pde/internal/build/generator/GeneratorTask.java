/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.generator;

import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.jarprocessor.Utils;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.publisher.compatibility.GeneratorApplication;
import org.eclipse.pde.internal.build.publisher.compatibility.IncrementalGenerator;
import org.eclipse.pde.internal.build.tasks.TaskMessages;

/**
 * An Ant task to call the p2 Metadata Generator application.
 */
public class GeneratorTask extends Task {
	private static final String ANT_PREFIX = "${"; //$NON-NLS-1$

	protected PublisherInfo info = null;
	private GeneratorApplication generator = null;
	private String mode;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			IncrementalGenerator incremental = new IncrementalGenerator();
			incremental.setMode(mode);
			incremental.run(generator, info);

			if (!"incremental".equals(mode)) { //$NON-NLS-1$
				info = null;
				generator = null;
			}
		} catch (Exception e) {
			throw new BuildException(TaskMessages.error_callingGenerator, e);
		}
	}

	protected PublisherInfo getInfo() {
		if (info == null) {
			info = new PublisherInfo();
		}
		return info;
	}

	protected GeneratorApplication getGenerator() {
		if (generator == null)
			generator = new GeneratorApplication();
		return generator;
	}

	public void setAppend(String value) {
		getGenerator().setAppend(Boolean.valueOf(value).booleanValue());
	}

	public void setArtifactRepository(String location) {
		if (location != null && !location.startsWith(ANT_PREFIX))
			try {
				getGenerator().setArtifactLocation(URIUtil.fromString(location));
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(NLS.bind(TaskMessages.error_artifactRepoNotURI, location));
			}
	}

	public void setArtifactRepositoryName(String name) {
		getGenerator().setArtifactRepositoryName(name);
	}

	public void setBase(String value) {
		if (generator == null)
			generator = new GeneratorApplication();
		//		generator.setBase(value);
	}

	public void setBundles(String value) {
		if (generator == null)
			generator = new GeneratorApplication();
		//		generator.setBundles(value);
	}

	public void setCompress(String value) {
		getGenerator().setCompress(Boolean.valueOf(value).booleanValue());
	}

	public void setConfig(String value) {
		getGenerator().setOperation(GeneratorApplication.OPERATION_CONFIG);
		getGenerator().setSource(value);
	}

	public void setInplace(String value) {
		getGenerator().setOperation(GeneratorApplication.OPERATION_INPLACE);
		getGenerator().setSource(value);
	}

	public void setSource(String location) {
		getGenerator().setOperation(GeneratorApplication.OPERATION_SOURCE);
		getGenerator().setSource(location);
	}

	public void setUpdateSite(String value) {
		getGenerator().setOperation(GeneratorApplication.OPERATION_UPDATE);
		getGenerator().setSource(value);
	}

	public void setExe(String value) {
		if (info == null)
			info = new PublisherInfo();
		//		info.setExecutableLocation(value);
	}

	public void setFeatures(String value) {
		if (generator == null)
			generator = new GeneratorApplication();
		//		generator.setFeatures(value);
	}

	public void setFlavor(String flavor) {
		if (flavor != null && !flavor.startsWith(ANT_PREFIX))
			getGenerator().setFlavor(flavor);
	}

	public void setLauncherConfig(String launcherConfig) {
		if (launcherConfig != null && !launcherConfig.startsWith(ANT_PREFIX)) {
			//config comes in as os_ws_arch, publisher wants ws.os.arch
			String[] array = Utils.toStringArray(launcherConfig, "_"); //$NON-NLS-1$
			if (array.length >= 3) {
				StringBuffer config = new StringBuffer(array[1]);
				config.append('.');
				config.append(array[0]);
				config.append('.');
				config.append(array[2]);
				if (array.length > 3) { //arch's like x86_64
					config.append('_');
					config.append(array[3]);
				}

				getInfo().setConfigurations(new String[] {config.toString()});
			}
		}
	}

	public void setMetadataRepository(String location) {
		if (location != null && !location.startsWith(ANT_PREFIX))
			try {
				getGenerator().setMetadataLocation(URIUtil.fromString(location));
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(NLS.bind(TaskMessages.error_metadataRepoNotURI, location));
			}
	}

	public void setMetadataRepositoryName(String name) {
		if (name != null && !name.startsWith(ANT_PREFIX))
			getGenerator().setMetadataRepositoryName(name);
	}

	public void setNoDefaultIUs(String value) {
		if (info == null)
			info = new PublisherInfo();
		//		info.setAddDefaultIUs(!Boolean.valueOf(value).booleanValue());
	}

	public void setP2OS(String value) {
		if (info == null)
			info = new PublisherInfo();
		//		info.setOS(value);
	}

	public void setProductFile(String file) {
		if (file != null && !file.startsWith(ANT_PREFIX)) {
			getGenerator().setProductFile(file);
		}
	}

	public void setPublishArtifactRepository(boolean value) {
		int options = getInfo().getArtifactOptions();
		if (value)
			info.setArtifactOptions(options | IPublisherInfo.A_INDEX);
		else
			info.setArtifactOptions(options & ~IPublisherInfo.A_INDEX);
	}

	public void setPublishArtifacts(boolean value) {
		int options = getInfo().getArtifactOptions();
		if (value)
			info.setArtifactOptions(options | IPublisherInfo.A_PUBLISH);
		else
			info.setArtifactOptions(options & ~IPublisherInfo.A_PUBLISH);
	}

	public void setRoot(String root) {
		if (root == null || root.startsWith("${")) //$NON-NLS-1$
			return;
		getGenerator().setRoodId(root);
	}

	public void setRootVersion(String rootVersion) {
		if (rootVersion == null || rootVersion.startsWith(ANT_PREFIX))
			return;
		getGenerator().setRootVersion(rootVersion);
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setVersionAdvice(String advice) {
		if (advice != null && !advice.startsWith(ANT_PREFIX))
			getGenerator().setVersionAdvice(advice);
	}

	public void setSite(String site) {
		if (site == null || site.startsWith(ANT_PREFIX))
			return;
		try {
			getGenerator().setSite(URIUtil.fromString(site));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(NLS.bind(TaskMessages.error_locationNotURI, site));
		}
	}

	public void setSiteVersion(String version) {
		if (version == null || version.startsWith(ANT_PREFIX))
			return;
		getGenerator().setSiteVersion(version);
	}
}
