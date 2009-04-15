/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;

public class GatherFeatureTask extends AbstractPublisherTask {
	private String buildResultFolder = null;
	private String targetFolder = null;

	public void execute() throws BuildException {
		GatheringComputer computer = createFeatureComputer();

		GatherFeatureAction action = null;
		if (targetFolder == null)
			action = new GatherFeatureAction(new File(baseDirectory), new File(buildResultFolder));
		else {
			File folder = new File(targetFolder);
			action = new GatherFeatureAction(folder, folder);
		}
		action.setComputer(computer);
		setGroupId(action);

		PublisherInfo info = getPublisherInfo();
		info.addAdvice(createRootAdvice());
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

		if (targetFolder != null) {
			FileSet fileSet = new FileSet();
			fileSet.setProject(getProject());
			fileSet.setDir(new File(targetFolder));
			NameEntry include = fileSet.createInclude();
			include.setName("**"); //$NON-NLS-1$

			String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
			if (files != null && files.length > 0) {
				GatheringComputer computer = new GatheringComputer();
				computer.addFiles(targetFolder, files);
				return computer;
			}
			return null;
		}

		String include = (String) properties.get(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		String exclude = (String) properties.get(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		if (include != null) {
			GatheringComputer computer = new GatheringComputer();

			FileSet fileSet = new FileSet();
			fileSet.setProject(getProject());
			fileSet.setDir(new File(baseDirectory));
			String[] splitIncludes = Utils.getArrayFromString(include);
			for (int i = 0; i < splitIncludes.length; i++) {
				String entry = splitIncludes[i];
				if (entry.equals(ModelBuildScriptGenerator.DOT))
					continue;

				NameEntry fileInclude = fileSet.createInclude();
				fileInclude.setName(entry);
			}

			String[] splitExcludes = Utils.getArrayFromString(exclude);
			for (int i = 0; i < splitExcludes.length; i++) {
				NameEntry fileExclude = fileSet.createExclude();
				fileExclude.setName(splitIncludes[i]);
			}
			computer.addFiles(baseDirectory, fileSet.getDirectoryScanner().getIncludedFiles());
			return computer;
		}
		return null;
	}

	private String reorderConfig(String config) {
		String[] parsed = Utils.getArrayFromString(config, "."); //$NON-NLS-1$
		return parsed[1] + '.' + parsed[0] + '.' + parsed[2];
	}

	protected FeatureRootAdvice createRootAdvice() {
		FeatureRootAdvice advice = new FeatureRootAdvice();

		Map configMap = Utils.processRootProperties(getBuildProperties(), false);
		for (Iterator iterator = configMap.keySet().iterator(); iterator.hasNext();) {
			String config = (String) iterator.next();
			Map rootMap = (Map) configMap.get(config);
			if (config.equals(Utils.ROOT_COMMON))
				config = ""; //$NON-NLS-1$
			else
				config = reorderConfig(config);
			GatheringComputer computer = new GatheringComputer();
			Set configFileSets = new HashSet();
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
							configFileSets.add(fileset);
						}
					}
				}
			}
			advice.addRootfiles(config, computer);

			//do permissions, out of the configFileSets, select the files to change permissions on.
			for (Iterator p = permissionsKeys.iterator(); p.hasNext();) {
				String permissionKey = (String) p.next();
				FilenameSelector nameSelector = new FilenameSelector();
				nameSelector.setProject(getProject());
				nameSelector.setName((String) rootMap.get(permissionKey));
				SelectSelector selector = new SelectSelector();
				selector.setProject(getProject());
				selector.addFilename(nameSelector);

				for (Iterator s = configFileSets.iterator(); s.hasNext();) {
					FileSet fileset = (FileSet) ((FileSet) s.next()).clone();
					fileset.addSelector(selector);

					String[] found = fileset.getDirectoryScanner().getIncludedFiles();
					advice.addPermissions(config, permissionKey.substring(Utils.ROOT_PERMISSIONS.length()), found);
				}
			}
		}
		return advice;
	}

	public void setBuildResultFolder(String buildResultFolder) {
		if (buildResultFolder != null && buildResultFolder.length() > 0 && !buildResultFolder.startsWith(ANT_PREFIX))
			this.buildResultFolder = buildResultFolder;
	}

	public void setTargetFolder(String targetFolder) {
		if (targetFolder != null && targetFolder.length() > 0 && !targetFolder.startsWith(ANT_PREFIX))
			this.targetFolder = targetFolder;
	}
}
