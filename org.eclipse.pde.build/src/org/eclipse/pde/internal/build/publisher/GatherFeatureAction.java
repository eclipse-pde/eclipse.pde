/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class GatherFeatureAction extends FeaturesAction {
	private GatheringComputer computer;
	private String groupId = null;
	private FeatureRootAdvice rootAdvice;
	private final File featureRoot;

	public GatherFeatureAction(File location, File featureRoot) {
		super(new File[] {location});
		this.featureRoot = featureRoot;
	}

	public void setComputer(GatheringComputer computer) {
		this.computer = computer;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	protected Feature[] getFeatures(File[] locations) {
		Feature feature = new FeatureParser().parse(featureRoot);
		if (feature != null) {
			feature.setLocation(locations[0].getAbsolutePath());
			rootAdvice.setFeatureId(feature.getId());
			rootAdvice.setFeatureVersion(Version.parseVersion(feature.getVersion()));
			return new Feature[] {feature};
		}
		return new Feature[0];
	}

	//	protected ArrayList generateRootFileIUs(Feature feature, IPublisherResult result, IPublisherInfo publisherInfo) {
	//		ArrayList ius = new ArrayList();
	//
	//		Collection collection = publisherInfo.getAdvice(null, false, null, null, FeatureRootAdvice.class);
	//		if (collection.size() == 0)
	//			return ius;
	//
	//		FeatureRootAdvice advice = (FeatureRootAdvice) collection.iterator().next();
	//		String[] configs = advice.getConfigs();
	//		for (int i = 0; i < configs.length; i++) {
	//			String config = configs[i];
	//
	//			GatheringComputer rootComputer = advice.getRootFileComputer(config);
	//
	//			if (rootComputer != null) {
	//				FileSetDescriptor descriptor = advice.getDescriptor(config);
	//				IInstallableUnit iu = (IInstallableUnit) createFeatureRootFileIU(feature.getId(), feature.getVersion(), null, descriptor)[0];
	//
	//				File[] files = rootComputer.getFiles();
	//				IArtifactKey artifactKey = iu.getArtifacts()[0];
	//				ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(artifactKey);
	//				publishArtifact(artifactDescriptor, files, null, publisherInfo, rootComputer);
	//
	//				result.addIU(iu, IPublisherResult.NON_ROOT);
	//				ius.add(iu);
	//			}
	//		}
	//		return ius;
	//	}

	protected String getGroupId(String featureId) {
		if (groupId != null)
			return groupId;
		return super.getGroupId(featureId);
	}

	protected IInstallableUnit generateFeatureJarIU(Feature feature, IPublisherInfo publisherInfo) {
		if (computer == null)
			return null;
		return createFeatureJarIU(feature, publisherInfo);
	}

	protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU, IPublisherInfo publisherInfo) {
		if (computer == null)
			return;

		// add all the artifacts associated with the feature
		Collection artifacts = featureIU.getArtifacts();
		if (artifacts.size() > 1) {
			//boo!
		}

		ArtifactDescriptor ad = (ArtifactDescriptor) PublisherHelper.createArtifactDescriptor(publisherInfo, (IArtifactKey) artifacts.iterator().next(), null);
		processArtifactPropertiesAdvice(featureIU, ad, publisherInfo);
		ad.setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);

		publishArtifact(ad, computer.getFiles(), null, publisherInfo, computer);
	}

	public void setRootAdvice(FeatureRootAdvice rootAdvice) {
		this.rootAdvice = rootAdvice;
	}
}
