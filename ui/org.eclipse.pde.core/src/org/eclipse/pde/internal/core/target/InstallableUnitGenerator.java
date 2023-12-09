/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureManifestParser;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.xml.sax.SAXException;

/**
 * Class to generate {@link IInstallableUnit}s from target objects
 */
@SuppressWarnings("restriction")
public class InstallableUnitGenerator {

	public static Stream<IInstallableUnit> generateInstallableUnits(TargetBundle[] bundles, TargetFeature[] features) {
		return Stream.concat(generateInstallableUnits(bundles), generateInstallableUnits(features));
	}

	public static Stream<IInstallableUnit> generateInstallableUnits(TargetBundle[] bundles) {
		if (bundles == null || bundles.length == 0) {
			return Stream.empty();
		}
		return Arrays.stream(bundles).flatMap(InstallableUnitGenerator::generateInstallableUnits);
	}

	public static Stream<IInstallableUnit> generateInstallableUnits(TargetBundle targetBundle) {
		BundleInfo bundleInfo = targetBundle.getBundleInfo();
		if (bundleInfo != null) {
			String manifest = bundleInfo.getManifest();
			if (manifest != null) {
				try {
					Manifest parsed = new Manifest(new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)));
					Attributes mainAttributes = parsed.getMainAttributes();
					CaseInsensitiveDictionaryMap<String, String> headers = new CaseInsensitiveDictionaryMap<>(
							mainAttributes.size());
					Set<Entry<Object, Object>> entrySet = mainAttributes.entrySet();
					for (Entry<Object, Object> entry : entrySet) {
						headers.put(entry.getKey().toString(), entry.getValue().toString());
					}
					PublisherInfo publisherInfo = new PublisherInfo();
					publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
					BundleDescription bundleDescription = BundlesAction.createBundleDescription(headers, null);
					// null if bundle contains invalid manifest headers
					if (bundleDescription != null) {
						IInstallableUnit iu = BundlesAction.createBundleIU(bundleDescription,
								BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(),
										bundleDescription.getVersion().toString()),
								publisherInfo);
						return Stream.of(iu);
					}
				} catch (IOException e) {
					// can't use it then...
				}
			}
		}
		return Stream.empty();
	}

	public static Stream<IInstallableUnit> generateInstallableUnits(TargetFeature[] features) {
		if (features == null || features.length == 0) {
			return Stream.empty();
		}
		return Arrays.stream(features).flatMap(InstallableUnitGenerator::generateInstallableUnits);
	}

	public static Stream<IInstallableUnit> generateInstallableUnits(TargetFeature targetFeature) {
		String location = targetFeature.getLocation();
		if (location != null) {
			Feature feature = new FeatureParser().parse(new File(location));
			if (feature != null) {
				feature.setLocation(location);
				return generateInstallableUnits(feature);
			}
		}
		IModel model = targetFeature.getFeatureModel();
		if (model instanceof IFeatureModel featureModel) {
			IFeature feature = featureModel.getFeature();
			if (feature != null) {
				StringWriter stringWriter = new StringWriter();
				PrintWriter printWriter = new PrintWriter(stringWriter);
				feature.write("", printWriter); //$NON-NLS-1$
				printWriter.flush();
				try {
					generateInstallableUnits(new FeatureManifestParser().parse(
							new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8)),
							new URL("file:/dummy")));//$NON-NLS-1$
				} catch (SAXException | IOException e) {
					// can't use it then...
				}
			}
		}
		return Stream.empty();
	}

	private static Stream<IInstallableUnit> generateInstallableUnits(Feature feature) {
		PublisherInfo publisherInfo = new PublisherInfo();
		publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX);
		FeaturesAction action = new FeaturesAction(new Feature[] { feature }) {
			@Override
			protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU,
					IPublisherInfo publisherInfo) {
				// so not call super as we don't wan't to copy anything --> Bug
				// in P2 with IPublisherInfo.A_INDEX option
			}
		};
		PublisherResult results = new PublisherResult();
		action.perform(publisherInfo, results, null);
		return results.query(QueryUtil.ALL_UNITS, null).stream();
	}

}
