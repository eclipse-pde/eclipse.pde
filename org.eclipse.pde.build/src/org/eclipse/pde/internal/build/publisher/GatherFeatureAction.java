package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class GatherFeatureAction extends FeaturesAction {
	private GatheringComputer computer;
	private final File featureRoot;

	public GatherFeatureAction(File location, File featureRoot) {
		super(new File[] {location});
		this.featureRoot = featureRoot;
	}

	public void setComputer(GatheringComputer computer) {
		this.computer = computer;
	}

	protected Feature[] getFeatures(File[] locations) {
		Feature feature = new FeatureParser().parse(featureRoot);
		if (feature != null) {
			feature.setLocation(locations[0].getAbsolutePath());
			return new Feature[] {feature};
		}
		return new Feature[0];
	}

	protected ArrayList generateRootFileIUs(Feature feature, IPublisherResult result, IPublisherInfo info) {
		ArrayList ius = new ArrayList();

		Collection collection = info.getAdvice(null, false, null, null, FeatureRootAdvice.class);
		if (collection.size() == 0)
			return ius;

		FeatureRootAdvice advice = (FeatureRootAdvice) collection.iterator().next();
		String[] configs = advice.getConfigs();
		for (int i = 0; i < configs.length; i++) {
			String config = configs[i];

			GatheringComputer rootComputer = advice.getRootFileComputer(config);

			FileSetDescriptor descriptor = advice.getDescriptor(config);
			IInstallableUnit iu = (IInstallableUnit) createFeatureRootFileIU(feature.getId(), feature.getVersion(), null, descriptor)[0];

			File[] files = rootComputer.getFiles();
			IArtifactKey artifactKey = iu.getArtifacts()[0];
			ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(artifactKey);
			publishArtifact(artifactDescriptor, files, null, info, rootComputer);

			result.addIU(iu, IPublisherResult.NON_ROOT);
			ius.add(iu);
		}
		return ius;
	}

	protected IInstallableUnit generateFeatureJarIU(Feature feature, ArrayList childIUs, IPublisherInfo info) {
		if (computer == null)
			return null;
		return FeaturesAction.createFeatureJarIU(feature, childIUs, info);
	}

	protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU, IPublisherInfo info) {
		if (computer == null)
			return;

		// add all the artifacts associated with the feature
		IArtifactKey[] artifacts = featureIU.getArtifacts();
		if (artifacts.length > 1) {
			//boo!
		}

		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(artifacts[0], null);
		addProperties((ArtifactDescriptor) ad, feature, info);
		((ArtifactDescriptor) ad).setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);

		publishArtifact(ad, computer.getFiles(), null, info, computer);
	}
}
