/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.publisher.eclipse.IPlatformEntry;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.AssemblyInformation;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.Config;

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
	private Set<String> compiledElements; //The elements we are compiling

	private boolean includePlatformIndependent = true;

	/** flag indicating whether or not the missing properties file should be logged */
	private boolean ignoreMissingPropertiesFile = true;

	static private Properties executionEnvironmentMappings = null;

	abstract protected Properties getBuildProperties() throws CoreException;

	static public Properties getExecutionEnvironmentMappings() {
		if (executionEnvironmentMappings != null) {
			return executionEnvironmentMappings;
		}

		Properties properties = new Properties();
		try (InputStream stream = BundleHelper.getDefault().getBundle().getEntry("data/env.properties").openStream()) { //$NON-NLS-1$
			properties.load(stream);
		} catch (IOException e) {
			//ignore
		}
		executionEnvironmentMappings = properties;
		return executionEnvironmentMappings;
	}

	public void setDevEntries(Path entries) {
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
	 * @return List
	 */
	public List<Config> selectConfigs(IPlatformEntry element) {
		List<Config> result = new ArrayList<>(getConfigInfos());

		if (((element.getOS() == null || element.getOS().equals(Config.ANY)) && includePlatformIndependent == false) && ((element.getWS() == null || element.getWS().equals(Config.ANY)) && includePlatformIndependent == false) && ((element.getArch() == null || element.getArch().equals(Config.ANY)) && includePlatformIndependent == false)) {
			result.clear();
			return result;
		}

		if (element.getOS() != null && !element.getOS().equals(Config.ANY)) {
			for (Iterator<Config> iter = result.iterator(); iter.hasNext();) {
				Config config = iter.next();
				if (!isMatching(element.getOS(), config.getOs())) {
					iter.remove();
				}
			}
		}
		if (element.getWS() != null && !element.getWS().equals(Config.ANY)) {
			for (Iterator<Config> iter = result.iterator(); iter.hasNext();) {
				Config config = iter.next();
				if (!isMatching(element.getWS(), config.getWs())) {
					iter.remove();
				}
			}
		}
		if (element.getArch() != null && !element.getArch().equals(Config.ANY)) {
			for (Iterator<Config> iter = result.iterator(); iter.hasNext();) {
				Config config = iter.next();
				if (!isMatching(element.getArch(), config.getArch())) {
					iter.remove();
				}
			}
		}
		return result;
	}

	private boolean isMatching(String candidateValues, String configValue) {
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (configValue.equalsIgnoreCase(token)) {
				return true;
			}
		}
		return false;
	}

	public Set<String> getCompiledElements() {
		if (compiledElements == null) {
			compiledElements = new HashSet<>();
		}
		return compiledElements;
	}

	/**
	 * Sets the compiledElements.
	 * @param compiledElements The compiledElements to set
	 */
	public void setCompiledElements(Set<String> compiledElements) {
		this.compiledElements = compiledElements;
	}

	public void setReportResolutionErrors(boolean value) {
		reportResolutionErrors = value;
	}

	/**
	 * @return Returns the ignoreMissingPropertiesFile.
	 */
	public boolean isIgnoreMissingPropertiesFile() {
		if (BundleHelper.getDefault().isDebugging()) {
			return false;
		}
		return ignoreMissingPropertiesFile;
	}

	/**
	 * @param value The ignoreMissingPropertiesFile to set.
	 */
	public void setIgnoreMissingPropertiesFile(boolean value) {
		ignoreMissingPropertiesFile = value;
	}
}
