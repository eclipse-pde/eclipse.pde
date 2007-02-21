/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.site.PDEState;
import org.eclipse.update.core.IFeature;
import org.osgi.framework.Version;

public class FeatureGenerator extends AbstractScriptGenerator {

	private static final String FEATURE_PLATFORM_LAUNCHERS = "org.eclipse.platform.launchers"; //$NON-NLS-1$
	private static final String FEATURE_EXECUTABLE = "org.eclipse.equinox.executable"; //$NON-NLS-1$
	private static final String BUNDLE_OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
	private static final String BUNDLE_LAUNCHER = "org.eclipse.equinox.launcher"; //$NON-NLS-1$

	private String featureId = null;
	private String productFile = null;
	private String[] pluginList = null;
	private String [] fragmentList = null;
	private String[] featureList = null;
	
	private boolean includeLaunchers = true;

	private ProductFile product = null;

	private boolean verify = false;
	
	/*
	 * Create and return a new Set with the given contents. If the arg
	 * is null then return an empty set.
	 */
	private static Set createSet(String[] contents) {
		if (contents == null)
			return new HashSet(0);
		Set result = new HashSet(contents.length);
		for (int i = 0; i < contents.length; i++)
			if (contents[i] != null)
				result.add(contents[i]);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		initialize();

		Set plugins = createSet(pluginList);
		Set features = createSet(featureList);
		Set fragments = createSet(fragmentList);
		if (product != null) {
			if(product.useFeatures()) {
				features.addAll(product.getFeatures());
			} else {
				plugins.addAll(product.getPlugins(false));
				fragments.addAll(product.getFragments());
			}
		}
		try {
			createFeature(featureId, plugins, fragments, features);
		} catch (FileNotFoundException e) {
			IStatus status = new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.error_creatingFeature, e.getLocalizedMessage()), e);
			throw new CoreException(status);
		}
	}

	public void setProductFile(String productFile) {
		this.productFile  = productFile;
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

		if (productFile != null && !productFile.startsWith("${") && productFile.length() > 0) { //$NON-NLS-1$
			String productPath = findFile(productFile, false);
			File f = null;
			if (productPath != null) {
				f = new File(productPath);
			} else {
				// couldn't find productFile, try it as a path directly
				f = new File(productFile);
				if (!f.exists() || !f.isFile()) {
					// doesn't exist, try it as a path relative to the working directory
					f = new File(getWorkingDirectory(), productFile);
					if (!f.exists() || !f.isFile()) {
						f = new File(getWorkingDirectory() + "/" + DEFAULT_PLUGIN_LOCATION, productFile); //$NON-NLS-1$
					}
				}
			} 
			if (f.exists() && f.isFile()) {
				product = new ProductFile(f.getAbsolutePath(), null);
			} else {
				IStatus error = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FILE, NLS.bind(Messages.exception_missingElement, productFile), null);
				throw new CoreException(error);
			}
		}
	}

	/*
	 * Based on the version of OSGi that we have in our state, add the appropriate plug-ins/fragments/features
	 * for the launcher.
	 */
	private void addLauncher(PDEState state, Set plugins, Set fragments, Set features) {
		BundleDescription bundle = state.getResolvedBundle(BUNDLE_OSGI);
		if (bundle == null)
			return;
		Version version = bundle.getVersion();
		if (version.compareTo(new Version("3.3")) < 0) { //$NON-NLS-1$
			// we have an OSGi version that is less than 3.3 so add the old launcher
			features.add(FEATURE_PLATFORM_LAUNCHERS);
		} else {
			// we have OSGi version 3.3 or greater so add the executable feature
			// and the launcher plug-in and fragments
			IFeature executableFeature = null;
			try {
				 executableFeature = getSite(false).findFeature(FEATURE_EXECUTABLE, null, false);
			} catch (CoreException e) {
				// ignore
			}
			if (executableFeature != null ) {
				/* the executable feature includes the launcher and fragments already */
				features.add(FEATURE_EXECUTABLE);
			} else {
				// We don't have the executable feature, at least try and get the launcher jar and fragments 
				plugins.add(BUNDLE_LAUNCHER);
				List configs = getConfigInfos();
				// only include the fragments for the platforms we are attempting to build, since the others
				// probably aren't around
				for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
					Config config = (Config) iterator.next();
					String fragment = BUNDLE_LAUNCHER + '.' + config.getWs() + '.' + config.getOs();
					//macosx doesn't have the arch on its fragment 
					if (config.getOs().compareToIgnoreCase("macosx") != 0) //$NON-NLS-1$
						fragment +=  '.' + config.getArch();
					
					fragments.add(fragment);
				}
			}
		}
	}

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
			Map parameters = new HashMap();
			Dictionary environment = new Hashtable(3);
	
			parameters.put("id", feature); //$NON-NLS-1$
			parameters.put("version", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.startTag("feature", parameters, true); //$NON-NLS-1$
	
			boolean fragment = false;
			List configs = getConfigInfos();
			//we do the generic config first as a special case
			configs.remove(Config.genericConfig());
			Iterator configIterator = configs.iterator();
			Iterator listIter = plugins.iterator();
			if(!listIter.hasNext()) {
				// no plugins, do fragments
				fragment = true;
				listIter = fragments.iterator();
			}
			for (	Config currentConfig = Config.genericConfig(); currentConfig != null; currentConfig = (Config) configIterator.next()) {
				environment.put("osgi.os", currentConfig.getOs()); //$NON-NLS-1$
				environment.put("osgi.ws", currentConfig.getWs()); //$NON-NLS-1$
				environment.put("osgi.arch", currentConfig.getArch()); //$NON-NLS-1$
				for (; listIter.hasNext();) {
					String name = (String) listIter.next();
					boolean unpack = true;
					boolean writeBundle = !verify;
					if (verify) {
						BundleDescription bundle = state.getResolvedBundle(name);
						if (bundle != null) {
							//Bundle resolved, write it out if it matches the current config
							String filterSpec = bundle.getPlatformFilter();
							if (filterSpec == null || helper.createFilter(filterSpec).match(environment)) {
								writeBundle = true;
								unpack = guessUnpack(bundle, (String[]) state.getExtraData().get(new Long(bundle.getBundleId())));
								if(currentConfig.equals(Config.genericConfig())){
									listIter.remove();
								}
							} 
						} else {
							//Bundle did not resolve, only ok if it was because of the platform filter
							BundleDescription [] bundles = state.getState().getBundles(name);
							boolean error = true;
							if(bundles != null && bundles.length > 0){	
								ResolverError[] errors = state.getState().getResolverErrors(bundles[0]);
								for (int i = 0; i < errors.length; i++) {
									if((errors[i].getType() & ResolverError.PLATFORM_FILTER) != 0){
										//didn't match config, this is ok
										error = false;
										break;
									} 
								}
							}
							if(error) {
								//throw error
								String message = NLS.bind(Messages.exception_missingPlugin, name);
								throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
							}
						}
					}
	
					if(writeBundle) {
						parameters.clear();
						parameters.put("id", name); //$NON-NLS-1$
						parameters.put("version", "0.0.0"); //$NON-NLS-1$//$NON-NLS-2$
						parameters.put("unpack", unpack ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						if(!currentConfig.equals(Config.genericConfig())){
							parameters.put("os", currentConfig.getOs()); //$NON-NLS-1$
							parameters.put("ws", currentConfig.getWs()); //$NON-NLS-1$
							parameters.put("arch", currentConfig.getArch()); //$NON-NLS-1$
						}
						if (fragment)
							parameters.put("fragment", "true"); //$NON-NLS-1$ //$NON-NLS-2$
						writer.printTag("plugin", parameters, true, true, true); //$NON-NLS-1$
					}
					
					if (!fragment && !listIter.hasNext() && fragments.size() > 0) {
						//finished the list of plugins, do the fragments now
						fragment = true;
						listIter = fragments.iterator();
					}
				}
				if(!verify || !configIterator.hasNext()){
					break;
				} else if(plugins.size() > 0 ){
					fragment = false;
					listIter = plugins.iterator();
				} else {
					listIter = fragments.iterator();
				}
			}
			
			for (Iterator iter = features.iterator(); iter.hasNext();) {
				String name = (String) iter.next();
				if (verify) {
					//this will throw an exception if the feature is not found.
					getSite(false).findFeature(name, null, true);
				}
				parameters.clear();
				parameters.put("id", name); //$NON-NLS-1$
				parameters.put("version", "0.0.0");  //$NON-NLS-1$//$NON-NLS-2$
				writer.printTag("includes", parameters, true, true, true); //$NON-NLS-1$
			}
			writer.endTag("feature"); //$NON-NLS-1$
		} finally {
			writer.close();
		}
		
		
		//create build.properties
		file = new File(directory, IPDEBuildConstants.PROPERTIES_FILE);
		Properties prop = new Properties();
		prop.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$
		OutputStream stream = null;
		try{
			stream = new BufferedOutputStream(new FileOutputStream(file));
			prop.store(stream, "Marker File so that the file gets written");  //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
			// nothing for now
		} finally {
			if( stream != null) {					
				try {
					stream.close();
				} catch (IOException e1) {
					// nothing
				}
			}
		}
	}

	public void setVerify(boolean verify) {
		this.verify = verify;
		reportResolutionErrors = verify;
	}
	
    public boolean guessUnpack(BundleDescription bundle, String[] classpath) {
		if (bundle == null)
			return true;
	
		// launcher fragments are a special case, they have no bundle-classpath and they must
		//be unpacked
		if (bundle.getHost() != null && bundle.getName().startsWith(BUNDLE_LAUNCHER))
			return true;
		
		if (new File(bundle.getLocation()).isFile()) 
			return false;
		
		if (classpath.length == 0)
			return false;
	
		for (int i = 0; i < classpath.length; i++) {
			if (classpath[i].equals(".")) //$NON-NLS-1$
				return false;
		}
		return true;
	}
}
