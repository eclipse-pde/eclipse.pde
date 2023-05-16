/**********************************************************************
 * Copyright (c) 2004, 2021 Eclipse Foundation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gunnar Wagenknecht - Initial API and implementation
 *     IBM Corporation - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.fetch;

import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.IAntScript;
import org.eclipse.pde.build.IFetchFactory;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Messages;
import org.eclipse.pde.internal.build.Utils;

/**
 * An <code>IFetchFactory</code> that fetches features and plugins by
 * copying from a specific location (id: <code>COPY</code>).
 * <p>
 * Map file arguments:
 * <code>&lt;ROOT_LOCATION&gt;[,&lt;ELEMENT_LOCATION&gt;]</code>
 * <dl>
 * <dt>ROOT_LOCATION</dt>
 * <dd>The ROOT_LOCATION (eg. <code>/source/eclipse</code>, or
 * <code>D:/dev/myproduct</code>) is used as root path to determine the
 * location to fetch. It can be overwritten via the
 * <code>fetchTag</code> to fetch from another location (for example, on a different machine).</dd>
 * </dl>
 * <dt>ELEMENT_LOCATION</dt>
 * <dd>A path withing the ROOT_LOCATION (eg.
 * <code>org.eclipse.sdk-feature/features/org.eclipse.rcp</code>) to retrive
 * the element from if it is not within the root. If this is not provided the
 * default path will be used which simply maps to the element name.</dd>
 * </dl>
 * </p>
 */
public class COPYFetchTasksFactory implements IFetchFactory, IPDEBuildConstants {
	public static final String ID = "COPY"; //$NON-NLS-1$

	private static final String SEPARATOR = ","; //$NON-NLS-1$
	private static final String OVERRIDE_TAG = ID;

	//COPY specific keys used in the map being passed around.
	private static final String KEY_PATH = "path"; //$NON-NLS-1$
	private static final String KEY_ROOT = "root"; //$NON-NLS-1$

	@Override
	public void generateRetrieveElementCall(Map<String, Object> entryInfos, IPath destination, IAntScript script) {
		String element = (String) entryInfos.get(KEY_ELEMENT_NAME);

		// we directly copy the disc content into the destination
		String root = (String) entryInfos.get(KEY_ROOT);
		String path = (String) entryInfos.get(KEY_PATH);
		IPath sourcePath = IPath.fromOSString(root);
		if (path != null) {
			sourcePath = sourcePath.append(path);
		} else {
			sourcePath = sourcePath.append(element);
		}

		printCopyTask(null, destination.toString(), new String[] {sourcePath.toString()}, false, true, script);
	}

	@Override
	public void generateRetrieveFilesCall(final Map<String, Object> entryInfos, IPath destination, final String[] files, IAntScript script) {
		String root = (String) entryInfos.get(KEY_ROOT);
		String path = (String) entryInfos.get(KEY_PATH);

		for (String file : files) {
			IPath filePath = IPath.fromOSString(root);
			if (path != null) {
				filePath = filePath.append(path).append(file);
			} else {
				filePath = filePath.append((String) entryInfos.get(KEY_ELEMENT_NAME)).append(file);
			}
			printCopyTask(filePath.toString(), destination.toString(), null, true, true, script);
		}
	}

	@Override
	public void addTargets(IAntScript script) {
		// no additional targets
	}

	@Override
	public void parseMapFileEntry(String repoSpecificentry, Properties overrideTags, Map<String, Object> entryInfos) throws CoreException {
		String[] arguments = Utils.getArrayFromStringWithBlank(repoSpecificentry, SEPARATOR);
		if (arguments.length < 1) {
			String message = NLS.bind(Messages.error_incorrectDirectoryEntry, entryInfos.get(KEY_ELEMENT_NAME));
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
		}

		String overrideTag = overrideTags != null ? overrideTags.getProperty(OVERRIDE_TAG) : null;
		entryInfos.put(KEY_ROOT, (null == overrideTag || overrideTag.trim().length() == 0) ? arguments[0] : overrideTag);
		entryInfos.put(KEY_PATH, (arguments.length > 1 && arguments[1].trim().length() > 0) ? arguments[1] : null);
	}

	/**
	 * Print a <code>copy</code> task to the script. The source file is specified 
	 * by the <code>file</code> OR the <code>dirs</code> parameter. 
	 * The destination directory is specified by the <code>todir</code> parameter. 
	 * @param file the source file
	 * @param todir the destination directory
	 * @param dirs the directories to copy
	 * @param overwrite indicates if existing files should be overwritten 
	 * @param script the script to print to
	 */
	private void printCopyTask(String file, String todir, String[] dirs, boolean failOnError, boolean overwrite, IAntScript script) {
		script.printTabs();
		script.print("<copy"); //$NON-NLS-1$
		script.printAttribute("file", file, false); //$NON-NLS-1$
		script.printAttribute("todir", todir, false); //$NON-NLS-1$
		script.printAttribute("failonerror", failOnError ? "true" : "false", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printAttribute("overwrite", overwrite ? "true" : "false", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (dirs == null)
			script.println("/>"); //$NON-NLS-1$
		else {
			script.println(">"); //$NON-NLS-1$
			for (String dir : dirs) {
				script.printTabs();
				script.print("\t<fileset"); //$NON-NLS-1$
				script.printAttribute("dir", dir, true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
			script.println("</copy>"); //$NON-NLS-1$
		}
	}
}
