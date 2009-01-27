/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.fetch;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.IAntScript;
import org.eclipse.pde.build.IFetchFactory;
import org.eclipse.pde.internal.build.*;

/**
 * Factory which interprets a p2IU entry in a map file for a build contribution.
 * 
 * @since 1.0
 */
public class P2IUFetchFactory implements IFetchFactory {

	private static final String ATTRIBUTE_DESTINATION = "destination"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SOURCE = "source"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VERSION = "version"; //$NON-NLS-1$
	private static final String KEY_REPOSITORY = "repository"; //$NON-NLS-1$
	private static final String KEY_ID = "id"; //$NON-NLS-1$
	private static final String KEY_VERSION = "version"; //$NON-NLS-1$
	private static final String SEPARATOR = ","; //$NON-NLS-1$
	private static final String TASK_IU = "iu"; //$NON-NLS-1$
	private static final String TASK_REPO2RUNNABLE = "p2.repo2runnable"; //$NON-NLS-1$

	/*
	 * Helper method to throw an exception with the given message.
	 */
	private static void throwException(String message, Exception e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, 0, message, e));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#addTargets(org.eclipse.pde.build.IAntScript)
	 */
	public void addTargets(IAntScript script) {
		//
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveElementCall(java.util.Map, org.eclipse.core.runtime.IPath, org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveElementCall(Map entryInfos, IPath destination, IAntScript script) {
		// <p2.transform source="${source}" destination="${destination}">
		//    <iu id="aBundle" version="1" />
		// </p2.transform>
		script.println();
		script.print('<' + TASK_REPO2RUNNABLE);
		// TODO ensure this is the right value to set. 
		// TODO also we might need to put the real expanded path here.
		script.printAttribute(ATTRIBUTE_DESTINATION, "${transformedRepoLocation}", true);
		script.printAttribute(ATTRIBUTE_SOURCE, (String) entryInfos.get(KEY_REPOSITORY), true);
		script.println(">"); //$NON-NLS-1$

		script.print("<"); //$NON-NLS-1$
		script.print(TASK_IU);
		script.printAttribute(ATTRIBUTE_ID, (String) entryInfos.get(KEY_ID), true);
		script.printAttribute(ATTRIBUTE_VERSION, (String) entryInfos.get(KEY_VERSION), true);
		script.println("/>"); //$NON-NLS-1$

		script.printEndTag(TASK_REPO2RUNNABLE);
		script.println();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveFilesCall(java.util.Map, org.eclipse.core.runtime.IPath, java.lang.String[], org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveFilesCall(Map entryInfos, IPath destination, String[] files, IAntScript script) {
		// 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#parseMapFileEntry(java.lang.String, java.util.Properties, java.util.Map)
	 */
	public void parseMapFileEntry(String rawEntry, Properties overrideTags, Map entryInfos) throws CoreException {
		String[] arguments = Utils.getArrayFromStringWithBlank(rawEntry, SEPARATOR);
		// we need an IU id, version, and repository
		if (arguments.length < 3)
			throwException(NLS.bind(Messages.error_incorrectDirectoryEntry, entryInfos.get(KEY_ELEMENT_NAME)), null);

		// build up the table of arguments in the map file entry
		Map table = new HashMap();
		for (int i = 0; i < arguments.length; i++) {
			String arg = arguments[i];
			// if we have at least one arg without an equals sign then we are malformed and should bail
			int index = arg.indexOf('=');
			if (index == -1)
				throwException(NLS.bind(Messages.error_incorrectDirectoryEntry, entryInfos.get(KEY_ELEMENT_NAME)), null);
			String key = arg.substring(0, index);
			String value = arg.substring(index + 1);
			table.put(key, value);
		}

		entryInfos.put(KEY_ID, table.get(KEY_ID));
		entryInfos.put(KEY_VERSION, table.get(KEY_VERSION));
		entryInfos.put(KEY_REPOSITORY, table.get(KEY_REPOSITORY));
	}

}
