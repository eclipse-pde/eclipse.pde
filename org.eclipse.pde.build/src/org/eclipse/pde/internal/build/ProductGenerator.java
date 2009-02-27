/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.internal.build.site.P2Utils;
import org.eclipse.pde.internal.build.site.PDEState;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;
import org.osgi.framework.Version;

public class ProductGenerator extends AbstractScriptGenerator {
	private static final String SIMPLE_CONFIGURATOR_CONFIG_URL = "org.eclipse.equinox.simpleconfigurator.configUrl"; //$NON-NLS-1$
	private static final String START_LEVEL_1 = "@1:start"; //$NON-NLS-1$
	private static final String START_LEVEL_2 = "@2:start"; //$NON-NLS-1$
	private static final String START_LEVEL_3 = "@3:start"; //$NON-NLS-1$
	private static final String START = "@start"; //$NON-NLS-1$

	private static final byte CONFIG_STYLE_ORIGINAL = 1;
	private static final byte CONFIG_STYLE_REFACTORED = 2;
	private static final byte CONFIG_STYLE_SIMPLE = 4;
	private static final byte CONFIG_STYLE_UPDATE = 8;
	private static final byte CONFIG_INCLUDES_DS = 16;

	private static final int INSTRUCTION_INSTALL = 0;
	private static final int INSTRUCTION_UNINSTALL = 1;
	private static final int INSTRUCTION_CONFIGURE = 2;
	private static final int INSTRCUTION_UNCONFIGURE = 3;

	private String product = null;
	private ProductFile productFile = null;
	private String root = null;
	private Properties buildProperties;
	private AssemblyInformation assembly = null;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.build.AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		initialize();

		if (productFile == null)
			return;

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
			String custom = findConfigFile(config.getOs());
			if (custom != null) {
				try {
					Utils.copyFile(custom, rootLocation + "/configuration/config.ini"); //$NON-NLS-1$
				} catch (IOException e) {
					//ignore
				}
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

	public void generateP2Info() throws CoreException {
		initialize();

		//For now, do nothing if there is alreayd a p2.inf
		File p2Inf = new File(root, "p2.inf"); //$NON-NLS-1$
		if (p2Inf.exists())
			return;

		//only generate if the .product said nothing
		if (productFile.getConfigurationInfo().size() > 0)
			return;

		StringBuffer buffer = new StringBuffer();
		generateP2InfCUs(buffer);

		try {
			Utils.writeBuffer(buffer, p2Inf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private BundleInfo[] getDefaultStartInfo() {
		//TODO merge this with config.ini generation
		//for now just set p2.inf values, implying refactored runtime and simpleconfigurator
		List configs = getConfigInfos();
		BundleInfo[] defaults = new BundleInfo[7 + configs.size()];
		defaults[0] = new BundleInfo(BUNDLE_SIMPLE_CONFIGURATOR, null, null, 1, true);
		defaults[1] = new BundleInfo(BUNDLE_EQUINOX_COMMON, null, null, 2, true);
		defaults[2] = new BundleInfo(BUNDLE_OSGI, null, null, -1, true);
		defaults[3] = new BundleInfo(BUNDLE_UPDATE_CONFIGURATOR, null, null, 4, true);
		defaults[4] = new BundleInfo(BUNDLE_CORE_RUNTIME, null, null, 4, true);
		defaults[5] = new BundleInfo(BUNDLE_DS, null, null, 1, true);

		//launcher and fragments are special
		defaults[6] = new BundleInfo(BUNDLE_EQUINOX_LAUNCHER, null, null, -1, false);
		for (int i = 0; i < configs.size(); i++) {
			Config config = (Config) configs.get(i);
			if (config.equals(Config.genericConfig()))
				continue;
			String fragmentName = BUNDLE_EQUINOX_LAUNCHER + '.' + config.getWs() + '.' + config.getOs();
			if (config.getOs().compareToIgnoreCase("macosx") != 0) //$NON-NLS-1$
				fragmentName += '.' + config.getArch();
			defaults[i + 7] = new BundleInfo(fragmentName, null, null, -1, false);
		}
		return defaults;
	}

	private void generateP2InfCUs(StringBuffer buffer) {
		BundleInfo[] infos = getDefaultStartInfo();
		for (int i = 0; i < infos.length && infos[i] != null; i++) {
			BundleDescription bundle = assembly.getPlugin(infos[i].getSymbolicName(), infos[i].getVersion());
			if (bundle == null)
				continue;

			String[] instructions = new String[4];
			instructions[INSTRUCTION_INSTALL] = "installBundle(bundle:${artifact});"; //$NON-NLS-1$
			instructions[INSTRUCTION_UNINSTALL] = "uninstallBundle(bundle:${artifact});"; //$NON-NLS-1$
			if (bundle.getSymbolicName().equals(BUNDLE_EQUINOX_LAUNCHER)) {
				instructions[INSTRUCTION_CONFIGURE] = "addProgramArg(programArg:-startup);addProgramArg(programArg:@artifact);"; //$NON-NLS-1$
				instructions[INSTRCUTION_UNCONFIGURE] = "removeProgramArg(programArg:-startup);removeProgramArg(programArg:@artifact);"; //$NON-NLS-1$
			} else if (bundle.getSymbolicName().startsWith(BUNDLE_EQUINOX_LAUNCHER)) {
				instructions[INSTRUCTION_CONFIGURE] = "addProgramArg(programArg:--launcher.library);addProgramArg(programArg:@artifact);"; //$NON-NLS-1$
				instructions[INSTRCUTION_UNCONFIGURE] = "removeProgramArg(programArg:--launcher.library);removeProgramArg(programArg:@artifact);"; //$NON-NLS-1$
			} else {
				instructions[INSTRUCTION_CONFIGURE] = "setStartLevel(startLevel:" + infos[i].getStartLevel() + ");markStarted(started:" + Boolean.toString(infos[i].isMarkedAsStarted()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				instructions[INSTRUCTION_CONFIGURE] = "setStartLevel(startLevel:-1);markStarted(started:false);"; //$NON-NLS-1$
			}

			printP2Unit(buffer, i, bundle.getSymbolicName(), bundle.getVersion(), bundle.getPlatformFilter(), instructions);
		}
	}

	/*
	 * Print a CU to the given string buffer.
	 * CUs are generated with a property "org.eclipse.pde.build.default" which can be used as a hint to give 
	 * others priority in the event of conflicts with other CUs
	 * We expect post-processing to be performed on these results to replace the @FLAVOR@ with the actual flavor
	 */
	private void printP2Unit(StringBuffer buffer, int i, String name, Version version, String filter, String[] instructions) {
		VersionRange range = new VersionRange(version, true, version, true);
		buffer.append("units." + i + ".id=@FLAVOR@" + name + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".version=" + version + '\n'); //$NON-NLS-1$//$NON-NLS-2$
		buffer.append("units." + i + ".properties.org.eclipse.pde.build.default=true\n"); //$NON-NLS-1$//$NON-NLS-2$
		if (filter != null)
			buffer.append("units." + i + ".filter=" + filter + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".hostRequirements.1.namespace=osgi.bundle\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".hostRequirements.1.name=" + name + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".hostRequirements.1.range=" + range.toString() + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".hostRequirements.2.namespace=org.eclipse.equinox.p2.eclipse.type\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".hostRequirements.2.name=bundle\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".hostRequirements.2.range=[1.0.0, 2.0.0)\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".hostRequirements.2.greedy=false\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".requires.1.namespace=osgi.bundle\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".requires.1.name=" + name + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".requires.1.range=" + range.toString() + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".requires.2.namespace=org.eclipse.equinox.p2.eclipse.type\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".requires.2.name=bundle\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".requires.2.range=[1.0.0, 2.0.0)\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".requires.2.greedy=false\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".provides.1.namespace=org.eclipse.equinox.p2.iu\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".provides.1.name=@FLAVOR@" + name + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".provides.1.version=" + version + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".provides.2.namespace=org.eclipse.equinox.p2.flavor\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".provides.2.name=@FLAVOR@\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + ".provides.2.version=1.0.0\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + "instructions.install=" + instructions[INSTRUCTION_INSTALL] + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + "instructions.uninstall=" + instructions[INSTRUCTION_INSTALL] + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + "instructions.unconfigure=" + instructions[INSTRUCTION_INSTALL] + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("units." + i + "instructions.configure=" + instructions[INSTRUCTION_INSTALL] + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String findConfigFile(String os) {
		String path = productFile.getConfigIniPath(os);
		if (path == null)
			return null;

		String result = findFile(path, false);
		if (result != null)
			return result;

		// couldn't find productFile, try it as a path directly
		File f = new File(path);
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		// relative to the working directory
		f = new File(getWorkingDirectory(), path);
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		// relative to the working directory/plugins
		f = new File(getWorkingDirectory() + "/" + DEFAULT_PLUGIN_LOCATION, path); //$NON-NLS-1$
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		//relative to .product file
		f = new File(new File(productFile.getLocation()).getParent(), path);
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		return null;
	}

	private void initialize() throws CoreException {
		productFile = loadProduct(product);
	}

	private byte determineConfigStyle(Config config) {
		byte result = 0;

		if (assembly.getPlugin(BUNDLE_SIMPLE_CONFIGURATOR, null) != null) {
			result |= CONFIG_STYLE_SIMPLE;
		} else if (assembly.getPlugin(BUNDLE_UPDATE_CONFIGURATOR, null) != null) {
			Properties props = productFile.getConfigProperties();
			if (Boolean.valueOf(props.getProperty("org.eclipse.update.reconcile", "true")).booleanValue()) //$NON-NLS-1$ //$NON-NLS-2$
				result |= CONFIG_STYLE_UPDATE;
		}

		if (assembly.getPlugin(BUNDLE_DS, null) != null)
			result |= CONFIG_INCLUDES_DS;

		if (assembly.getPlugin(BUNDLE_EQUINOX_COMMON, null) != null)
			return (byte) (result | CONFIG_STYLE_REFACTORED);

		return (byte) (result | CONFIG_STYLE_ORIGINAL);
	}

	private List getBundlesFromProductFile(Config config) {
		List pluginList = productFile.getProductEntries();
		List results = new ArrayList(pluginList.size());
		for (Iterator iter = pluginList.iterator(); iter.hasNext();) {
			FeatureEntry entry = (FeatureEntry) iter.next();
			if (!entry.isPlugin())
				continue;

			BundleDescription bundle = assembly.getPlugin(entry.getId(), entry.getVersion());
			if (bundle != null) {
				results.add(bundle);
			}
		}
		return results;
	}

	private void printSimpleBundles(StringBuffer buffer, Config config, File configDir, byte style) {
		buffer.append("osgi.bundles="); //$NON-NLS-1$
		buffer.append(BUNDLE_SIMPLE_CONFIGURATOR);
		buffer.append(START_LEVEL_1);
		buffer.append("\n"); //$NON-NLS-1$

		Collection plugins = null;
		if (productFile.useFeatures())
			plugins = assembly.getPlugins(config);
		else
			plugins = getBundlesFromProductFile(config);

		File bundlesTxt = P2Utils.writeBundlesTxt(plugins, configDir, productFile, (style & CONFIG_STYLE_REFACTORED) > 0);
		if (bundlesTxt != null) {
			buffer.append(SIMPLE_CONFIGURATOR_CONFIG_URL);
			buffer.append("=file:"); //$NON-NLS-1$
			buffer.append(P2Utils.BUNDLE_TXT_PATH);
			buffer.append("\n"); //$NON-NLS-1$
		}
	}

	private void printBundleInfo(StringBuffer buffer, BundleInfo info) {
		buffer.append(info.getSymbolicName());
		if (info.getStartLevel() != BundleInfo.NO_LEVEL || info.isMarkedAsStarted())
			buffer.append('@');
		if (info.getStartLevel() != BundleInfo.NO_LEVEL) {
			buffer.append(info.getStartLevel());
			if (info.isMarkedAsStarted())
				buffer.append(':');
		}
		if (info.isMarkedAsStarted())
			buffer.append("start"); //$NON-NLS-1$
	}

	private void printUpdateBundles(StringBuffer buffer, int style) {
		Map infos = productFile.getConfigurationInfo();
		buffer.append("osgi.bundles="); //$NON-NLS-1$
		if (infos.size() > 0) {
			//user specified
			for (Iterator iterator = infos.values().iterator(); iterator.hasNext();) {
				BundleInfo info = (BundleInfo) iterator.next();
				printBundleInfo(buffer, info);
				if (iterator.hasNext())
					buffer.append(',');
			}
		} else {
			if ((style & CONFIG_STYLE_REFACTORED) > 0) {
				//start levels for eclipse 3.2
				//org.eclipse.equinox.common@2:start,  
				buffer.append(BUNDLE_EQUINOX_COMMON);
				buffer.append(START_LEVEL_2);
				buffer.append(',');
				//org.eclipse.update.configurator@3:start
				buffer.append(BUNDLE_UPDATE_CONFIGURATOR);
				buffer.append(START_LEVEL_3);
				buffer.append(',');
				if ((style & CONFIG_INCLUDES_DS) > 0) {
					//org.eclipse.equinox.ds@1:start
					buffer.append(BUNDLE_DS);
					buffer.append(START_LEVEL_1);
					buffer.append(',');
				}
				//org.eclipse.core.runtime
				buffer.append(BUNDLE_CORE_RUNTIME);
				buffer.append(START);
			} else {
				//start level for 3.1 and 3.0
				buffer.append(BUNDLE_CORE_RUNTIME);
				buffer.append(START_LEVEL_2);
				buffer.append(',');
				buffer.append(BUNDLE_UPDATE_CONFIGURATOR);
				buffer.append(START_LEVEL_3);
			}
		}
		buffer.append('\n');
	}

	private void printAllBundles(StringBuffer buffer, Config config, byte style) {
		buffer.append("osgi.bundles="); //$NON-NLS-1$

		//When the plugins are all listed.
		Dictionary environment = new Hashtable(3);
		environment.put("osgi.os", config.getOs()); //$NON-NLS-1$
		environment.put("osgi.ws", config.getWs()); //$NON-NLS-1$
		environment.put("osgi.arch", config.getArch()); //$NON-NLS-1$

		Collection bundles = null;
		if (productFile.useFeatures())
			bundles = assembly.getPlugins(config);
		else
			bundles = getBundlesFromProductFile(config);
		BundleHelper helper = BundleHelper.getDefault();
		Map infos = productFile.getConfigurationInfo();
		boolean first = true;
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			String id = bundle.getSymbolicName();
			if (BUNDLE_OSGI.equals(id) || BUNDLE_EQUINOX_LAUNCHER.equals(id))
				continue;
			String filter = bundle.getPlatformFilter();
			if (filter == null || helper.createFilter(filter).match(environment)) {
				if (first)
					first = false;
				else
					buffer.append(',');
				if (infos.size() > 0) {
					if (infos.containsKey(id))
						printBundleInfo(buffer, (BundleInfo) infos.get(id));
					else
						buffer.append(bundle.getSymbolicName());
				} else {
					buffer.append(bundle.getSymbolicName());
					if (BUNDLE_EQUINOX_COMMON.equals(id)) {
						buffer.append(START_LEVEL_2);
					} else if (BUNDLE_DS.equals(id)) {
						buffer.append(START_LEVEL_1);
					} else if (BUNDLE_CORE_RUNTIME.equals(id)) {
						if ((style & CONFIG_STYLE_REFACTORED) > 0) {
							buffer.append(START);
						} else {
							buffer.append(START_LEVEL_2);
						}
					}
				}
			}
		}
		buffer.append('\n');
	}

	private void createConfigIni(Config config, String location) throws CoreException {
		File configDir = new File(location + "/configuration"); //$NON-NLS-1$
		if ((!configDir.exists() && !configDir.mkdirs()) || configDir.isFile())
			return; //we will fail trying to create the file, TODO log warning/error

		byte configStyle = determineConfigStyle(config);

		StringBuffer buffer = new StringBuffer();
		buffer.append("#Product Runtime Configuration File\n"); //$NON-NLS-1$

		Properties properties = productFile.getConfigProperties();
		String splash = getSplashLocation(config);
		if (splash != null)
			properties.put("osgi.splashPath", splash); //$NON-NLS-1$

		String application = productFile.getApplication();
		if (application != null)
			properties.put("eclipse.application", application); //$NON-NLS-1$

		String productId = productFile.getId();
		if (productId != null)
			properties.put("eclipse.product", productId); //$NON-NLS-1$

		if (!properties.containsKey("osgi.bundles.defaultStartLevel")) //$NON-NLS-1$
			properties.put("osgi.bundles.defaultStartLevel", "4"); //$NON-NLS-1$ //$NON-NLS-2$

		for (Iterator iterator = properties.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			buffer.append(key);
			buffer.append('=');
			buffer.append(properties.getProperty(key));
			buffer.append('\n');
		}

		if ((configStyle & CONFIG_STYLE_SIMPLE) > 0) {
			printSimpleBundles(buffer, config, configDir, configStyle);
		} else {
			//When update configurator is present or when feature based product
			if ((configStyle & CONFIG_STYLE_UPDATE) > 0) {
				printUpdateBundles(buffer, configStyle);
			} else {
				printAllBundles(buffer, config, configStyle);
			}
		}

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
			properties.put(ID, productFile.getId());

		if (properties.size() == 0)
			return;

		String branding = getBrandingPlugin();
		if (branding != null) {
			BundleDescription bundle = getSite(false).getRegistry().getResolvedBundle(branding);
			if (bundle != null)
				properties.put(VERSION, bundle.getVersion().toString());
		}
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
				tokenizer.whitespaceChars(0, 0x20);
				tokenizer.wordChars(0x21, 0xFF);
				tokenizer.quoteChar('"');
				tokenizer.quoteChar('\'');
				while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
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
				tokenizer.whitespaceChars(0, 0x20);
				tokenizer.wordChars(0x21, 0xFF);
				tokenizer.quoteChar('"');
				tokenizer.quoteChar('\'');
				while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
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

	public void setAssemblyInfo(AssemblyInformation info) {
		this.assembly = info;
	}

}
