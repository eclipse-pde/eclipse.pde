/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.update.core.SiteManager;

/**
 * Generic super-class for all script generator classes. 
 * It contains basic informations like the script, the configurations, and a location 
 */
public abstract class AbstractScriptGenerator implements IXMLConstants, IPDEBuildConstants {

	private static List configInfos;
	protected static String workingDirectory;

	protected AntScript script;

	static {
		// By default, a generic configuration is set
		configInfos = new ArrayList(1);
		configInfos.add(Config.genericConfig());
	}

	public static List getConfigInfos() {
		return configInfos;
	}
	/**
	 * Starting point for script generation. See subclass implementations for
	 * individual comments.
	 * 
	 * @throws CoreException
	 */
	public abstract void generate() throws CoreException;

	/**
	 * Return a string with the given property name in the format:
	 * <pre>${propertyName}</pre>.
	 * 
	 * @param propertyName the name of the property
	 * @return String
	 */
	public String getPropertyFormat(String propertyName) {
		StringBuffer sb = new StringBuffer();
		sb.append(PROPERTY_ASSIGNMENT_PREFIX);
		sb.append(propertyName);
		sb.append(PROPERTY_ASSIGNMENT_SUFFIX);
		return sb.toString();
	}

	public static void setConfigInfo(String spec) throws CoreException {
		configInfos.clear();
		String[] configs = Utils.getArrayFromStringWithBlank(spec, "&"); //$NON-NLS-1$
		configInfos = new ArrayList(configs.length);
		String[] os = new String[configs.length];
		String[] ws = new String[configs.length];
		String[] archs = new String[configs.length];
		for (int i = 0; i < configs.length; i++) {
			Config aConfig = new Config(configs[i]); //$NON-NLS-1$
			if (aConfig.equals(Config.genericConfig()))
				configInfos.add(Config.genericConfig());
			else
				configInfos.add(aConfig);

			// create a list of all ws, os and arch to feed the SiteManager
			os[i] = aConfig.getOs();
			ws[i] = aConfig.getWs();
			archs[i] = aConfig.getArch();
		}
		SiteManager.setOS(Utils.getStringFromArray(os, ",")); //$NON-NLS-1$
		SiteManager.setWS(Utils.getStringFromArray(ws, ",")); //$NON-NLS-1$
		SiteManager.setOSArch(Utils.getStringFromArray(archs, ",")); //$NON-NLS-1$
	}

	public void setWorkingDirectory(String location) {
		workingDirectory = location;
	}

	/**
	 * Return the file system location for the given plug-in model object.
	 * 
	 * @param model the plug-in
	 * @return String
	 * @throws CoreException if a valid file-system location could not be constructed
	 */
	public String getLocation(PluginModel model) throws CoreException {
		try {
			return new URL(model.getLocation()).getFile();
		} catch (MalformedURLException e) {
			String message = Policy.bind("exception.url"); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, message, e));
		}
	}

	protected Properties readProperties(String location, String fileName) throws CoreException {
		Properties result = new Properties();
		File file = new File(location, fileName);
		try {
			InputStream input = new FileInputStream(file);
			try {
				result.load(input);
			} finally {
				input.close();
			}
		} catch (FileNotFoundException e) {
			// ignore and return the empty Properties object
		} catch (IOException e) {
			String message = Policy.bind("exception.readingFile", file.toString()); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		}
		return result;
	}

	public void openScript(String scriptLocation, String scriptName) throws CoreException {
		if (script != null)
			return;

		try {
			OutputStream scriptStream = new BufferedOutputStream(new FileOutputStream(scriptLocation + "/" + scriptName)); //$NON-NLS-1$
			try {
				script = new AntScript(scriptStream);
			} catch (IOException e) {
				try {
					scriptStream.close();
					String message = Policy.bind("exception.writingFile", scriptLocation + "/" + scriptName); //$NON-NLS-1$ //$NON-NLS-2$
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				} catch (IOException e1) {
					// Ignored		
				}
			}
		} catch (FileNotFoundException e) {
			String message = Policy.bind("exception.writingFile", scriptLocation + "/" + scriptName); //$NON-NLS-1$ //$NON-NLS-2$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
	}

	public void closeScript() {
		script.close();
	}
	protected boolean buildingOSGi = false;
	public void setBuildingOSGi(boolean b) {
		buildingOSGi = b;
	}
	public boolean getBuildingOSGi() {
		return buildingOSGi;
	}
}
