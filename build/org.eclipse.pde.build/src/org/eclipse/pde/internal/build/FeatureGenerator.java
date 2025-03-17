/*******************************************************************************
 * Copyright (c) 2006, 2024 IBM Corporation and others.
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
package org.eclipse.pde.internal.build;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeSite;
import org.eclipse.pde.internal.build.site.PDEState;
import org.osgi.framework.Filter;
import org.osgi.framework.Version;

public class FeatureGenerator extends AbstractScriptGenerator {

	private static class Entry {
		private final String id;
		private String version = "0.0.0"; //$NON-NLS-1$
		private Map<String, String> attributes;

		public Entry(String id) {
			this.id = id;
		}

		public Entry(String id, String version) {
			this.id = id;
			this.version = version;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Entry objEntry) {
				if (!(id.equals(((Entry) obj).id) && version.equals(objEntry.version))) {
					return false;
				}
				return getAttributes().equals(objEntry.getAttributes());
			}

			return false;
		}

		@Override
		public int hashCode() {
			return id.hashCode() + version.hashCode() + getAttributes().hashCode();
		}

		public Map<String, String> getAttributes() {
			if (attributes != null) {
				return attributes;
			}
			return Collections.emptyMap();
		}

		public void addAttribute(String key, String value) {
			if (VERSION.equals(key)) {
				if (value != null && value.length() > 0) {
					version = value;
				}
				return;
			}
			if (attributes == null) {
				attributes = new LinkedHashMap<>();
			}
			attributes.put(key, value);
		}

		public String getId() {
			return id;
		}

		public String getVersion() {
			return version;
		}

		@Override
		public String toString() {
			return id + '_' + version;
		}
	}

	private String featureId = null;
	private String version = null;
	private String nestedInclusions = null;
	private String productFile = null;
	private String[] pluginList = null;
	private String[] fragmentList = null;
	private String[] featureList = null;

	private boolean includeLaunchers = true;

	private ProductFile product = null;

	private boolean verify = false;

	private Properties antProperties;
	private Properties buildProperties;

	private static Set<Entry> createSet(String[] contents) {
		if (contents == null) {
			return new LinkedHashSet<>(0);
		}
		Set<Entry> result = new LinkedHashSet<>(contents.length);
		for (String content : contents) {
			if (content != null) {
				StringTokenizer tokenizer = new StringTokenizer(content, ";"); //$NON-NLS-1$
				Entry entry = new Entry(tokenizer.nextToken());
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					int idx = token.indexOf('=');
					if (idx != -1) {
						String value = token.substring(idx + 1, token.length()).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) { //$NON-NLS-1$ //$NON-NLS-2$
							value = value.substring(1, value.length() - 1); //trim off " because FeatureWriter adds them
						}
						entry.addAttribute(token.substring(0, idx), value);
					}
				}
				result.add(entry);
			}
		}
		return result;
	}

	@Override
	public void generate() throws CoreException {
		AbstractScriptGenerator.setStaticAntProperties(antProperties);
		try {
			initialize();

			Set<Entry> plugins = null;
			Set<Entry> features = null;
			Set<Entry> fragments = null;
			if (shouldNestInclusions()) {
				features = createSet(new String[] {generateNestedRequirements()});
				fragments = new LinkedHashSet<>();
				plugins = new LinkedHashSet<>();
			} else {
				plugins = createSet(pluginList);
				features = createSet(featureList);
				fragments = createSet(fragmentList);
			}

			if (product != null) {
				List<FeatureEntry> entries = product.getProductEntries();
				for (FeatureEntry featureEntry : entries) {
					Entry newEntry = new Entry(featureEntry.getId(), featureEntry.getVersion());
					if (featureEntry.unpackSet()) {
						newEntry.addAttribute(Utils.EXTRA_UNPACK, String.valueOf(featureEntry.isUnpack()));
					}
					if (featureEntry.isFragment()) {
						fragments.add(newEntry);
					} else if (featureEntry.isPlugin()) {
						plugins.add(newEntry);
					} else {
						features.add(newEntry);
					}
				}
			}
			try {
				createFeature(featureId, plugins, fragments, features);
			} catch (FileNotFoundException e) {
				IStatus status = new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.error_creatingFeature, e.getLocalizedMessage()), e);
				throw new CoreException(status);
			}
		} finally {
			AbstractScriptGenerator.setStaticAntProperties(null);
		}
	}

	private boolean shouldNestInclusions() {
		if (nestedInclusions == null || nestedInclusions.equalsIgnoreCase(FALSE)) {
			return false;
		}

		if (product != null) {
			//will need to generate a .eclipseproduct file
			if (buildProperties == null) {
				buildProperties = new Properties();
			}
			buildProperties.put(IBuildPropertiesConstants.PROPERTY_GENERATE_ECLIPSEPRODUCT, TRUE);
		}

		//make sure there's actually something to nest
		if ((pluginList == null || pluginList.length == 0) && (fragmentList == null || fragmentList.length == 0) && (featureList == null || featureList.length == 0) && (buildProperties == null || buildProperties.size() == 0)) {
			return false;
		}

		// use the product-id to generate a name if nestedRequirements==true
		if (nestedInclusions.equalsIgnoreCase(TRUE)) {
			return product != null;
		}

		//else nestedRequirements specifies the name to use for the nested feature
		return true;
	}

	private String generateNestedRequirements() throws CoreException {
		String nestedId = null;
		String nestedVersion = null;
		String productKey = null;
		if (product != null) {
			nestedId = product.getId() + ".root.feature"; //$NON-NLS-1$
			nestedVersion = product.getVersion();
			productKey = PRODUCT_PREFIX + product.getId();
			if (!buildProperties.containsKey(PROPERTY_GENERATED_FEATURE_LABEL) && product.getProductName() != null) {
				buildProperties.put(PROPERTY_GENERATED_FEATURE_LABEL, product.getProductName() + " Root Files"); //$NON-NLS-1$
			}
		} else {
			nestedId = nestedInclusions;
			nestedVersion = version != null ? version : "1.0.0.qualifier"; //$NON-NLS-1$
		}

		String extraRequires = null;
		if (buildProperties != null && productKey != null) {
			extraRequires = (String) buildProperties.remove(productKey);
		}

		FeatureGenerator generator = new FeatureGenerator();
		generator.setVerify(verify);
		generator.setPluginList(pluginList);
		generator.setFeatureList(featureList);
		generator.setBuildProperties(buildProperties);
		generator.setIncludeLaunchers(false);
		generator.setBuildSiteFactory(siteFactory);
		generator.setFeatureId(nestedId);
		generator.setVersion(nestedVersion);
		generator.setPluginPath(pluginPath);
		generator.generate();

		//get the siteFactory back from the nested generator so we don't need to recreate it
		if (siteFactory == null) {
			siteFactory = generator.siteFactory;
		}

		if (productKey != null) {
			buildProperties = new Properties();
			extraRequires = (extraRequires == null) ? "" : extraRequires + ","; //$NON-NLS-1$ //$NON-NLS-2$
			extraRequires += "feature@" + nestedId + ";version=" + nestedVersion; //$NON-NLS-1$ //$NON-NLS-2$
			buildProperties.put(productKey, extraRequires);
		} else {
			buildProperties = null;
		}

		return nestedId + ";version=" + nestedVersion; //$NON-NLS-1$
	}

	public void setProductFile(String productFile) {
		this.productFile = productFile;
	}

	public void setPluginList(String[] pluginList) {
		this.pluginList = pluginList;
	}

	public void setFeatureList(String[] featureList) {
		this.featureList = featureList;
	}

	public void setFragmentList(String[] fragmentList) {
		this.fragmentList = fragmentList;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setIncludeLaunchers(boolean includeLaunchers) {
		this.includeLaunchers = includeLaunchers;
	}

	private void initialize() throws CoreException {
		//get rid of old feature that we will be overwriting, we don't want it in the state accidently.
		File dir = new File(getWorkingDirectory(), IPDEBuildConstants.DEFAULT_FEATURE_LOCATION + '/' + featureId);
		File xml = new File(dir, Constants.FEATURE_FILENAME_DESCRIPTOR);
		if (xml.exists()) {
			xml.delete();
		}

		product = loadProduct(productFile);
	}

	/*
	 * Based on the version of OSGi that we have in our state, add the appropriate plug-ins/fragments/features
	 * for the launcher.
	 */
	private void addLauncher(PDEState state, Set<Entry> plugins, Set<Entry> fragments, Set<Entry> features) {
		BundleDescription bundle = state.getResolvedBundle(BUNDLE_OSGI);
		if (bundle == null) {
			return;
		}
		Version osgiVersion = bundle.getVersion();
		if (osgiVersion.compareTo(new Version("3.3")) < 0) { //$NON-NLS-1$
			// we have an OSGi version that is less than 3.3 so add the old launcher
			if (!contains(features, FEATURE_PLATFORM_LAUNCHERS)) {
				features.add(new Entry(FEATURE_PLATFORM_LAUNCHERS));
			}
		} else {
			// we have OSGi version 3.3 or greater so add the executable feature
			// and the launcher plug-in and fragments
			BuildTimeFeature executableFeature = null;
			try {
				executableFeature = getSite(false).findFeature(FEATURE_EQUINOX_EXECUTABLE, null, false);
			} catch (CoreException e) {
				BundleHelper.getDefault().getLog().log(e.getStatus());
			}
			if (executableFeature != null) {
				/* the executable feature includes the launcher and fragments already */
				if (!contains(features, FEATURE_EQUINOX_EXECUTABLE)) {
					features.add(new Entry(FEATURE_EQUINOX_EXECUTABLE));
				}
			} else {
				// We don't have the executable feature, at least try and get the launcher jar and fragments 
				plugins.add(new Entry(BUNDLE_EQUINOX_LAUNCHER));
				List<Config> configs = getConfigInfos();
				// only include the fragments for the platforms we are attempting to build, since the others
				// probably aren't around
				for (Config config : configs) {
					String fragment = BUNDLE_EQUINOX_LAUNCHER + '.' + config.getWs() + '.' + config.getOs() + '.' + config.getArch();
					if (!contains(fragments, fragment)) {
						Entry entry = new Entry(fragment);
						entry.addAttribute("unpack", "true"); //$NON-NLS-1$//$NON-NLS-2$
						fragments.add(entry);
					}
				}
			}
		}
	}

	private boolean contains(Set<Entry> features, String id) {
		return features.stream().anyMatch(e -> Objects.equals(e.getId(), id));
	}

	/**
	 * Generate a feature that includes the given plug-ins, fragments and features.
	 * Feature order matters at compile time if there is dependencies between the features' contents. 
	 * Make sure to pass an ordered set if this matters.
	 * @param feature - Name of the feature to generate
	 * @param plugins - plug-ins to include
	 * @param fragments - fragments to include
	 * @param features - An ordered set of features to include
	 */
	protected void createFeature(String feature, Set<Entry> plugins, Set<Entry> fragments, Set<Entry> features) throws CoreException, FileNotFoundException {
		String location = IPDEBuildConstants.DEFAULT_FEATURE_LOCATION + '/' + feature;
		File directory = new File(getWorkingDirectory(), location);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		PDEState state = verify ? getSite(false).getRegistry() : null;
		BundleHelper helper = BundleHelper.getDefault();

		if (verify && includeLaunchers) {
			addLauncher(state, plugins, fragments, features);
		}

		String featureName = buildProperties != null ? (String) buildProperties.get(PROPERTY_GENERATED_FEATURE_LABEL) : null;

		//Create feature.xml
		File file = new File(directory, Constants.FEATURE_FILENAME_DESCRIPTOR);
		try (XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(file)))) {
			Map<String, String> parameters = new LinkedHashMap<>();
			Dictionary<String, String> environment = new Hashtable<>(3);

			parameters.put(ID, feature);
			parameters.put(VERSION, version != null ? version : "1.0.0"); //$NON-NLS-1$ 
			if (featureName != null) {
				parameters.put(LABEL, featureName);
			}
			writer.startTag(FEATURE, parameters, true);

			boolean fragment = false;
			List<Config> configs = new ArrayList<>(getConfigInfos());
			//we do the generic config first as a special case
			configs.remove(Config.genericConfig());
			Iterator<Config> configIterator = configs.iterator();
			Iterator<Entry> listIter = plugins.iterator();
			if (!listIter.hasNext()) {
				// no plugins, do fragments
				fragment = true;
				listIter = fragments.iterator();
			}
			for (Config currentConfig = Config.genericConfig(); currentConfig != null; currentConfig = configIterator.next()) {
				environment.put("osgi.os", currentConfig.getOs()); //$NON-NLS-1$
				environment.put("osgi.ws", currentConfig.getWs()); //$NON-NLS-1$
				environment.put("osgi.arch", currentConfig.getArch()); //$NON-NLS-1$
				for (; listIter.hasNext();) {
					Entry entry = listIter.next();
					String name = entry.getId();
					String bundleVersion = entry.getVersion();
					boolean guessedUnpack = false;
					boolean writeBundle = !verify;
					if (verify) {
						BundleDescription bundle = state.getResolvedBundle(name, bundleVersion);
						if (bundle != null) {
							//Bundle resolved, write it out if it matches the current config
							Filter filter = helper.getFilter(bundle);
							if (filter == null || filter.match(environment)) {
								writeBundle = true;
								guessedUnpack = Utils.guessUnpack(bundle, state.getExtraData().get(Long.valueOf(bundle.getBundleId())));
								if (currentConfig.equals(Config.genericConfig())) {
									listIter.remove();
								}
							}
						} else {
							//Bundle did not resolve, only ok if it was because of the platform filter
							if (bundleVersion != null) {
								bundle = state.getBundle(name, bundleVersion, false);
							} else {
								//There are no resolved bundles with this name, if there is more than one unresolved just use the first
								BundleDescription[] bundles = state.getState().getBundles(name);
								bundle = (bundles != null && bundles.length > 0) ? bundles[0] : null;
							}
							if (bundle != null) {
								ResolverError[] errors = state.getState().getResolverErrors(bundle);
								//ok if we didn't match the config
								if (!BuildTimeSite.isConfigError(bundle, errors, configs)) {
									BuildTimeSite.missingPlugin(bundle, errors, null, true); //throws CoreException
								}
							} else {
								//throw error
								String message = NLS.bind(Messages.exception_missingPlugin, bundleVersion != null ? name + "_" + bundleVersion : name); //$NON-NLS-1$
								throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
							}
						}
					}

					if (writeBundle) {
						parameters.clear();

						parameters.put(ID, name);
						parameters.put(VERSION, bundleVersion);
						parameters.put("unpack", guessedUnpack ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						if (!currentConfig.equals(Config.genericConfig())) {
							parameters.put("os", currentConfig.getOs()); //$NON-NLS-1$
							parameters.put("ws", currentConfig.getWs()); //$NON-NLS-1$
							parameters.put("arch", currentConfig.getArch()); //$NON-NLS-1$
						}
						if (fragment) {
							parameters.put(FRAGMENT, "true"); //$NON-NLS-1$
						}

						//add the attributes from the entry, these override values we set above
						parameters.putAll(entry.getAttributes());

						writer.printTag(PLUGIN, parameters, true, true, true);
					}

					if (!fragment && !listIter.hasNext() && fragments.size() > 0) {
						//finished the list of plugins, do the fragments now
						fragment = true;
						listIter = fragments.iterator();
					}
				}
				if (!verify || !configIterator.hasNext()) {
					break;
				} else if (plugins.size() > 0) {
					fragment = false;
					listIter = plugins.iterator();
				} else {
					listIter = fragments.iterator();
				}
			}

			for (Entry entry : features) {
				String name = entry.getId();
				String featureVersion = entry.getVersion();
				if (verify) {
					//this will throw an exception if the feature is not found.
					boolean exception = true;
					if (buildProperties != null && buildProperties.containsKey("generate.feature@" + name)) { //$NON-NLS-1$
						exception = false;
					}
					getSite(false).findFeature(name, featureVersion, exception);
				}
				parameters.clear();
				parameters.put(ID, name);
				parameters.put(VERSION, featureVersion);
				parameters.putAll(entry.getAttributes());
				writer.printTag("includes", parameters, true, true, true); //$NON-NLS-1$
			}
			writer.endTag(FEATURE);
		}

		createBuildProperties(directory);
		getSite(false).addFeatureReferenceModel(directory);
	}

	protected void createBuildProperties(File directory) {
		File file;
		//create build.properties
		file = new File(directory, IPDEBuildConstants.PROPERTIES_FILE);
		if (buildProperties == null) {
			buildProperties = new Properties();
			buildProperties.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
			buildProperties.store(stream, ""); //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
			// nothing for now
		}
	}

	public void setBuildProperties(String file) {
		buildProperties = new Properties();

		File propertiesFile = new File(file);
		if (propertiesFile.exists()) {
			try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
				buildProperties.load(input);
			} catch (IOException e) {
				// nothing
			}
		}
	}

	public void setBuildProperties(Properties properties) {
		this.buildProperties = properties;
	}

	public void setVerify(boolean verify) {
		this.verify = verify;
		reportResolutionErrors = verify;
	}

	public void setImmutableAntProperties(Properties properties) {
		antProperties = properties;
	}

	public void setNestedInclusions(String nested) {
		this.nestedInclusions = nested;
	}
}
