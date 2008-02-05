/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.site.PDEState;

public class ProductGenerator extends AbstractScriptGenerator {
	private static final String BUNDLE_EQUINOX_COMMON = "org.eclipse.equinox.common"; //$NON-NLS-1$
	private static final String BUNDLE_OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
	private static final String BUNDLE_CORE_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String BUNDLE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	private static final String START_LEVEL_2 = "@2:start"; //$NON-NLS-1$
	private static final String START_LEVEL_3 = "@3:start"; //$NON-NLS-1$
	private static final String START = "@start"; //$NON-NLS-1$

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

		//we need at least a product id
		if (productFile.getId() == null) {
			return;
		}

		String custom = findFile(productFile.getConfigIniPath(), false);
		String location = null, fileList = null;
		for (Iterator iter = getConfigInfos().iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			location = DEFAULT_PRODUCT_ROOT_FILES_DIR + '/' + config.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$

			String rootLocation = root + location;
			File rootDir = new File(rootLocation);
			if ((!rootDir.exists() && !rootDir.mkdirs()) || rootDir.isFile())
				continue; //we will fail trying to create the files, TODO log warning/error

			//add generated root files to build.properties
			if (buildProperties != null) {
				fileList = buildProperties.getProperty(ROOT_PREFIX + config.toString("."), ""); //$NON-NLS-1$ //$NON-NLS-2$
				fileList += (fileList.length() > 0) ? ',' + location : location;
				buildProperties.put(ROOT_PREFIX + config.toString("."), fileList); //$NON-NLS-1$
			}

			//configuration/config.ini
			if (custom != null) {
				copyFile(custom, rootLocation + "/configuration/config.ini"); //$NON-NLS-1$
			} else {
				createConfigIni(config, rootLocation);
			}

			//only the config.ini makes sense in the any config
			if (config.getOs().equals(Config.ANY))
				continue;

			//.eclipseproduct
			createEclipseProductFile(rootLocation);

			//eclipse.ini
			createLauncherIniFile(rootLocation, config.getOs());
		}

	}

	private void initialize() throws CoreException {
		productFile = loadProduct(product);

		PDEState state = getSite(false).getRegistry();
		refactoredRuntime = state.getResolvedBundle(BUNDLE_EQUINOX_COMMON) != null;
	}

	private void copyFile(String src, String dest) {
		File source = new File(src);
		if (!source.exists())
			return;
		File destination = new File(dest);
		File destDir = destination.getParentFile();
		if ((!destDir.exists() && !destDir.mkdirs()) || destDir.isFile())
			return; //we will fail trying to create the file, TODO log warning/error

		InputStream in = null;
		OutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(source));
			out = new BufferedOutputStream(new FileOutputStream(destination));

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
		File configDir = new File(location + "/configuration"); //$NON-NLS-1$
		if ((!configDir.exists() && !configDir.mkdirs()) || configDir.isFile())
			return; //we will fail trying to create the file, TODO log warning/error

		PDEState state = getSite(false).getRegistry();

		StringBuffer buffer = new StringBuffer();
		buffer.append("#Product Runtime Configuration File\n"); //$NON-NLS-1$

		String splash = getSplashLocation(config);
		if (splash != null)
			buffer.append("osgi.splashPath=" + splash + '\n'); //$NON-NLS-1$

		String application = productFile.getApplication();
		if (application != null)
			buffer.append("eclipse.application=" + application + '\n'); //$NON-NLS-1$
		buffer.append("eclipse.product=" + productFile.getId() + '\n'); //$NON-NLS-1$
		buffer.append("osgi.bundles="); //$NON-NLS-1$
		//When update configurator is present or when feature based product
		if (productFile.useFeatures() || productFile.containsPlugin(BUNDLE_UPDATE_CONFIGURATOR)) {
			if (refactoredRuntime) {
				//start levels for eclipse 3.2
				//org.eclipse.equinox.common@2:start,  
				buffer.append(BUNDLE_EQUINOX_COMMON);
				buffer.append(START_LEVEL_2);
				buffer.append(',');
				//org.eclipse.update.configurator@3:start
				buffer.append(BUNDLE_UPDATE_CONFIGURATOR);
				buffer.append(START_LEVEL_3);
				buffer.append(',');
				//org.eclipse.core.runtime
				buffer.append(BUNDLE_CORE_RUNTIME);
				buffer.append(START);
				buffer.append('\n');
			} else {
				//start level for 3.1 and 3.0
				buffer.append(BUNDLE_CORE_RUNTIME);
				buffer.append(START_LEVEL_2);
				buffer.append(',');
				buffer.append(BUNDLE_UPDATE_CONFIGURATOR);
				buffer.append(START_LEVEL_3);
				buffer.append('\n');
			}
		} else {
			//When the plugins are all listed.
			Dictionary environment = new Hashtable(3);
			environment.put("osgi.os", config.getOs()); //$NON-NLS-1$
			environment.put("osgi.ws", config.getWs()); //$NON-NLS-1$
			environment.put("osgi.arch", config.getArch()); //$NON-NLS-1$

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
					if (BUNDLE_EQUINOX_COMMON.equals(id)) {
						buffer.append(START_LEVEL_2);
					} else if (BUNDLE_CORE_RUNTIME.equals(id)) {
						if (refactoredRuntime) {
							buffer.append(START);
						} else {
							buffer.append(START_LEVEL_2);
						}
					}
				}
			}
			buffer.append('\n');
		}
		buffer.append("osgi.bundles.defaultStartLevel=4\n"); //$NON-NLS-1$ 	

		FileWriter writer = null;
		try {
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

	private void createEclipseProductFile(String directory) throws CoreException {
		File dir = new File(directory);
		if ((!dir.exists() && !dir.mkdirs()) || dir.isFile())
			return; //we will fail trying to create the file, TODO log warning/error

		Properties properties = new Properties();
		if (productFile.getProductName() != null)
			properties.put("name", productFile.getProductName()); //$NON-NLS-1$
		if (productFile.getId() != null)
			properties.put("id", productFile.getId()); //$NON-NLS-1$

		BundleDescription bundle = getSite(false).getRegistry().getResolvedBundle(getBrandingPlugin());
		if (bundle != null)
			properties.put("version", bundle.getVersion().toString()); //$NON-NLS-1$

		OutputStream stream = null;
		try {
			File file = new File(dir, ".eclipseproduct"); //$NON-NLS-1$
			stream = new BufferedOutputStream(new FileOutputStream(file));
			properties.store(stream, "Eclipse Product File"); //$NON-NLS-1$
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
		if (id == null)
			return null;
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

	private void createLauncherIniFile(String directory, String os) {
		String launcher = getLauncherName();

		if (os.equals(Platform.OS_MACOSX)) {
			directory += "/" + launcher + ".app/Contents/MacOS"; //$NON-NLS-1$//$NON-NLS-2$
		}
		File dir = new File(directory);
		if ((!dir.exists() && !dir.mkdirs()) || dir.isFile())
			return; //we will fail trying to create the file TODO log warning/error

		String programArgs = productFile.getProgramArguments(os);
		String vmArgs = productFile.getVMArguments(os);

		if ((programArgs == null || programArgs.length() == 0) && (vmArgs == null || vmArgs.length() == 0))
			return;

		String lineDelimiter = Platform.OS_WIN32.equals(os) ? "\r\n" : "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(dir, launcher + ".ini"))); //$NON-NLS-1$
			if (programArgs != null && programArgs.length() > 0) {
				StringReader reader = new StringReader(programArgs);
				StreamTokenizer tokenizer = new StreamTokenizer(reader);
				tokenizer.resetSyntax();
				tokenizer.whitespaceChars(0,0x20);
				tokenizer.wordChars(0x21, 0xFF);
				tokenizer.quoteChar('"');
				tokenizer.quoteChar('\'');
				while (tokenizer.nextToken() != StreamTokenizer.TT_EOF){
					writer.print(tokenizer.sval);
					writer.print(lineDelimiter);
				}
			}
			if (vmArgs != null && vmArgs.length() > 0) {
				writer.print("-vmargs"); //$NON-NLS-1$
				writer.print(lineDelimiter);
				StringReader reader = new StringReader(vmArgs);
				StreamTokenizer tokenizer = new StreamTokenizer(reader);
				tokenizer.resetSyntax();
				tokenizer.whitespaceChars(0,0x20);
				tokenizer.wordChars(0x21, 0xFF);
				tokenizer.quoteChar('"');
				tokenizer.quoteChar('\'');
				while (tokenizer.nextToken() != StreamTokenizer.TT_EOF){
					writer.print(tokenizer.sval);
					writer.print(lineDelimiter);
				}
			}
		} catch (IOException e) {
			//nothing
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private String getLauncherName() {
		String name = productFile.getLauncherName();

		if (name != null && name.length() > 0) {
			name = name.trim();
			if (name.endsWith(".exe")) //$NON-NLS-1$
				name = name.substring(0, name.length() - 4);
			return name;
		}
		return "eclipse"; //$NON-NLS-1$
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
