/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.apache.tools.ant.filters.ReplaceTokens;
import org.apache.tools.ant.util.ReaderInputStream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.build.tests.Activator;
import org.eclipse.pde.internal.build.FeatureGenerator;

public class Utils {

	/**
	 * Transfer the contents of resource into the destination IFile.  During the transfer, replace all 
	 * instances of "@replaceTag@" with "replaceString"
	 * 
	 * @param resource		- input URL
	 * @param destination	- IFile destination
	 * @param replacements	- map of tokens and values to replaces, file should contain "@replaceTag@"
	 * @throws IOException 
	 * @throws CoreException 
	 */
	static public void transferAndReplace(URL resource, IFile destination, Map replacements) throws IOException, CoreException {
		Reader reader = new InputStreamReader(new BufferedInputStream(resource.openStream()));
		final ReplaceTokens replaces = new ReplaceTokens(reader);
		
		for (Iterator iterator = replacements.keySet().iterator(); iterator.hasNext();) {
			String replaceTag = (String) iterator.next();
			String replaceString = (String) replacements.get(replaceTag);
			ReplaceTokens.Token token = new ReplaceTokens.Token();
			token.setKey(replaceTag);
			token.setValue(replaceString);
			replaces.addConfiguredToken(token);
		}
			

		ReaderInputStream inputStream = new ReaderInputStream(replaces);
		destination.create(inputStream, true, null);
		inputStream.close();
	}

	static public void generateAllElements(IFolder buildFolder, String element) throws CoreException, IOException {
		if (element != null) {
			// get the productBuild/allElements.xml and replace @ELEMENT@ with element
			URL allElements = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/resources/allElements.xml"), null);
			Map replacements = new HashMap(1);
			replacements.put( "ELEMENT", element);
			transferAndReplace(allElements, buildFolder.getFile("allElements.xml"), replacements);
			buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
	}

	static public void generatePluginBuildProperties(IFolder folder, Properties properties) throws FileNotFoundException, IOException {
		Properties buildProperties = properties != null ? properties : new Properties();

		//default contents:
		if (!buildProperties.containsKey("source.."))
			buildProperties.put("source..", "src/");
		if (!buildProperties.containsKey("output.."))
			buildProperties.put("output..", "bin/");
		if (!buildProperties.containsKey("bin.includes"))
			buildProperties.put("bin.includes", "META-INF/, .");

		storeBuildProperties(folder, buildProperties);
	}

	static public void generateBundleManifest(IFolder folder, String bundleId, String bundleVersion, Attributes additionalAttributes) throws CoreException, IOException {
		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Name.MANIFEST_VERSION, "1.0");
		mainAttributes.put(new Name("Bundle-ManifestVersion"), "2");
		mainAttributes.put(new Name("Bundle-Name"), "Test Bundle " + bundleId);
		mainAttributes.put(new Name("Bundle-SymbolicName"), bundleId);
		mainAttributes.put(new Name("Bundle-Version"), bundleVersion);
		if (additionalAttributes != null)
			mainAttributes.putAll(additionalAttributes);

		IFolder metaInf = folder.getFolder("META-INF");
		if (!metaInf.exists())
			metaInf.create(true, true, null);
		IFile manifestFile = metaInf.getFile("manifest.mf");
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(manifestFile.getLocation().toFile()));
		manifest.write(outputStream);
		outputStream.close();
	}

	static public void generateBundle(IFolder folder, String bundleId) throws CoreException, IOException {
		generateBundleManifest(folder, bundleId, "1.0.0", null);
		generatePluginBuildProperties(folder, null);
	}
	
	static public void storeBuildProperties(IFolder buildFolder, Properties buildProperties) throws FileNotFoundException, IOException {
		File buildPropertiesFile = new File(buildFolder.getLocation().toFile(), "build.properties");
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(buildPropertiesFile));
		buildProperties.store(outputStream, "");
		outputStream.close();
	}
	
	static public void generateFeature(IFolder workingDirectory, String id, String[] featureList, String[] pluginList ) throws CoreException {
		FeatureGenerator generator = new FeatureGenerator();
		generator.setIncludeLaunchers(false);
		generator.setVerify(false);
		generator.setFeatureId(id);
		generator.setFeatureList(featureList);
		generator.setPluginList(pluginList);
		generator.setWorkingDirectory(workingDirectory.getLocation().toOSString());
		generator.generate();
	}
}
