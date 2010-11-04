/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.tasks.TaskMessages;

public class GatherFeatureTask extends AbstractPublisherTask {
	private String buildResultFolder = null;
	private String targetFolder = null;
	private String licenseDirectory = null;

	public void execute() throws BuildException {
		GatheringComputer computer = createFeatureComputer();

		GatherFeatureAction action = null;
		if (targetFolder == null)
			action = new GatherFeatureAction(new File(baseDirectory), new File(buildResultFolder));
		else {
			action = new GatherFeatureAction(new File(baseDirectory), new File(targetFolder));
		}
		action.setComputer(computer);
		setGroupId(action);

		FeatureRootAdvice advice = createRootAdvice();
		action.setRootAdvice(advice);
		PublisherInfo info = getPublisherInfo();
		info.addAdvice(advice);
		BuildPublisherApplication application = createPublisherApplication();
		application.addAction(action);
		try {
			application.run(info);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setGroupId(GatherFeatureAction action) {
		Properties properties = getBuildProperties();
		if (properties.containsKey(IBuildPropertiesConstants.PROPERTY_P2_GROUP_ID))
			action.setGroupId(properties.getProperty(IBuildPropertiesConstants.PROPERTY_P2_GROUP_ID));
	}

	protected GatheringComputer createFeatureComputer() {
		Properties properties = getBuildProperties();

		String include = (String) properties.get(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		String exclude = (String) properties.get(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		if (include == null)
			return null;

		if (targetFolder != null) {
			FileSet fileSet = new FileSet();
			fileSet.setProject(getProject());
			fileSet.setDir(new File(targetFolder));
			NameEntry includeEntry = fileSet.createInclude();
			includeEntry.setName("**"); //$NON-NLS-1$

			String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
			if (files != null && files.length > 0) {
				GatheringComputer computer = new GatheringComputer();
				computer.addFiles(targetFolder, files);
				return computer;
			}
			return null;
		}

		GatheringComputer computer = new GatheringComputer();

		if (licenseDirectory != null) {
			try {
				// Default includes and excludes for binary license features
				String licenseInclude = "**"; //$NON-NLS-1$
				String licenseExclude = "META-INF/"; //$NON-NLS-1$

				// Read build.properties from license feature for source features
				if (new File(licenseDirectory, IPDEBuildConstants.PROPERTIES_FILE).exists()) {
					Properties licenseProperties = AbstractScriptGenerator.readProperties(licenseDirectory, IPDEBuildConstants.PROPERTIES_FILE, IStatus.WARNING);
					licenseInclude = (String) licenseProperties.get(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
					licenseExclude = (String) licenseProperties.get(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);
				}
				licenseExclude = (licenseExclude != null ? licenseExclude + "," : "") + IPDEBuildConstants.LICENSE_DEFAULT_EXCLUDES; //$NON-NLS-1$//$NON-NLS-2$

				FileSet licenseFiles = createFileSet(licenseDirectory, licenseInclude, licenseExclude);
				computer.addFiles(licenseDirectory, licenseFiles.getDirectoryScanner().getIncludedFiles());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		FileSet fileSet = createFileSet(buildResultFolder, include, exclude);
		computer.addFiles(buildResultFolder, fileSet.getDirectoryScanner().getIncludedFiles());
		return computer;
	}

	private FileSet createFileSet(String folder, String includes, String excludes) {
		FileSet fileSet = new FileSet();
		fileSet.setProject(getProject());
		fileSet.setDir(new File(folder));
		String[] splitIncludes = Utils.getArrayFromString(includes);
		for (int i = 0; i < splitIncludes.length; i++) {
			String entry = splitIncludes[i];
			if (entry.equals(ModelBuildScriptGenerator.DOT))
				continue;

			NameEntry fileInclude = fileSet.createInclude();
			fileInclude.setName(entry);
		}

		String[] splitExcludes = Utils.getArrayFromString(excludes);
		for (int i = 0; i < splitExcludes.length; i++) {
			NameEntry fileExclude = fileSet.createExclude();
			fileExclude.setName(splitExcludes[i]);
		}
		return fileSet;
	}

	private String reorderConfig(String config) {
		String[] parsed = Utils.getArrayFromString(config, "."); //$NON-NLS-1$
		return parsed[1] + '.' + parsed[0] + '.' + parsed[2];
	}

	protected FeatureRootAdvice createRootAdvice() {
		FeatureRootAdvice advice = new FeatureRootAdvice();

		Map configMap = Utils.processRootProperties(getBuildProperties(), true);
		for (Iterator iterator = configMap.keySet().iterator(); iterator.hasNext();) {
			String config = (String) iterator.next();
			Map rootMap = (Map) configMap.get(config);
			if (config.equals(Utils.ROOT_COMMON))
				config = ""; //$NON-NLS-1$
			else
				config = reorderConfig(config);
			GatheringComputer computer = new GatheringComputer();
			Map configFileSets = new HashMap();
			ArrayList permissionsKeys = new ArrayList();
			for (Iterator rootEntries = rootMap.keySet().iterator(); rootEntries.hasNext();) {
				String key = (String) rootEntries.next();
				if (key.startsWith(Utils.ROOT_PERMISSIONS)) {
					permissionsKeys.add(key);
					continue;
				} else if (key.equals(Utils.ROOT_LINK)) {
					advice.addLinks(config, (String) rootMap.get(key));
					continue;
				} else {
					//files!
					String fileList = (String) rootMap.get(key);
					String[] files = Utils.getArrayFromString(fileList, ","); //$NON-NLS-1$
					for (int i = 0; i < files.length; i++) {
						String file = files[i];
						String fromDir = baseDirectory;
						File base = null;
						if (file.startsWith("absolute:")) { //$NON-NLS-1$
							file = file.substring(9);
							fromDir = null;
						} else if (file.startsWith("license:")) { //$NON-NLS-1$
							if (licenseDirectory == null) {
								throw new BuildException(NLS.bind(TaskMessages.error_licenseRootWithoutLicenseRef, baseDirectory));
							}
							file = file.substring(8);
							fromDir = licenseDirectory;
						}
						if (file.startsWith("file:")) { //$NON-NLS-1$
							File temp = fromDir != null ? new File(fromDir, file.substring(5)) : new File(file.substring(5));
							base = temp.getParentFile();
							file = temp.getName();
						} else {
							base = fromDir != null ? new File(fromDir, file) : new File(file);
							file = "**"; //$NON-NLS-1$
						}

						if (base.exists()) {
							FileSet fileset = new FileSet();
							fileset.setProject(getProject());
							fileset.setDir(base);
							NameEntry include = fileset.createInclude();
							include.setName(file);

							String[] found = fileset.getDirectoryScanner().getIncludedFiles();
							for (int k = 0; k < found.length; k++) {
								if (key.length() > 0)
									computer.addFile(key + "/" + found[k], new File(base, found[k])); //$NON-NLS-1$
								else
									computer.addFile(base.getAbsolutePath(), found[k]);
							}
							configFileSets.put(fileset, key);
						}
					}
				}
			}
			if (computer.size() > 0)
				advice.addRootfiles(config, computer);

			//do permissions, out of the configFileSets, select the files to change permissions on.
			for (Iterator p = permissionsKeys.iterator(); p.hasNext();) {
				String permissionKey = (String) p.next();
				String permissionString = (String) rootMap.get(permissionKey);
				String[] names = Utils.getArrayFromString(permissionString);

				OrSelector orSelector = new OrSelector();
				orSelector.setProject(getProject());
				for (int i = 0; i < names.length; i++) {
					FilenameSelector nameSelector = new FilenameSelector();
					nameSelector.setProject(getProject());
					nameSelector.setName(names[i]);
					orSelector.addFilename(nameSelector);
				}
				for (Iterator s = configFileSets.keySet().iterator(); s.hasNext();) {
					FileSet fileset = (FileSet) s.next();
					String finalFolder = (String) configFileSets.get(fileset);
					String[] found = selectFiles(orSelector, finalFolder, fileset.getDirectoryScanner().getIncludedFiles());
					if (found.length > 0)
						advice.addPermissions(config, permissionKey.substring(Utils.ROOT_PERMISSIONS.length()), found);
				}
			}
		}
		return advice;
	}

	/*
	 * Select from the given files those that match the passed in group of OR'ed FilenameSelectors
	 * We can't just use the selectors directly on the fileset because the files may be going to different
	 * folders.  They will end up under the given folder, which is either the root, or the folder corresponding 
	 * to "root.folder" properties. 
	 */
	private String[] selectFiles(OrSelector selector, String folder, String[] files) {
		String prefix = (folder.length() > 0) ? folder + '/' : ""; //$NON-NLS-1$
		ArrayList result = new ArrayList();

		for (int i = 0; i < files.length; i++) {
			String finalLocation = prefix + files[i];
			//FilenameSelector is checking based on File.separatorChar, so normalize
			finalLocation = finalLocation.replace('/', File.separatorChar).replace('\\', File.separatorChar);
			//FilenameSelector objects only care about the filename and not the other arguments
			if (selector.isSelected(null, finalLocation, null))
				result.add(finalLocation.replace('\\', '/')); //we work with '/'
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public void setBuildResultFolder(String buildResultFolder) {
		if (buildResultFolder != null && buildResultFolder.length() > 0 && !buildResultFolder.startsWith(ANT_PREFIX))
			this.buildResultFolder = buildResultFolder;
	}

	public void setTargetFolder(String targetFolder) {
		if (targetFolder != null && targetFolder.length() > 0 && !targetFolder.startsWith(ANT_PREFIX))
			this.targetFolder = targetFolder;
	}

	public void setLicenseDirectory(String licenseDirectory) {
		if (licenseDirectory != null && licenseDirectory.length() > 0 && !licenseDirectory.startsWith(ANT_PREFIX))
			this.licenseDirectory = licenseDirectory;
	}
}
