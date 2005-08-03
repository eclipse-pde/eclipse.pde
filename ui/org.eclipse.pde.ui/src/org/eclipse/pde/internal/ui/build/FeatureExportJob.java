/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FeatureExportJob extends Job implements IPreferenceConstants {
	
	// write to the ant build listener log
	private static boolean fHasErrors;

	// Location where the build takes place
	protected String fBuildTempLocation;
	private String fDevProperties;

	protected HashMap fAntBuildProperties;

	protected static String FEATURE_POST_PROCESSING = "features.postProcessingSteps.properties"; //$NON-NLS-1$
	protected static String PLUGIN_POST_PROCESSING = "plugins.postProcessingSteps.properties"; //$NON-NLS-1$
	
	private State fStateCopy;

	protected FeatureExportInfo fInfo;

	private static int fNumberErrors;

	class SchedulingRule implements ISchedulingRule {

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
		 */
		public boolean contains(ISchedulingRule rule) {
			return rule instanceof SchedulingRule;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
		 */
		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof SchedulingRule;
		}
	}

	public FeatureExportJob(FeatureExportInfo info) {
		super(PDEUIMessages.FeatureExportJob_name); 
		fInfo = info;
		fBuildTempLocation = PDEPlugin.getDefault().getStateLocation().append("temp").toString(); //$NON-NLS-1$
		setRule(new SchedulingRule());
	}

	protected IStatus run(IProgressMonitor monitor) {
		String errorMessage = null;
		try {
			fHasErrors = false;
			fNumberErrors = 0;
			doExports(monitor);
		} catch (final CoreException e) {
			final Display display = getStandardDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					ErrorDialog.openError(display.getActiveShell(), PDEUIMessages.FeatureExportJob_error, PDEUIMessages.FeatureExportJob_problems, e.getStatus()); // 
					done(new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null)); //$NON-NLS-1$
				}
			});
			return Job.ASYNC_FINISH;
		} catch (InvocationTargetException e) {
			String message = e.getTargetException().getMessage();
			if (message != null && message.length() > 0) {
				errorMessage = e.getTargetException().getMessage();
			}
		}
		if (errorMessage == null && fNumberErrors > 0) {
			errorMessage = getLogFoundMessage();
		}

		if (errorMessage != null) {
			final String em = errorMessage;
			getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					asyncNotifyExportException(em);
				}
			});
			return Job.ASYNC_FINISH;
		}
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
	
	public static void errorFound() {
		fHasErrors = true;
		fNumberErrors += 1;
	}

	protected void doExports(IProgressMonitor monitor) throws InvocationTargetException, CoreException {
		createDestination();
		String[][] configurations = fInfo.targets;
		if (configurations == null)
			configurations = new String[][] { null };
		
		monitor.beginTask("", configurations.length * fInfo.items.length * 10); //$NON-NLS-1$
		for (int i = 0; i < configurations.length; i++) {
			for (int j = 0; j < fInfo.items.length; j++) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				try {
					doExport((IFeatureModel) fInfo.items[j], configurations[i], new SubProgressMonitor(monitor, 9));
				} finally {
					cleanup(configurations[i], new SubProgressMonitor(monitor, 1));
				}
			}
		}
		monitor.done();
	}

	private void doExport(IFeatureModel model, String os, String ws, String arch, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		try {
			String location = model.getInstallLocation();
			if (fInfo.useJarFormat) {
				createPostProcessingFile(new File(location, FEATURE_POST_PROCESSING));
				createPostProcessingFile(new File(location, PLUGIN_POST_PROCESSING));
			}
			IFeature feature = model.getFeature();
			doExport(feature.getId(), feature.getVersion(), location, os, ws, arch, monitor);
		} finally {
			deleteBuildFiles(model);
		}
	}
	
	protected void createPostProcessingFile(File file) {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			Properties prop = new Properties();
			prop.put("*", "updateJar"); //$NON-NLS-1$ //$NON-NLS-2$
			prop.store(stream, ""); //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
			}			
		}
	}

	private String getOS(IFeature feature) {
		String os = feature.getOS();
		if (os == null || os.trim().length() == 0 || os.indexOf(',') != -1 || os.equals("*")) //$NON-NLS-1$
			return TargetPlatform.getOS();
		return os;
	}

	private String getWS(IFeature feature) {
		String ws = feature.getWS();
		if (ws == null || ws.trim().length() == 0 || ws.indexOf(',') != -1 || ws.equals("*")) //$NON-NLS-1$
			return TargetPlatform.getWS();
		return ws;
	}

	private String getOSArch(IFeature feature) {
		String arch = feature.getArch();
		if (arch == null || arch.trim().length() == 0 || arch.indexOf(',') != -1 || arch.equals("*")) //$NON-NLS-1$
			return TargetPlatform.getOSArch();
		return arch;
	}

	private void createDestination() throws InvocationTargetException {
		File file = new File(fInfo.destinationDirectory);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs())
				throw new InvocationTargetException(new Exception(PDEUIMessages.ExportWizard_badDirectory)); 
		}
	}

	private void createDestination(String os, String ws, String arch) throws InvocationTargetException {
		if (!fInfo.toDirectory)
			return;
		File file = new File(fInfo.destinationDirectory, os + '.' + ws + '.' + arch);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs())
				throw new InvocationTargetException(new Exception(PDEUIMessages.ExportWizard_badDirectory)); 
		}
	}

	protected void doExport(IFeatureModel model, String[] config, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		if (config == null) {
			IFeature feature = model.getFeature();
			doExport(model, getOS(feature), getWS(feature), getOSArch(feature), monitor);
		} else {
			createDestination(config[0], config[1], config[2]);
			doExport(model, config[0], config[1], config[2], monitor);
		}
	}
		
	protected void doExport(String featureID, String version, String featureLocation, String os, String ws, String arch, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		fHasErrors = false;
		monitor.beginTask("", 9); //$NON-NLS-1$
		monitor.setTaskName(PDEUIMessages.FeatureExportJob_taskName); 
		try {
			HashMap properties = createAntBuildProperties(os, ws, arch);
			BuildScriptGenerator generator = new BuildScriptGenerator();
			setupGenerator(generator, featureID, version, os, ws, arch, featureLocation);
			generator.generate();
			monitor.worked(1);
			runScript(getBuildScriptName(featureLocation), getBuildExecutionTargets(), properties, new SubProgressMonitor(monitor, 2));
			runScript(getAssemblyScriptName(featureID, os, ws, arch, featureLocation), new String[] {"main"}, //$NON-NLS-1$
					properties, new SubProgressMonitor(monitor, 2));
			runScript(getPackagerScriptName(featureID, os, ws, arch, featureLocation), null, properties, new SubProgressMonitor(monitor, 2));
			properties.put("destination.temp.folder", fBuildTempLocation + "/pde.logs"); //$NON-NLS-1$ //$NON-NLS-2$
			runScript(getBuildScriptName(featureLocation), new String[] {"gather.logs"}, properties, new SubProgressMonitor(monitor, 2)); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}

	protected String getPackagerScriptName(String featureID, String os, String ws, String arch, String featureLocation) {
		return featureLocation + IPath.SEPARATOR + "package." //$NON-NLS-1$
				+ featureID + "." + os + "." //$NON-NLS-1$ //$NON-NLS-2$
				+ ws + "." + arch //$NON-NLS-1$
				+ ".xml"; //$NON-NLS-1$
	}

	protected HashMap createAntBuildProperties(String os, String ws, String arch) {
		if (fAntBuildProperties == null) {
			fAntBuildProperties = new HashMap(15);
			if (fInfo.signingInfo != null) {
				fAntBuildProperties.put("sign.alias", fInfo.signingInfo[0]); //$NON-NLS-1$
				fAntBuildProperties.put("sign.keystore", fInfo.signingInfo[1]); //$NON-NLS-1$
				fAntBuildProperties.put("sign.storepass", fInfo.signingInfo[2]); //$NON-NLS-1$
			}
			if (fInfo.jnlpInfo != null) {
				fAntBuildProperties.put("jnlp.codebase", fInfo.jnlpInfo[0]); //$NON-NLS-1$
				fAntBuildProperties.put("jnlp.j2se", fInfo.jnlpInfo[1]); //$NON-NLS-1$
			}

			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_TEMP, fBuildTempLocation + "/destination"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_FEATURE_TEMP_FOLDER, fBuildTempLocation + "/destination"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
			fAntBuildProperties.put("eclipse.running", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_OS, os);
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_WS, ws);
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_ARCH, arch);
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BASE_NL, TargetPlatform.getNL());
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BOOTCLASSPATH, BaseBuildAction.getBootClasspath());
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_FAIL_ON_ERROR, "false"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_DEBUG_INFO, "on"); //$NON-NLS-1$ 
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_VERBOSE, "true"); //$NON-NLS-1$

			Preferences pref = JavaCore.getPlugin().getPluginPreferences();
			String source = fInfo.javacSource == null ?  pref.getString(JavaCore.COMPILER_SOURCE) : fInfo.javacSource; 
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, source);
			String target = fInfo.javacTarget == null ? pref.getString(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM) : fInfo.javacTarget;
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, target);

			// for the assembler...
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_DIRECTORY, fBuildTempLocation + "/assemblyLocation"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_LABEL, "."); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, "."); //$NON-NLS-1$
			String prefix = Platform.getOS().equals("macosx") ? "." : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, prefix);

			if (!fInfo.toDirectory) {
				String filename = fInfo.zipFileName;
				if (fInfo.targets != null) {
					int i = filename.lastIndexOf('.');
					filename = filename.substring(0, i) + '.' + os + '.' + ws + '.' + arch + filename.substring(i);
				}
				fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_FULLPATH, fInfo.destinationDirectory + File.separator + filename);
			} else {
				String dir = fInfo.destinationDirectory;
				if (fInfo.targets != null)
					dir += File.separatorChar + os + '.' + ws + '.' + arch;
				fAntBuildProperties.put(IXMLConstants.PROPERTY_ASSEMBLY_TMP, dir);
			}
			fAntBuildProperties.put(IXMLConstants.PROPERTY_TAR_ARGS, ""); //$NON-NLS-1$
		}
		return fAntBuildProperties;
	}

	protected void setupGenerator(BuildScriptGenerator generator, String featureID, String versionId, String os, String ws, String arch, String featureLocation) throws CoreException {
		generator.setBuildingOSGi(PDECore.getDefault().getModelManager().isOSGiRuntime());
		generator.setChildren(true);
		generator.setWorkingDirectory(featureLocation);
		generator.setDevEntries(getDevProperties());
		generator.setElements(new String[] {"feature@" + featureID + (versionId == null ? "" : ":" + versionId)}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		generator.setPluginPath(getPaths());
		generator.setReportResolutionErrors(false);
		generator.setIgnoreMissingPropertiesFile(true);
		generator.setSignJars(fInfo.signingInfo != null);
		generator.setGenerateJnlp(fInfo.jnlpInfo != null);
		String config = os + ',' + ws + ',' + arch;
		AbstractScriptGenerator.setConfigInfo(config);  //This needs to be set before we set the format
		String format;
		if (fInfo.toDirectory)
			format = config + '-' + IXMLConstants.FORMAT_FOLDER;
		else
			format = config + '-' + IXMLConstants.FORMAT_ANTZIP;
		generator.setArchivesFormat(format);
		generator.setPDEState(getState(os, ws, arch));
		generator.setNextId(TargetPlatform.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatform.getBundleClasspaths(TargetPlatform.getPDEState()));
		AbstractScriptGenerator.setForceUpdateJar(false);
		AbstractScriptGenerator.setEmbeddedSource(fInfo.exportSource);		
	}
	
	protected State getState(String os, String ws, String arch) {
		State main = TargetPlatform.getState();
		if (os.equals(TargetPlatform.getOS()) 
				&& ws.equals(TargetPlatform.getWS())
				&& arch.equals(TargetPlatform.getOSArch())) {
			return main;
		} 			
		if (fStateCopy == null) {
			fStateCopy = main.getFactory().createState(main);
			fStateCopy.setResolver(Platform.getPlatformAdmin().getResolver());
			fStateCopy.setPlatformProperties(main.getPlatformProperties()[0]);
		}
			
		Dictionary properties = fStateCopy.getPlatformProperties()[0];
		properties.put("osgi.os", os); //$NON-NLS-1$
		properties.put("osgi.ws", ws); //$NON-NLS-1$
		properties.put("osgi.arch", arch); //$NON-NLS-1$
		fStateCopy.setPlatformProperties(properties);
		fStateCopy.resolve(false);
		return fStateCopy;
	}
	
	private String getDevProperties() {
		if (fDevProperties == null) {
			fDevProperties = ClasspathHelper.getDevEntriesProperties(fBuildTempLocation + "/dev.properties", false); //$NON-NLS-1$
		}
		return fDevProperties;
	}

	protected void runScript(String location, String[] targets, Map properties, IProgressMonitor monitor) throws InvocationTargetException, CoreException {
		AntRunner runner = new AntRunner();
		runner.addUserProperties(properties);
		runner.setAntHome(location);
		runner.setBuildFileLocation(location);
		runner.addBuildListener("org.eclipse.pde.internal.ui.ant.ExportBuildListener"); //$NON-NLS-1$
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

	private String getBuildScriptName(String featureLocation) {
		return featureLocation + IPath.SEPARATOR + "build.xml"; //$NON-NLS-1$
	}

	protected String getAssemblyScriptName(String featureID, String os, String ws, String arch, String featureLocation) {
		return featureLocation + IPath.SEPARATOR + "assemble." //$NON-NLS-1$
				+ featureID + "." + os + "." //$NON-NLS-1$ //$NON-NLS-2$
				+ ws + "." + arch //$NON-NLS-1$
				+ ".xml"; //$NON-NLS-1$
	}

	private String[] getBuildExecutionTargets() {
		if (fInfo.exportSource)
			return new String[] {"build.jars", "build.sources"}; //$NON-NLS-1$ //$NON-NLS-2$ 
		return new String[] {"build.jars"}; //$NON-NLS-1$ 
	}

	public void deleteBuildFiles(Object object) throws CoreException {
		IModel model = null;
		if (object instanceof BundleDescription) {
			model = PDECore.getDefault().getModelManager().findModel((BundleDescription)object);
		} else if (object instanceof IModel){
			model = (IModel)object;
		}
		
		if (model == null)
			return;

		if (model.getUnderlyingResource() != null && !isCustomBuild(model)) {
			String directory = (model instanceof IFeatureModel) ? ((IFeatureModel) model).getInstallLocation() : ((IPluginModelBase) model).getInstallLocation();
			File dir = new File(directory);
			File[] children = dir.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					if (!children[i].isDirectory()) {
						String filename = children[i].getName();
						if (filename.equals("build.xml") || //$NON-NLS-1$
								(filename.startsWith("assemble.") && filename.endsWith(".xml")) //$NON-NLS-1$ //$NON-NLS-2$
								|| (filename.startsWith("package.") && filename.endsWith(".xml")) //$NON-NLS-1$ //$NON-NLS-2$
								|| filename.equals(FEATURE_POST_PROCESSING)
								|| filename.equals(PLUGIN_POST_PROCESSING)) {
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
			for (int i = 0; i < children.length; i++) {
				IFeature ref = ((FeatureChild) children[i]).getReferencedFeature();
				if (ref != null) {
					deleteBuildFiles(ref.getModel());
				}
			}

			IFeaturePlugin[] plugins = feature.getPlugins();
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			for (int i = 0; i < plugins.length; i++) {
				ModelEntry entry = manager.findEntry(plugins[i].getId());
				if (entry != null) {
					deleteBuildFiles(entry.getActiveModel());
				}
			}
		}
	}

	protected boolean isCustomBuild(IModel model) throws CoreException {
		IBuildModel buildModel = null;
		IFile buildFile = model.getUnderlyingResource().getProject().getFile("build.properties"); //$NON-NLS-1$
		if (buildFile.exists()) {
			buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
		}
		if (buildModel != null) {
			IBuild build = buildModel.getBuild();
			if (build == null)
				return false;
			IBuildEntry entry = build.getEntry("custom"); //$NON-NLS-1$
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].equals("true")) //$NON-NLS-1$
						return true;
				}
			}
		}
		return false;
	}

	protected String[] getPaths() {
		return TargetPlatform.getFeaturePaths();
	}
	
	protected void cleanup(String[] config, IProgressMonitor monitor) {
        monitor.beginTask("", 2); //$NON-NLS-1$
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
							
			if (fHasErrors) {
				target = doc.createElement("target"); //$NON-NLS-1$
				target.setAttribute("name", "zip.logs"); //$NON-NLS-1$ //$NON-NLS-2$
				child = doc.createElement("zip"); //$NON-NLS-1$
				child.setAttribute("zipfile", fInfo.destinationDirectory + logName(config)); //$NON-NLS-1$
				child.setAttribute("basedir", fBuildTempLocation + "/pde.logs"); //$NON-NLS-1$ //$NON-NLS-2$
				target.appendChild(child);
				root.appendChild(target);
			}	
			XMLPrintHandler.writeFile(doc, scriptFile);
			
			String[] targets = fHasErrors 
						? new String[] {"zip.logs", "clean"}  //$NON-NLS-1$ //$NON-NLS-2$
						: new String[] {"clean"}; //$NON-NLS-1$
			AntRunner runner = new AntRunner();
			runner.setBuildFileLocation(scriptFile.getAbsolutePath());
			runner.setExecutionTargets(targets);
			runner.run(new SubProgressMonitor(monitor, 1));
		} catch (FactoryConfigurationError e) {
		} catch (ParserConfigurationException e) {
		} catch (CoreException e) {
		} catch (IOException e) {
		} finally {
			if (scriptFile != null && scriptFile.exists())
				scriptFile.delete();
			monitor.done();
		}
	}

	protected File createScriptFile(String filename) throws IOException {
		String path = PDEPlugin.getDefault().getStateLocation().toOSString();
		File zip = new File(path, filename); 
		if (zip.exists()) {
			zip.delete();
			zip.createNewFile();
		}
		return zip;
	}

	private String logName(String[] config) {
		if (config == null)
			return "/logs.zip"; //$NON-NLS-1$
		return "/logs." + config[0] + '.' + config[1] + '.' + config[2] + ".zip";  //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns the standard display to be used. The method first checks, if the
	 * thread calling this method has an associated disaply. If so, this display
	 * is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display;
		display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	private void asyncNotifyExportException(String errorMessage) {
		getStandardDisplay().beep();
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureExportJob_error, errorMessage); 
		done(new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null)); //$NON-NLS-1$
	}

	protected String getLogFoundMessage() {
		return NLS.bind(PDEUIMessages.ExportJob_error_message, fInfo.destinationDirectory); 
	}
    
    protected void createFeature(String featureID, String featureLocation, String[] config, boolean includeLauncher) throws IOException {
        File file = new File(featureLocation);
        if (!file.exists() || !file.isDirectory())
            file.mkdirs();
        
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("feature"); //$NON-NLS-1$
			root.setAttribute("id", featureID); //$NON-NLS-1$
			root.setAttribute("version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);
			
			if (includeLauncher) {
				IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModel("org.eclipse.platform.launchers"); //$NON-NLS-1$
				if (model != null) {
					IFeature feature = model.getFeature();
					Element includes = doc.createElement("includes"); //$NON-NLS-1$
					includes.setAttribute("id", feature.getId()); //$NON-NLS-1$
					includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
					root.appendChild(includes);
				}
			}
            Dictionary environment = new Hashtable(4);
            environment.put("osgi.os", config[0]); //$NON-NLS-1$
            environment.put("osgi.ws", config[1]); //$NON-NLS-1$
            environment.put("osgi.arch", config[2]); //$NON-NLS-1$
            environment.put("osgi.nl", config[3]); //$NON-NLS-1$

            BundleContext context = PDEPlugin.getDefault().getBundleContext();
            for (int i = 0; i < fInfo.items.length; i++) {
            	if (fInfo.items[i] instanceof IFeatureModel) {
                    IFeature feature = ((IFeatureModel) fInfo.items[i]).getFeature();
  					Element includes = doc.createElement("includes"); //$NON-NLS-1$
 					includes.setAttribute("id", feature.getId()); //$NON-NLS-1$
 					includes.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
 					root.appendChild(includes);
                } else {
	            	BundleDescription bundle = null;
	                if (fInfo.items[i] instanceof IPluginModelBase) {
	                	bundle = ((IPluginModelBase)fInfo.items[i]).getBundleDescription();
	                }
	                if (bundle == null) {
	                	if (fInfo.items[i] instanceof BundleDescription)
	                		bundle = (BundleDescription)fInfo.items[i];
	                }
	                if (bundle == null)
	                	continue;
                    try {
                        String filterSpec = bundle.getPlatformFilter();
                        if (filterSpec == null|| context.createFilter(filterSpec).match(environment)) {
                        	Element plugin = doc.createElement("plugin"); //$NON-NLS-1$
                        	plugin.setAttribute("id", bundle.getSymbolicName()); //$NON-NLS-1$
                            plugin.setAttribute("version", "0.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
                            if (!fInfo.useJarFormat) {
                                plugin.setAttribute("unpack", Boolean.toString(isUnpack(bundle))); //$NON-NLS-1$
                             }
                            root.appendChild(plugin);
                         }
                    } catch (InvalidSyntaxException e) {
                    }
                }
            }
            XMLPrintHandler.writeFile(doc, new File(file, "feature.xml")); //$NON-NLS-1$
 		} catch (DOMException e1) {
		} catch (FactoryConfigurationError e1) {
		} catch (ParserConfigurationException e1) {
		}      	
    }
    
    protected boolean isUnpack(BundleDescription bundle){
    	return CoreUtility.guessUnpack(bundle);
    }
}
