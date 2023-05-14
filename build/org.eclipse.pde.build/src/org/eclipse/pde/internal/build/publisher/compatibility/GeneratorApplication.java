/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher.compatibility;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherApplication;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IVersionAdvice;
import org.eclipse.equinox.p2.publisher.actions.RootFilesAction;
import org.eclipse.equinox.p2.publisher.actions.RootIUAction;
import org.eclipse.equinox.p2.publisher.actions.RootIUResultFilterAdvice;
import org.eclipse.equinox.p2.publisher.actions.VersionAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.EquinoxLauncherCUAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

public class GeneratorApplication extends AbstractPublisherApplication {
	static final public int OPERATION_SOURCE = 1;
	static final public int OPERATION_INPLACE = 2;
	static final public int OPERATION_CONFIG = 3;
	static final public int OPERATION_UPDATE = 4;

	private int operation = 0;
	private IPublisherResult result = null;
	private URI site = null;
	private String siteVersion = null;
	private String flavor;
	private ProductFile product;
	private String rootVersion;
	private String versionAdvice;
	private String rootId;

	@Override
	protected IPublisherAction[] createActions() {

		File sourceFile = source != null ? new File(source) : null;

		List<IPublisherAction> actions = new ArrayList<>();
		switch (operation) {
			case 0 :
				if (product != null) {
					actions.add(new RootFileParentAction(product, flavor));
					actions.add(new EquinoxLauncherCUAction(flavor, info.getConfigurations()));
					// We depend on the product action to remove entries that have no version advice (Bug 398066)
					// Entries may have no version advice if they are for a different environment (no delta pack)
					actions.add(new ProductAction(source, product, flavor, null));
				} else if (rootId != null) {
					info.addAdvice(new RootIUResultFilterAdvice(null));
					actions.add(new RootFileParentAction(rootId, rootVersion, flavor));
					actions.add(new RootIUAction(rootId, Version.parseVersion(rootVersion), null));
				}
				break;
			case OPERATION_SOURCE :
				actions.add(new FeaturesAction(new File[] {new File(sourceFile, "features")})); //$NON-NLS-1$
				actions.add(new BundlesAction(new File[] {new File(sourceFile, "plugins")})); //$NON-NLS-1$
				if (site != null) {
					SiteXMLAction siteAction = new SiteXMLAction(site, ""); //$NON-NLS-1$
					if (siteVersion != null && siteVersion.length() > 0)
						siteAction.setCategoryVersion(siteVersion);
					actions.add(siteAction);
				}
				break;
			case OPERATION_CONFIG :
				String[] configs = info.getConfigurations();
				if (configs.length == 1) {
					info.addAdvice(new AssembledConfigAdvice(configs[0], sourceFile, product != null ? product.getLauncherName() : null));
					info.addAdvice(new RootFileTouchpointAdvice(product, sourceFile, new File[] {sourceFile}, null, configs[0]));
					actions.add(createRootFileAction(configs[0]));
				}
				break;
		}

		if (versionAdvice != null)
			info.addAdvice(createVersionAdvice());

		return actions.toArray(new IPublisherAction[actions.size()]);
	}

	protected IVersionAdvice createVersionAdvice() {
		File adviceFile = new File(versionAdvice);
		boolean features = adviceFile.getName().indexOf("feature") > 0; //$NON-NLS-1$
		VersionAdvice advice = new VersionAdvice();
		advice.load(IInstallableUnit.NAMESPACE_IU_ID, versionAdvice, features ? ".feature.group" : ""); //$NON-NLS-1$ //$NON-NLS-2$
		return advice;
	}

	protected IPublisherAction createRootFileAction(String configSpec) {
		String id = product != null ? product.getId() : rootId;
		Version version = Version.parseVersion(getProductVersion());

		RootFilesAction action = new RootFilesAction(info, id, version, flavor);
		action.setCreateParent(false);
		return action;
	}

	private String getProductVersion() {
		String version = "1.0.0"; //$NON-NLS-1$
		if (product != null && !product.getVersion().equals(IPDEBuildConstants.GENERIC_VERSION_NUMBER)) {
			version = product.getVersion();
		} else if (rootVersion != null && !rootVersion.equals(IPDEBuildConstants.GENERIC_VERSION_NUMBER)) {
			version = rootVersion;
		}
		return version;
	}

	@Override
	protected Publisher createPublisher(PublisherInfo publisherInfo) {
		if (result != null)
			return new Publisher(publisherInfo, result);
		return new Publisher(publisherInfo);
	}

	public void setAppend(boolean value) {
		super.append = value;
	}

	public void setArtifactRepositoryName(String name) {
		super.artifactRepoName = name;
	}

	@Override
	public void setCompress(boolean value) {
		super.compress = value;
	}

	public void setMetadataRepositoryName(String name) {
		super.metadataRepoName = name;
	}

	public void setOperation(int operation) {
		this.operation = operation;
	}

	public void setSource(String location) {
		this.source = location;
	}

	public void setSite(URI location) {
		this.site = location;
	}

	public void setIncrementalResult(IPublisherResult result) {
		this.result = result;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public void setRootVersion(String rootVersion) {
		this.rootVersion = rootVersion;
	}

	public void setRoodId(String root) {
		this.rootId = root;
	}

	public void setProductFile(String file) {
		if (file != null && new File(file).exists()) {
			try {
				product = new ProductFile(file);
			} catch (Exception e) {
				// problem
			}
		}
	}

	public void setVersionAdvice(String advice) {
		this.versionAdvice = advice;
	}

	public void setSiteVersion(String version) {
		this.siteVersion = version;
	}
}