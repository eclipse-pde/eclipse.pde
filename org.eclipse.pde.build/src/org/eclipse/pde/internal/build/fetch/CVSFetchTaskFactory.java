/**********************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     IBM Corporation - initial API and implementation
 *     Gunnar Wagenknecht - adaption to new fetch script builder API
 **********************************************************************/
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
 * Map file arguments:
 * <code>&lt;TAG&gt;,&lt;CVSROOT&gt;[,&lt;PASSWORD&gt;[,&lt;PATH&gt;[,&lt;CVSPASSFILE&gt;]]]</code>
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

	//Properties used in the CVS part of the scripts
	private static final String PROP_DESTINATIONFOLDER = "destinationFolder"; //$NON-NLS-1$
	private static final String PROP_CVSROOT = "cvsRoot"; //$NON-NLS-1$
	private static final String PROP_MODULE = "module"; //$NON-NLS-1$
	private static final String PROP_TAG = "tag"; //$NON-NLS-1$
	private static final String PROP_QUIET = "quiet"; //$NON-NLS-1$
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

		Map params = new HashMap(5);
		// we directly export the CVS content into the destination
		params.put(PROP_DESTINATIONFOLDER, destination.uptoSegment(destination.segmentCount() - 1).toString());
		params.put(PROP_TAG, entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG));
		params.put(PROP_CVSROOT, entryInfos.get(KEY_CVSROOT));
		params.put(PROP_QUIET, "${cvs.quiet}"); //$NON-NLS-1$
		params.put(PROP_ELEMENTNAME, element);
		params.put(PROP_MODULE, (entryInfos.get(KEY_PATH) == null ? element : (String) entryInfos.get(KEY_PATH)));

		IPath locationToCheck = (IPath) destination.clone();
		if (type.equals(ELEMENT_TYPE_FEATURE)) {
			locationToCheck = locationToCheck.append(Constants.FEATURE_FILENAME_DESCRIPTOR);
		} else if (type.equals(ELEMENT_TYPE_PLUGIN)) {
			locationToCheck = locationToCheck.append(Constants.PLUGIN_FILENAME_DESCRIPTOR);
		} else if (type.equals(ELEMENT_TYPE_FRAGMENT)) {
			locationToCheck = locationToCheck.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR);
		} else if (type.equals(ELEMENT_TYPE_BUNDLE)) {
			locationToCheck = locationToCheck.append(Constants.BUNDLE_FILENAME_DESCRIPTOR);
		}
		params.put(PROP_FILETOCHECK, locationToCheck.toString());

		printAvailableTask(locationToCheck.toString(), locationToCheck.toString(), script);
		if (type.equals(IFetchFactory.ELEMENT_TYPE_PLUGIN) || type.equals(IFetchFactory.ELEMENT_TYPE_FRAGMENT)) {
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
			printCVSTask("export -r " + (String) entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG) + ' ' + filePath.toString(), (String) entryInfos.get(KEY_CVSROOT), destination.toString(), null, null, "true", null, null, script);  //$NON-NLS-1$//$NON-NLS-2$
			script.println("<move file=\"" + destination + '/' + filePath + "\"" + " tofile=\"" + destination.append(file) + "\" failonerror=\"false\" />");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	public void addTargets(IAntScript script) {
		script.printTargetDeclaration(TARGET_GET_FROM_CVS, null, null, "${fileToCheck}", null); //$NON-NLS-1$
		printCVSTask("export -d ${" + PROP_ELEMENTNAME + "}", "${" + PROP_CVSROOT + "}", "${" + PROP_DESTINATIONFOLDER + "}", "${" + PROP_MODULE + "}", "${" + PROP_TAG + "}", "${" + PROP_QUIET + "}", null, "CVS - ${" + PROP_MODULE + "}", script); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$
		script.printTargetEnd();
	}

	public void parseMapFileEntry(String repoSpecificentry, Properties overrideTags, Map entryInfos) throws CoreException {
		String[] arguments = Utils.getArrayFromStringWithBlank(repoSpecificentry, SEPARATOR);
		if (arguments.length < 2) {
			String message = NLS.bind(Messages.error_incorrectDirectoryEntry, entryInfos.get(KEY_ELEMENT_NAME));
			throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, 1, message, null)); //TODO Need to fix this
		}

		String overrideTag = overrideTags.getProperty(OVERRIDE_TAG);
		entryInfos.put(KEY_CVSPASSFILE, (arguments.length > 4 && !arguments[4].equals("")) ? arguments[4] : null); //$NON-NLS-1$
		entryInfos.put(IFetchFactory.KEY_ELEMENT_TAG, (overrideTag != null && overrideTag.trim().length() != 0 ? overrideTag : arguments[0]));
		entryInfos.put(KEY_CVSROOT, arguments[1]);
		entryInfos.put(KEY_PASSWORD, (arguments.length > 2 && !arguments[2].equals("")) ? arguments[2] : null); //$NON-NLS-1$
		entryInfos.put(KEY_PATH, (arguments.length > 3 && !arguments[3].equals("")) ? arguments[3] : null); //$NON-NLS-1$ 
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
	 * @param passFile the name of the password file
	 */
	private void printCVSTask(String command, String cvsRoot, String dest, String module, String tag, String quiet, String passFile, String taskname, IAntScript script) {
		script.printTabs();
		script.print("<cvs"); //$NON-NLS-1$
		script.printAttribute("command", command, false); //$NON-NLS-1$
		script.printAttribute("cvsRoot", cvsRoot, false); //$NON-NLS-1$
		script.printAttribute("dest", dest, false); //$NON-NLS-1$
		script.printAttribute("package", module, false); //$NON-NLS-1$
		script.printAttribute("tag", tag, false); //$NON-NLS-1$
		script.printAttribute("quiet", quiet, false); //$NON-NLS-1$
		script.printAttribute("passfile", passFile, false); //$NON-NLS-1$
		script.printAttribute("taskname", taskname, false); //$NON-NLS-1$
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
