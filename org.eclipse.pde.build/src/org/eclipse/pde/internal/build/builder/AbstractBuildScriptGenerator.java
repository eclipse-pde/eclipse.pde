/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
import org.eclipse.update.core.IPlatformEnvironment;

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

	static public Properties getExecutionEnvironmentMappings(){
		if(executionEnvironmentMappings != null)
			return executionEnvironmentMappings;
		
		executionEnvironmentMappings = new Properties();
		InputStream stream = null;
		try {
			stream = BundleHelper.getDefault().getBundle().getEntry("data/env.properties").openStream(); //$NON-NLS-1$
			executionEnvironmentMappings.load(stream);
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
	 * 
	 * @param buf
	 * @param start
	 * @param target
	 * @return int
	 */
	protected int scan(StringBuffer buf, int start, String target) {
		return scan(buf, start, new String[] {target});
	}

	/**
	 * 
	 * @param buf
	 * @param start
	 * @param targets
	 * @return int
	 */
	protected int scan(StringBuffer buf, int start, String[] targets) {
		for (int i = start; i < buf.length(); i++) {
			for (int j = 0; j < targets.length; j++) {
				if (i < buf.length() - targets[j].length()) {
					String match = buf.substring(i, i + targets[j].length());
					if (targets[j].equals(match))
						return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Return a buffer containing the contents of the file at the specified location.
	 * 
	 * @param target the file
	 * @return StringBuffer
	 * @throws IOException
	 */
	protected StringBuffer readFile(File target) throws IOException {
		return readFile(new FileInputStream(target));
	}

	protected StringBuffer readFile(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(new BufferedInputStream(stream));
		StringBuffer result = new StringBuffer();
		char[] buf = new char[4096];
		int count;
		try {
			count = reader.read(buf, 0, buf.length);
			while (count != -1) {
				result.append(buf, 0, count);
				count = reader.read(buf, 0, buf.length);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore exceptions here
			}
		}
		return result;
	}

	/**
	 * Custom build scripts should have their version number matching the
	 * version number defined by the feature/plugin/fragment descriptor.
	 * This is a best effort job so do not worry if the expected tags were
	 * not found and just return without modifying the file.
	 * 
	 * @param buildFile
	 * @param propertyName
	 * @param version
	 * @throws IOException
	 *
	 */
	protected void updateVersion(File buildFile, String propertyName, String version) throws IOException {
		StringBuffer buffer = readFile(buildFile);
		int pos = scan(buffer, 0, propertyName);
		if (pos == -1)
			return;
		pos = scan(buffer, pos, "value"); //$NON-NLS-1$
		if (pos == -1)
			return;
		int begin = scan(buffer, pos, "\""); //$NON-NLS-1$
		if (begin == -1)
			return;
		begin++;
		int end = scan(buffer, begin, "\""); //$NON-NLS-1$
		if (end == -1)
			return;
		String currentVersion = buffer.substring(begin, end);
		String newVersion = version;
		if (currentVersion.equals(newVersion))
			return;
		buffer.replace(begin, end, newVersion);
		Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(buildFile));
	}

	/**
	 * Method selectConfigs.
	 * Return a list containing all the configurations that are valid for the
	 * element
	 * @param element
	 * @return List
	 */
	public List selectConfigs(IPlatformEnvironment element) {
		List result = new ArrayList(getConfigInfos());

		if (((element.getOS() == null || element.getOS().equals(Config.ANY)) && includePlatformIndependent == false) && 
			((element.getWS() == null || element.getWS().equals(Config.ANY)) && includePlatformIndependent == false) && 
			((element.getOSArch() == null || element.getOSArch().equals(Config.ANY)) && includePlatformIndependent == false)) {
			result.clear();
			return result;
		}

		if (element.getOS() != null && !element.getOS().equals(Config.ANY)) {
			for (Iterator iter = result.iterator(); iter.hasNext();) {
				Config config = (Config) iter.next();
				if (! isMatching(element.getOS(), config.getOs()) )
					iter.remove();
			}
		}
		if (element.getWS() != null && !element.getWS().equals(Config.ANY)) {
			for (Iterator iter = result.iterator(); iter.hasNext();) {
				Config config = (Config) iter.next();
				if (! isMatching(element.getWS(), config.getWs()) )
					iter.remove();
			}
		}
		if (element.getOSArch() != null && !element.getOSArch().equals(Config.ANY)) {
			for (Iterator iter = result.iterator(); iter.hasNext();) {
				Config config = (Config) iter.next();
				if (! isMatching(element.getOSArch(), config.getArch()))
					iter.remove();
			}
		}
		return result;
	}

	private boolean isMatching(String candidateValues, String configValue) {
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (configValue.equalsIgnoreCase(token)) return true;
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
