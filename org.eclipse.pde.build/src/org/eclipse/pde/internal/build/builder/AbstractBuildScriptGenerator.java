/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.BuildTimeSite;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;
import org.eclipse.update.core.IPlatformEnvironment;

/**
 * Instance of this class and subclasses are created on a plugin / feature basis. 
 */
public abstract class AbstractBuildScriptGenerator extends AbstractScriptGenerator {
	/** Location of the plug-ins and fragments. */
	protected String[] pluginPath;
	/** Additional dev entries for the compile classpath. */
	protected DevClassPathHelper devEntries;

	/** Contain the elements that will be assembled */
	protected AssemblyInformation assemblyData;

	/** The content of the build.properties file associated to the element for which the script is generated */
	protected Properties buildProperties;
	protected BuildTimeSiteFactory siteFactory;
	private Set compiledElements; //The elements we are compiling

	private boolean includePlatformIndependent = true;

	private boolean reportResolutionErrors;
	
	abstract protected Properties getBuildProperties() throws CoreException;

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
	 * Return the path of the plugins		//TODO Do we need to add support for features, or do we simply consider one list of URL? It is just a matter of style/
	 * @return URL[]
	 */
	protected String[] getPluginPath() {
		return pluginPath;
	}

	/**
	 * Sets the pluginPath.
	 * 
	 * @param path
	 */
	public void setPluginPath(String[] path) {
		pluginPath = path;
	}

	/**
	 * Return a build time site referencing things to be built.   
	 * @param refresh : indicate if a refresh must be performed. Although this flag is set to true, a new site is not rebuild if the urls of the site did not changed 
	 * @return BuildTimeSite
	 * @throws CoreException
	 */
	public BuildTimeSite getSite(boolean refresh) throws CoreException {
		if (siteFactory != null && refresh == false)
			return (BuildTimeSite) siteFactory.createSite();

		if (siteFactory == null || refresh == true) {
			siteFactory = new BuildTimeSiteFactory();
			siteFactory.setReportResolutionErrors(reportResolutionErrors);
		}

		siteFactory.setSitePaths(getPaths());
		return (BuildTimeSite) siteFactory.createSite();
	}

	/**
	 * Method getPaths. 
	 * @return URL[]
	 */
	private String[] getPaths() {
		if (pluginPath != null)
			return pluginPath;

		return new String[] {workingDirectory};
	}

	public void setBuildSiteFactory(BuildTimeSiteFactory siteFactory) {
		this.siteFactory = siteFactory;
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
		String newVersion = "_" + version; //$NON-NLS-1$
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
	

}