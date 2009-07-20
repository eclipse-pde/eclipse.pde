/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.PDEState;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;
import org.osgi.framework.Version;

public class FeatureGenerator extends AbstractScriptGenerator {

	private static class Entry {
		private final String id;
		private String version = "0.0.0"; //$NON-NLS-1$
		private Map attributes;

		public Entry(String id) {
			this.id = id;
		}

		public Entry(String id, String version) {
			this.id = id;
			this.version = version;
		}

		public boolean equals(Object obj) {
			if (obj instanceof Entry) {
				Entry objEntry = (Entry) obj;
				return id.equals(((Entry) obj).id) && version.equals(objEntry.version);
			}

			return id.equals(obj);
		}

		public int hashCode() {
			return id.hashCode() + version.hashCode();
		}

		public Map getAttributes() {
			if (attributes != null)
				return attributes;
			return Collections.EMPTY_MAP;
		}

		public void addAttribute(String key, String value) {
			if (VERSION.equals(key)) {
				if (value != null && value.length() > 0)
					version = value;
				return;
			}
			if (attributes == null)
				attributes = new LinkedHashMap();
			attributes.put(key, value);
		}

		public String getId() {
			return id;
		}

		public String getVersion() {
			return version;
		}

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

	private static Set createSet(String[] contents) {
		if (contents == null)
			return new LinkedHashSet(0);
		Set result = new LinkedHashSet(contents.length);
		for (int i = 0; i < contents.length; i++)
			if (contents[i] != null) {
				StringTokenizer tokenizer = new StringTokenizer(contents[i], ";"); //$NON-NLS-1$
				Entry entry = new Entry(tokenizer.nextToken());
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					int idx = token.indexOf('=');
					if (idx != -1) {
						String value = token.substring(idx + 1, token.length()).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
							value = value.substring(1, value.length() - 1); //trim off " because FeatureWriter adds them
						entry.addAttribute(token.substring(0, idx), value);
					}
				}
				result.add(entry);
			}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		AbstractScriptGenerator.setStaticAntProperties(antProperties);
		try {
			initialize();

			Set plugins = null;
			Set features = null;
			Set fragments = null;
			if (shouldNestInclusions()) {
				features = createSet(new String[] {generateNestedRequirements()});
				fragments = new LinkedHashSet();
				plugins = new LinkedHashSet();
			} else {
				plugins = createSet(pluginList);
				features = createSet(featureList);
				fragments = createSet(fragmentList);
			}

			if (product != null) {
				List entries = product.getProductEntries();
				for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
					FeatureEntry featureEntry = (FeatureEntry) iterator.next();
					Entry newEntry = new Entry(featureEntry.getId(), featureEntry.getVersion());
					if (featureEntry.unpackSet())
						newEntry.addAttribute(Utils.EXTRA_UNPACK, String.valueOf(featureEntry.isUnpack()));
					if (featureEntry.isFragment())
						fragments.add(newEntry);
					else if (featureEntry.isPlugin())
						plugins.add(newEntry);
					else
						features.add(newEntry);
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
		if (nestedInclusions == null || nestedInclusions.equalsIgnoreCase(FALSE))
			return false;

		//make sure there's actually something to nest
		if ((pluginList == null || pluginList.length == 0) && (fragmentList == null || fragmentList.length == 0) && (featureList == null || featureList.length == 0) && (buildProperties != null || buildProperties.size() == 0))
			return false;

		// use the product-id to generate a name if nestedRequirements==true
		if (nestedInclusions.equalsIgnoreCase(TRUE))
			return product != null;

		//else nestedRequirements specifies the name to use for the nested feature
		return true;
	}

	private String generateNestedRequirements() throws CoreException {
		String nestedId = null;
		String nestedVersion = null;
		String productKey = null;
		if (product != null) {
			nestedId = product.getProductId() + ".root.feature"; //$NON-NLS-1$
			nestedVersion = product.getVersion();
			productKey = PRODUCT_PREFIX + product.getProductId();
		} else {
			nestedId = nestedInclusions;
			nestedVersion = version != null ? version : "1.0.0.qualifier"; //$NON-NLS-1$
		}

		String extraRequires = null;
		if (buildProperties != null && productKey != null)
			extraRequires = (String) buildProperties.remove(productKey);

		FeatureGenerator generator = new FeatureGenerator();
		generator.setVerify(verify);
		generator.setPluginList(pluginList);
		generator.setFeatureList(featureList);
		generator.setBuildProperties(buildProperties);
		generator.setIncludeLaunchers(false);
		generator.setBuildSiteFactory(siteFactory);
		generator.setFeatureId(nestedId);
		generator.setVersion(nestedVersion);
		generator.generate();

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
	private void addLauncher(PDEState state, Set plugins, Set fragments, Set features) {
		BundleDescription bundle = state.getResolvedBundle(BUNDLE_OSGI);
		if (bundle == null)
			return;
		Version osgiVersion = bundle.getVersion();
		if (osgiVersion.compareTo(new Version("3.3")) < 0) { //$NON-NLS-1$
			// we have an OSGi version that is less than 3.3 so add the old launcher
			if (!features.contains(FEATURE_PLATFORM_LAUNCHERS))
				features.add(new Entry(FEATURE_PLATFORM_LAUNCHERS));
		} else {
			// we have OSGi version 3.3 or greater so add the executable feature
			// and the launcher plug-in and fragments
			BuildTimeFeature executableFeature = null;
			try {
				executableFeature = getSite(false).findFeature(FEATURE_EQUINOX_EXECUTABLE, null, false);
			} catch (CoreException e) {
				// ignore
			}
			if (executableFeature != null) {
				/* the executable feature includes the launcher and fragments already */
				if (!features.contains(FEATURE_EQUINOX_EXECUTABLE))
					features.add(new Entry(FEATURE_EQUINOX_EXECUTABLE));
			} else {
				// We don't have the executable feature, at least try and get the launcher jar and fragments 
				plugins.add(new Entry(BUNDLE_EQUINOX_LAUNCHER));
				List configs = getConfigInfos();
				// only include the fragments for the platforms we are attempting to build, since the others
				// probably aren't around
				for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
					Config config = (Config) iterator.next();
					String fragment = BUNDLE_EQUINOX_LAUNCHER + '.' + config.getWs() + '.' + config.getOs();
					//macosx doesn't have the arch on its fragment 
					if (config.getOs().compareToIgnoreCase("macosx") != 0 || config.getArch().equals("x86_64")) //$NON-NLS-1$ //$NON-NLS-2$
						fragment += '.' + config.getArch();

					if (!fragments.contains(fragment)) {
						Entry entry = new Entry(fragment);
						entry.addAttribute("unpack", "true"); //$NON-NLS-1$//$NON-NLS-2$
						fragments.add(entry);
					}
				}
			}
		}
	}

	/**
	 * Generate a feature that includes the given plug-ins, fragments and features.
	 * Feature order matters at compile time if there is dependencies between the features' contents. 
	 * Make sure to pass an ordered set if this matters.
	 * @param feature - Name of the feature to generate
	 * @param plugins - plug-ins to include
	 * @param fragments - fragments to include
	 * @param features - An ordered set of features to include
	 * @throws CoreException
	 * @throws FileNotFoundException
	 */
	protected void createFeature(String feature, Set plugins, Set fragments, Set features) throws CoreException, FileNotFoundException {
		String location = IPDEBuildConstants.DEFAULT_FEATURE_LOCATION + '/' + feature;
		File directory = new File(getWorkingDirectory(), location);
		if (!directory.exists())
			directory.mkdirs();

		PDEState state = verify ? getSite(false).getRegistry() : null;
		BundleHelper helper = BundleHelper.getDefault();

		if (verify && includeLaunchers)
			addLauncher(state, plugins, fragments, features);

		//Create feature.xml
		File file = new File(directory, Constants.FEATURE_FILENAME_DESCRIPTOR);
		OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
		XMLWriter writer = null;
		try {
			writer = new XMLWriter(output);
		} catch (UnsupportedEncodingException e) {
			//should not happen
			return;
		}
		try {
			Map parameters = new LinkedHashMap();
			Dictionary environment = new Hashtable(3);

			parameters.put(ID, feature);
			parameters.put(VERSION, version != null ? version : "1.0.0"); //$NON-NLS-1$ 
			writer.startTag(FEATURE, parameters, true);

			boolean fragment = false;
			List configs = getConfigInfos();
			//we do the generic config first as a special case
			configs.remove(Config.genericConfig());
			Iterator configIterator = configs.iterator();
			Iterator listIter = plugins.iterator();
			if (!listIter.hasNext()) {
				// no plugins, do fragments
				fragment = true;
				listIter = fragments.iterator();
			}
			for (Config currentConfig = Config.genericConfig(); currentConfig != null; currentConfig = (Config) configIterator.next()) {
				environment.put("osgi.os", currentConfig.getOs()); //$NON-NLS-1$
				environment.put("osgi.ws", currentConfig.getWs()); //$NON-NLS-1$
				environment.put("osgi.arch", currentConfig.getArch()); //$NON-NLS-1$
				for (; listIter.hasNext();) {
					Entry entry = (Entry) listIter.next();
					String name = entry.getId();
					String bundleVersion = entry.getVersion();
					boolean guessedUnpack = true;
					boolean writeBundle = !verify;
					if (verify) {
						BundleDescription bundle = state.getResolvedBundle(name, bundleVersion);
						if (bundle != null) {
							//Bundle resolved, write it out if it matches the current config
							String filterSpec = bundle.getPlatformFilter();
							if (filterSpec == null || helper.createFilter(filterSpec).match(environment)) {
								writeBundle = true;
								guessedUnpack = Utils.guessUnpack(bundle, (String[]) state.getExtraData().get(new Long(bundle.getBundleId())));
								if (currentConfig.equals(Config.genericConfig())) {
									listIter.remove();
								}
							}
						} else {
							//Bundle did not resolve, only ok if it was because of the platform filter
							BundleDescription[] bundles = state.getState().getBundles(name);
							boolean error = true;
							if (bundles != null && bundles.length > 0) {
								ResolverError[] errors = state.getState().getResolverErrors(bundles[0]);
								for (int i = 0; i < errors.length; i++) {
									if ((errors[i].getType() & ResolverError.PLATFORM_FILTER) != 0) {
										//didn't match config, this is ok
										error = false;
										break;
									}
								}
							}
							if (error) {
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
						if (fragment)
							parameters.put(FRAGMENT, "true"); //$NON-NLS-1$

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

			for (Iterator iter = features.iterator(); iter.hasNext();) {
				Entry entry = (Entry) iter.next();
				String name = entry.getId();
				String featureVersion = entry.getVersion();
				if (verify) {
					//this will throw an exception if the feature is not found.
					getSite(false).findFeature(name, featureVersion, true);
				}
				parameters.clear();
				parameters.put(ID, name);
				parameters.put(VERSION, featureVersion);
				parameters.putAll(entry.getAttributes());
				writer.printTag("includes", parameters, true, true, true); //$NON-NLS-1$
			}
			writer.endTag(FEATURE);
		} finally {
			writer.close();
		}

		createBuildProperties(directory);
	}

	protected void createBuildProperties(File directory) {
		File file;
		//create build.properties
		file = new File(directory, IPDEBuildConstants.PROPERTIES_FILE);
		if (buildProperties == null) {
			buildProperties = new Properties();
			buildProperties.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		OutputStream stream = null;
		try {
			stream = new BufferedOutputStream(new FileOutputStream(file));
			buildProperties.store(stream, ""); //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
			// nothing for now
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e1) {
					// nothing
				}
			}
		}
	}

	public void setBuildProperties(String file) {
		buildProperties = new Properties();

		File propertiesFile = new File(file);
		if (propertiesFile.exists()) {
			try {
				InputStream input = new BufferedInputStream(new FileInputStream(file));
				try {
					buildProperties.load(input);
				} finally {
					input.close();
				}
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
