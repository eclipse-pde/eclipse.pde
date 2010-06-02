/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gunnar Wagenknecht - adaption to new fetch script builder API
 *******************************************************************************/
package org.eclipse.pde.internal.build.fetch;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.*;
import org.eclipse.pde.internal.build.*;

/**
 * An <code>FetchTaskFactory</code> for building fetch scripts that will
 * fetch content from a CVS repository (id: <code>CVS</code>).
 * <p>
 * <code><pre>
 * Map file format:
 * 	type@id,[version]=CVS,args
 * args is a comma-separated list of key/value pairs
 * Accepted args include:
 * 	cvsPassFile - optional password file
 * 	cvsRoot - mandatory repo location
 * 	password - optional password
 * 	path - optional path relative to the cvsRoot
 * 	prebuilt - optional boolean value indicating that the entry points to a pre-built bundle in the repository
 * 	tag - mandatory CVS tag
 * </pre></code>
 * </p>
 */
public class CVSFetchTaskFactory implements IFetchFactory {
	public static final String ID = "CVS"; //$NON-NLS-1$

	private static final String TARGET_GET_FROM_CVS = "FetchFromCVS"; //$NON-NLS-1$
	private static final String SEPARATOR = ","; //$NON-NLS-1$
	public static final String OVERRIDE_TAG = ID;

	//CVS specific keys used in the map being passed around.
	private static final String KEY_CVSROOT = "cvsRoot"; //$NON-NLS-1$
	private static final String KEY_CVSPASSFILE = "cvsPassFile"; //$NON-NLS-1$
	private static final String KEY_PASSWORD = "password"; //$NON-NLS-1$
	private static final String KEY_PATH = "path"; //$NON-NLS-1$
	private static final String KEY_PREBUILT = "prebuilt"; //$NON-NLS-1$

	//Properties used in the CVS part of the scripts
	private static final String PROP_DESTINATIONFOLDER = "destinationFolder"; //$NON-NLS-1$
	private static final String PROP_CVSROOT = "cvsRoot"; //$NON-NLS-1$
	private static final String PROP_MODULE = "module"; //$NON-NLS-1$
	private static final String PROP_TAG = "tag"; //$NON-NLS-1$
	private static final String PROP_QUIET = "quiet"; //$NON-NLS-1$
	private static final String PROP_REALLYQUIET = "reallyquiet"; //$NON-NLS-1$
	private static final String PROP_FILETOCHECK = "fileToCheck"; //$NON-NLS-1$
	private static final String PROP_ELEMENTNAME = "elementName"; //$NON-NLS-1$

	private void generateAuthentificationAntTask(Map entryInfos, IAntScript script) {
		String password = (String) entryInfos.get(KEY_PASSWORD);
		String cvsPassFileLocation = (String) entryInfos.get(KEY_CVSPASSFILE);
		if (password != null)
			printCVSPassTask((String) entryInfos.get(KEY_CVSROOT), password, cvsPassFileLocation, script);
	}

	public void generateRetrieveElementCall(Map entryInfos, IPath destination, IAntScript script) {
		String type = (String) entryInfos.get(KEY_ELEMENT_TYPE);
		String element = (String) entryInfos.get(KEY_ELEMENT_NAME);
		boolean prebuilt = Boolean.valueOf((String) entryInfos.get(KEY_PREBUILT)).booleanValue();

		Map params = new HashMap(5);
		// we directly export the CVS content into the destination. if we have a pre-built JAR then
		// we want to put it right in the /plugins directory and not a sub-directory so strip off 2 segments
		// to leave us with the build directory (/plugins will be added by the "element" attribute)
		int remove = prebuilt ? 2 : 1;
		String suggestedPath = destination.lastSegment();
		params.put(PROP_DESTINATIONFOLDER, destination.removeLastSegments(remove).toString());
		params.put(PROP_TAG, entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG));
		params.put(PROP_CVSROOT, entryInfos.get(KEY_CVSROOT));
		params.put(PROP_QUIET, "${cvs.quiet}"); //$NON-NLS-1$
		params.put(PROP_REALLYQUIET, "${cvs.reallyquiet}"); //$NON-NLS-1$
		// the call to CVS requires us to pass a destination directory for the files that we are
		// retrieving, so give it the /plugins dir here
		if (prebuilt) {
			if (type.equals(ELEMENT_TYPE_PLUGIN))
				element = IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION;
			else if (type.equals(ELEMENT_TYPE_FEATURE))
				element = IPDEBuildConstants.DEFAULT_FEATURE_LOCATION;
		} else {
			if (suggestedPath != null)
				element = suggestedPath;
		}
		params.put(PROP_ELEMENTNAME, element);
		String module = entryInfos.get(KEY_PATH) == null ? element : (String) entryInfos.get(KEY_PATH);
		params.put(PROP_MODULE, module);

		IPath locationToCheck = (IPath) destination.clone();
		// if we have a pre-built plug-in then we want to check the existence of the JAR file
		// rather than the plug-in manifest.
		if (prebuilt) {
			locationToCheck = locationToCheck.removeLastSegments(1);
			locationToCheck = locationToCheck.append(new Path(module).lastSegment());
		} else {
			if (type.equals(ELEMENT_TYPE_FEATURE)) {
				locationToCheck = locationToCheck.append(Constants.FEATURE_FILENAME_DESCRIPTOR);
			} else if (type.equals(ELEMENT_TYPE_PLUGIN)) {
				locationToCheck = locationToCheck.append(Constants.PLUGIN_FILENAME_DESCRIPTOR);
			} else if (type.equals(ELEMENT_TYPE_FRAGMENT)) {
				locationToCheck = locationToCheck.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR);
			} else if (type.equals(ELEMENT_TYPE_BUNDLE)) {
				locationToCheck = locationToCheck.append(Constants.BUNDLE_FILENAME_DESCRIPTOR);
			}
		}
		params.put(PROP_FILETOCHECK, locationToCheck.toString());

		printAvailableTask(locationToCheck.toString(), locationToCheck.toString(), script);
		if (!prebuilt && (type.equals(IFetchFactory.ELEMENT_TYPE_PLUGIN) || type.equals(IFetchFactory.ELEMENT_TYPE_FRAGMENT))) {
			printAvailableTask(locationToCheck.toString(), locationToCheck.removeLastSegments(1).append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toString(), script);
		}

		generateAuthentificationAntTask(entryInfos, script);
		script.printAntCallTask(TARGET_GET_FROM_CVS, true, params);
	}

	public void generateRetrieveFilesCall(final Map entryInfos, IPath destination, final String[] files, IAntScript script) {
		generateAuthentificationAntTask(entryInfos, script);
		String path = (String) entryInfos.get(KEY_PATH);
		for (int i = 0; i < files.length; i++) {
			String file = files[i];
			IPath filePath;
			if (path != null) {
				filePath = new Path(path).append(file);
			} else {
				filePath = new Path((String) entryInfos.get(KEY_ELEMENT_NAME)).append(file);
			}
			String tag = (String) entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG);
			String cvsRoot = (String) entryInfos.get(KEY_CVSROOT);
			String dest = "true".equalsIgnoreCase((String) entryInfos.get(KEY_PREBUILT)) ? destination.removeLastSegments(1).toString() : destination.toString(); //$NON-NLS-1$
			printCVSTask("export -r " + tag + ' ' + filePath.toString(), cvsRoot, dest, null, null, "true", Utils.getPropertyFormat(PROP_REALLYQUIET), null, null, "${fetch.failonerror}", script); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			script.println("<move file=\"" + destination + '/' + filePath + "\"" + " tofile=\"" + destination.append(file) + "\" failonerror=\"false\" />"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	public void addTargets(IAntScript script) {
		script.printTargetDeclaration(TARGET_GET_FROM_CVS, null, null, "${fileToCheck}", null); //$NON-NLS-1$
		printCVSTask("export -d " + Utils.getPropertyFormat(PROP_ELEMENTNAME), Utils.getPropertyFormat(PROP_CVSROOT), Utils.getPropertyFormat(PROP_DESTINATIONFOLDER), Utils.getPropertyFormat(PROP_MODULE), Utils.getPropertyFormat(PROP_TAG), Utils.getPropertyFormat(PROP_QUIET), Utils.getPropertyFormat(PROP_REALLYQUIET), null, "CVS - " + Utils.getPropertyFormat(PROP_MODULE), script); //$NON-NLS-1$ //$NON-NLS-2$
		script.printTargetEnd();
	}

	/*
	 * Handle the old file format:
	 * Map file arguments:
	 * <code>&lt;TAG&gt;,&lt;CVSROOT&gt;[,&lt;PASSWORD&gt;[,&lt;PATH&gt;[,&lt;CVSPASSFILE&gt;]]]</code>
	 */
	private void legacyParseMapFileEntry(String[] arguments, Properties overrideTags, Map entryInfos) {
		String overrideTag = overrideTags != null ? overrideTags.getProperty(OVERRIDE_TAG) : null;
		entryInfos.put(KEY_CVSPASSFILE, (arguments.length > 4 && !arguments[4].equals("")) ? arguments[4] : null); //$NON-NLS-1$
		entryInfos.put(IFetchFactory.KEY_ELEMENT_TAG, (overrideTag != null && overrideTag.trim().length() != 0 ? overrideTag : arguments[0]));
		entryInfos.put(KEY_CVSROOT, arguments[1]);
		entryInfos.put(KEY_PASSWORD, (arguments.length > 2 && !arguments[2].equals("")) ? arguments[2] : null); //$NON-NLS-1$
		entryInfos.put(KEY_PATH, (arguments.length > 3 && !arguments[3].equals("")) ? arguments[3] : null); //$NON-NLS-1$ 
	}

	public void parseMapFileEntry(String repoSpecificentry, Properties overrideTags, Map entryInfos) throws CoreException {
		String[] arguments = Utils.getArrayFromStringWithBlank(repoSpecificentry, SEPARATOR);
		if (arguments.length < 2) {
			String message = NLS.bind(Messages.error_incorrectDirectoryEntry, entryInfos.get(KEY_ELEMENT_NAME));
			throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, 1, message, null)); //TODO Need to fix this
		}

		// build up the table of arguments in the map file entry
		Map table = new HashMap();
		for (int i = 0; i < arguments.length; i++) {
			String arg = arguments[i];
			// if we have at least one arg without an equals sign, then
			// revert back to the legacy parsing
			int index = arg.indexOf('=');
			if (index == -1) {
				legacyParseMapFileEntry(arguments, overrideTags, entryInfos);
				addProjectReference(entryInfos);
				return;
			}
			String key = arg.substring(0, index);
			String value = arg.substring(index + 1);
			table.put(key, value);
		}

		// add entries to the entryInfo map here instead of inside the loop
		// in case we revert to legacy parsing in the middle of the loop (we
		// don't want to contaminate entryInfos
		entryInfos.put(KEY_CVSPASSFILE, table.get(KEY_CVSPASSFILE));
		String overrideTag = overrideTags != null ? overrideTags.getProperty(OVERRIDE_TAG) : null;
		entryInfos.put(IFetchFactory.KEY_ELEMENT_TAG, (overrideTag != null && overrideTag.trim().length() != 0 ? overrideTag : table.get(IFetchFactory.KEY_ELEMENT_TAG)));
		entryInfos.put(KEY_CVSROOT, table.get(KEY_CVSROOT));
		entryInfos.put(KEY_PASSWORD, table.get(KEY_PASSWORD));
		entryInfos.put(KEY_PATH, table.get(KEY_PATH));
		entryInfos.put(KEY_PREBUILT, table.get(KEY_PREBUILT));
		addProjectReference(entryInfos);
	}

	private void addProjectReference(Map entryInfos) {
		String repoLocation = (String) entryInfos.get(KEY_CVSROOT);
		String module = (String) entryInfos.get(KEY_PATH);
		String projectName = (String) entryInfos.get(KEY_ELEMENT_NAME);
		String tag = (String) entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG);

		if (repoLocation != null && projectName != null) {
			String sourceURLs = asReference(repoLocation, module != null ? module : projectName, projectName, tag);
			if (sourceURLs != null) {
				entryInfos.put(Constants.KEY_SOURCE_REFERENCES, sourceURLs);
			}
		}
	}

	/**
	 * Creates an SCMURL reference to the associated source.
	 * 
	 * @param repoLocation
	 * @param module
	 * @param projectName
	 * @return project reference string or <code>null</code> if none
	 */
	private String asReference(String repoLocation, String module, String projectName, String tagName) {
		// parse protocol, host, repository root from repoLocation
		String protocol = null;
		String host = null;
		String root = null;

		int at = repoLocation.indexOf('@');
		if (at < 0) {
			// should be a local protocol
			if (repoLocation.startsWith(":local:")) { //$NON-NLS-1$
				protocol = "local"; //$NON-NLS-1$
				root = repoLocation.substring(7);
			}
		} else if (at < (repoLocation.length() - 2)) {
			String serverRoot = repoLocation.substring(at + 1);
			String protocolUserPass = repoLocation.substring(0, at);
			int colon = serverRoot.indexOf(':');
			if (colon > 0) {
				host = serverRoot.substring(0, colon);
				if (colon < (serverRoot.length() - 2)) {
					root = serverRoot.substring(colon + 1);
				}
				if (protocolUserPass.startsWith(":")) { //$NON-NLS-1$
					colon = protocolUserPass.indexOf(':', 1);
					if (colon > 0) {
						protocol = protocolUserPass.substring(1, colon);
					}
				} else {
					// missing protocol, assume p-server
					protocol = "pserver"; //$NON-NLS-1$
				}
			}
		}

		if (protocol == null || root == null) {
			return null; // invalid syntax
		}

		// use '|' as separator if the root location uses a colon for a Windows path
		String sep = ":"; //$NON-NLS-1$
		if (root.indexOf(':') >= 0) {
			sep = "|"; //$NON-NLS-1$
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append("scm:cvs"); //$NON-NLS-1$
		buffer.append(sep);
		buffer.append(protocol);
		buffer.append(sep);
		if (host != null) {
			buffer.append(host);
			buffer.append(sep);
		}
		buffer.append(root);
		buffer.append(sep);
		buffer.append(module);

		Path modulePath = new Path(module);
		if (!modulePath.lastSegment().equals(projectName)) {
			buffer.append(";project=\""); //$NON-NLS-1$
			buffer.append(projectName);
			buffer.append('"');
		}

		if (tagName != null && !tagName.equals("HEAD")) { //$NON-NLS-1$
			buffer.append(";tag="); //$NON-NLS-1$
			buffer.append(tagName);
		}
		return buffer.toString();
	}

	/**
	 * Print a <code>cvs</code> task to the Ant script.
	 * 
	 * @param command the CVS command to run
	 * @param cvsRoot value for the CVSROOT variable
	 * @param dest the destination directory for the checked out resources
	 * @param module the module name to check out
	 * @param tag the tag of the module to check out
	 * @param quiet whether or not to print informational messages to the output
	 * @param reallyquiet whether or not to print any messages to the output
	 * @param passFile the name of the password file
	 */
	private void printCVSTask(String command, String cvsRoot, String dest, String module, String tag, String quiet, String reallyquiet, String passFile, String taskname, IAntScript script) {
		printCVSTask(command, cvsRoot, dest, module, tag, quiet, reallyquiet, passFile, taskname, null, script);
	}

	/**
	 * Print a <code>cvs</code> task to the Ant script.
	 * 
	 * @param command the CVS command to run
	 * @param cvsRoot value for the CVSROOT variable
	 * @param dest the destination directory for the checked out resources
	 * @param module the module name to check out
	 * @param tag the tag of the module to check out
	 * @param quiet whether or not to print informational messages to the output
	 * @param reallyquiet whether or not to print any messages to the output
	 * @param passFile the name of the password file
	 * @param failOnError whether or not to throw an exception if something goes wrong
	 */
	private void printCVSTask(String command, String cvsRoot, String dest, String module, String tag, String quiet, String reallyquiet, String passFile, String taskname, String failOnError, IAntScript script) {
		script.printTabs();
		script.print("<cvs"); //$NON-NLS-1$
		script.printAttribute("command", command, false); //$NON-NLS-1$
		script.printAttribute("cvsRoot", cvsRoot, false); //$NON-NLS-1$
		script.printAttribute("dest", dest, false); //$NON-NLS-1$
		script.printAttribute("package", module, false); //$NON-NLS-1$
		script.printAttribute("tag", tag, false); //$NON-NLS-1$
		script.printAttribute("quiet", quiet, false); //$NON-NLS-1$
		script.printAttribute("reallyquiet", reallyquiet, false); //$NON-NLS-1$
		script.printAttribute("passfile", passFile, false); //$NON-NLS-1$
		script.printAttribute("taskname", taskname, false); //$NON-NLS-1$
		script.printAttribute("failonerror", failOnError, false); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$
	}

	/**
	 * Print a <code>cvspass</code> task to the Ant script.
	 * 
	 * @param cvsRoot the name of the repository
	 * @param password the password
	 * @param passFile the name of the password file
	 */
	private void printCVSPassTask(String cvsRoot, String password, String passFile, IAntScript script) {
		script.printTabs();
		script.print("<cvspass"); //$NON-NLS-1$
		script.printAttribute("cvsRoot", cvsRoot, true); //$NON-NLS-1$
		script.printAttribute("password", password, true); //$NON-NLS-1$
		script.printAttribute("passfile", passFile, false); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$
	}

	/**
	 * Print the <code>available</code> Ant task to this script. This task sets a property
	 * value if the given file exists at runtime.
	 * 
	 * @param property the property to set
	 * @param file the file to look for
	 */
	private void printAvailableTask(String property, String file, IAntScript script) {
		script.printTabs();
		script.print("<available"); //$NON-NLS-1$
		script.printAttribute("property", property, true); //$NON-NLS-1$
		script.printAttribute("file", file, false); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$
	}
}
