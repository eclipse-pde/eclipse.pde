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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.publisher.eclipse.IPlatformEntry;
import org.eclipse.pde.internal.build.*;

/**
 * Instance of this class and subclasses are created on a plugin / feature basis. 
 */
public abstract class AbstractBuildScriptGenerator extends AbstractScriptGenerator {
	/** Additional dev entries for the compile classpath. */
	protected DevClassPathHelper devEntries;

	/** Contain the elements that will be assembled */
	protected AssemblyInformation assemblyData;

	/** The content of the build.properties file associated to the element for which the script is generated */
	protected Properties buildProperties;
	private Set compiledElements; //The elements we are compiling

	private boolean includePlatformIndependent = true;

	/** flag indicating whether or not the missing properties file should be logged */
	private boolean ignoreMissingPropertiesFile = true;

	static private Properties executionEnvironmentMappings = null;

	abstract protected Properties getBuildProperties() throws CoreException;

	static public Properties getExecutionEnvironmentMappings() {
		if (executionEnvironmentMappings != null)
			return executionEnvironmentMappings;

		Properties properties = new Properties();
		InputStream stream = null;
		try {
			stream = BundleHelper.getDefault().getBundle().getEntry("data/env.properties").openStream(); //$NON-NLS-1$
			properties.load(stream);
		} catch (IOException e) {
			//ignore
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				//ignore
			}
		}
		executionEnvironmentMappings = properties;
		return executionEnvironmentMappings;
	}

	public void setDevEntries(String entries) {
		devEntries = new DevClassPathHelper(entries);
	}

	public void setDevEntries(DevClassPathHelper entries) {
		devEntries = entries;
	}

	public void includePlatformIndependent(boolean value) {
		includePlatformIndependent = value;
	}

	public boolean isPlatformIndependentIncluded() {
		return includePlatformIndependent;
	}

	/**
	 * Method selectConfigs.
	 * Return a list containing all the configurations that are valid for the
	 * element
	 * @param element
	 * @return List
	 */
	public List selectConfigs(IPlatformEntry element) {
		List result = new ArrayList(getConfigInfos());

		if (((element.getOS() == null || element.getOS().equals(Config.ANY)) && includePlatformIndependent == false) && ((element.getWS() == null || element.getWS().equals(Config.ANY)) && includePlatformIndependent == false) && ((element.getArch() == null || element.getArch().equals(Config.ANY)) && includePlatformIndependent == false)) {
			result.clear();
			return result;
		}

		if (element.getOS() != null && !element.getOS().equals(Config.ANY)) {
			for (Iterator iter = result.iterator(); iter.hasNext();) {
				Config config = (Config) iter.next();
				if (!isMatching(element.getOS(), config.getOs()))
					iter.remove();
			}
		}
		if (element.getWS() != null && !element.getWS().equals(Config.ANY)) {
			for (Iterator iter = result.iterator(); iter.hasNext();) {
				Config config = (Config) iter.next();
				if (!isMatching(element.getWS(), config.getWs()))
					iter.remove();
			}
		}
		if (element.getArch() != null && !element.getArch().equals(Config.ANY)) {
			for (Iterator iter = result.iterator(); iter.hasNext();) {
				Config config = (Config) iter.next();
				if (!isMatching(element.getArch(), config.getArch()))
					iter.remove();
			}
		}
		return result;
	}

	private boolean isMatching(String candidateValues, String configValue) {
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (configValue.equalsIgnoreCase(token))
				return true;
		}
		return false;
	}

	public Set getCompiledElements() {
		if (compiledElements == null)
			compiledElements = new HashSet();
		return compiledElements;
	}

	/**
	 * Sets the compiledElements.
	 * @param compiledElements The compiledElements to set
	 */
	public void setCompiledElements(Set compiledElements) {
		this.compiledElements = compiledElements;
	}

	public void setReportResolutionErrors(boolean value) {
		reportResolutionErrors = value;
	}

	/**
	 * @return Returns the ignoreMissingPropertiesFile.
	 */
	public boolean isIgnoreMissingPropertiesFile() {
		if (BundleHelper.getDefault().isDebugging())
			return false;
		return ignoreMissingPropertiesFile;
	}

	/**
	 * @param value The ignoreMissingPropertiesFile to set.
	 */
	public void setIgnoreMissingPropertiesFile(boolean value) {
		ignoreMissingPropertiesFile = value;
	}
}
