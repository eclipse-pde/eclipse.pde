/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.internal.build.*;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** 
 * @deprecated
 */
public class PluginRegistryConverter extends PDEState {
	private PluginRegistryModel registry;

	private URL[] removeInvalidURLs(URL[] files) {
		URL[] validURLs = new URL[files.length];
		int validURL = 0;
		for (int i = 0; i < files.length; i++) {
			if (!files[i].toExternalForm().endsWith("feature.xml") && !files[i].toExternalForm().endsWith("MANIFEST.MF"))  //$NON-NLS-1$//$NON-NLS-2$
				validURLs[validURL++] = files[i];
		}
		if (files.length == validURL)
			return validURLs;
		URL[] result = new URL[validURL];
		System.arraycopy(validURLs, 0, result, 0, validURL);
		return result;
	}

	private PluginRegistryModel getPluginRegistry(URL[] files) throws CoreException {
		if (registry == null) {
			files = removeInvalidURLs(files);
			// create the registry according to the site where the code to compile is, and a existing installation of eclipse 
			MultiStatus problems = new MultiStatus(IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_MODEL_PARSE, Messages.exception_pluginParse, null);
			Factory factory = new Factory(problems);
			registry = PluginRegistryModel.parsePlugins(files, factory);
			registry.resolve(true, false);
			IStatus status = factory.getStatus();
			if (!status.isOK())
				throw new CoreException(status);
		}
		return registry;
	}

	public void addRegistryToState() {
		PluginModel[] plugins = registry.getPlugins();
		PluginFragmentModel[] fragments = registry.getFragments();

		for (int i = 0; i < plugins.length; i++) {
			BundleDescription bd = state.getFactory().createBundleDescription(getNextId(), plugins[i].getPluginId(), Version.parseVersion(plugins[i].getVersion()), plugins[i].getLocation(), createBundleSpecification(plugins[i].getRequires()), (HostSpecification) null, null, null, null, true);
			String libs = createClasspath(plugins[i].getRuntime());
			Properties manifest = new Properties();
			if (libs != null)
				manifest.put(Constants.BUNDLE_CLASSPATH, libs);
			loadPropertyFileIn(manifest, new File(fragments[i].getLocation()));
			bd.setUserObject(manifest);
			addBundleDescription(bd);
		}

		for (int i = 0; i < fragments.length; i++) {
			HostSpecification host = state.getFactory().createHostSpecification(fragments[i].getPluginId(), new VersionRange(fragments[i].getPluginVersion()));
			BundleDescription bd = state.getFactory().createBundleDescription(getNextId(), fragments[i].getId(), Version.parseVersion(fragments[i].getVersion()), fragments[i].getLocation(), createBundleSpecification(fragments[i].getRequires()), host, null, null, null, true);
			String libs = createClasspath(fragments[i].getRuntime());
			Properties manifest = new Properties();
			if (libs != null)
				manifest.put(Constants.BUNDLE_CLASSPATH, libs);
			loadPropertyFileIn(manifest, new File(fragments[i].getLocation()));
			bd.setUserObject(manifest);
			addBundleDescription(bd);
		}
	}

	protected BundleSpecification[] createBundleSpecification(PluginPrerequisiteModel[] prereqs) {
		if (prereqs == null)
			return new BundleSpecification[0];
		BundleSpecification[] specs = new BundleSpecification[prereqs.length];
		for (int i = 0; i < prereqs.length; i++) {
			specs[i] = state.getFactory().createBundleSpecification(prereqs[i].getPlugin(), new VersionRange(prereqs[i].getVersion()), prereqs[i].getExport(), prereqs[i].getOptional());
		}
		return specs;
	}

	private String createClasspath(LibraryModel[] libs) {
		if (libs == null || libs.length == 0)
			return null;

		String result = ""; //$NON-NLS-1$
		for (int i = 0; i < libs.length; i++) {
			result += libs[i].getName() + (i == libs.length - 1 ? "" : ","); //$NON-NLS-1$//$NON-NLS-2$
		}
		return result;
	}

	public void addBundles(Collection bundles) {
		try {
			getPluginRegistry(Utils.asURL(bundles));
		} catch (CoreException e) {
			IStatus status = new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_STATE_PROBLEM, Messages.exception_registryResolution, e);
			BundleHelper.getDefault().getLog().log(status);
		}
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			File bundle = (File) iter.next();
			addBundle(bundle);
		}
	}
}
