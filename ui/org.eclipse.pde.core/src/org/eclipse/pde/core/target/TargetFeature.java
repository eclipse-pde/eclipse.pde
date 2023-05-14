/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - Bug 576568 - org.eclipse.pde.core.target.TargetFeature should support a (protected) constructor without a file
 *******************************************************************************/
package org.eclipse.pde.core.target;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.Messages;

/**
 * Describes a single feature in a target definition.
 *
 * @since 3.8
 */
public class TargetFeature {

	private final IFeatureModel featureModel;

	/**
	 * Constructs a target feature for a feature on the local filesystem. The
	 * file may point at the feature.xml or a folder containing the feature.xml.
	 * The feature.xml will be read to collect the information about the
	 * feature.
	 *
	 * @param featureLocation
	 *            the location of the feature (feature.xml or directory
	 *            containing it) never <code>null</code>
	 * @throws CoreException
	 *             if there is a problem opening the feature.xml or
	 *             featureLocation is <code>null</code>
	 */
	public TargetFeature(File featureLocation) throws CoreException {
		this(loadModel(featureLocation));
	}

	/**
	 * Constructs a target feature for a feature from an {@link IFeatureModel}.
	 *
	 * @param featureModel
	 *            the model to use to delegate the calls, must not be
	 *            <code>null</code>
	 * @throws NullPointerException
	 *             if the model is null
	 *
	 * @since 3.15
	 */
	protected TargetFeature(IModel featureModel) throws NullPointerException {
		Objects.requireNonNull(featureModel, "The feature model can't be null"); //$NON-NLS-1$
		this.featureModel = Objects.requireNonNull(Adapters.adapt(featureModel, IFeatureModel.class),
				"The feature model must be an instance of IFeatureModel or adapt to it!"); //$NON-NLS-1$
	}

	/**
	 * Returns the id of this feature or <code>null</code> if no id is set.
	 *
	 * @return id or <code>null</code>
	 */
	public String getId() {
		return featureModel.getFeature().getId();
	}

	/**
	 * Returns the version of this feature or <code>null</code> if no version is
	 * set.
	 *
	 * @return version or <code>null</code>
	 */
	public String getVersion() {
		return featureModel.getFeature().getVersion();
	}

	/**
	 * Returns the string path to the directory containing the feature.xml or
	 * <code>null</code> if no install location is known.
	 *
	 * @return install location path or <code>null</code>
	 */
	public String getLocation() {
		return featureModel.getInstallLocation();
	}

	/**
	 * Returns a list of name version descriptor that describes the set of
	 * plug-ins that this feature includes.
	 *
	 * @return a list of name version descriptors, possibly empty
	 */
	public NameVersionDescriptor[] getPlugins() {
		return Arrays.stream(featureModel.getFeature().getPlugins())
				.map(plugin -> new NameVersionDescriptor(plugin.getId(), plugin.getVersion()))
				.toArray(NameVersionDescriptor[]::new);
	}

	/**
	 * Returns a list of name version descriptors that describe the set of
	 * features that this feature depends on as imports or included features.
	 *
	 * @return a list of name version descriptors, possibly empty
	 */
	public NameVersionDescriptor[] getDependentFeatures() {
		List<NameVersionDescriptor> result = new ArrayList<>();
		IFeature feature = featureModel.getFeature();
		IFeatureImport[] featureImports = feature.getImports();
		for (IFeatureImport featureImport : featureImports) {
			if (featureImport.getType() == IFeatureImport.FEATURE) {
				result.add(new NameVersionDescriptor(featureImport.getId(), null, NameVersionDescriptor.TYPE_FEATURE));
			}
		}
		IFeatureChild[] featureIncludes = feature.getIncludedFeatures();
		for (IFeatureChild featureInclude : featureIncludes) {
			result.add(new NameVersionDescriptor(featureInclude.getId(), null, NameVersionDescriptor.TYPE_FEATURE));
		}
		return result.toArray(new NameVersionDescriptor[result.size()]);
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0} {1} (Feature)", getId(), getVersion());//$NON-NLS-1$
	}

	/**
	 * @return the internal feature model this {@link TargetFeature} is backed
	 *         with
	 * @since 3.15
	 */
	public final IModel getFeatureModel() {
		return featureModel;
	}

	/**
	 * Initializes the content of this target feature by reading the feature.xml
	 *
	 * @param file
	 *            feature.xml or directory containing it
	 * @return the loaded {@link IFeatureModel}
	 */
	private static IFeatureModel loadModel(File file) throws CoreException {
		if (file == null || !file.exists()) {
			throw new CoreException(Status.error(NLS.bind(Messages.TargetFeature_FileDoesNotExist, file)));
		}
		File featureXML;
		if (ICoreConstants.FEATURE_FILENAME_DESCRIPTOR.equalsIgnoreCase(file.getName())) {
			featureXML = file;
		} else {
			featureXML = new File(file, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			if (!featureXML.exists()) {
				throw new CoreException(Status.error(NLS.bind(Messages.TargetFeature_FileDoesNotExist, featureXML)));
			}
		}
		return ExternalFeatureModelManager.createModel(featureXML);
	}

}
