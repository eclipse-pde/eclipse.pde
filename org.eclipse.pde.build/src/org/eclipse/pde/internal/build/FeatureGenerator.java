/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

public class FeatureGenerator extends AbstractScriptGenerator {
	private static final String LAUNCHER_FEATURE_NAME = "org.eclipse.platform.launchers"; //$NON-NLS-1$

	private String featureId = null;
	private String productFile = null;
	private String[] pluginList = null;
	private String [] fragmentList = null;
	private String[] featureList = null;

	private ProductFile product = null;

	private boolean verify = false;
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		initialize();

		List plugins = pluginList != null ? new ArrayList(Arrays.asList(pluginList)) : new ArrayList();
		List features = featureList != null ? new ArrayList(Arrays.asList(featureList)) : new ArrayList();
		List fragments = fragmentList != null ? new ArrayList(Arrays.asList(fragmentList)) : new ArrayList();
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
	
	private void initialize() throws CoreException{
		//get rid of old feature that we will be overwriting, we don't want it in the state accidently.
		File dir = new File(getWorkingDirectory(), IPDEBuildConstants.DEFAULT_FEATURE_LOCATION + '/' + featureId);
		File xml = new File(dir, Constants.FEATURE_FILENAME_DESCRIPTOR);
		if(xml.exists()){
			xml.delete();
		}
		
		if (productFile != null && !productFile.startsWith("${") && productFile.length() > 0) { //$NON-NLS-1$
			String productPath = findFile(productFile, false);
			if (productPath == null)
				productPath = productFile;
			File f = new File(productPath);
			if (f.exists() && f.isFile()) {
				product = new ProductFile(productPath, null);
			} else {
				IStatus error = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FILE, NLS.bind(Messages.exception_missingElement, productFile), null);
				throw new CoreException(error);
			}
		} 
	}
	
	protected void createFeature(String feature, List plugins, List fragments, List features) throws CoreException, FileNotFoundException {
		String location = IPDEBuildConstants.DEFAULT_FEATURE_LOCATION + '/' + feature;
		File directory = new File(getWorkingDirectory(), location);
		if (!directory.exists())
			directory.mkdirs();

		PDEState state = verify ? getSite(false).getRegistry() : null;
		BundleHelper helper = BundleHelper.getDefault();

		//Create feature.xml
		File file = new File(directory, Constants.FEATURE_FILENAME_DESCRIPTOR);
		OutputStream output = new FileOutputStream(file);
		XMLWriter writer = null;
		try {
			writer = new XMLWriter(output);
		} catch (UnsupportedEncodingException e) {
			//should not happen
			return;
		}
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
		ListIterator listIter = plugins.listIterator();
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
					listIter = fragments.listIterator();
				}
			}
			if(!verify || !configIterator.hasNext()){
				break;
			} else if(plugins.size() > 0 ){
				fragment = false;
				listIter = plugins.listIterator();
			} else {
				listIter = fragments.listIterator();
			}
		}
		
		boolean hasLaunchers = false;
		for (Iterator iter = features.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			if (name.equals(LAUNCHER_FEATURE_NAME)) {
				hasLaunchers = true;	
			}
			if (verify) {
				//this will throw an exception if the feature is not found.
				getSite(false).findFeature(name, null, true);
			}
			parameters.clear();
			parameters.put("id", name); //$NON-NLS-1$
			parameters.put("version", "0.0.0");  //$NON-NLS-1$//$NON-NLS-2$
			writer.printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
		if (!hasLaunchers) {
			parameters.clear();
			parameters.put("id", LAUNCHER_FEATURE_NAME); //$NON-NLS-1$
			parameters.put("version", "0.0.0");  //$NON-NLS-1$//$NON-NLS-2$
			parameters.put("optional", "true");  //$NON-NLS-1$//$NON-NLS-2$
			writer.printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
		writer.endTag("feature"); //$NON-NLS-1$
		writer.close();
		
		//create build.properties
		file = new File(directory, IPDEBuildConstants.PROPERTIES_FILE);
		Properties prop = new Properties();
		prop.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$
		FileOutputStream stream = null;
		try{
			stream = new FileOutputStream(file);
			prop.store(stream, "Marker File so that the file gets written");  //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
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
