/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *     Jan-Ove Weichel <janove.weichel@vogella.com> - Bug 477658
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 525701
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.core.Property;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.build.site.QualifierReplacer;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.target.TargetMetadataCollector;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.InvalidSyntaxException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FeatureExportOperation extends Job {

	// Location where the build takes place
	protected String fBuildTempLocation;
	protected String fBuildTempMetadataLocation;
	private String fDevProperties;
	private static boolean fHasErrors;
	protected HashMap<String, String> fAntBuildProperties;
	protected WorkspaceExportHelper fWorkspaceExportHelper;

	protected State fStateCopy;

	protected static String FEATURE_POST_PROCESSING = "features.postProcessingSteps.properties"; //$NON-NLS-1$
	protected static String PLUGIN_POST_PROCESSING = "plugins.postProcessingSteps.properties"; //$NON-NLS-1$

	private static final String[] GENERIC_CONFIG = new String[] {"*", "*", "*", ""}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	protected FeatureExportInfo fInfo;

	public FeatureExportOperation(FeatureExportInfo info, String name) {
		super(name);
		fInfo = info;
		String qualifier = info.qualifier;
		if (qualifier == null) {
			qualifier = QualifierReplacer.getDateQualifier();
		}
		QualifierReplacer.setGlobalQualifier(qualifier);
		fBuildTempLocation = PDECore.getDefault().getStateLocation().append("temp").toString(); //$NON-NLS-1$
		fBuildTempMetadataLocation = PDECore.getDefault().getStateLocation().append("tempp2metadata").toString(); //$NON-NLS-1$
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			createDestination();
			String[][] configurations = fInfo.targets;
			if (configurations == null) {
				configurations = new String[][] {null};
			}
			SubMonitor subMonitor = SubMonitor.convert(monitor, "Exporting...", (fInfo.items.length * 23) + 5 + 10); //$NON-NLS-1$

			IStatus status = testBuildWorkspaceBeforeExport(subMonitor.split(10));

			if (fInfo.exportSource && fInfo.exportSourceBundle) {
				// create a feature to contain all plug-ins and features depth first
				String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
				String fFeatureLocation = fBuildTempLocation + File.separator + featureID;
				createFeature(featureID, fFeatureLocation, fInfo.items, null, null, null);
				ExternalFeatureModel model = new ExternalFeatureModel();
				model.setInstallLocation(fFeatureLocation);
				try (InputStream stream = new BufferedInputStream(new FileInputStream(
						new File(fFeatureLocation + File.separator + ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)))) {
					model.load(stream, true);
				}
				doExport(model, null, subMonitor.split(20));

			} else {
				for (Object item : fInfo.items) {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					try {
						doExport((IFeatureModel) item, configurations, subMonitor.split(20));
					} catch (CoreException e) {
						return e.getStatus();
					} finally {
						cleanup(subMonitor.split(3));
					}
				}
			}
			return status;
		} catch (InvocationTargetException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e.getCause() != null ? e.getCause() : e);
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e.getCause() != null ? e.getCause() : e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.FeatureBasedExportOperation_ProblemDuringExport, e);
		}

	}

	protected void save(File file, Properties properties, String header) {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			properties.store(stream, header);
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	/**
	 * Takes the generated metadata and adds it to the destination zip.
	 * This method should only be called if exporting to an archive file
	 * and metadata was generated at fBuildMetadataLocation.
	 * @param monitor progress monitor
	 */
	protected void appendMetadataToArchive(String[] configuration, IProgressMonitor monitor) {
		String filename = fInfo.zipFileName;
		if (configuration != null) {
			int i = filename.lastIndexOf('.');
			filename = filename.substring(0, i) + '.' + configuration[0] + '.' + configuration[1] + '.' + configuration[2] + filename.substring(i);
		}
		String archive = fInfo.destinationDirectory + File.separator + filename;
		File scriptFile = null;
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		try {
			scriptFile = createScriptFile("append.xml"); //$NON-NLS-1$

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();

			Element root = doc.createElement("project"); //$NON-NLS-1$
			root.setAttribute("name", "temp"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("default", "append"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("basedir", "."); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);

			Element target = doc.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "clean"); //$NON-NLS-1$ //$NON-NLS-2$
			Element child = doc.createElement("delete"); //$NON-NLS-1$
			child.setAttribute("dir", fBuildTempMetadataLocation); //$NON-NLS-1$
			target.appendChild(child);
			root.appendChild(target);

			target = doc.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "append"); //$NON-NLS-1$ //$NON-NLS-2$
			child = doc.createElement("zip"); //$NON-NLS-1$
			child.setAttribute("zipfile", archive); //$NON-NLS-1$
			child.setAttribute("basedir", fBuildTempMetadataLocation); //$NON-NLS-1$
			child.setAttribute("update", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			target.appendChild(child);
			root.appendChild(target);

			XMLPrintHandler.writeFile(doc, scriptFile);

			String[] targets = new String[] {"append", "clean"}; //$NON-NLS-1$ //$NON-NLS-2$
			AntRunner runner = new AntRunner();
			runner.setBuildFileLocation(scriptFile.getAbsolutePath());
			runner.setExecutionTargets(targets);
			runner.run(subMonitor.split(1));
		} catch (FactoryConfigurationError | ParserConfigurationException | CoreException | IOException e) {
			PDECore.logException(e);
		} finally {
			if (scriptFile != null && scriptFile.exists()) {
				scriptFile.delete();
			}
		}
	}

	private void createDestination(String os, String ws, String arch) throws InvocationTargetException {
		if (!fInfo.toDirectory || groupedConfigurations()) {
			return;
		}
		File file = new File(fInfo.destinationDirectory, os + '.' + ws + '.' + arch);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				throw new InvocationTargetException(new Exception(PDECoreMessages.ExportWizard_badDirectory));
			}
		}
	}

	private void doExport(IFeatureModel model, String[][] configs, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		IFeature feature = model.getFeature();
		if (configs == null || configs.length == 0 || configs[0] == null) {
			configs = new String[][] {{getOS(feature), getWS(feature), getOSArch(feature)}};
		} else {
			for (String[] config : configs) {
				createDestination(config[0], config[1], config[2]);
			}
		}
		try {
			String location = model.getInstallLocation();
			if (fInfo.useJarFormat) {
				createPostProcessingFile(new File(location, FEATURE_POST_PROCESSING));
				createPostProcessingFile(new File(location, PLUGIN_POST_PROCESSING));
			}
			doExport(feature.getId(), feature.getVersion(), location, configs, monitor);
		} finally {
			deleteBuildFiles(model);
		}
	}

	protected void createPostProcessingFile(File file) {
		try (FileOutputStream stream = new FileOutputStream(file);) {
			Properties prop = new Properties();
			prop.put("*", "updateJar"); //$NON-NLS-1$ //$NON-NLS-2$
			prop.store(stream, ""); //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
		}
	}

	protected void doExport(String featureID, String version, String featureLocation, String[][] configs, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		fHasErrors = false;

		int subTaskLength = 6 + (configs.length * 4) + (publishingP2Metadata() ? 2 : 0);
		SubMonitor subMonitor = SubMonitor.convert(monitor, PDECoreMessages.FeatureExportJob_taskName, subTaskLength);

		HashMap<String, String> properties = createAntBuildProperties(configs);
		BuildScriptGenerator generator = new BuildScriptGenerator();
		setupGenerator(generator, featureID, version, configs, featureLocation);
		generator.generate();
		subMonitor.split(1);
		subMonitor.setTaskName(PDECoreMessages.FeatureExportOperation_runningBuildScript);
		// compile the classes
		runScript(featureLocation + IPath.SEPARATOR + "compile." + featureID + ".xml", new String[] {"main"}, properties, subMonitor.split(1)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// grab the source if needed
		if (fInfo.exportSource && !fInfo.exportSourceBundle) {
			runScript(getBuildScriptName(featureLocation), new String[] {"build.sources"}, properties, subMonitor.split(1)); //$NON-NLS-1$
		}

		if (publishingP2Metadata()) {
			subMonitor.setTaskName(PDECoreMessages.FeatureExportOperation_publishingMetadata);
			runScript(getAssembleP2ScriptName(featureID, featureLocation), new String[] {"main"}, properties, subMonitor.split(2)); //$NON-NLS-1$

			//metadata implies groups if we aren't exporting products
			if (groupedConfigurations()) {
				configs = new String[][] {{"group", "group", "group"}}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

		}

		subMonitor.setTaskName(PDECoreMessages.FeatureExportOperation_runningAssemblyScript);
		for (String[] config : configs) {
			setArchiveLocation(properties, config[0], config[1], config[2]);
			runScript(getAssemblyScriptName(featureID, config[0], config[1], config[2], featureLocation), new String[] {"main"}, //$NON-NLS-1$
					properties, subMonitor.split(2));
		}

		subMonitor.setTaskName(PDECoreMessages.FeatureExportOperation_runningPackagerScript);
		for (String[] config : configs) {
			setArchiveLocation(properties, config[0], config[1], config[2]);
			runScript(getPackagerScriptName(featureID, config[0], config[1], config[2], featureLocation), null, properties, subMonitor.split(2));
		}
		properties.put("destination.temp.folder", fBuildTempLocation + "/pde.logs"); //$NON-NLS-1$ //$NON-NLS-2$
		runScript(getBuildScriptName(featureLocation), new String[] {"gather.logs"}, properties, subMonitor.split(2)); //$NON-NLS-1$
	}

	protected boolean groupedConfigurations() {
		//feature export with p2 metadata results in a grouped repo
		return publishingP2Metadata();
	}

	private void setArchiveLocation(Map<String, String> antProperties, String os, String ws, String arch) {
		if (!fInfo.toDirectory) {
			String filename = fInfo.zipFileName;
			if (fInfo.targets != null && !groupedConfigurations()) {
				int i = filename.lastIndexOf('.');
				filename = filename.substring(0, i) + '.' + os + '.' + ws + '.' + arch + filename.substring(i);
			}
			antProperties.put(IXMLConstants.PROPERTY_ARCHIVE_FULLPATH, fInfo.destinationDirectory + File.separator + filename);
		} else {
			String dir = fInfo.destinationDirectory;
			if (fInfo.targets != null && !groupedConfigurations()) {
				dir += File.separatorChar + os + '.' + ws + '.' + arch;
			}
			antProperties.put(IXMLConstants.PROPERTY_ASSEMBLY_TMP, dir);
		}
	}

	public void deleteBuildFiles(Object object) throws CoreException {
		IModel model = null;
		if (object instanceof BundleDescription) {
			model = PluginRegistry.findModel((BundleDescription) object);
		} else if (object instanceof IModel) {
			model = (IModel) object;
		}

		if (model == null) {
			return;
		}

		if (model.getUnderlyingResource() != null && !isCustomBuild(model)) {
			String directory = (model instanceof IFeatureModel) ? ((IFeatureModel) model).getInstallLocation() : ((IPluginModelBase) model).getInstallLocation();
			File dir = new File(directory);
			File[] children = dir.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					if (!children[i].isDirectory()) {
						String filename = children[i].getName();
						if (filename.equals("build.xml") //$NON-NLS-1$
								|| (filename.startsWith("javaCompiler.") && filename.endsWith(".args")) //$NON-NLS-1$ //$NON-NLS-2$
								|| (filename.startsWith("assemble.") && filename.endsWith(".xml")) //$NON-NLS-1$ //$NON-NLS-2$
								|| (filename.startsWith("package.") && filename.endsWith(".xml")) //$NON-NLS-1$ //$NON-NLS-2$
								|| (filename.startsWith("compile.") && filename.endsWith(".xml")) //$NON-NLS-1$ //$NON-NLS-2$
								|| filename.equals(FEATURE_POST_PROCESSING) || filename.equals(PLUGIN_POST_PROCESSING)) {
							children[i].delete();
						}
					} else if (children[i].getName().equals("temp.folder")) { //$NON-NLS-1$
						CoreUtility.deleteContent(children[i]);
					}
				}
			}
		}

		if (model instanceof IFeatureModel) {
			IFeature feature = ((IFeatureModel) model).getFeature();
			IFeatureChild[] children = feature.getIncludedFeatures();
			for (IFeatureChild featureChild : children) {
				IFeature ref = ((FeatureChild) featureChild).getReferencedFeature();
				if (ref != null) {
					deleteBuildFiles(ref.getModel());
				}
			}

			IFeaturePlugin[] plugins = feature.getPlugins();
			for (IFeaturePlugin featurePlugin : plugins) {
				IPluginModelBase plugin = PluginRegistry.findModel(featurePlugin.getId());
				if (plugin != null) {
					deleteBuildFiles(plugin);
				}
			}
		}
	}

	private String getBuildScriptName(String featureLocation) {
		return featureLocation + IPath.SEPARATOR + "build.xml"; //$NON-NLS-1$
	}

	protected String getAssemblyScriptName(String featureID, String os, String ws, String arch, String featureLocation) {
		return featureLocation + IPath.SEPARATOR + "assemble." //$NON-NLS-1$
				+ featureID + "." + os + "." //$NON-NLS-1$ //$NON-NLS-2$
				+ ws + "." + arch //$NON-NLS-1$
				+ ".xml"; //$NON-NLS-1$
	}

	protected String getAssembleP2ScriptName(String featureID, String featureLocation) {
		return featureLocation + IPath.SEPARATOR + "assemble." + featureID + ".p2.xml"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Execute the script at the given location.
	 *
	 * @param location the script to run
	 * @param targets the targets in the script to run, use <code>null</code> to run all
	 * @param properties map of user properties
	 * @param monitor progress monitor
	 * @throws InvocationTargetException
	 * @throws CoreException
	 */
	protected void runScript(String location, String[] targets, Map<String, String> properties, IProgressMonitor monitor) throws InvocationTargetException, CoreException {
		AntRunner runner = new AntRunner();
		runner.addUserProperties(properties);
		runner.setAntHome(location);
		runner.setBuildFileLocation(location);
		runner.addBuildListener("org.eclipse.pde.internal.core.ant.ExportBuildListener"); //$NON-NLS-1$
		runner.setExecutionTargets(targets);
		if (fInfo.signingInfo != null) {
			AntCorePreferences preferences = AntCorePlugin.getPlugin().getPreferences();
			IAntClasspathEntry entry = preferences.getToolsJarEntry();
			if (entry != null) {
				IAntClasspathEntry[] classpath = preferences.getAntHomeClasspathEntries();
				URL[] urls = new URL[classpath.length + 2];
				for (int i = 0; i < classpath.length; i++) {
					urls[i] = classpath[i].getEntryURL();
				}
				IPath path = new Path(entry.getEntryURL().toString()).removeLastSegments(2);
				path = path.append("bin"); //$NON-NLS-1$
				try {
					urls[classpath.length] = new URL(path.toString());
				} catch (MalformedURLException e) {
					urls[classpath.length] = entry.getEntryURL();
				} finally {
					urls[classpath.length + 1] = entry.getEntryURL();
				}
				runner.setCustomClasspath(urls);
			}
		}
		runner.run(monitor);
	}

	protected String getPackagerScriptName(String featureID, String os, String ws, String arch, String featureLocation) {
		return featureLocation + IPath.SEPARATOR + "package." //$NON-NLS-1$
				+ featureID + "." + os + "." //$NON-NLS-1$ //$NON-NLS-2$
				+ ws + "." + arch //$NON-NLS-1$
				+ ".xml"; //$NON-NLS-1$
	}

	protected HashMap<String, String> createAntBuildProperties(String[][] configs) {
		if (fAntBuildProperties == null) {
			fAntBuildProperties = new HashMap<>(15);

			List<?> defaultProperties = AntCorePlugin.getPlugin().getPreferences().getProperties();
			ListIterator<?> li = defaultProperties.listIterator();
			while (li.hasNext()) {
				Property prop = (Property) li.next();
				fAntBuildProperties.put(prop.getName(), prop.getValue());
			}

			if (fInfo.signingInfo != null) {
				fAntBuildProperties.put(IXMLConstants.PROPERTY_SIGN_ALIAS, fInfo.signingInfo[0]);
				fAntBuildProperties.put(IXMLConstants.PROPERTY_SIGN_KEYSTORE, fInfo.signingInfo[1]);
				fAntBuildProperties.put(IXMLConstants.PROPERTY_SIGN_STOREPASS, fInfo.signingInfo[2]);
				fAntBuildProperties.put(IXMLConstants.PROPERTY_SIGN_KEYPASS, fInfo.signingInfo[3]);
			}
			if (fInfo.jnlpInfo != null) {
				fAntBuildProperties.put(IXMLConstants.PROPERTY_JNLP_CODEBASE, fInfo.jnlpInfo[0]);
				fAntBuildProperties.put(IXMLConstants.PROPERTY_JNLP_J2SE, fInfo.jnlpInfo[1]);
			}

			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_TEMP, fBuildTempLocation + "/destination"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_FEATURE_TEMP_FOLDER, fBuildTempLocation + "/destination"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
			fAntBuildProperties.put("eclipse.running", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_GENERATE_API_DESCRIPTION, "true"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_OS, TargetPlatform.getOS());
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_WS, TargetPlatform.getWS());
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_ARCH, TargetPlatform.getOSArch());
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_NL, TargetPlatform.getNL());
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BOOTCLASSPATH, BuildUtilities.getBootClasspath());
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
			for (IExecutionEnvironment env : envs) {
				String id = env.getId();
				if (id != null) {
					fAntBuildProperties.put(id, BuildUtilities.getBootClasspath(id));
				}
			}
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_FAIL_ON_ERROR, "false"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_DEBUG_INFO, "on"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_VERBOSE, "false"); //$NON-NLS-1$

			IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
			IEclipsePreferences def = DefaultScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
			String source = prefs.get(JavaCore.COMPILER_SOURCE, null);
			if (source == null) {
				source = def.get(JavaCore.COMPILER_SOURCE, null);
			}
			if (source != null) {
				fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, source);
			}
			String target = prefs.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, null);
			if (target == null) {
				target = def.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, null);
			}
			if (target != null) {
				fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, target);
			}

			// for the assembler...
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_DIRECTORY, fBuildTempLocation + "/assemblyLocation"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_LABEL, "."); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, "."); //$NON-NLS-1$
			String prefix = Platform.getOS().equals("macosx") ? "." : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, prefix);
			fAntBuildProperties.put(IXMLConstants.PROPERTY_TAR_ARGS, ""); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_RUN_PACKAGER, "true"); //$NON-NLS-1$
		}

		setP2MetaDataProperties(fAntBuildProperties);

		return fAntBuildProperties;
	}

	/**
	 * Whether or not to use new metadata publishing or old generation
	 */
	protected boolean publishingP2Metadata() {
		return fInfo.useJarFormat && fInfo.exportMetadata;
	}

	/**
	 * Returns a list of URI metadata repository locations that contain metadata for some
	 * or all of the target platform.  The repositories may not contain metadata for all
	 * plugins in the target.  This method returns <code>null</code> if the target does not
	 * have any repositories.
	 *
	 * @return list of URI representing metadata repositories or <code>null</code>
	 */
	protected URI[] getMetadataContextFromTargetPlatform() {
		try {
			URI[] context = TargetMetadataCollector.getMetadataRepositories(null);
			if (context.length > 0) {
				return context;
			}
		} catch (CoreException e) {
			return null;
		}
		return null;
	}

	/**
	 * @return the location of the category definition file, or null if none is specified
	 */
	protected String getCategoryDefinition() {
		return fInfo.categoryDefinition;
	}

	/**
	* Adds the necessary properties to invoke the p2 metadata generator.  This method will
	* be called when creating the ant build properties map.
	*
	* @param map the map to add generator properties to
	*/
	protected void setP2MetaDataProperties(Map<String, String> map) {
		if (fInfo.useJarFormat && fInfo.exportMetadata) {
			map.put(IXMLConstants.TARGET_P2_METADATA, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FLAVOR, P2Utils.P2_FLAVOR_DEFAULT);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_PUBLISH_ARTIFACTS, IBuildPropertiesConstants.FALSE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FINAL_MODE_OVERRIDE, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_COMPRESS, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_GATHERING, Boolean.toString(publishingP2Metadata()));
			if (getCategoryDefinition() != null) {
				map.put(IBuildPropertiesConstants.PROPERTY_P2_CATEGORY_DEFINITION, getCategoryDefinition());
			}
			try {
				String destination = ""; //$NON-NLS-1$
				if (publishingP2Metadata()) {
					destination = new File(fBuildTempMetadataLocation).toURL().toString();
					map.put(IBuildPropertiesConstants.PROPERTY_P2_BUILD_REPO, destination);
				} else {
					if (fInfo.toDirectory) {
						destination = new File(fInfo.destinationDirectory).toURL().toString();
					} else {
						destination = new File(fBuildTempMetadataLocation).toURI().toURL().toString();
					}
					map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO, destination);
					map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO, destination);
				}
				map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO_NAME, PDECoreMessages.FeatureExportOperation_0);
				map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO_NAME, PDECoreMessages.FeatureExportOperation_0);
			} catch (MalformedURLException e) {
				PDECore.log(e);
			}
		}
	}

	private String getOS(IFeature feature) {
		String os = feature.getOS();
		if (os == null || os.trim().length() == 0 || os.indexOf(',') != -1 || os.equals("*")) { //$NON-NLS-1$
			return TargetPlatform.getOS();
		}
		return os;
	}

	private String getWS(IFeature feature) {
		String ws = feature.getWS();
		if (ws == null || ws.trim().length() == 0 || ws.indexOf(',') != -1 || ws.equals("*")) { //$NON-NLS-1$
			return TargetPlatform.getWS();
		}
		return ws;
	}

	private String getOSArch(IFeature feature) {
		String arch = feature.getArch();
		if (arch == null || arch.trim().length() == 0 || arch.indexOf(',') != -1 || arch.equals("*")) { //$NON-NLS-1$
			return TargetPlatform.getOSArch();
		}
		return arch;
	}

	protected void createDestination() throws InvocationTargetException {
		File file = new File(fInfo.destinationDirectory);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				throw new InvocationTargetException(new Exception(PDECoreMessages.ExportWizard_badDirectory));
			}
		}

		File metadataTemp = new File(fBuildTempMetadataLocation);
		if (metadataTemp.exists()) {
			//make sure our build metadata repo is clean
			deleteDir(metadataTemp);
		}
	}

	protected void deleteDir(File dir) {
		if (dir.exists()) {
			if (dir.isDirectory()) {
				File[] children = dir.listFiles();
				if (children != null) {
					for (File element : children) {
						deleteDir(element);
					}
				}
			}
			dir.delete();
		}
	}

	private String getConfigInfo(String[][] configs) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < configs.length; i++) {
			if (i > 0) {
				buffer.append('&');
			}
			buffer.append(configs[i][0]);
			buffer.append(',');
			buffer.append(configs[i][1]);
			buffer.append(',');
			buffer.append(configs[i][2]);
		}
		return buffer.toString();
	}

	private String getArchivesFormat(String[][] configs) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < configs.length; i++) {
			if (i > 0) {
				buffer.append('&');
			}
			buffer.append(configs[i][0]);
			buffer.append(',');
			buffer.append(configs[i][1]);
			buffer.append(',');
			buffer.append(configs[i][2]);
			buffer.append('-');
			buffer.append(fInfo.toDirectory ? IXMLConstants.FORMAT_FOLDER : IXMLConstants.FORMAT_ANTZIP);
		}

		if (groupedConfigurations()) {
			buffer.append('&');
			buffer.append("group,group,group-"); //$NON-NLS-1$
			buffer.append(fInfo.toDirectory ? IXMLConstants.FORMAT_FOLDER : IXMLConstants.FORMAT_ANTZIP);
		}

		return buffer.toString();
	}

	protected void setupGenerator(BuildScriptGenerator generator, String featureID, String versionId, String[][] configs, String featureLocation) throws CoreException {
		generator.setChildren(true);
		generator.setWorkingDirectory(featureLocation);
		generator.setDevEntries(getDevProperties());
		generator.setElements(new String[] {"feature@" + featureID + (versionId == null ? "" : ":" + versionId)}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		generator.setPluginPath(getPaths());
		generator.setReportResolutionErrors(false);
		generator.setIgnoreMissingPropertiesFile(true);
		generator.setSignJars(fInfo.signingInfo != null);
		generator.setGenerateJnlp(fInfo.jnlpInfo != null);
		generator.setFlattenDependencies(true);

		AbstractScriptGenerator.setConfigInfo(getConfigInfo(configs)); //This needs to be set before we set the format
		generator.setArchivesFormat(getArchivesFormat(configs));
		generator.setPDEState(getBuildState());
		generator.setNextId(TargetPlatformHelper.getPDEState().getNextId());

		if (fInfo.useWorkspaceCompiledClasses) {
			generator.setUseWorkspaceBinaries(true);
			generator.setStateExtraData(TargetPlatformHelper.getBundleClasspaths(TargetPlatformHelper.getPDEState()), TargetPlatformHelper.getPatchMap(TargetPlatformHelper.getPDEState()), getWorkspaceExportHelper().getWorkspaceOutputFolders(fInfo.items));
		} else {
			generator.setStateExtraData(TargetPlatformHelper.getBundleClasspaths(TargetPlatformHelper.getPDEState()), TargetPlatformHelper.getPatchMap(TargetPlatformHelper.getPDEState()));
		}

		AbstractScriptGenerator.setForceUpdateJar(false);
		AbstractScriptGenerator.setEmbeddedSource(fInfo.exportSource && !fInfo.exportSourceBundle);

		// allow for binary cycles
		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES, Boolean.toString(fInfo.allowBinaryCycles));
		properties.put(IBuildPropertiesConstants.PROPERTY_P2_GATHERING, Boolean.toString(publishingP2Metadata()));
		//TODO this is duplicate from createAntBuildProperties
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		for (IExecutionEnvironment env : envs) {
			String id = env.getId();
			if (id != null) {
				properties.put(id, BuildUtilities.getBootClasspath(id));
			}
		}
		generator.setImmutableAntProperties(properties);

		// allow for custom execution environments
		String[] extraLocations = ExecutionEnvironmentProfileManager.getCustomProfileLocations();
		if (extraLocations != null) {
			generator.setEESources(extraLocations);
		}

		// if we are exporting metadata, provide context repositories from the target if available and option is turned on
		if (publishingP2Metadata()) {
			URI[] contexts = getMetadataContextFromTargetPlatform();
			if (contexts != null) {
				generator.setContextMetadataRepositories(contexts);
			}
		}
	}

	protected State getState(String os, String ws, String arch) {
		State main = TargetPlatformHelper.getState();
		if (os.equals(TargetPlatform.getOS()) && ws.equals(TargetPlatform.getWS()) && arch.equals(TargetPlatform.getOSArch())) {
			return main;
		}
		if (fStateCopy == null) {
			copyState(main);
		}

		Dictionary<String, String>[] dictionaries = fStateCopy.getPlatformProperties();
		for (Dictionary<String, String> properties : dictionaries) {
			properties.put("osgi.os", os); //$NON-NLS-1$
			properties.put("osgi.ws", ws); //$NON-NLS-1$
			properties.put("osgi.arch", arch); //$NON-NLS-1$
		}
		fStateCopy.resolve(false);
		return fStateCopy;
	}

	protected State getBuildState() {
		State main = TargetPlatformHelper.getState();
		if (fStateCopy == null) {
			copyState(main);
		}
		//this state is expected to get platform properties set by pde.build and re-resolved there.
		//TODO should main.getPlatformProperties() be passed to pde/build?
		return fStateCopy;
	}

	protected void copyState(State state) {
		fStateCopy = state.getFactory().createState(state);
		fStateCopy.setResolver(Platform.getPlatformAdmin().createResolver());
		fStateCopy.setPlatformProperties(state.getPlatformProperties());
	}

	private String getDevProperties() {
		if (fDevProperties == null) {
			fDevProperties = ClasspathHelper.getDevEntriesProperties(fBuildTempLocation + "/dev.properties", false); //$NON-NLS-1$
		}
		return fDevProperties;
	}

	protected boolean isCustomBuild(IModel model) throws CoreException {
		IBuildModel buildModel = null;
		IFile buildFile = PDEProject.getBuildProperties(model.getUnderlyingResource().getProject());
		if (buildFile.exists()) {
			buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
		}
		if (buildModel != null) {
			IBuild build = buildModel.getBuild();
			if (build == null) {
				return false;
			}
			IBuildEntry entry = build.getEntry("custom"); //$NON-NLS-1$
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (final String token : tokens) {
					if (token.equals("true")) { //$NON-NLS-1$
						return true;
					}
				}
			}
		}
		return false;
	}

	protected String[] getPaths() {
		Map<String, String> map = new LinkedHashMap<>(); // merge workspace and external features using workspace over external
		FeatureModelManager fmm = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] models = fmm.getExternalModels();
		for (IFeatureModel model : models) {
			map.put(model.getFeature().getId(), model.getInstallLocation());
		}
		// remove anything that we have in the workspace models
		models = fmm.getWorkspaceModels();
		String[] locations = new String[models.length];
		for (int i = 0; i < models.length; i++) {
			map.remove(models[i].getFeature().getId());
			locations[i] = models[i].getInstallLocation();
		}
		// add all workspace models
		String[] paths = new String[map.size() + models.length];
		paths = map.values().toArray(paths);
		System.arraycopy(locations, 0, paths, map.size(), models.length);
		return paths;
	}

	protected void cleanup(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		// clear out some cached values that depend on the configuration being built.
		fDevProperties = null;
		fAntBuildProperties = null;

		File scriptFile = null;
		try {
			scriptFile = createScriptFile("zip.xml"); //$NON-NLS-1$
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();

			Element root = doc.createElement("project"); //$NON-NLS-1$
			root.setAttribute("name", "temp"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("default", "clean"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("basedir", "."); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);

			Element target = doc.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "clean"); //$NON-NLS-1$ //$NON-NLS-2$
			Element child = doc.createElement("delete"); //$NON-NLS-1$
			child.setAttribute("dir", fBuildTempLocation); //$NON-NLS-1$
			target.appendChild(child);
			root.appendChild(target);

			if (hasAntErrors()) {
				target = doc.createElement("target"); //$NON-NLS-1$
				target.setAttribute("name", "zip.logs"); //$NON-NLS-1$ //$NON-NLS-2$
				child = doc.createElement("zip"); //$NON-NLS-1$
				child.setAttribute("zipfile", fInfo.destinationDirectory + logName(null)); //$NON-NLS-1$
				child.setAttribute("basedir", fBuildTempLocation + "/pde.logs"); //$NON-NLS-1$ //$NON-NLS-2$
				target.appendChild(child);
				root.appendChild(target);
			}
			XMLPrintHandler.writeFile(doc, scriptFile);

			String[] targets = hasAntErrors() ? new String[] {"zip.logs", "clean"} //$NON-NLS-1$ //$NON-NLS-2$
					: new String[] {"clean"}; //$NON-NLS-1$
			AntRunner runner = new AntRunner();
			runner.setBuildFileLocation(scriptFile.getAbsolutePath());
			runner.setExecutionTargets(targets);
			runner.run(subMonitor.split(1));
		} catch (FactoryConfigurationError e) {
		} catch (ParserConfigurationException e) {
		} catch (CoreException e) {
		} catch (IOException e) {
		} finally {
			if (scriptFile != null && scriptFile.exists()) {
				scriptFile.delete();
			}
		}
	}

	protected File createScriptFile(String filename) throws IOException {
		String path = PDECore.getDefault().getStateLocation().toOSString();
		File zip = new File(path, filename);
		if (zip.exists()) {
			zip.delete();
			zip.createNewFile();
		}
		return zip;
	}

	private String logName(String[] config) {
		if (config == null) {
			return "/logs.zip"; //$NON-NLS-1$
		}
		return "/logs." + config[0] + '.' + config[1] + '.' + config[2] + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * This method recurses on the feature list and creates feature.xml and build.properties.
	 *
	 * @param featureID
	 * @param featureLocation
	 * @param featuresExported
	 * @param doc
	 * @param root
	 * @param prop
	 * @throws IOException
	 */
	private void createFeature(String featureID, String featureLocation, Object[] featuresExported, Document doc, Element root, Properties prop) throws IOException {
		try {
			if (doc == null) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				doc = factory.newDocumentBuilder().newDocument();
				root = doc.createElement("feature"); //$NON-NLS-1$
				root.setAttribute("id", featureID); //$NON-NLS-1$
				root.setAttribute("version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
				doc.appendChild(root);

				prop = new Properties();
				prop.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$
				prop.put("individualSourceBundles", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			boolean returnAfterLoop = false;
			for (final Object element : featuresExported) {
				if (element instanceof IFeatureModel) {
					IFeature feature = ((IFeatureModel) element).getFeature();

					if (feature.getIncludedFeatures().length > 0) {
						createFeature(featureID, featureLocation, feature.getIncludedFeatures(), doc, root, prop);
					}

					Element includes = doc.createElement("includes"); //$NON-NLS-1$
					includes.setAttribute("id", feature.getId() + ".source"); //$NON-NLS-1$ //$NON-NLS-2$
					includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
					root.appendChild(includes);

					prop.put("generate.feature@" + feature.getId() + ".source", feature.getId()); //$NON-NLS-1$ //$NON-NLS-2$

					includes = doc.createElement("includes"); //$NON-NLS-1$
					includes.setAttribute("id", feature.getId()); //$NON-NLS-1$
					includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
					root.appendChild(includes);
				} else if (element instanceof IFeatureChild) {
					returnAfterLoop = true;
					IFeature feature = ((FeatureChild) element).getReferencedFeature();
					if (feature != null) {
						if (feature.getIncludedFeatures().length > 0) {
							createFeature(featureID, featureLocation, feature.getIncludedFeatures(), doc, root, prop);
						}

						Element includes = doc.createElement("includes"); //$NON-NLS-1$
						includes.setAttribute("id", feature.getId() + ".source"); //$NON-NLS-1$ //$NON-NLS-2$
						includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
						root.appendChild(includes);

						prop.put("generate.feature@" + feature.getId() + ".source", feature.getId()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			if (returnAfterLoop) {
				return;
			}

			File file = new File(featureLocation);
			if (!file.exists() || !file.isDirectory()) {
				file.mkdirs();
			}

			save(new File(file, ICoreConstants.BUILD_FILENAME_DESCRIPTOR), prop, "Marker File"); //$NON-NLS-1$
			XMLPrintHandler.writeFile(doc, new File(file, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR));
		} catch (DOMException e1) {
		} catch (FactoryConfigurationError e1) {
		} catch (ParserConfigurationException e1) {
		}
	}

	private Dictionary<String, String> getEnvironment(String[] config) {
		Dictionary<String, String> environment = new Hashtable<>(4);
		environment.put("osgi.os", config[0]); //$NON-NLS-1$
		environment.put("osgi.ws", config[1]); //$NON-NLS-1$
		environment.put("osgi.arch", config[2]); //$NON-NLS-1$
		environment.put("osgi.nl", config[3]); //$NON-NLS-1$
		return environment;
	}

	private void setFilterAttributes(Element plugin, String[] config) {
		if (config != GENERIC_CONFIG) {
			plugin.setAttribute("os", config[0]); //$NON-NLS-1$
			plugin.setAttribute("ws", config[1]); //$NON-NLS-1$
			plugin.setAttribute("arch", config[2]); //$NON-NLS-1$
		}
	}

	private BundleDescription getMatchingLauncher(String[] configuration, BundleDescription[] fragments) {
		//return the launcher fragment that matches the given configuration
		Dictionary<String, String> environment = getEnvironment(configuration);
		for (int i = 0; i < fragments.length; i++) {
			if (!isNLFragment(fragments[i]) && shouldAddPlugin(fragments[i], environment)) {
				return fragments[i];
			}
		}
		return null;
	}

	private boolean isNLFragment(BundleDescription fragment) {
		//this assumes a name like org.eclipse.equinox.launcher.<ws>.<os>.<arch>.nl_de
		String symbolicName = fragment.getSymbolicName();
		int idx = symbolicName.lastIndexOf('.');
		return (idx > -1 && symbolicName.regionMatches(true, idx + 1, "nl", 0, 2)); //$NON-NLS-1$
	}

	protected void createFeature(String featureID, String featureLocation, String[][] configurations, boolean includeLauncher) throws IOException {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory()) {
			file.mkdirs();
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("feature"); //$NON-NLS-1$
			root.setAttribute("id", featureID); //$NON-NLS-1$
			root.setAttribute("version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);

			if (includeLauncher) {
				IFeatureModel model = PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature();
				if (model != null) {
					IFeature feature = model.getFeature();
					Element includes = doc.createElement("includes"); //$NON-NLS-1$
					includes.setAttribute("id", feature.getId()); //$NON-NLS-1$
					includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
					root.appendChild(includes);
				} else {
					IPluginModelBase launcherPlugin = PluginRegistry.findModel(IPDEBuildConstants.BUNDLE_EQUINOX_LAUNCHER);
					if (launcherPlugin != null) {
						BundleDescription bundle = launcherPlugin.getBundleDescription();
						if (bundle != null) {
							Element plugin = doc.createElement("plugin"); //$NON-NLS-1$
							plugin.setAttribute("id", bundle.getSymbolicName()); //$NON-NLS-1$
							plugin.setAttribute("version", bundle.getVersion().toString()); //$NON-NLS-1$
							plugin.setAttribute("unpack", "false"); //$NON-NLS-1$ //$NON-NLS-2$
							root.appendChild(plugin);
							BundleDescription[] fragments = bundle.getFragments();
							for (String[] configuration : configurations) {
								BundleDescription launcherFragment = getMatchingLauncher(configuration, fragments);
								if (launcherFragment != null) {
									Element fragment = doc.createElement("plugin"); //$NON-NLS-1$
									fragment.setAttribute("id", launcherFragment.getSymbolicName()); //$NON-NLS-1$
									fragment.setAttribute("version", launcherFragment.getVersion().toString()); //$NON-NLS-1$
									fragment.setAttribute("fragment", "true"); //$NON-NLS-1$ //$NON-NLS-2$
									setFilterAttributes(fragment, configuration);
									root.appendChild(fragment);
								}
							}
						}
					}
				}
			}

			List<IPluginModelBase> workspacePlugins = Arrays.asList(PluginRegistry.getWorkspaceModels());

			for (Object item : fInfo.items) {
				if (item instanceof IFeatureModel) {
					IFeature feature = ((IFeatureModel) item).getFeature();
					Element includes = doc.createElement("includes"); //$NON-NLS-1$
					includes.setAttribute("id", feature.getId()); //$NON-NLS-1$
					includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
					root.appendChild(includes);

					if (fInfo.exportSource && fInfo.exportSourceBundle) {
						includes = doc.createElement("includes"); //$NON-NLS-1$
						includes.setAttribute("id", feature.getId() + ".source"); //$NON-NLS-1$ //$NON-NLS-2$
						includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
						root.appendChild(includes);
					}
				} else {
					BundleDescription bundle = null;
					if (item instanceof IPluginModelBase) {
						bundle = ((IPluginModelBase) item).getBundleDescription();
					}
					if (bundle == null) {
						if (item instanceof BundleDescription) {
							bundle = (BundleDescription) item;
						}
					}
					if (bundle == null) {
						continue;
					}

					List<String[]> configs = new ArrayList<>();
					if (configurations.length > 1) {
						configs.add(GENERIC_CONFIG);
					}
					configs.addAll(Arrays.asList(configurations));

					//when doing multiplatform we need filters set on the plugin elements that need them
					//so check the Bundle's platfrom filter against each config
					for (String[] currentConfig : configs) {
						Dictionary<String, String> environment = getEnvironment(currentConfig);
						if (shouldAddPlugin(bundle, environment)) {
							Element plugin = doc.createElement("plugin"); //$NON-NLS-1$
							plugin.setAttribute("id", bundle.getSymbolicName()); //$NON-NLS-1$
							plugin.setAttribute("version", bundle.getVersion().toString()); //$NON-NLS-1$
							setFilterAttributes(plugin, currentConfig);
							setAdditionalAttributes(plugin, bundle);
							root.appendChild(plugin);

							if (fInfo.exportSource && fInfo.exportSourceBundle) {
								if (workspacePlugins.contains(PluginRegistry.findModel(bundle))) { // Is it a workspace plugin?
									plugin = doc.createElement("plugin"); //$NON-NLS-1$
									plugin.setAttribute("id", bundle.getSymbolicName() + ".source"); //$NON-NLS-1$ //$NON-NLS-2$
									plugin.setAttribute("version", bundle.getVersion().toString()); //$NON-NLS-1$
									setFilterAttributes(plugin, currentConfig);
									setAdditionalAttributes(plugin, bundle);
									root.appendChild(plugin);
								} else // include the .source plugin, if available
								{
									IPluginModelBase model = PluginRegistry.findModel(bundle.getSymbolicName() + ".source"); //$NON-NLS-1$
									if (model != null) {
										bundle = model.getBundleDescription();
										plugin = doc.createElement("plugin"); //$NON-NLS-1$
										plugin.setAttribute("id", bundle.getSymbolicName()); //$NON-NLS-1$
										plugin.setAttribute("version", bundle.getVersion().toString()); //$NON-NLS-1$
										setFilterAttributes(plugin, currentConfig);
										setAdditionalAttributes(plugin, bundle);
										root.appendChild(plugin);
									}
								}
							}
							if (currentConfig == GENERIC_CONFIG) {
								//if the bundle matched the generic configuration, we don't need to generated additional inclusions
								//for the rest of the configs
								break;
							}
						}
					}
				}
			}
			XMLPrintHandler.writeFile(doc, new File(file, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR));
		} catch (DOMException e1) {
		} catch (FactoryConfigurationError e1) {
		} catch (ParserConfigurationException e1) {
		}
	}

	protected void setAdditionalAttributes(Element plugin, BundleDescription bundle) {
	}

	public static void errorFound() {
		fHasErrors = true;
	}

	public boolean hasAntErrors() {
		return fHasErrors;
	}

	protected boolean shouldAddPlugin(BundleDescription bundle, Dictionary<String, String> environment) {
		String filterSpec = bundle.getPlatformFilter();
		try {
			return (filterSpec == null || PDECore.getDefault().getBundleContext().createFilter(filterSpec).match(environment));
		} catch (InvalidSyntaxException e) {
		}
		return false;
	}

	/**
	 * If we are exporting using the compiled classes from the workspace, this method will
	 * start an incremental build and test for build errors.  Returns a status explaining
	 * any errors found or Status.OK_STATUS.
	 * @param monitor progress monitor
	 * @return status explaining build errors or an OK status.
	 * @throws CoreException
	 */
	protected IStatus testBuildWorkspaceBeforeExport(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 50);

		if (fInfo.useWorkspaceCompiledClasses) {
			getWorkspaceExportHelper().buildBeforeExport(fInfo.items, subMonitor.split(45));
			Set<?> errors = getWorkspaceExportHelper().checkForErrors(fInfo.items);
			subMonitor.split(5);
			if (!errors.isEmpty()) {
				return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(PDECoreMessages.FeatureExportOperation_workspaceBuildErrorsFoundDuringExport, errors.toString()));
			}
		}
		return Status.OK_STATUS;

	}

	/**
	 * @return an instance of the WorkspaceExportHelper used to set up exports using class files built in the workspace
	 */
	protected WorkspaceExportHelper getWorkspaceExportHelper() {
		if (fWorkspaceExportHelper == null) {
			fWorkspaceExportHelper = new WorkspaceExportHelper();
		}
		return fWorkspaceExportHelper;
	}

}
