/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.NativeCodeSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.Config;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.build.Messages;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.site.compatibility.FeatureReference;
import org.osgi.framework.Filter;
import org.osgi.framework.Version;

/**
 * This site represent a site at build time. A build time site is made of code
 * to compile, and a potential installation of eclipse (or derived products)
 * against which the code must be compiled. Moreover this site provide access to
 * a pluginRegistry.
 */
public class BuildTimeSite /*extends Site*/ implements IPDEBuildConstants, IXMLConstants {
	private final BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
	private final Map<String, Set<BuildTimeFeature>> featureCache = new HashMap<>();
	private final Map<URL, BuildTimeFeature> featureURLCache = new HashMap<>();
	private List<FeatureReference> featureReferences;
	private BuildTimeSiteContentProvider contentProvider;
	private boolean featuresResolved = false;

	private PDEState state;
	private Properties repositoryVersions; //version for the features
	private boolean reportResolutionErrors;
	private Properties platformProperties;
	private String[] eeSources;

	//Support for filtering what is added to the state
	private List<String> rootFeaturesForFilter;
	private List<String> rootPluginsForFiler;
	private boolean filter = false;

	private final Comparator<Feature> featureComparator = (arg0, arg1) -> {
		Version v0 = new Version(arg0.getVersion());
		Version v1 = new Version(arg1.getVersion());
		return -1 * v0.compareTo(v1);
	};

	public void setReportResolutionErrors(boolean value) {
		reportResolutionErrors = value;
	}

	public void setPlatformPropeties(Properties platformProperties) {
		this.platformProperties = platformProperties;
	}

	public Properties getFeatureVersions() {
		if (repositoryVersions == null) {
			repositoryVersions = new Properties();
			try (InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_FEATURE_REPOTAG_FILENAME_DESCRIPTOR))) {
				repositoryVersions.load(input);
			} catch (IOException e) {
				//Ignore
			}
		}
		return repositoryVersions;
	}

	private Properties getUIPlatformProperties() {
		Properties result = new Properties();
		result.put(IPDEBuildConstants.PROPERTY_RESOLVE_OPTIONAL, IBuildPropertiesConstants.TRUE);
		result.put(IPDEBuildConstants.PROPERTY_RESOLVER_MODE, IPDEBuildConstants.VALUE_DEVELOPMENT);
		return result;
	}

	private Collection<File> removeDuplicates(Collection<File> bundles) {
		Set<File> result = new LinkedHashSet<>(bundles.size() / 2);
		for (File bundle : bundles) {
			try {
				bundle = bundle.getCanonicalFile();
			} catch (IOException e) {
				// ignore
			}
			if (result.contains(bundle))
				continue;
			result.add(bundle);
		}
		return result;
	}

	public PDEState getRegistry() throws CoreException {
		if (state == null) {
			// create the registry according to the site where the code to
			// compile is, and a existing installation of eclipse
			BuildTimeSiteContentProvider provider = getSiteContentProvider();

			if (provider.getInitialState() != null) {
				state = new PDEState(provider.getInitialState());
				state.setEESources(eeSources);
				state.setPlatformProperties(getUIPlatformProperties());
				state.resolveState();
				return state;
			}

			if (filter) {
				state = new FilteringState();
				((FilteringState) state).setFilter(findAllReferencedPlugins());
			} else {
				state = new PDEState();
			}
			if (platformProperties != null)
				state.setPlatformProperties(platformProperties);

			Collection<File> bundles = removeDuplicates(provider.getPluginPaths());
			state.addBundles(bundles);
			state.setEESources(eeSources);

			//Once all the elements have been added to the state, the filter is removed to allow for the generated plug-ins to be added
			if (state instanceof FilteringState) {
				((FilteringState) state).setFilter(null);
			}
			state.resolveState();
			BundleDescription[] allBundles = state.getState().getBundles();
			BundleDescription[] resolvedBundles = state.getState().getResolvedBundles();
			if (allBundles.length == resolvedBundles.length)
				return state;

			if (reportResolutionErrors) {
				MultiStatus errors = new MultiStatus(IPDEBuildConstants.PI_PDEBUILD, 1, Messages.exception_registryResolution, null);
				BundleDescription[] all = state.getState().getBundles();
				StateHelper helper = BundleHelper.getPlatformAdmin().getStateHelper();
				for (BundleDescription element : all) {
					if (!element.isResolved()) {
						ResolverError[] resolutionErrors = state.getState().getResolverErrors(element);
						VersionConstraint[] versionErrors = helper.getUnsatisfiedConstraints(element);

						//ignore problems when they are caused by bundles not being built for the right config
						if (isConfigError(element, resolutionErrors, AbstractScriptGenerator.getConfigInfos()))
							continue;

						String errorMessage = "Bundle " + element.getSymbolicName() + ":\n" + getResolutionErrorMessage(resolutionErrors); //$NON-NLS-1$ //$NON-NLS-2$
						for (VersionConstraint versionError : versionErrors) {
							errorMessage += '\t' + getResolutionFailureMessage(versionError) + '\n';
						}
						errors.add(new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, IStatus.WARNING, errorMessage, null));
					}
				}
				if (errors.getChildren().length > 0)
					BundleHelper.getDefault().getLog().log(errors);
			}
		}
		if (!state.getState().isResolved())
			state.state.resolve(true);
		return state;
	}

	public IStatus missingPlugin(String id, String version, Feature containingFeature, boolean throwException) throws CoreException {
		BundleDescription bundle = state.getBundle(id, version, false);
		if (bundle == null) {
			String message = NLS.bind(Messages.exception_missingPlugin, id + "_" + version); //$NON-NLS-1$
			if (containingFeature != null)
				message = NLS.bind(Messages.includedFromFeature, containingFeature.getId(), message);
			IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null);
			if (throwException)
				throw new CoreException(status);
			return status;
		}

		//we expect this bundle to not be resolved, but just in case...
		if (bundle.isResolved())
			return null;

		ResolverError[] resolutionErrors = state.getState().getResolverErrors(bundle);
		return missingPlugin(bundle, resolutionErrors, containingFeature, throwException);
	}

	public static IStatus missingPlugin(BundleDescription bundle, ResolverError[] resolutionErrors, Feature containingFeature, boolean throwException) throws CoreException {
		StateHelper helper = BundleHelper.getPlatformAdmin().getStateHelper();
		VersionConstraint[] versionErrors = helper.getUnsatisfiedConstraints(bundle);

		String message = NLS.bind(Messages.exception_unresolvedPlugin, bundle.getSymbolicName() + '_' + bundle.getVersion().toString());
		if (containingFeature != null)
			message = NLS.bind(Messages.includedFromFeature, containingFeature.getId(), message);
		message += ":\n" + BuildTimeSite.getResolutionErrorMessage(resolutionErrors); //$NON-NLS-1$
		for (VersionConstraint versionError : versionErrors) {
			message += '\t' + BuildTimeSite.getResolutionFailureMessage(versionError) + '\n';
		}

		IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null);
		if (throwException)
			throw new CoreException(status);
		return status;
	}

	//Return whether the resolution error is caused because we are not building for the proper configurations.
	static public boolean isConfigError(BundleDescription bundle, ResolverError[] errors, List<Config> configs) {
		Dictionary<String, String> environment = new Hashtable<>(3);
		Filter bundleFilter = BundleHelper.getDefault().getFilter(bundle);
		if (bundleFilter != null && hasPlatformFilterError(errors) != null) {
			for (Config aConfig : configs) {
				environment.put("osgi.os", aConfig.getOs()); //$NON-NLS-1$
				environment.put("osgi.ws", aConfig.getWs()); //$NON-NLS-1$
				environment.put("osgi.arch", aConfig.getArch()); //$NON-NLS-1$
				if (bundleFilter.match(environment)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	//Check if the set of errors contain a platform filter
	static private ResolverError hasPlatformFilterError(ResolverError[] errors) {
		for (ResolverError error : errors) {
			if ((error.getType() & ResolverError.PLATFORM_FILTER) != 0)
				return error;
			if ((error.getType() & ResolverError.NO_NATIVECODE_MATCH) != 0)
				return error;
		}
		return null;
	}

	static public String getResolutionErrorMessage(ResolverError[] errors) {
		String errorMessage = ""; //$NON-NLS-1$
		for (ResolverError error : errors) {
			if ((error.getType() & (ResolverError.SINGLETON_SELECTION | ResolverError.FRAGMENT_CONFLICT | ResolverError.IMPORT_PACKAGE_USES_CONFLICT | ResolverError.REQUIRE_BUNDLE_USES_CONFLICT | ResolverError.MISSING_EXECUTION_ENVIRONMENT)) != 0)
				errorMessage += '\t' + error.toString() + '\n';
		}
		return errorMessage;
	}

	static public String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification)
			return NLS.bind(Messages.unsatisfied_import, displayVersionConstraint(unsatisfied));
		if (unsatisfied instanceof NativeCodeSpecification)
			return NLS.bind(Messages.unsatisfied_nativeSpec, unsatisfied.toString());
		if (unsatisfied instanceof BundleSpecification) {
			if (((BundleSpecification) unsatisfied).isOptional())
				return NLS.bind(Messages.unsatisfied_optionalBundle, displayVersionConstraint(unsatisfied));
			return NLS.bind(Messages.unsatisfied_required, displayVersionConstraint(unsatisfied));
		}
		return NLS.bind(Messages.unsatisfied_host, displayVersionConstraint(unsatisfied));
	}

	static private String displayVersionConstraint(VersionConstraint constraint) {
		VersionRange versionSpec = constraint.getVersionRange();
		if (versionSpec == null)
			return constraint.getName();
		return constraint.getName() + '_' + versionSpec;
	}

	public BuildTimeFeature findFeature(String featureId, String versionId, boolean throwsException) throws CoreException {
		VersionRange range = Utils.createVersionRange(versionId);
		return findFeature(featureId, range, throwsException);
	}

	private BuildTimeFeature findFeature(String featureId, VersionRange range, boolean throwsException) throws CoreException {
		if (range == null)
			range = VersionRange.emptyRange;

		if (!featuresResolved)
			resolveFeatureReferences();

		if (featureCache.containsKey(featureId)) {
			//Set is ordered highest version to lowest, return the first that matches the range
			Set<BuildTimeFeature> featureSet = featureCache.get(featureId);
			for (BuildTimeFeature feature : featureSet) {
				Version featureVersion = new Version(feature.getVersion());
				if (range.isIncluded(featureVersion)) {
					return feature;
				}
			}
		}

		if (throwsException) {
			String message = null;
			if (range.equals(VersionRange.emptyRange))
				message = NLS.bind(Messages.exception_missingFeature, featureId);
			else
				message = NLS.bind(Messages.exception_missingFeatureInRange, featureId, range);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
		}

		return null;
	}

	private void resolveFeatureReferences() {
		FeatureReference[] features = getFeatureReferences();
		for (FeatureReference feature2 : features) {
			try {
				//getting the feature for the first time will result in it being added to featureCache
				feature2.getFeature();
			} catch (CoreException e) {
				// just log the exception, but do not re-throw it - let other features to be resolved 
				String message = NLS.bind(Messages.exception_featureParse, feature2.getURL());
				IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, e);
				BundleHelper.getDefault().getLog().log(status);
			}
		}
		featuresResolved = true;
	}

	public void addFeatureReferenceModel(File featureXML) {
		URL featureURL;
		FeatureReference featureRef;
		if (featureXML.exists()) {
			// Here we could not use toURL() on currentFeatureDir, because the
			// URL has a slash after the colons (file:/c:/foo) whereas the
			// plugins don't
			// have it (file:d:/eclipse/plugins) and this causes problems later
			// to compare URLs... and compute relative paths
			try {
				featureURL = new URL("file:" + featureXML.getAbsolutePath() + '/'); //$NON-NLS-1$
				featureRef = new FeatureReference();
				featureRef.setSiteModel(this);
				featureRef.setURLString(featureURL.toExternalForm());
				addFeatureReferenceModel(featureRef);
			} catch (MalformedURLException e) {
				BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, PI_PDEBUILD, WARNING_MISSING_SOURCE, NLS.bind(Messages.warning_cannotLocateSource, featureXML.getAbsolutePath()), e));
			}
		}
	}

	public void addFeatureReferenceModel(FeatureReference featureReference) {
		if (this.featureReferences == null)
			this.featureReferences = new ArrayList<>();

		this.featureReferences.add(featureReference);
		featuresResolved = false;
	}

	private SortedSet<ReachablePlugin> findAllReferencedPlugins() throws CoreException {
		ArrayList<BuildTimeFeature> rootFeatures = new ArrayList<>();
		SortedSet<ReachablePlugin> allPlugins = new TreeSet<>();
		for (String string : rootFeaturesForFilter) {
			BuildTimeFeature correspondingFeature = findFeature(string, (String) null, true);
			if (correspondingFeature == null)
				return null;
			rootFeatures.add(correspondingFeature);
		}
		for (String string : rootPluginsForFiler) {
			allPlugins.add(new ReachablePlugin(string, ReachablePlugin.WIDEST_RANGE));
		}
		int it = 0;
		while (it < rootFeatures.size()) {
			BuildTimeFeature toAnalyse = null;
			try {
				toAnalyse = rootFeatures.get(it++);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			FeatureEntry[] includedRefs = toAnalyse.getIncludedFeatureReferences();
			for (int i = 0; i < includedRefs.length; i++) {
				String featureId = includedRefs[i].getId();
				BuildTimeFeature nested = findFeature(featureId, includedRefs[i].getVersion(), false);
				if (nested != null)
					rootFeatures.add(nested);
				else {
					// missing feature, ok if it will be a generated source feature
					Properties props = AbstractScriptGenerator.readProperties(toAnalyse.getRootLocation(), PROPERTIES_FILE, IStatus.OK);
					boolean doSourceFeatureGeneration = props.containsKey(IBuildPropertiesConstants.GENERATION_SOURCE_FEATURE_PREFIX + featureId);
					if (doSourceFeatureGeneration) {
						//generate property may add extra plugins or features
						String[] extraEntries = Utils.getArrayFromString(props.getProperty(IBuildPropertiesConstants.GENERATION_SOURCE_FEATURE_PREFIX + featureId));
						for (int j = 1; j < extraEntries.length; j++) {
							Map<String, Object> items = Utils.parseExtraBundlesString(extraEntries[j], true);
							String id = (String) items.get(Utils.EXTRA_ID);
							Version version = (Version) items.get(Utils.EXTRA_VERSION);
							if (extraEntries[j].startsWith("feature@")) { //$NON-NLS-1$
								FeatureEntry added = new FeatureEntry(id, version.toString(), false);
								FeatureEntry[] expanded = new FeatureEntry[includedRefs.length + 1];
								System.arraycopy(includedRefs, 0, expanded, 0, includedRefs.length);
								expanded[includedRefs.length] = added;
								includedRefs = expanded;
							} else if (extraEntries[j].startsWith("plugin@")) { //$NON-NLS-1$
								VersionRange range = new VersionRange(version, true, version.equals(Version.emptyVersion) ? (Version) null : version, true);
								allPlugins.add(new ReachablePlugin(id, range));
							}
						}
					} else {
						String message = NLS.bind(Messages.exception_missingFeature, featureId);
						throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
					}
				}
			}
			FeatureEntry[] entries = toAnalyse.getPluginEntries();
			for (FeatureEntry entry : entries) {
				allPlugins.add(new ReachablePlugin(entry));
			}
			FeatureEntry[] imports = toAnalyse.getImports();
			for (FeatureEntry import1 : imports) {
				if (!import1.isPlugin()) {
					rootFeatures.add(findFeature(import1.getId(), Utils.createVersionRange(import1), true));
				} else {
					allPlugins.add(new ReachablePlugin(import1));
				}
			}
		}
		return allPlugins;
	}

	public void setFilter(boolean filter) {
		this.filter = filter;
	}

	public void setRootFeaturesForFilter(List<String> rootFeaturesForFilter) {
		this.rootFeaturesForFilter = rootFeaturesForFilter;
	}

	public void setRootPluginsForFiler(List<String> rootPluginsForFiler) {
		this.rootPluginsForFiler = rootPluginsForFiler;
	}

	public FeatureReference[] getFeatureReferences() {
		return getRawFeatureReferences();

	}

	public FeatureReference[] getRawFeatureReferences() {
		if (featureReferences == null || featureReferences.size() == 0)
			return new FeatureReference[0];
		return featureReferences.toArray(new FeatureReference[featureReferences.size()]);
	}

	public void addPluginEntry(FeatureEntry pluginEntry) {
		// TODO Auto-generated method stub
	}

	public Feature createFeature(URL url) throws CoreException {
		BuildTimeFeature feature = featureURLCache.get(url);
		if (feature != null)
			return feature;

		feature = factory.createFeature(url, this);
		feature.setFeatureContentProvider(getSiteContentProvider());
		featureURLCache.put(url, feature);

		if (featureCache.containsKey(feature.getId())) {
			Set<BuildTimeFeature> set = featureCache.get(feature.getId());
			set.add(feature);
		} else {
			TreeSet<BuildTimeFeature> set = new TreeSet<>(featureComparator);
			set.add(feature);
			featureCache.put(feature.getId(), set);
		}

		return feature;
	}

	public BuildTimeSiteContentProvider getSiteContentProvider() {
		return contentProvider;
	}

	public void setSiteContentProvider(BuildTimeSiteContentProvider siteContentProvider) {
		this.contentProvider = siteContentProvider;
	}

	public void setEESources(String[] eeSources) {
		this.eeSources = eeSources;
	}
}
