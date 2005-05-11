/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;

public class PackageConfigScriptGenerator extends AssembleConfigScriptGenerator {
	
	protected void generateGatherBinPartsCalls() { //TODO Here we should try to use cp because otherwise we will loose the permissions
		String excludedFiles = "build.properties, .project, .classpath"; //$NON-NLS-1$
		for (int i = 0; i < plugins.length; i++) {
			Path pluginLocation = new Path(plugins[i].getLocation());
			boolean isFolder = isFolder(pluginLocation);
			if (isFolder) {
				script.printCopyTask(null, Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + ModelBuildScriptGenerator.getNormalizedName(plugins[i]), new FileSet[] {new FileSet(pluginLocation.toOSString(), null, null, null, excludedFiles, null, null)}, false, false);
			} else {
				script.printCopyFileTask(pluginLocation.toOSString(), Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + pluginLocation.lastSegment(), false);
			}
		}

		for (int i = 0; i < features.length; i++) {
			IPath featureLocation = new Path(features[i].getURL().getPath()); // Here we assume that all the features are local
			featureLocation = featureLocation.removeLastSegments(1);
			script.printCopyTask(null, Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + FeatureBuildScriptGenerator.getNormalizedName(features[i]), new FileSet[] {new FileSet(featureLocation.toOSString(), null, null, null, null, null, null)}, false, false);
		}
		
		if (rootFileProviders.size() != 0) {
			//When the root files are copied, it is assumed that are all in a folder called eclipse
			FileSet rootFiles = new FileSet(Utils.getPropertyFormat("tempDirectory") + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + "/eclipse", null, "**/**", null, null, null, null);
			String target = Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER);
			script.printCopyTask(null, target, new FileSet[] { rootFiles }, false, false);
		}
	}
	
	public String getTargetName() {
		return "package" + (featureId.equals("") ? "" : ('.' + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}
	
	private boolean isFolder(Path pluginLocation) {
		return pluginLocation.toFile().isDirectory();
	}
	
	public void setPackagingPropertiesLocation(String packagingPropertiesLocation) throws CoreException {
		Properties packagingProperties = new Properties();
		if (packagingPropertiesLocation == null || packagingPropertiesLocation.equals("")) //$NON-NLS-1$
			return;

		InputStream propertyStream = null;
		try {
			propertyStream = new BufferedInputStream(new FileInputStream(packagingPropertiesLocation));
			try {
				packagingProperties.load(new BufferedInputStream(propertyStream));
			} finally {
				propertyStream.close();
			}
		} catch (FileNotFoundException e) {
			String message = NLS.bind(Messages.exception_readingFile, packagingPropertiesLocation);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_readingFile, packagingPropertiesLocation);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		}
		//Add a marker to trigger
		if (packagingProperties.size() != 0) {
			rootFileProviders = new ArrayList(1);
			rootFileProviders.add("elt");	
		}
	}
	
	protected void generateGatherSourceCalls() {
		//In the packager, we do not gather source
	}
}
