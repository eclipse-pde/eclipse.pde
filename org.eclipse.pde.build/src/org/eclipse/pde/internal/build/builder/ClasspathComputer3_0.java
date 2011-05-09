/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.PDEState;
import org.osgi.framework.Filter;

public class ClasspathComputer3_0 implements IClasspathComputer, IPDEBuildConstants, IXMLConstants, IBuildPropertiesConstants {
	public class ClasspathElement {
		private final String path;
		private final String subPath;
		private String accessRules;

		/**
		 * Create a ClasspathElement object
		 * @param path
		 * @param accessRules
		 * @throws NullPointerException if path is null
		 */
		protected ClasspathElement(String path, String subPath, String accessRules) {
			if (path == null)
				throw new NullPointerException();
			this.path = path;
			this.subPath = subPath;
			this.accessRules = accessRules;
		}

		public String toString() {
			return path;
		}

		public String getPath() {
			return path;
		}

		public String getSubPath() {
			return subPath;
		}

		public String getAccessRules() {
			return accessRules;
		}

		public String getAbsolutePath() {
			File f = new File(path);
			if (f.isAbsolute()) {
				try {
					return f.getCanonicalPath();
				} catch (IOException e) {
					return path;
				}
			}

			f = new File(modelLocation, path);
			try {
				return f.getCanonicalPath();
			} catch (IOException e) {
				return f.getPath();
			}
		}

		public void addRules(String newRule) {
			if (accessRules.equals("") || accessRules.equals(newRule)) //$NON-NLS-1$
				return;
			if (!newRule.equals("")) { //$NON-NLS-1$
				String join = accessRules.substring(0, accessRules.length() - EXCLUDE_ALL_RULE.length() - 1);
				newRule = join + newRule.substring(1);
			}
			accessRules = newRule;
			return;
		}

		/**
		 * ClasspathElement objects are equal if they have the same path.
		 * Access rules are not considered.
		 */
		public boolean equals(Object obj) {
			if (obj instanceof ClasspathElement) {
				ClasspathElement element = (ClasspathElement) obj;
				if (!path.equals(element.getPath()))
					return false;
				if (subPath != null && subPath.equals(element.getSubPath()))
					return false;
				return true;
			}
			return false;
		}

		public int hashCode() {
			int result = path.hashCode();
			return 13 * result + ((subPath == null) ? 0 : subPath.hashCode());
		}

	}

	private static String normalize(String path) {
		if (path == null)
			return null;
		//always use '/' as a path separator to help with comparing paths in equals
		return path.replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static final String EXCLUDE_ALL_RULE = "?**/*"; //$NON-NLS-1$

	private final ModelBuildScriptGenerator generator;
	private Map visiblePackages = null;
	private Map pathElements = null;
	private boolean allowBinaryCycles = false;
	private Set requiredIds = null;
	protected String modelLocation = null;

	public ClasspathComputer3_0(ModelBuildScriptGenerator modelGenerator) {
		this.generator = modelGenerator;
	}

	/**
	 * Compute the classpath for the given jar.
	 * The path returned conforms to Parent / Prerequisite / Self  
	 * 
	 * @param model the plugin containing the jar compiled
	 * @param jar the jar for which the classpath is being compiled
	 * @return String the classpath
	 * @throws CoreException
	 */
	public List getClasspath(BundleDescription model, ModelBuildScriptGenerator.CompiledEntry jar) throws CoreException {
		List classpath = new ArrayList(20);
		List pluginChain = new ArrayList(10); //The list of plugins added to detect cycle
		modelLocation = generator.getLocation(model);
		Set addedPlugins = new HashSet(10); //The set of all the plugins already added to the classpath (this allows for optimization)
		pathElements = new HashMap();
		visiblePackages = getVisiblePackages(model);
		requiredIds = new HashSet();
		allowBinaryCycles = AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES);

		//PREREQUISITE
		addPrerequisites(model, classpath, modelLocation, pluginChain, addedPlugins);

		//SELF
		addSelf(model, jar, classpath, modelLocation, pluginChain, addedPlugins);

		recordRequiredIds(model);

		return classpath;

	}

	private void recordRequiredIds(BundleDescription model) {
		Properties bundleProperties = null;
		bundleProperties = (Properties) model.getUserObject();
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			model.setUserObject(bundleProperties);
		}
		StringBuffer buffer = new StringBuffer();
		for (Iterator iterator = requiredIds.iterator(); iterator.hasNext();) {
			buffer.append(iterator.next().toString());
			buffer.append(':');
		}
		bundleProperties.setProperty(PROPERTY_REQUIRED_BUNDLE_IDS, buffer.toString());
	}

	private Map getVisiblePackages(BundleDescription model) {
		Map packages = new HashMap(20);
		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		addVisiblePackagesFromState(helper, model, packages);
		if (model.getHost() != null)
			addVisiblePackagesFromState(helper, (BundleDescription) model.getHost().getSupplier(), packages);
		return packages;
	}

	private void addVisiblePackagesFromState(StateHelper helper, BundleDescription model, Map packages) {
		ExportPackageDescription[] exports = helper.getVisiblePackages(model);
		for (int i = 0; i < exports.length; i++) {
			BundleDescription exporter = exports[i].getExporter();
			if (exporter == null)
				continue;

			boolean discouraged = helper.getAccessCode(model, exports[i]) == StateHelper.ACCESS_DISCOURAGED;
			String pattern = exports[i].getName().replaceAll("\\.", "/") + "/*"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String rule = (discouraged ? '~' : '+') + pattern;

			String packagesKey = exporter.getSymbolicName() + "_" + exporter.getVersion(); //$NON-NLS-1$
			String rules = (String) packages.get(packagesKey);
			if (rules != null) {
				if (rules.indexOf(rule) == -1)
					rules = rules + File.pathSeparator + rule;
			} else {
				rules = rule;
			}

			packages.put(packagesKey, rules);
		}
	}

	/**
	 * Add the specified plugin (including its jars) and its fragments 
	 * @param plugin
	 * @param classpath
	 * @param location
	 * @throws CoreException
	 */
	private void addPlugin(BundleDescription plugin, List classpath, String location) throws CoreException {
		boolean allFragments = true;
		String patchInfo = (String) generator.getSite(false).getRegistry().getPatchData().get(new Long(plugin.getBundleId()));
		if (patchInfo != null && plugin != generator.getModel()) {
			addFragmentsLibraries(plugin, classpath, location, false, false);
			allFragments = false;
		}

		requiredIds.add(new Long(plugin.getBundleId()));

		addRuntimeLibraries(plugin, classpath, location);
		addFragmentsLibraries(plugin, classpath, location, true, allFragments);
	}

	/**
	 * Add the runtime libraries for the specified plugin. 
	 * @param model
	 * @param classpath
	 * @param baseLocation
	 * @throws CoreException
	 */
	private void addRuntimeLibraries(BundleDescription model, List classpath, String baseLocation) throws CoreException {
		String[] libraries = getClasspathEntries(model);
		String root = generator.getLocation(model);
		IPath base = Utils.makeRelative(new Path(root), new Path(baseLocation));
		Properties modelProps = getBuildPropertiesFor(model);
		if (modelProps != AbstractScriptGenerator.MissingProperties.getInstance())
			ModelBuildScriptGenerator.specialDotProcessing(modelProps, libraries);
		for (int i = 0; i < libraries.length; i++) {
			addDevEntries(model, baseLocation, classpath, Utils.getArrayFromString(modelProps.getProperty(PROPERTY_OUTPUT_PREFIX + libraries[i])), modelProps);
			addPathAndCheck(model, base, libraries[i], modelProps, classpath);
		}
	}

	/**
	 * Add all fragments of the given plugin
	 * @param plugin
	 * @param classpath
	 * @param baseLocation
	 * @throws CoreException
	 */
	private void addFragmentsLibraries(BundleDescription plugin, List classpath, String baseLocation, boolean afterPlugin, boolean all) throws CoreException {
		// if plugin is not a plugin, it's a fragment and there is no fragment for a fragment. So we return.
		BundleDescription[] fragments = plugin.getFragments();
		if (fragments == null)
			return;

		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i] == generator.getModel())
				continue;
			if (matchFilter(fragments[i]) == false)
				continue;

			requiredIds.add(new Long(fragments[i].getBundleId()));

			if (!afterPlugin && isPatchFragment(fragments[i])) {
				addPluginLibrariesToFragmentLocations(plugin, fragments[i], classpath, baseLocation);
				addRuntimeLibraries(fragments[i], classpath, baseLocation);
				continue;
			}
			if ((afterPlugin && !isPatchFragment(fragments[i])) || all) {
				addRuntimeLibraries(fragments[i], classpath, baseLocation);
				addPluginLibrariesToFragmentLocations(plugin, fragments[i], classpath, baseLocation);
				continue;
			}
		}
	}

	private boolean isPatchFragment(BundleDescription fragment) throws CoreException {
		return generator.getSite(false).getRegistry().getPatchData().get(new Long(fragment.getBundleId())) != null;
	}

	/**
	 * There are cases where the plug-in only declares a library but the real JAR is under
	 * a fragment location. This method gets all the plugin libraries and place them in the
	 * possible fragment location.
	 * 
	 * @param plugin
	 * @param fragment
	 * @param classpath
	 * @param baseLocation
	 * @throws CoreException
	 */
	private void addPluginLibrariesToFragmentLocations(BundleDescription plugin, BundleDescription fragment, List classpath, String baseLocation) throws CoreException {
		//TODO This methods causes the addition of a lot of useless entries. See bug #35544
		//If we reintroduce the test below, we reintroduce the problem 35544	
		//	if (fragment.getRuntime() != null)
		//		return;
		String[] libraries = getClasspathEntries(plugin);

		String root = generator.getLocation(fragment);
		IPath base = Utils.makeRelative(new Path(root), new Path(baseLocation));
		Properties modelProps = getBuildPropertiesFor(fragment);
		for (int i = 0; i < libraries.length; i++) {
			addPathAndCheck(fragment, base, libraries[i], modelProps, classpath);
		}
	}

	private Properties getBuildPropertiesFor(BundleDescription bundle) {
		try {
			Properties bundleProperties = AbstractScriptGenerator.readProperties(generator.getLocation(bundle), PROPERTIES_FILE, IStatus.OK);
			if (Utils.isStringIn(generator.getClasspathEntries(bundle), ModelBuildScriptGenerator.DOT) != -1) {
				String sourceFolder = bundleProperties.getProperty(PROPERTY_SOURCE_PREFIX + ModelBuildScriptGenerator.DOT);
				if (sourceFolder != null) {
					bundleProperties.setProperty(PROPERTY_SOURCE_PREFIX + ModelBuildScriptGenerator.EXPANDED_DOT, sourceFolder);
					bundleProperties.remove(PROPERTY_SOURCE_PREFIX + ModelBuildScriptGenerator.DOT);
				}
				String outputValue = bundleProperties.getProperty(PROPERTY_OUTPUT_PREFIX + ModelBuildScriptGenerator.DOT);
				if (outputValue != null) {
					bundleProperties.setProperty(PROPERTY_OUTPUT_PREFIX + ModelBuildScriptGenerator.EXPANDED_DOT, outputValue);
					bundleProperties.remove(PROPERTY_OUTPUT_PREFIX + ModelBuildScriptGenerator.DOT);
				}
			}
			return bundleProperties;
		} catch (CoreException e) {
			//ignore
		}
		return null;
	}

	// Add a path into the classpath for a given model
	// pluginId the plugin we are adding to the classpath
	// basePath : the relative path between the plugin from which we are adding the classpath and the plugin that is requiring this entry 
	// classpath : The classpath in which we want to add this path 
	private void addPathAndCheck(BundleDescription model, IPath basePath, String libraryName, Properties modelProperties, List classpath) {
		String pluginKey = model != null ? model.getSymbolicName() + "_" + model.getVersion() : null; //$NON-NLS-1$
		String rules = ""; //$NON-NLS-1$
		//only add access rules to libraries that are not part of the current bundle
		//and are not this bundle's host if we are a fragment
		BundleDescription currentBundle = generator.getModel();
		if (model != null && model != currentBundle && (currentBundle.getHost() == null || currentBundle.getHost().getSupplier() != model)) {
			String packageKey = pluginKey;
			if (model.isResolved() && model.getHost() != null) {
				BundleDescription host = (BundleDescription) model.getHost().getSupplier();
				packageKey = host.getSymbolicName() + "_" + host.getVersion(); //$NON-NLS-1$
			}
			if (visiblePackages.containsKey(packageKey)) {
				rules = "[" + (String) visiblePackages.get(packageKey) + File.pathSeparator + EXCLUDE_ALL_RULE + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				rules = "[" + EXCLUDE_ALL_RULE + "]"; //$NON-NLS-1$//$NON-NLS-2$
			}
		}

		String path = null;
		String subPath = null;
		Path libraryPath = new Path(libraryName);
		if (libraryPath.isAbsolute()) {
			path = libraryPath.toOSString();
		} else if ("jar".equalsIgnoreCase(basePath.getFileExtension())) { //$NON-NLS-1$
			if ("jar".equalsIgnoreCase(libraryPath.getFileExtension())) //$NON-NLS-1$
				subPath = libraryPath.toOSString();
			path = basePath.toOSString();
		} else {
			path = basePath.append(libraryPath).toOSString();
		}
		path = ModelBuildScriptGenerator.replaceVariables(path, pluginKey == null ? false : generator.getCompiledElements().contains(pluginKey));
		String secondaryPath = null;
		if (generator.getCompiledElements().contains(pluginKey)) {
			if (modelProperties == null || modelProperties.getProperty(IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX + libraryName) != null)
				path = Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER) + '/' + path;
			secondaryPath = Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER) + "/../" + model.getSymbolicName() + '_' + model.getVersion() + '/' + libraryName; //$NON-NLS-1$

		}

		addClasspathElementWithRule(classpath, path, subPath, rules);
		if (secondaryPath != null) {
			addClasspathElementWithRule(classpath, secondaryPath, null, rules);
		}
	}

	private void addClasspathElementWithRule(List classpath, String path, String subPath, String rules) {
		path = normalize(path);
		subPath = normalize(subPath);

		String elementsKey = subPath != null ? path + '/' + subPath : path;
		ClasspathElement existing = (ClasspathElement) pathElements.get(elementsKey);
		if (existing != null) {
			existing.addRules(rules);
		} else {
			ClasspathElement element = new ClasspathElement(path, subPath, rules);
			classpath.add(element);
			pathElements.put(elementsKey, element);
		}
	}

	private void addSelf(BundleDescription model, ModelBuildScriptGenerator.CompiledEntry jar, List classpath, String location, List pluginChain, Set addedPlugins) throws CoreException {
		// If model is a fragment, we need to add in the classpath the plugin to which it is related
		HostSpecification host = model.getHost();
		if (host != null) {
			BundleDescription[] hosts = host.getHosts();
			for (int i = 0; i < hosts.length; i++)
				addPluginAndPrerequisites(hosts[i], classpath, location, pluginChain, addedPlugins);
		}

		// Add the libraries
		Properties modelProperties = generator.getBuildProperties();
		String jarOrder = (String) modelProperties.get(PROPERTY_JAR_ORDER);
		if (jarOrder == null) {
			// if no jar order was specified in build.properties, we add all the libraries but the current one
			// based on the order specified by the plugin.xml. Both library that we compile and .jar provided are processed
			String[] libraries = getClasspathEntries(model);
			if (libraries != null) {
				for (int i = 0; i < libraries.length; i++) {
					String libraryName = libraries[i];
					if (jar.getName(false).equals(libraryName))
						continue;

					boolean isSource = (modelProperties.getProperty(PROPERTY_SOURCE_PREFIX + libraryName) != null);
					if (isSource) {
						addDevEntries(model, location, classpath, Utils.getArrayFromString(modelProperties.getProperty(PROPERTY_OUTPUT_PREFIX + libraryName)), modelProperties);
					}
					//Potential pb: here there maybe a nasty case where the libraries variable may refer to something which is part of the base
					//but $xx$ will replace it by the $xx instead of $basexx. The solution is for the user to use the explicitly set the content
					// of its build.property file
					addPathAndCheck(model, Path.EMPTY, libraryName, modelProperties, classpath);
				}
			}
		} else {
			// otherwise we add all the predecessor jars
			String[] order = Utils.getArrayFromString(jarOrder);
			for (int i = 0; i < order.length; i++) {
				if (order[i].equals(jar.getName(false)))
					break;
				addDevEntries(model, location, classpath, Utils.getArrayFromString((String) modelProperties.get(PROPERTY_OUTPUT_PREFIX + order[i])), modelProperties);
				addPathAndCheck(model, Path.EMPTY, order[i], modelProperties, classpath);
			}
			// Then we add all the "pure libraries" (the one that does not contain source)
			String[] libraries = getClasspathEntries(model);
			for (int i = 0; i < libraries.length; i++) {
				String libraryName = libraries[i];
				if (modelProperties.get(PROPERTY_SOURCE_PREFIX + libraryName) == null) {
					//Potential pb: if the pure library is something that is being compiled (which is supposetly not the case, but who knows...)
					//the user will get $basexx instead of $ws 
					addPathAndCheck(model, Path.EMPTY, libraryName, modelProperties, classpath);
				}
			}
		}

		// add extra classpath if it exists. this code is kept for backward compatibility
		String extraClasspath = (String) modelProperties.get(PROPERTY_JAR_EXTRA_CLASSPATH);
		if (extraClasspath != null) {
			String[] extra = Utils.getArrayFromString(extraClasspath, ";,"); //$NON-NLS-1$

			for (int i = 0; i < extra.length; i++) {
				//Potential pb: if the path refers to something that is being compiled (which is supposetly not the case, but who knows...)
				//the user will get $basexx instead of $ws 
				String[] toAdd = computeExtraPath(extra[i], classpath, location);
				if (toAdd != null && toAdd.length == 2)
					addPathAndCheck(null, new Path(toAdd[0]), toAdd[1], modelProperties, classpath);
			}
		}

		//	add extra classpath if it is specified for the given jar
		String[] jarSpecificExtraClasspath = jar.getExtraClasspath();
		for (int i = 0; i < jarSpecificExtraClasspath.length; i++) {
			//Potential pb: if the path refers to something that is being compiled (which is supposetly not the case, but who knows...)
			//the user will get $basexx instead of $ws 
			String[] toAdd = computeExtraPath(jarSpecificExtraClasspath[i], classpath, location);
			if (toAdd != null && toAdd.length == 2)
				addPathAndCheck(null, new Path(toAdd[0]), toAdd[1], modelProperties, classpath);
		}
	}

	/** 
	 * Convenience method that compute the relative classpath of extra.classpath entries  
	 * @param url a url
	 * @param location location used as a base location to compute the relative path 
	 * @return String the relative path 
	 * @throws CoreException
	 */
	private String[] computeExtraPath(String url, List classpath, String location) throws CoreException {
		String relativePath = null;

		String[] urlfragments = Utils.getArrayFromString(url, "/"); //$NON-NLS-1$

		// A valid platform url for a plugin has a leat 3 segments.
		if (urlfragments.length > 2 && urlfragments[0].equals(PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR)) {
			String bundleLocation = null;
			BundleDescription bundle = null;
			if (urlfragments[1].equalsIgnoreCase(PLUGIN) || urlfragments[1].equalsIgnoreCase(FRAGMENT)) {
				bundle = generator.getSite(false).getRegistry().getResolvedBundle(urlfragments[2]);
				if (bundle == null) {
					String message = NLS.bind(Messages.exception_url, generator.getModel().getSymbolicName() + '/' + generator.getPropertiesFileName() + ": " + url); //$NON-NLS-1$
					MultiStatus status = new MultiStatus(PI_PDEBUILD, EXCEPTION_MALFORMED_URL, message, null);
					message = NLS.bind(Messages.exception_missingElement, urlfragments[2]);
					status.add(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
					throw new CoreException(status);
				}

				if (urlfragments.length == 3) {
					addPlugin(bundle, classpath, location);
					return null;
				}

				bundleLocation = generator.getLocation(bundle);
				if (bundleLocation != null) {
					String entry = urlfragments[3];
					for (int i = 4; i < urlfragments.length; i++) {
						entry += '/' + urlfragments[i];
					}
					return new String[] {Utils.makeRelative(new Path(bundleLocation), new Path(location)).toOSString(), entry};
				}
			} else if (urlfragments[1].equalsIgnoreCase("resource")) { //$NON-NLS-1$
				String message = NLS.bind(Messages.exception_url, generator.getModel().getSymbolicName() + '/' + generator.getPropertiesFileName() + ": " + url); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, message, null));
			}
		}

		// Then it's just a regular URL, or just something that will be added at the end of the classpath for backward compatibility.......
		try {
			URL extraURL = new URL(url);
			try {
				relativePath = Utils.makeRelative(new Path(FileLocator.resolve(extraURL).getFile()), new Path(location)).toOSString();
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_url, generator.getModel().getSymbolicName() + '/' + generator.getPropertiesFileName() + ": " + url); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, message, e));
			}
		} catch (MalformedURLException e) {
			relativePath = url;
			//TODO remove this backward compatibility support for as soon as we go to 2.2 and put back the exception
			//		String message = Policy.bind("exception.url", PROPERTIES_FILE + "::"+url); //$NON-NLS-1$  //$NON-NLS-2$
			//		throw new CoreException(new Status(IStatus.ERROR,PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_MALFORMED_URL, message,e));
		}
		return new String[] {relativePath, ""}; //$NON-NLS-1$
	}

	//Add the prerequisite of a given plugin (target)
	private void addPrerequisites(BundleDescription target, List classpath, String baseLocation, List pluginChain, Set addedPlugins) throws CoreException {
		if (pluginChain.contains(target)) {
			if (allowBinaryCycles && isAllowableCycle(target, pluginChain)) {
				return;
			}
			// else exception
			String cycleString = ""; //$NON-NLS-1$
			for (Iterator iter = pluginChain.iterator(); iter.hasNext();)
				cycleString += iter.next().toString() + ", "; //$NON-NLS-1$
			cycleString += target.toString();
			String message = NLS.bind(Messages.error_pluginCycle, cycleString);
			throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_CLASSPATH_CYCLE, message, null));
		}
		if (addedPlugins.contains(target)) //the plugin we are considering has already been added	
			return;

		// add libraries from pre-requisite plug-ins.  Don't worry about the export flag
		// as all required plugins may be required for compilation.
		BundleDescription[] requires = PDEState.getDependentBundles(target);
		pluginChain.add(target);
		for (int i = 0; i < requires.length; i++) {
			addPluginAndPrerequisites(requires[i], classpath, baseLocation, pluginChain, addedPlugins);
		}
		pluginChain.remove(target);
		addedPlugins.add(target);
	}

	/* We can allow a cycle if it only contains 1 bundle that needs to be built and the rest are  binary. */
	private boolean isAllowableCycle(BundleDescription target, List pluginChain) {
		boolean haveNonBinary = false;
		boolean inCycle = false;
		for (Iterator iterator = pluginChain.iterator(); iterator.hasNext();) {
			BundleDescription bundle = (BundleDescription) iterator.next();
			if (bundle == target) {
				inCycle = true;
				haveNonBinary = !Utils.isBinary(bundle);
				continue;
			}
			if (inCycle && !Utils.isBinary(bundle)) {
				if (haveNonBinary)
					return false;
				haveNonBinary = true;
			}
		}
		return true;
	}

	/**
	 * The pluginChain parameter is used to keep track of possible cycles. If prerequisite is already
	 * present in the chain it is not included in the classpath.
	 * 
	 * @param target : the plugin for which we are going to introduce
	 * @param classpath 
	 * @param baseLocation
	 * @param pluginChain
	 * @param addedPlugins
	 * @throws CoreException
	 */
	private void addPluginAndPrerequisites(BundleDescription target, List classpath, String baseLocation, List pluginChain, Set addedPlugins) throws CoreException {
		if (matchFilter(target) == false)
			return;

		addPlugin(target, classpath, baseLocation);
		addPrerequisites(target, classpath, baseLocation, pluginChain, addedPlugins);
	}

	private boolean matchFilter(BundleDescription target) {
		Filter filter = BundleHelper.getDefault().getFilter(target);
		if (filter == null) //Target is platform independent, add it 
			return true;

		FeatureEntry associatedEntry = generator.getAssociatedEntry();
		if (associatedEntry == null)
			return true;

		String os = associatedEntry.getOS();
		String ws = associatedEntry.getWS();
		String arch = associatedEntry.getArch();
		String nl = associatedEntry.getNL();
		if (os == null && ws == null && arch == null && nl == null) //I'm a platform independent plugin
			return true;

		//The plugin for which we are generating the classpath and target are not platform independent
		Dictionary properties = new Hashtable(3);
		if (os != null) {
			Object value = os.indexOf(',') > -1 ? (Object) Utils.getArrayFromString(os, ",") : os; //$NON-NLS-1$
			properties.put(OSGI_OS, value);
		} else {
			properties.put(OSGI_OS, CatchAllValue.singleton);
		}
		if (ws != null) {
			Object value = ws.indexOf(',') > -1 ? (Object) Utils.getArrayFromString(ws, ",") : ws; //$NON-NLS-1$
			properties.put(OSGI_WS, value);
		} else
			properties.put(OSGI_WS, CatchAllValue.singleton);

		if (arch != null) {
			Object value = arch.indexOf(',') > -1 ? (Object) Utils.getArrayFromString(arch, ",") : arch; //$NON-NLS-1$
			properties.put(OSGI_ARCH, value);
		} else
			properties.put(OSGI_ARCH, CatchAllValue.singleton);

		if (nl != null) {
			Object value = nl.indexOf(',') > -1 ? (Object) Utils.getArrayFromString(nl, ",") : nl; //$NON-NLS-1$
			properties.put(OSGI_NL, value);
		} else
			properties.put(OSGI_NL, CatchAllValue.singleton);

		return filter.match(properties);
	}

	/**
	 * 
	 * @param model
	 * @param baseLocation
	 * @param classpath
	 */
	private void addDevEntries(BundleDescription model, String baseLocation, List classpath, String[] jarSpecificEntries, Properties modelProperties) {
		if (generator.devEntries == null && (jarSpecificEntries == null || jarSpecificEntries.length == 0))
			return;

		String[] entries;
		// if jarSpecificEntries is given, then it overrides devEntries 
		if (jarSpecificEntries != null && jarSpecificEntries.length > 0)
			entries = jarSpecificEntries;
		else
			entries = generator.devEntries.getDevClassPath(model.getSymbolicName());

		IPath root = Utils.makeRelative(new Path(generator.getLocation(model)), new Path(baseLocation));
		for (int i = 0; i < entries.length; i++) {
			addPathAndCheck(model, root, entries[i], modelProperties, classpath);
		}
	}

	//Return the jar name from the classpath 
	private String[] getClasspathEntries(BundleDescription bundle) throws CoreException {
		return generator.getClasspathEntries(bundle);
	}
}
