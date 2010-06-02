/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.fetch;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
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

	private static class IUFetchInfo {
		String id, version;

		public IUFetchInfo(String id, String version) {
			this.id = id;
			this.version = version;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;

			IUFetchInfo other = (IUFetchInfo) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (version == null) {
				if (other.version != null)
					return false;
			} else if (!version.equals(other.version))
				return false;
			return true;
		}
	}

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
	private static final String TARGET_GET_IUS_FROM_REPO = "FetchIUsFromRepo"; //$NON-NLS-1$

	private final Map iusToFetchBySource = new LinkedHashMap(2);

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
		// single target to fetch all IUs from different repos
		//
		// <p2.repo2runnable source="source1" destination="${trasnformedRepoLocation}">
		//    <iu id="aBundle" version="1" />
		//    ...
		// </p2.repo2runnable>
		// <p2.repo2runnable source="source2" destination="${transformedRepoLocation}">
		//    <iu id="bBundle" version="2" />
		//    ...
		// </p2.repo2runnable>
		// ...
		script.println();
		script.printTargetDeclaration(TARGET_GET_IUS_FROM_REPO, null, null, null, null);

		Map args = new LinkedHashMap(2);
		for (Iterator stream = iusToFetchBySource.entrySet().iterator(); stream.hasNext();) {
			Entry entry = (Entry) stream.next();
			String sourceRepository = (String) entry.getKey();
			List iusToFetch = (List) entry.getValue();

			script.printEchoTask(null, NLS.bind(Messages.fetching_p2Repo, new String[] {sourceRepository, Utils.getPropertyFormat(IBuildPropertiesConstants.PROPERTY_TRANSFORMED_REPO)}), "info"); //$NON-NLS-1$
			args.clear();
			args.put(ATTRIBUTE_SOURCE, sourceRepository);
			args.put(ATTRIBUTE_DESTINATION, Utils.getPropertyFormat(IBuildPropertiesConstants.PROPERTY_TRANSFORMED_REPO));
			script.printStartTag(TASK_REPO2RUNNABLE, args);
			script.incrementIdent();

			for (Iterator stream2 = iusToFetch.iterator(); stream2.hasNext();) {
				IUFetchInfo iuFetchInfo = (IUFetchInfo) stream2.next();
				args.clear();
				args.put(ATTRIBUTE_ID, iuFetchInfo.id);
				args.put(ATTRIBUTE_VERSION, iuFetchInfo.version);
				script.printElement(TASK_IU, args);
			}

			script.decrementIdent();
			script.printEndTag(TASK_REPO2RUNNABLE);
			script.println();

		}

		script.printTargetEnd();
		script.println();

		// clear the map
		iusToFetchBySource.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveElementCall(java.util.Map, org.eclipse.core.runtime.IPath, org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveElementCall(Map entryInfos, IPath destination, IAntScript script) {
		// generate at most one fetch call
		if (iusToFetchBySource.isEmpty()) {
			// <antcall target="FetchIUsFromRepo" />
			script.printAntCallTask(TARGET_GET_IUS_FROM_REPO, true, null);
		}

		// collect the IU and repo to fetch from for single fetch call later
		String sourceRepository = (String) entryInfos.get(KEY_REPOSITORY);

		if (!iusToFetchBySource.containsKey(sourceRepository)) {
			iusToFetchBySource.put(sourceRepository, new ArrayList());
		}

		IUFetchInfo iuFetchInfo = new IUFetchInfo((String) entryInfos.get(KEY_ID), (String) entryInfos.get(KEY_VERSION));

		List iusToFetch = (List) iusToFetchBySource.get(sourceRepository);
		if (!iusToFetch.contains(iuFetchInfo))
			iusToFetch.add(iuFetchInfo);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#generateRetrieveFilesCall(java.util.Map, org.eclipse.core.runtime.IPath, java.lang.String[], org.eclipse.pde.build.IAntScript)
	 */
	public void generateRetrieveFilesCall(Map entryInfos, IPath destination, String[] files, IAntScript script) {
		Map args = new HashMap();
		args.put(ATTRIBUTE_SOURCE, entryInfos.get(KEY_REPOSITORY));
		args.put(ATTRIBUTE_DESTINATION, destination.toOSString());
		script.printStartTag(TASK_REPO2RUNNABLE, args);
		script.incrementIdent();
		args.clear();
		args.put(ATTRIBUTE_ID, entryInfos.get(KEY_ID));
		args.put(ATTRIBUTE_VERSION, entryInfos.get(KEY_VERSION));
		script.printElement(TASK_IU, args);
		script.decrementIdent();
		script.printEndTag(TASK_REPO2RUNNABLE);
		script.println();

		//create a dummy build.properties file which will be overwritten if the feature actually contains one
		args.clear();
		args.put("message", "#empty"); //$NON-NLS-1$ //$NON-NLS-2$
		args.put("file", new File(destination.toFile(), "build.properties").getAbsolutePath()); //$NON-NLS-1$//$NON-NLS-2$
		script.printElement("echo", args); //$NON-NLS-1$

		//move the files to the destination
		args.clear();
		args.put("todir", destination.toOSString()); //$NON-NLS-1$
		args.put("flatten", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printStartTag("move", args); //$NON-NLS-1$
		script.incrementIdent();
		args.clear();
		args.put("dir", destination.toOSString()); //$NON-NLS-1$
		for (int i = 0; i < files.length; i++) {
			args.put("includes", "features/*/" + files[i]); //$NON-NLS-1$ //$NON-NLS-2$
			script.printElement("fileset", args); //$NON-NLS-1$
		}
		script.decrementIdent();
		script.printEndTag("move"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.build.IFetchFactory#parseMapFileEntry(java.lang.String, java.util.Properties, java.util.Map)
	 */
	public void parseMapFileEntry(String rawEntry, Properties overrideTags, Map entryInfos) throws CoreException {
		String[] arguments = Utils.getArrayFromStringWithBlank(rawEntry, SEPARATOR);

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

		// we need an IU id, and repository
		if (entryInfos.get(KEY_ID) == null || entryInfos.get(KEY_REPOSITORY) == null)
			throwException(NLS.bind(Messages.error_directoryEntryRequiresIdAndRepo, entryInfos.get(KEY_ELEMENT_NAME)), null);
	}

}
