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
	private static final String TARGET_GET_IU_FROM_REPO = "FetchIUFromRepo"; //$NON-NLS-1$

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
		// Macro for:
		// <p2.repo2runnable source="${source}" destination="${destination}">
		//    <iu id="aBundle" version="1" />
		// </p2.repo2runnable>

		script.println();

		List attributes = new ArrayList();
		attributes.add(ATTRIBUTE_ID);
		attributes.add(ATTRIBUTE_VERSION);
		attributes.add(ATTRIBUTE_SOURCE);
		script.printMacroDef(TARGET_GET_IU_FROM_REPO, attributes);
		script.printEchoTask(null, NLS.bind(Messages.fetching_p2Repo, new String[] {Utils.getMacroFormat(ATTRIBUTE_ID), Utils.getMacroFormat(ATTRIBUTE_VERSION), Utils.getMacroFormat(ATTRIBUTE_SOURCE), Utils.getPropertyFormat(IBuildPropertiesConstants.PROPERTY_TRANSFORMED_REPO)}), "info"); //$NON-NLS-1$

		// TODO ensure this is the right value to set. 
		// TODO also we might need to put the real expanded path here.
		Map args = new LinkedHashMap(2);
		args.put(ATTRIBUTE_DESTINATION, Utils.getPropertyFormat(IBuildPropertiesConstants.PROPERTY_TRANSFORMED_REPO));
		args.put(ATTRIBUTE_SOURCE, Utils.getMacroFormat(ATTRIBUTE_SOURCE));
		script.printStartTag(TASK_REPO2RUNNABLE, args);
		script.incrementIdent();

		args.clear();
		args.put(ATTRIBUTE_ID, Utils.getMacroFormat(ATTRIBUTE_ID));
		args.put(ATTRIBUTE_VERSION, Utils.getMacroFormat(ATTRIBUTE_VERSION));
		script.printElement(TASK_IU, args);

		script.decrementIdent();
		script.printEndTag(TASK_REPO2RUNNABLE);

		script.printEndMacroDef();
		script.println();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveElementCall(java.util.Map, org.eclipse.core.runtime.IPath, org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveElementCall(Map entryInfos, IPath destination, IAntScript script) {

		// <FetchIUFromRepo id="aBundle" version="1" source="file:/fromRepo" />

		Map params = new LinkedHashMap(4);
		params.put(ATTRIBUTE_ID, entryInfos.get(KEY_ID));
		params.put(ATTRIBUTE_VERSION, entryInfos.get(KEY_VERSION));
		params.put(ATTRIBUTE_SOURCE, entryInfos.get(KEY_REPOSITORY));
		script.printElement(TARGET_GET_IU_FROM_REPO, params);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveFilesCall(java.util.Map, org.eclipse.core.runtime.IPath, java.lang.String[], org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveFilesCall(Map entryInfos, IPath destination, String[] files, IAntScript script) {
		script.println();
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
		if (table.containsKey(KEY_VERSION))
			entryInfos.put(KEY_VERSION, table.get(KEY_VERSION));
		else
			entryInfos.put(KEY_VERSION, ""); //$NON-NLS-1$
		entryInfos.put(KEY_REPOSITORY, table.get(KEY_REPOSITORY));
	}

}
