/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.site.PDEState;

public class ProductGenerator extends AbstractScriptGenerator {
	private static final String BUNDLE_EQUINOX_COMMON = "org.eclipse.equinox.common"; //$NON-NLS-1$
	private static final String BUNDLE_EQUINOX_REGISTRY = "org.eclipse.equinox.registry"; //$NON-NLS-1$
	private static final String BUNDLE_EQUINOX_PREFERENCES = "org.eclipse.equinox.preferences"; //$NON-NLS-1$
	private static final String BUNDLE_OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
	private static final String BUNDLE_CORE_JOBS = "org.eclipse.core.jobs"; //$NON-NLS-1$
	private static final String BUNDLE_CORE_CONTENTTYPE = "org.eclipse.core.contenttype"; //$NON-NLS-1$
	private static final String BUNDLE_CORE_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String BUNDLE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	private static final String START_LEVEL_2 = "@2:start"; //$NON-NLS-1$
	private static final String START_LEVEL_3 = "@3:start"; //$NON-NLS-1$
	private static final String START_LEVEL_4 = "@4:start"; //$NON-NLS-1$
	
	private String product = null;
	private ProductFile productFile = null;
	private String root = null;
	private boolean refactoredRuntime = false;
	private Properties buildProperties;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		initialize();
		
		if (productFile == null)
			return;

		String custom = findFile(productFile.getConfigIniPath(), false);
		String location = null, fileList = null;
		for (Iterator iter = getConfigInfos().iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			location = DEFAULT_PRODUCT_ROOT_FILES_DIR + '/' + config.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$
			
			//add generated root files to build.properties
			if (buildProperties != null) {
				fileList = buildProperties.getProperty(ROOT_PREFIX + config.toString("."), ""); //$NON-NLS-1$ //$NON-NLS-2$
				fileList += (fileList.length() > 0) ? ',' + location : location;
				buildProperties.put(ROOT_PREFIX + config.toString("."), fileList); //$NON-NLS-1$
			}
			
			//configuration/config.ini
			if (custom != null) {
				copyFile(custom, root + location + "/configuration/config.ini"); //$NON-NLS-1$
			} else {
				createConfigIni(config, root + location);				
			}
			
			//.eclipseproduct
			createEclipseProductFile(root + location);
		}
		
	}
	
	private void initialize() throws CoreException {
		loadProduct();
		
		PDEState state = getSite(false).getRegistry();
		refactoredRuntime = state.getResolvedBundle(BUNDLE_EQUINOX_COMMON) != null;
	}
	
	private void loadProduct() throws CoreException {
		if (product == null || product.startsWith("${")) { //$NON-NLS-1$
			productFile = null;
			return;
		}
		String productPath = findFile(product, false);
		if (productPath == null)
			productPath = product;
		
		//the ProductFile uses the OS to determine which icons to return, we don't care so can use null
		//this is better since this generator may be used for multiple OS's
		productFile = new ProductFile(productPath, null);
	}
	
	private void copyFile(String src, String dest) {
		File source = new File(src);
		if (!source.exists())
			return;
		File destination = new File(dest);
		File destDir = destination.getParentFile();
		if (!destDir.exists())
			destDir.mkdirs();
		
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(destination);
		
			//Transfer bytes from in to out
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf, 0, len);
	        }
		} catch (IOException e) {
			//nothing
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//nothing
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					//nothing
				}
			}
		}
	}

	
	private void createConfigIni(Config config, String location) throws CoreException {
		PDEState state = getSite(false).getRegistry();
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("#Product Runtime Configuration File\n"); //$NON-NLS-1$
		
		String splash = getSplashLocation(config);
		if (splash != null)
			buffer.append("osgi.splashPath=" + splash + '\n'); //$NON-NLS-1$

		buffer.append("eclipse.product=" + productFile.getId() + '\n'); //$NON-NLS-1$
		buffer.append("osgi.bundles="); //$NON-NLS-1$
		if (productFile.useFeatures() || productFile.containsPlugin(BUNDLE_UPDATE_CONFIGURATOR)) {
			if (refactoredRuntime) {
				buffer.append(BUNDLE_EQUINOX_COMMON);		buffer.append(START_LEVEL_2);	buffer.append(',');
				buffer.append(BUNDLE_CORE_JOBS);			buffer.append(START_LEVEL_2);	buffer.append(',');
				buffer.append(BUNDLE_EQUINOX_REGISTRY); 	buffer.append(START_LEVEL_2);	buffer.append(',');
				buffer.append(BUNDLE_EQUINOX_PREFERENCES);									buffer.append(',');
				buffer.append(BUNDLE_CORE_CONTENTTYPE);										buffer.append(',');
				buffer.append(BUNDLE_CORE_RUNTIME);			buffer.append(START_LEVEL_3);	buffer.append(',');
				buffer.append(BUNDLE_UPDATE_CONFIGURATOR);	buffer.append(START_LEVEL_4);	buffer.append('\n');
			} else {
				buffer.append(BUNDLE_CORE_RUNTIME);			buffer.append(START_LEVEL_2);	buffer.append(',');
				buffer.append(BUNDLE_UPDATE_CONFIGURATOR);	buffer.append(START_LEVEL_3);	buffer.append('\n');
			}
		} else {
			Dictionary environment = new Hashtable(4);
			environment.put("osgi.os", config.getOs()); //$NON-NLS-1$
			environment.put("osgi.ws", config.getWs()); //$NON-NLS-1$
			environment.put("osgi.arch", config.getArch()); //$NON-NLS-1$
			//??environment.put("osgi.nl", null); //$NON-NLS-1$

			List pluginList = productFile.getPlugins();
			BundleHelper helper = BundleHelper.getDefault();
			boolean first = true;
			for (Iterator iter = pluginList.iterator(); iter.hasNext();) {
				String id = (String) iter.next();
				BundleDescription bundle = state.getResolvedBundle(id);
				if (bundle == null) {
					//TODO error?
					continue;
				}
				String filter = bundle.getPlatformFilter();
				if (filter == null || helper.createFilter(filter).match(environment)) {
					if (BUNDLE_OSGI.equals(id))
						continue;
					if (first)
						first = false;
					else
						buffer.append(","); //$NON-NLS-1$
					buffer.append(id);
					if (BUNDLE_EQUINOX_COMMON.equals(id) || BUNDLE_CORE_JOBS.equals(id) || BUNDLE_EQUINOX_REGISTRY.equals(id)) {
						buffer.append(START_LEVEL_2);
					} else if (BUNDLE_CORE_RUNTIME.equals(id)) {
						if (refactoredRuntime) {
							buffer.append(START_LEVEL_3);
						} else {
							buffer.append(START_LEVEL_2);
						}
					}
				}
			}
			buffer.append('\n');
		}
		if (refactoredRuntime)
			buffer.append("osgi.bundles.defaultStartLevel=5\n"); //$NON-NLS-1$ 		
		else
			buffer.append("osgi.bundles.defaultStartLevel=4\n"); //$NON-NLS-1$ 	

		FileWriter writer = null;
		try {
			File configDir = new File(location + "/configuration"); //$NON-NLS-1$
			if (!configDir.exists())
				configDir.mkdirs();
			writer = new FileWriter(new File(configDir, "config.ini")); //$NON-NLS-1$
			writer.write(buffer.toString());
		} catch (IOException e) {
			//nothing
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				//nothing
			}
		}

	}

	private void createEclipseProductFile(String directory) throws CoreException  {
		File dir = new File(directory);
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();
		
		Properties properties = new Properties();
		properties.put("name", productFile.getProductName()); //$NON-NLS-1$
		properties.put("id", productFile.getId());		 //$NON-NLS-1$

		BundleDescription bundle = getSite(false).getRegistry().getResolvedBundle(getBrandingPlugin());
		if (bundle != null)
			properties.put("version", bundle.getVersion().toString()); //$NON-NLS-1$
		
		FileOutputStream stream = null;
		try {
			File file = new File(dir, ".eclipseproduct"); //$NON-NLS-1$
			stream = new FileOutputStream(file);
			properties.store(stream, "Eclipse Product File");  //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
			//nothing
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					//nothing
				}
			}
		}
	}
	
	private String getBrandingPlugin() {
		String id = productFile.getId();
		int dot = id.lastIndexOf('.');
		return (dot != -1) ? id.substring(0, dot) : null;
	}
	
	private String getSplashLocation(Config config) throws CoreException {
		String plugin = productFile.getSplashLocation();

		if (plugin == null) {
			plugin = getBrandingPlugin();
		}
		
		if (plugin == null)
			return null;
		
		StringBuffer buffer = new StringBuffer("platform:/base/plugins/"); //$NON-NLS-1$
		buffer.append(plugin);
		
		Dictionary environment = new Hashtable(4);
		environment.put("osgi.os", config.getOs()); //$NON-NLS-1$
		environment.put("osgi.ws", config.getWs()); //$NON-NLS-1$
		environment.put("osgi.arch", config.getArch()); //$NON-NLS-1$
		
		PDEState state = getSite(false).getRegistry();
		BundleHelper helper = BundleHelper.getDefault();
		BundleDescription bundle = state.getResolvedBundle(plugin);
		if (bundle != null) {
			BundleDescription[] fragments = bundle.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				String filter = fragments[i].getPlatformFilter();
				if (filter == null || helper.createFilter(filter).match(environment)) {
					String fragmentId = fragments[i].getSymbolicName();
					if (productFile.containsPlugin(fragmentId)) {
						buffer.append(",platform:/base/plugins/"); //$NON-NLS-1$
						buffer.append(fragmentId);
					}
				}
			}
		}	
		return buffer.toString();
	}
	
	public void setProduct(String product) {
		this.product = product;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	public void setBuildProperties(Properties buildProperties) {
		this.buildProperties = buildProperties;
	}

}
