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
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.site.PDEState;

public class FeatureGenerator extends AbstractScriptGenerator {
	private static final String LAUNCHER_FEATURE_NAME = "org.eclipse.platform.launchers"; //$NON-NLS-1$

	private String featureId = null;
	private String productFile = null;
	private String[] pluginList = null;
	private String[] featureList = null;

	private ProductFile product = null;

	private boolean verify = false;
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		initialize();

		List plugins = pluginList != null ? Arrays.asList(pluginList) : new ArrayList();
		List features = featureList != null ? Arrays.asList(featureList) : new ArrayList();
		if (product != null) {
			List productElements = product.useFeatures() ? product.getFeatures() : product.getPlugins();
			if (productElements != null && productElements.size() > 0) {
				if (product.useFeatures())
					features.addAll(productElements);
				else
					plugins.addAll(productElements);
			}
		}
		//TODO Give a warning if the product is null and no features or plugins have been given
		try {
			createFeature(featureId, plugins, features);
		} catch (FileNotFoundException e) {
			//TODO Log error
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

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}
	
	private void initialize(){
		if (productFile != null && !productFile.startsWith("${")) { //$NON-NLS-1$
			String productPath = findFile(productFile, false);
			if (productPath == null)
				productPath = productFile;
			File f = new File(productPath);
			if (f.exists() && f.isFile()) {
				try {
					product = new ProductFile(productPath, null);
				} catch (CoreException e) {
					// log
				}
			}
		} 
	}
	
	protected void createFeature(String feature, List plugins, List features) throws CoreException, FileNotFoundException {
		String location = IPDEBuildConstants.DEFAULT_FEATURE_LOCATION + '/' + feature;
		File directory = new File(getWorkingDirectory(), location);
		if (!directory.exists())
			directory.mkdirs();

		PDEState state = verify ? getSite(false).getRegistry() : null;

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

		parameters.put("id", feature); //$NON-NLS-1$
		parameters.put("version", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.startTag("feature", parameters, true); //$NON-NLS-1$

		for (Iterator iter = plugins.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			boolean unpack = true;
			if (verify) {
				BundleDescription bundle = state.getResolvedBundle(name);
				if (bundle != null){
					unpack = guessUnpack(bundle, (String[]) state.getExtraData().get(new Long(bundle.getBundleId())));
				} else {
					//throw error
					String message = NLS.bind(Messages.exception_missingPlugin, name);
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
				}
			}
			parameters.clear();
			parameters.put("id", name); //$NON-NLS-1$
			parameters.put("version", "0.0.0"); //$NON-NLS-1$//$NON-NLS-2$
			parameters.put("unpack", unpack ? "true" : "false");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			writer.printTag("plugin", parameters, true, true, true); //$NON-NLS-1$
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
