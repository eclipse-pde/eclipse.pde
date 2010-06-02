/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.PDEState;

public class ClasspathComputer2_1 implements IClasspathComputer, IPDEBuildConstants, IXMLConstants, IBuildPropertiesConstants {
	private final ModelBuildScriptGenerator generator;

	public ClasspathComputer2_1(ModelBuildScriptGenerator modelGenerator) {
		this.generator = modelGenerator;
	}

	/**
	 * Compute the classpath for the given jar.
	 * The path returned conforms to Parent / Self / Prerequisite
	 * 
	 * @param model the plugin containing the jar compiled
	 * @param jar the jar for which the classpath is being compiled
	 * @return String the classpath
	 * @throws CoreException
	 */
	public List getClasspath(BundleDescription model, ModelBuildScriptGenerator.CompiledEntry jar) throws CoreException {
		List classpath = new ArrayList(20);
		List pluginChain = new ArrayList(10);
		Set addedPlugins = new HashSet(20);
		String location = generator.getLocation(model);

		//PARENT
		addPlugin(getPlugin(PI_BOOT, null), classpath, location);

		//SELF
		addSelf(model, jar, classpath, location, pluginChain, addedPlugins);

		//PREREQUISITE
		addPrerequisites(model, classpath, location, pluginChain, addedPlugins);

		return classpath;

	}

	/**
	 * Add the specified plugin (including its jars) and its fragments 
	 * @param plugin
	 * @param classpath
	 * @param location
	 * @throws CoreException
	 */
	private void addPlugin(BundleDescription plugin, List classpath, String location) throws CoreException {
		addRuntimeLibraries(plugin, classpath, location);
		addFragmentsLibraries(plugin, classpath, location);
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
		for (int i = 0; i < libraries.length; i++) {
			addDevEntries(model, baseLocation, classpath, Utils.getArrayFromString(generator.getBuildProperties().getProperty(PROPERTY_OUTPUT_PREFIX + libraries[i])));
			addPathAndCheck(model, base, libraries[i], modelProps, classpath);
		}
	}

	/**
	 * Return the plug-in model object from the plug-in registry for the given
	 * plug-in identifier and version. If the plug-in is not in the registry then
	 * throw an exception.
	 * 
	 * @param id the plug-in identifier
	 * @param version the plug-in version
	 * @return BundleDescription
	 * @throws CoreException if the specified plug-in version does not exist in the registry
	 */
	private BundleDescription getPlugin(String id, String version) throws CoreException {
		return generator.getSite(false).getRegistry().getResolvedBundle(id, version);
	}

	/**
	 * Add all fragments of the given plugin
	 * @param plugin
	 * @param classpath
	 * @param baseLocation
	 * @throws CoreException
	 */
	private void addFragmentsLibraries(BundleDescription plugin, List classpath, String baseLocation) throws CoreException {
		// if plugin is not a plugin, it's a fragment and there is no fragment for a fragment. So we return.
		BundleDescription[] fragments = plugin.getFragments();
		if (fragments == null)
			return;

		for (int i = 0; i < fragments.length; i++) {
			if (fragments[i] == generator.getModel())
				continue;
			addPluginLibrariesToFragmentLocations(plugin, fragments[i], classpath, baseLocation);
			addRuntimeLibraries(fragments[i], classpath, baseLocation);
		}
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
			return AbstractScriptGenerator.readProperties(generator.getLocation(bundle), PROPERTIES_FILE, IStatus.OK);
		} catch (CoreException e) {
			//ignore
		}
		return null;
	}

	// Add a path into the classpath for a given model
	// path : The path to add
	// classpath : The classpath in which we want to add this path 
	private void addPathAndCheck(BundleDescription model, IPath basePath, String libraryName, Properties modelProperties, List classpath) {
		String pluginKey = model != null ? model.getSymbolicName() + "_" + model.getVersion() : null; //$NON-NLS-1$
		String path = basePath.append(libraryName).toString();
		path = ModelBuildScriptGenerator.replaceVariables(path, pluginKey == null ? false : generator.getCompiledElements().contains(pluginKey));
		if (generator.getCompiledElements().contains(pluginKey)) {
			if (modelProperties == null || modelProperties.getProperty("source." + libraryName) != null) //$NON-NLS-1$
				path = Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER) + '/' + path;
		}
		if (!classpath.contains(path))
			classpath.add(path);
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
						addDevEntries(model, location, classpath, Utils.getArrayFromString(modelProperties.getProperty(PROPERTY_OUTPUT_PREFIX + libraryName)));
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
				addDevEntries(model, location, classpath, Utils.getArrayFromString((String) modelProperties.get(PROPERTY_OUTPUT_PREFIX + order[i])));
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
				addPathAndCheck(null, new Path(computeExtraPath(extra[i], location)), "", modelProperties, classpath); //$NON-NLS-1$
			}
		}

		//	add extra classpath if it is specified for the given jar
		String[] jarSpecificExtraClasspath = jar.getExtraClasspath();
		for (int i = 0; i < jarSpecificExtraClasspath.length; i++) {
			//Potential pb: if the path refers to something that is being compiled (which is supposetly not the case, but who knows...)
			//the user will get $basexx instead of $ws 
			addPathAndCheck(null, new Path(computeExtraPath(jarSpecificExtraClasspath[i], location)), "", modelProperties, classpath); //$NON-NLS-1$
		}
	}

	/** 
	 * Convenience method that compute the relative classpath of extra.classpath entries  
	 * @param url a url
	 * @param location location used as a base location to compute the relative path 
	 * @return String the relative path 
	 * @throws CoreException
	 */
	private String computeExtraPath(String url, String location) throws CoreException {
		String relativePath = null;

		String[] urlfragments = Utils.getArrayFromString(url, "/"); //$NON-NLS-1$

		// A valid platform url for a plugin has a leat 3 segments.
		if (urlfragments.length > 2 && urlfragments[0].equals(PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR)) {
			String modelLocation = null;
			if (urlfragments[1].equalsIgnoreCase(PLUGIN))
				modelLocation = generator.getLocation(generator.getSite(false).getRegistry().getResolvedBundle(urlfragments[2]));

			if (urlfragments[1].equalsIgnoreCase(FRAGMENT))
				modelLocation = generator.getLocation(generator.getSite(false).getRegistry().getResolvedBundle(urlfragments[2]));

			if (urlfragments[1].equalsIgnoreCase("resource")) { //$NON-NLS-1$
				String message = NLS.bind(Messages.exception_url, generator.getPropertiesFileName() + "::" + url); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, message, null));
			}
			if (modelLocation != null) {
				for (int i = 3; i < urlfragments.length; i++) {
					if (i == 3)
						modelLocation += urlfragments[i];
					else
						modelLocation += '/' + urlfragments[i];
				}
				return Utils.makeRelative(new Path(modelLocation), new Path(location)).toOSString();
			}
		}

		// Then it's just a regular URL, or just something that will be added at the end of the classpath for backward compatibility.......
		try {
			URL extraURL = new URL(url);
			try {
				relativePath = Utils.makeRelative(new Path(FileLocator.resolve(extraURL).getFile()), new Path(location)).toOSString();
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_url, generator.getPropertiesFileName() + "::" + url); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, message, e));
			}
		} catch (MalformedURLException e) {
			String message = NLS.bind(Messages.exception_url, PROPERTIES_FILE + "::" + url); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_MALFORMED_URL, message, e));
		}
		return relativePath;
	}

	//Add the prerequisite of a given plugin (target)
	private void addPrerequisites(BundleDescription target, List classpath, String baseLocation, List pluginChain, Set addedPlugins) throws CoreException {

		if (pluginChain.contains(target)) {
			if (target == getPlugin(PI_RUNTIME, null))
				return;
			String cycleString = ""; //$NON-NLS-1$
			for (Iterator iter = pluginChain.iterator(); iter.hasNext();)
				cycleString += iter.next().toString() + ", "; //$NON-NLS-1$
			cycleString += target.toString();
			String message = NLS.bind(Messages.error_pluginCycle, cycleString);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_CLASSPATH_CYCLE, message, null));
		}

		if (addedPlugins.contains(target)) {
			return;
		}

		//	The first prerequisite is ALWAYS runtime	
		if (target != getPlugin(PI_RUNTIME, null))
			addPluginAndPrerequisites(getPlugin(PI_RUNTIME, null), classpath, baseLocation, pluginChain, addedPlugins);

		// add libraries from pre-requisite plug-ins.  Don't worry about the export flag
		// as all required plugins may be required for compilation.
		BundleDescription[] requires = PDEState.getDependentBundles(target);
		if (requires != null) {
			pluginChain.add(target);
			for (int i = 0; i < requires.length; i++) {
				BundleDescription plugin = getPlugin(requires[i].getSymbolicName(), requires[i].getVersion().toString());
				if (plugin != null)
					addPluginAndPrerequisites(plugin, classpath, baseLocation, pluginChain, addedPlugins);
			}
			pluginChain.remove(target);
			addedPlugins.add(target);
		}

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
		addPlugin(target, classpath, baseLocation);
		addPrerequisites(target, classpath, baseLocation, pluginChain, addedPlugins);
	}

	/**
	 * 
	 * @param model
	 * @param baseLocation
	 * @param classpath
	 */
	private void addDevEntries(BundleDescription model, String baseLocation, List classpath, String[] jarSpecificEntries) {
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
			addPathAndCheck(model, root, entries[i], null, classpath);
		}
	}

	//Return the jar name from the classpath 
	private String[] getClasspathEntries(BundleDescription bundle) throws CoreException {
		return generator.getClasspathEntries(bundle);
	}
}
