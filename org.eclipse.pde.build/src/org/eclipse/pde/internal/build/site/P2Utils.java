/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.*;

/**
 * Temporary utilities until P2 and FrameworkAdmin are graduated into the SDK.
 * 
 * @since 3.4
 */
public class P2Utils {

	/**
	 * Returns bundles defined by the 'bundles.info' file in the
	 * specified location, or <code>null</code> if none. The "bundles.info" file
	 * is assumed to be at a fixed relative location to the specified file.  This 
	 * method will also look for a "source.info".  If available, any source
	 * bundles found will also be added to the returned list.
	 * 
	 * @param platformHome absolute path in the local file system to an installation
	 * @return URLs of all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.info
	 */
	public static URL[] readBundlesTxt(String platformHome) {
		SimpleConfiguratorManipulator manipulator = (SimpleConfiguratorManipulator) BundleHelper.getDefault().acquireService(SimpleConfiguratorManipulator.class.getName());

		File root = new File(platformHome);
		File bundlesTxt = new File(root, "configuration/" + SimpleConfiguratorManipulator.BUNDLES_INFO_PATH); //$NON-NLS-1$
		File sourceTxt = new File(root, "configuration/" + SimpleConfiguratorManipulator.SOURCE_INFO_PATH); //$NON-NLS-1$

		List infos = new ArrayList();
		try {
			//streams are closed for us
			if (bundlesTxt.exists())
				infos.addAll(Arrays.asList(manipulator.loadConfiguration(new FileInputStream(bundlesTxt), root.toURI())));
			if (sourceTxt.exists())
				infos.addAll(Arrays.asList(manipulator.loadConfiguration(new FileInputStream(sourceTxt), root.toURI())));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		URL[] bundles = null;
		if (infos.size() > 0) {
			bundles = new URL[infos.size()];
			for (int i = 0; i < bundles.length; i++) {
				try {
					bundles[i] = new File(((BundleInfo) infos.get(i)).getLocation()).toURL();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			bundles = new URL[0];
		}
		return bundles;
	}

	/**
	 * Creates a bundles.info file in the given directory containing the name,
	 * version, location, start level and expected state of every bundle in the
	 * given collection.  Will also create a source.info file containing
	 * a lsit of all source bundles found in the given collection. Uses special 
	 * defaults for the start level and auto start settings. Returns the URL 
	 * location of the bundle.txt or <code>null</code> if there was a problem 
	 * creating it.
	 * 
	 * @param bundles collection of IPluginModelBase objects to write into the bundles.info/source.info
	 * @param directory directory to create the bundles.info and source.info files in
	 * @return URL location of the bundles.info or <code>null</code>
	 */
	public static File writeBundlesTxt(Collection bundles, File directory, ProductFile productFile, boolean refactoredRuntime) {
		List bundleInfos = new ArrayList(bundles.size());
		List sourceInfos = new ArrayList(bundles.size());
		ShapeAdvisor advisor = new ShapeAdvisor();

		int defaultStartLevel = 4;
		Properties props = productFile != null ? productFile.getConfigProperties() : null;
		if (props != null && props.containsKey("osgi.bundles.defaultStartLevel")) { //$NON-NLS-1$
			try {
				defaultStartLevel = Integer.parseInt(props.getProperty("osgi.bundles.defaultStartLevel")); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				//ignore and keep 4
			}
		}

		Map userInfos = productFile != null ? productFile.getConfigurationInfo() : null;

		for (Iterator iterator = bundles.iterator(); iterator.hasNext();) {
			BundleDescription desc = (BundleDescription) iterator.next();
			if (desc != null) {
				String modelName = desc.getSymbolicName();

				URI location = null;
				try {
					location = new URI("plugins/" + (String) advisor.getFinalShape(desc)[0]); //$NON-NLS-1$
				} catch (URISyntaxException e) {
					continue;
				}
				BundleInfo info = new BundleInfo();
				info.setLocation(location);
				info.setSymbolicName(modelName);
				info.setVersion(desc.getVersion().toString());
				if (userInfos != null && userInfos.size() > 0) {
					if (userInfos.containsKey(modelName)) {
						BundleInfo userInfo = (BundleInfo) userInfos.get(modelName);
						int start = userInfo.getStartLevel();
						if (start <= 0)
							start = defaultStartLevel;
						info.setStartLevel(start);
						info.setMarkedAsStarted(userInfo.isMarkedAsStarted());
					} else {
						info.setStartLevel(defaultStartLevel);
						info.setMarkedAsStarted(false);
					}
				} else {
					if (IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR.equals(modelName)) {
						info.setStartLevel(1);
						info.setMarkedAsStarted(true);
					} else if (IPDEBuildConstants.BUNDLE_EQUINOX_COMMON.equals(modelName)) {
						info.setStartLevel(2);
						info.setMarkedAsStarted(true);
					} else if (IPDEBuildConstants.BUNDLE_OSGI.equals(modelName)) {
						info.setStartLevel(-1);
						info.setMarkedAsStarted(true);
					} else if (IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR.equals(modelName)) {
						info.setStartLevel(3);
						info.setMarkedAsStarted(true);
					} else if (IPDEBuildConstants.BUNDLE_CORE_RUNTIME.equals(modelName)) {
						info.setStartLevel(refactoredRuntime ? 4 : 2);
						info.setMarkedAsStarted(true);
					} else if (IPDEBuildConstants.BUNDLE_DS.equals(modelName)) {
						info.setStartLevel(2);
						info.setMarkedAsStarted(true);
					} else {
						info.setStartLevel(defaultStartLevel);
						info.setMarkedAsStarted(false);
					}
				}
				if (Utils.isSourceBundle(desc))
					sourceInfos.add(info);
				else
					bundleInfos.add(info);
			}
		}

		File bundlesTxt = new File(directory, SimpleConfiguratorManipulator.BUNDLES_INFO_PATH);
		File srcBundlesTxt = new File(directory, SimpleConfiguratorManipulator.SOURCE_INFO_PATH);
		File base = directory.getParentFile();

		BundleInfo[] infos = (BundleInfo[]) bundleInfos.toArray(new BundleInfo[bundleInfos.size()]);
		BundleInfo[] sources = (BundleInfo[]) sourceInfos.toArray(new BundleInfo[sourceInfos.size()]);

		SimpleConfiguratorManipulator manipulator = (SimpleConfiguratorManipulator) BundleHelper.getDefault().acquireService(SimpleConfiguratorManipulator.class.getName());
		try {
			manipulator.saveConfiguration(infos, bundlesTxt, base.toURI());
			manipulator.saveConfiguration(sources, srcBundlesTxt, base.toURI());
		} catch (IOException e) {
			return null;
		}

		return bundlesTxt;
	}
}
