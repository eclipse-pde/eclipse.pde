/*******************************************************************************
 * Copyright (c) 2008,2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.*;
import java.util.jar.JarFile;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator.CompiledEntry;

public class GatherBundleTask extends AbstractPublisherTask {
	static final private String API_DESCRIPTION = ".api_description"; //$NON-NLS-1$

	static public class OutputFileSet extends FileSet {
		private String library;

		public OutputFileSet() {
			super();
		}

		public String getLibrary() {
			return this.library;
		}

		public void setLibrary(String value) {
			this.library = value;
		}

		public synchronized void setIncludes(String includes) {
			super.setIncludes(includes);
		}
	}

	private String buildResultFolder = null;
	private String targetFolder = null;
	private String gatheredSource = null;
	private String unpack = null;
	private final Map sourceMap = new HashMap();

	public void execute() throws BuildException {
		GatheringComputer computer = createComputer();

		GatherBundleAction action = null;
		if (targetFolder != null) {
			File targetFile = new File(targetFolder);
			action = new GatherBundleAction(targetFile, targetFile);
		} else
			action = new GatherBundleAction(new File(baseDirectory), new File(buildResultFolder));
		action.setComputer(computer);
		action.setUnpack(unpack);

		PublisherInfo info = getPublisherInfo();
		BuildPublisherApplication application = createPublisherApplication();
		application.addAction(action);
		try {
			application.run(info);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected GatheringComputer createComputer() {
		Properties properties = getBuildProperties();
		GatheringComputer computer = new GatheringComputer();

		if (targetFolder != null) {
			FileSet fileSet = new FileSet();
			fileSet.setProject(getProject());
			fileSet.setDir(new File(targetFolder));
			NameEntry includes = fileSet.createInclude();
			includes.setName("**"); //$NON-NLS-1$
			NameEntry excludes = fileSet.createExclude();
			excludes.setName(JarFile.MANIFEST_NAME);

			if (new File(targetFolder, JarFile.MANIFEST_NAME).exists())
				computer.addFile(targetFolder, JarFile.MANIFEST_NAME);
			computer.addFiles(targetFolder, fileSet.getDirectoryScanner().getIncludedFiles());
			return computer;
		}

		CompiledEntry[] entries = null;
		try {
			entries = ModelBuildScriptGenerator.extractEntriesToCompile(properties, null);
		} catch (CoreException e) {
			//nothing being compiled?
		}

		int numIncludes = 0;
		String include = (String) properties.get(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		String[] splitIncludes = Utils.getArrayFromString(include);
		String exclude = (String) properties.get(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		FileSet fileSet = new FileSet();
		fileSet.setProject(getProject());
		fileSet.setDir(new File(baseDirectory));

		for (int i = 0; i < splitIncludes.length; i++) {
			String entry = splitIncludes[i];
			if (entry.equals(ModelBuildScriptGenerator.DOT))
				continue;

			NameEntry fileInclude = fileSet.createInclude();
			fileInclude.setName(entry);
			++numIncludes;
		}

		if (numIncludes > 0) {
			//we want to exclude the manifest and compiled libraries from this set, they are added separately
			String extraExcludes = JarFile.MANIFEST_NAME;
			for (int i = 0; entries != null && i < entries.length; i++) {
				String name = entries[i].getName(false);
				if (!name.equals(ModelBuildScriptGenerator.DOT)) {
					String formatedName = name + (entries[i].getType() == CompiledEntry.FOLDER ? "/" : ""); //$NON-NLS-1$//$NON-NLS-2$
					extraExcludes += "," + formatedName; //$NON-NLS-1$
				}
			}

			String[] splitExcludes = Utils.getArrayFromString(exclude != null ? exclude + "," + extraExcludes : extraExcludes); //$NON-NLS-1$
			for (int i = 0; i < splitExcludes.length; i++) {
				NameEntry fileExclude = fileSet.createExclude();
				fileExclude.setName(splitExcludes[i]);
			}

			List includedFiles = Arrays.asList(fileSet.getDirectoryScanner().getIncludedFiles());
			LinkedHashSet set = new LinkedHashSet(includedFiles);

			// Manifest must go first, and must have been specifically excluded earlier from the buildResultFolder to not get added.
			if (new File(buildResultFolder, JarFile.MANIFEST_NAME).exists())
				computer.addFile(buildResultFolder, JarFile.MANIFEST_NAME);

			// The plugin.xml and fragment.xml may have had versions changed, take them from the buildResultFolder
			if (set.contains(Constants.PLUGIN_FILENAME_DESCRIPTOR) && new File(buildResultFolder, Constants.PLUGIN_FILENAME_DESCRIPTOR).exists()) {
				set.remove(Constants.PLUGIN_FILENAME_DESCRIPTOR);
				computer.addFile(buildResultFolder, Constants.PLUGIN_FILENAME_DESCRIPTOR);
			}
			if (set.contains(Constants.FRAGMENT_FILENAME_DESCRIPTOR) && new File(buildResultFolder, Constants.FRAGMENT_FILENAME_DESCRIPTOR).exists()) {
				set.remove(Constants.FRAGMENT_FILENAME_DESCRIPTOR);
				computer.addFile(buildResultFolder, Constants.FRAGMENT_FILENAME_DESCRIPTOR);
			}

			if (new File(buildResultFolder, API_DESCRIPTION).exists())
				computer.addFile(buildResultFolder, API_DESCRIPTION);

			//everything else
			computer.addFiles(baseDirectory, (String[]) set.toArray(new String[set.size()]));
		}

		boolean dotIncluded = false;
		if (entries != null) {
			//add all the compiled libraries
			boolean haveEntries = false;
			fileSet = new FileSet();
			fileSet.setProject(getProject());
			fileSet.setDir(new File(buildResultFolder));
			for (int i = 0; i < entries.length; i++) {
				String name = entries[i].getName(false);
				if (name.equals(ModelBuildScriptGenerator.DOT)) {
					dotIncluded = true;
					continue;
				}

				if (sourceMap.containsKey(name) && entries[i].getType() == CompiledEntry.FOLDER) {
					Set folders = (Set) sourceMap.get(name);
					processOutputFolders(folders, name, computer);
				} else {
					NameEntry fileInclude = fileSet.createInclude();
					fileInclude.setName(name + ((entries[i].getType() == CompiledEntry.FOLDER) ? "/" : "")); //$NON-NLS-1$ //$NON-NLS-2$
					haveEntries = true;
				}
			}
			if (haveEntries) {
				computer.addFiles(buildResultFolder, fileSet.getDirectoryScanner().getIncludedFiles());
			}
		}

		if (dotIncluded) {
			//special handling for '.'
			if (sourceMap.containsKey(ModelBuildScriptGenerator.DOT)) {
				Set folders = (Set) sourceMap.get(ModelBuildScriptGenerator.DOT);
				processOutputFolders(folders, ModelBuildScriptGenerator.DOT, computer);
			} else {
				fileSet = new FileSet();
				fileSet.setProject(getProject());
				fileSet.setDir(new File(buildResultFolder, ModelBuildScriptGenerator.EXPANDED_DOT));
				NameEntry fileInclude = fileSet.createInclude();
				fileInclude.setName("**"); //$NON-NLS-1$
				if (exclude != null) {
					String[] splitExcludes = Utils.getArrayFromString(exclude);
					for (int i = 0; i < splitExcludes.length; i++) {
						NameEntry fileExclude = fileSet.createExclude();
						fileExclude.setName(splitExcludes[i]);
					}
				}

				computer.addFiles(buildResultFolder + '/' + ModelBuildScriptGenerator.EXPANDED_DOT, fileSet.getDirectoryScanner().getIncludedFiles());
			}
		}

		if (gatheredSource != null) {
			File sourceFile = new File(gatheredSource);
			if (sourceFile.exists()) {
				fileSet = new FileSet();
				fileSet.setProject(getProject());
				fileSet.setDir(sourceFile);
				NameEntry fileInclude = fileSet.createInclude();
				fileInclude.setName("**"); //$NON-NLS-1$
				computer.addFiles(gatheredSource, fileSet.getDirectoryScanner().getIncludedFiles());
			}
		}

		return computer;
	}

	private void processOutputFolders(Set folders, String key, GatheringComputer computer) {
		boolean dot = key.equals(ModelBuildScriptGenerator.DOT);
		for (Iterator iterator = folders.iterator(); iterator.hasNext();) {
			OutputFileSet outputFiles = (OutputFileSet) iterator.next();
			File baseDir = outputFiles.getDir();
			String[] includes = outputFiles.mergeIncludes(getProject());
			//handling more than one include here would involve correlating the includes files
			//with the pattern that included them.
			if (includes.length == 1) {
				IPath prefix = new Path(includes[0]).removeLastSegments(1);
				int count = prefix.segmentCount();
				String[] files = outputFiles.getDirectoryScanner().getIncludedFiles();
				for (int i = 0; i < files.length; i++) {
					IPath suffix = new Path(files[i]).removeFirstSegments(count);
					String computerPath = dot ? suffix.toString() : key + '/' + suffix.toString();
					computer.addFile(computerPath, new File(baseDir, files[i]));
				}
			}
		}
	}

	public void setBuildResultFolder(String buildResultFolder) {
		this.buildResultFolder = buildResultFolder;
	}

	public void setUnpack(String unpack) {
		if (unpack != null && unpack.length() > 0 && !unpack.startsWith(ANT_PREFIX))
			this.unpack = unpack;
	}

	public void setGatheredSource(String gatheredSource) {
		if (gatheredSource != null && gatheredSource.length() > 0 && !gatheredSource.startsWith(ANT_PREFIX))
			this.gatheredSource = gatheredSource;
	}

	public void setTargetFolder(String targetFolder) {
		if (targetFolder != null && targetFolder.length() > 0 && !targetFolder.startsWith(ANT_PREFIX))
			this.targetFolder = targetFolder;
	}

	public void addConfiguredOutputFolder(OutputFileSet output) {
		String key = output.getLibrary();
		if (sourceMap.containsKey(key)) {
			Set set = (Set) sourceMap.get(key);
			set.add(output);
		} else {
			Set set = new HashSet();
			set.add(output);
			sourceMap.put(key, set);
		}
	}
}
