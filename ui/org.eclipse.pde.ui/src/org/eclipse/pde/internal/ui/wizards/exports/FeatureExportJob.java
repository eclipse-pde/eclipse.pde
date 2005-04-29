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
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.eclipse.ant.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.build.*;
import org.eclipse.swt.widgets.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class FeatureExportJob extends Job implements IPreferenceConstants {

	// write to the ant build listener log
	protected static PrintWriter writer;
	protected static File logFile;

	// Export options specified in the wizard
	protected boolean fExportToDirectory;
	protected boolean fUseJarFormat;
	protected boolean fExportSource;
	protected String fDestinationDirectory;
	protected String fZipFilename;
	protected Object[] fItems;

	// Location where the build takes place
	protected String fBuildTempLocation;
	private String fDevProperties;

	protected HashMap fAntBuildProperties;
	private String[] fSigningInfo = null;
	private String[] fJnlpInfo = null;

	protected static String FEATURE_POST_PROCESSING = "features.postProcessingSteps.properties"; //$NON-NLS-1$
	protected static String PLUGIN_POST_PROCESSING = "plugins.postProcessingSteps.properties"; //$NON-NLS-1$
	protected String[][] fTargets;

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

	public FeatureExportJob(String name) {
		super(name);
		fBuildTempLocation = PDEPlugin.getDefault().getStateLocation().append("temp").toString(); //$NON-NLS-1$
		setRule(new SchedulingRule());
	}
	
	public FeatureExportJob(boolean toDirectory, boolean useJarFormat, boolean exportSource, String destination, String zipFileName, Object[] items, String[] signingInfo, String[] jnlpInfo, String[][] targets) {
		super(PDEUIMessages.FeatureExportJob_name); //$NON-NLS-1$
		fExportToDirectory = toDirectory;
		fUseJarFormat = useJarFormat;
		fExportSource = exportSource;
		fDestinationDirectory = destination;
		fZipFilename = zipFileName;
		fItems = items;
		fSigningInfo = signingInfo;
		fJnlpInfo = jnlpInfo;
		fTargets = targets;
		// TODO remove when there is UI to set ftargets
//		if (ftargets == null)
//			ftargets = new String[][] { { "linux", "gtk", "x86", ""} , {"win32", "win32", "x86", ""} };
		fBuildTempLocation = PDEPlugin.getDefault().getStateLocation().append("temp").toString(); //$NON-NLS-1$
		setRule(new SchedulingRule());
	}

	public FeatureExportJob(boolean toDirectory, boolean useJarFormat, boolean exportSource, String destination, String zipFileName, Object[] items) {
		this(toDirectory, useJarFormat, exportSource, destination, zipFileName, items, null, null, null);
	}

	protected IStatus run(IProgressMonitor monitor) {
		String errorMessage = null;
		try {
			createLogWriter();
			doExports(monitor);
		} catch (final CoreException e) {
			final Display display = getStandardDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					ErrorDialog.openError(display.getActiveShell(), PDEUIMessages.FeatureExportJob_error, PDEUIMessages.FeatureExportJob_problems, e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
					done(new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null)); //$NON-NLS-1$
				}
			});
			return Job.ASYNC_FINISH;
		} catch (InvocationTargetException e) {
			String message = e.getTargetException().getMessage();
			if (message != null && message.length() > 0) {
				errorMessage = e.getTargetException().getMessage();
			}
		} finally {
			if (writer != null)
				writer.close();
		}
		if (errorMessage == null && logFile != null && logFile.exists() && logFile.length() > 0) {
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

	protected void doExports(IProgressMonitor monitor) throws InvocationTargetException, CoreException {
		createDestination();
		monitor.beginTask("", fItems.length + 1); //$NON-NLS-1$
		String[][] configurations = fTargets;
		if (configurations == null)
			configurations = new String[][] { null };
		
		for (int i = 0; i < configurations.length; i++) {
			try {
				for (int j = 0; j < fItems.length; j++) {
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					doExport((IFeatureModel) fItems[j], configurations[i], monitor);
				}
			} finally {
				cleanup(configurations[i], new SubProgressMonitor(monitor, 1));
				monitor.done();
			}
		}
	}

	private void doExport(IFeatureModel model, String os, String ws, String arch, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		try {
			String location = model.getInstallLocation();
			if (fUseJarFormat) {
				createPostProcessingFile(new File(location, FEATURE_POST_PROCESSING));
				createPostProcessingFile(new File(location, PLUGIN_POST_PROCESSING));
			}
			IFeature feature = model.getFeature();
			doExport(feature.getId(), feature.getVersion(), location, os, ws, arch, new SubProgressMonitor(monitor, 1));
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
		File file = new File(fDestinationDirectory);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs())
				throw new InvocationTargetException(new Exception(PDEUIMessages.ExportWizard_badDirectory)); //$NON-NLS-1$
		}
	}

	private void createDestination(String os, String ws, String arch) throws InvocationTargetException {
		if (!fExportToDirectory)
			return;
		File file = new File(fDestinationDirectory, os + '.' + ws + '.' + arch);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs())
				throw new InvocationTargetException(new Exception(PDEUIMessages.ExportWizard_badDirectory)); //$NON-NLS-1$
		}
	}

	protected void doExport(IFeatureModel model, String[] config, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		// TODO progress monitoring
		if (config == null) {
			IFeature feature = model.getFeature();
			doExport(model, getOS(feature), getWS(feature), getOSArch(feature), monitor);
		} else {
			createDestination(config[0], config[1], config[2]);
			doExport(model, config[0], config[1], config[2], monitor);
		}
	}
		
	protected void doExport(String featureID, String version, String featureLocation, String os, String ws, String arch, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		monitor.beginTask("", 5); //$NON-NLS-1$
		monitor.setTaskName(PDEUIMessages.FeatureExportJob_taskName); //$NON-NLS-1$
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
			if (fSigningInfo != null) {
				fAntBuildProperties.put("sign.alias", fSigningInfo[0]); //$NON-NLS-1$
				fAntBuildProperties.put("sign.keystore", fSigningInfo[1]); //$NON-NLS-1$
				fAntBuildProperties.put("sign.storepass", fSigningInfo[2]); //$NON-NLS-1$
			}
			if (fJnlpInfo != null) {
				fAntBuildProperties.put("jnlp.codebase", fJnlpInfo[0]); //$NON-NLS-1$
				fAntBuildProperties.put("jnlp.j2se", fJnlpInfo[1]); //$NON-NLS-1$
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
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_DEBUG_INFO, "on"); //$NON-NLS-1$ //$NON-NLS-2$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_VERBOSE, "true"); //$NON-NLS-1$

			Preferences pref = JavaCore.getPlugin().getPluginPreferences();
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, pref.getString(JavaCore.COMPILER_SOURCE));
			fAntBuildProperties.put(IXMLConstants.PROPERTY_JAVAC_TARGET, pref.getString(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));

			// for the assembler...
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_DIRECTORY, fBuildTempLocation + "/assemblyLocation"); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_BUILD_LABEL, "."); //$NON-NLS-1$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, "."); //$NON-NLS-1$
			String prefix = Platform.getOS().equals("macosx") ? "." : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, prefix);

			if (!fExportToDirectory) {
				String filename = fZipFilename;
				if (fTargets != null) {
					int i = filename.lastIndexOf('.');
					filename = filename.substring(0, i) + '.' + os + '.' + ws + '.' + arch + filename.substring(i);
				}
				fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_FULLPATH, fDestinationDirectory + File.separator + filename);
			} else {
				String dir = fDestinationDirectory;
				if (fTargets != null)
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
		generator.setSignJars(fSigningInfo != null);
		generator.setGenerateJnlp(fJnlpInfo != null);
		String config = os + ',' + ws + ',' + arch;
		AbstractScriptGenerator.setConfigInfo(config);  //This needs to be set before we set the format
		String format;
		if (fExportToDirectory)
			format = config + '-' + IXMLConstants.FORMAT_FOLDER;
		else
			format = config + '-' + IXMLConstants.FORMAT_ANTZIP;
		generator.setArchivesFormat(format);
		generator.setPDEState(TargetPlatform.getState());
		generator.setNextId(TargetPlatform.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatform.getBundleClasspaths(TargetPlatform.getPDEState()));
		AbstractScriptGenerator.setForceUpdateJar(false);
		AbstractScriptGenerator.setEmbeddedSource(fExportSource);		
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
		if (fSigningInfo != null) {
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
		if (fExportSource)
			return new String[] {"build.jars", "build.sources"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new String[] {"build.jars"}; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void deleteBuildFiles(IModel model) throws CoreException {
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

	private static void createLogWriter() {
		try {
			String path = PDEPlugin.getDefault().getStateLocation().toOSString();
			logFile = new File(path, "exportLog.txt"); //$NON-NLS-1$
			if (logFile.exists()) {
				logFile.delete();
				logFile.createNewFile();
			}
			writer = new PrintWriter(new FileWriter(logFile), true);
		} catch (IOException e) {
		}
	}

	public static PrintWriter getWriter() {
		if (writer == null)
			createLogWriter();
		return writer;
	}

	protected void cleanup(String[] config, IProgressMonitor monitor) {
        monitor.beginTask("", 2);
        // clear out some cached values that depend on the configuration being built.
        fDevProperties = null;
        fAntBuildProperties = null;

		File scriptFile = null;
		try {
			scriptFile = createScriptFile();
			writer = new PrintWriter(new FileWriter(scriptFile), true);
			generateHeader(writer);
			generateDeleteZipTarget(writer, config);
			generateCleanTarget(writer);
			boolean errors = generateZipLogsTarget(writer, config);
			generateClosingTag(writer);
			writer.close();

			ArrayList targets = new ArrayList();
			targets.add("deleteZip"); //$NON-NLS-1$
			if (errors)
				targets.add("zip.logs"); //$NON-NLS-1$
			targets.add("clean"); //$NON-NLS-1$
			AntRunner runner = new AntRunner();
			runner.setBuildFileLocation(scriptFile.getAbsolutePath());
			runner.setExecutionTargets((String[]) targets.toArray(new String[targets.size()]));
			runner.run(new SubProgressMonitor(monitor, 1));
		} catch (IOException e) {
		} catch (CoreException e) {
		} finally {
			if (scriptFile != null && scriptFile.exists())
				scriptFile.delete();
            monitor.done();
		}
	}

	private File createScriptFile() throws IOException {
		String path = PDEPlugin.getDefault().getStateLocation().toOSString();
		File zip = new File(path, "zip.xml"); //$NON-NLS-1$
		if (zip.exists()) {
			zip.delete();
			zip.createNewFile();
		}
		return zip;
	}

	private void generateHeader(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.println("<project name=\"temp\" default=\"clean\" basedir=\".\">"); //$NON-NLS-1$
	}

	private void generateCleanTarget(PrintWriter writer) {
		writer.println("<target name=\"clean\">"); //$NON-NLS-1$
		writer.println("<delete dir=\"" + fBuildTempLocation + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("</target>"); //$NON-NLS-1$
	}

	private String logName(String[] config) {
		if (config == null)
			return "/logs.zip";
		return "/logs." + config[0] + '.' + config[1] + '.' + config[2] + ".zip"; 
	}
	private void generateDeleteZipTarget(PrintWriter writer, String[] config) {
		writer.println("<target name=\"deleteZip\">"); //$NON-NLS-1$
		writer.println("<delete file=\"" + fDestinationDirectory + logName(config) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("</target>"); //$NON-NLS-1$
	}

	private boolean generateZipLogsTarget(PrintWriter writer, String[] config) {
		if (logFile != null && logFile.exists() && logFile.length() > 0) {
			writer.println("<target name=\"zip.logs\">"); //$NON-NLS-1$
			writer.println("<zip zipfile=\"" + fDestinationDirectory + logName(config) + "\" basedir=\"" + fBuildTempLocation + "/pde.logs\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			writer.println("</target>"); //$NON-NLS-1$
			return true;
		}
		return false;
	}

	private void generateClosingTag(PrintWriter writer) {
		writer.println("</project>"); //$NON-NLS-1$
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
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureExportJob_error, errorMessage); //$NON-NLS-1$
		done(new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null)); //$NON-NLS-1$
	}

	protected String getLogFoundMessage() {
		return NLS.bind(PDEUIMessages.ExportJob_error_message, fDestinationDirectory + File.separator + "logs.zip"); //$NON-NLS-1$ //$NON-NLS-2$
	}
    
    protected void createFeature(String featureID, String featureLocation, String[] config, boolean includeLauncher) throws IOException {
        File file = new File(featureLocation);
        if (!file.exists() || !file.isDirectory())
            file.mkdirs();
        
        File featureXML = new File(file, "feature.xml"); //$NON-NLS-1$
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(featureXML), "UTF-8"), true); //$NON-NLS-1$
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
        writer.println("<feature id=\"" + featureID + "\" version=\"1.0\">"); //$NON-NLS-1$ //$NON-NLS-2$

		if (includeLauncher) {
			IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
			for (int i = 0; i < models.length; i++) {
				IFeature feature = models[i].getFeature();
				if ("org.eclipse.platform.launchers".equals(feature.getId()))
			        writer.println("<includes id=\"" + feature.getId() + "\" version=\"" + feature.getVersion() + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

        Dictionary environment = new Hashtable(4);
        environment.put("osgi.os", config[0]);
        environment.put("osgi.ws", config[1]);
        environment.put("osgi.arch", config[2]);
        environment.put("osgi.nl", config[3]);
        	
        BundleContext context = PDEPlugin.getDefault().getBundleContext();
        for (int i = 0; i < fItems.length; i++) {
            if (fItems[i] instanceof IPluginModelBase) {
                IPluginModelBase model = (IPluginModelBase) fItems[i];
                try {
                    String filterSpec = model.getBundleDescription().getPlatformFilter();
                    if (filterSpec == null|| context.createFilter(filterSpec).match(environment)) {
                        writer.print("<plugin id=\"");
                        writer.print(model.getPluginBase().getId());
                        writer.print("\" version=\"0.0.0\"");
                        if (!fUseJarFormat) {
                            writer.print(" unpack=\"");
                            writer.print(Boolean.toString(doUnpack(model)));
                            writer.print("\"");
                        }
                        writer.println("/>");
                    }
                } catch (InvalidSyntaxException e) {
                }
            } else if (fItems[i] instanceof IFeatureModel) {
                IFeature feature = ((IFeatureModel) fItems[i]).getFeature();
                writer.println("<includes id=\"" + feature.getId() + "\" version=\"" + feature.getVersion() + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        writer.println("</feature>"); //$NON-NLS-1$
        writer.close();
    }

    private boolean doUnpack(IPluginModelBase model) {
        if (new File(model.getInstallLocation()).isFile())
            return false;
        
        IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
        if (libraries.length == 0 && PDECore.getDefault().getModelManager().isOSGiRuntime())
            return false;
        
        for (int i = 0; i < libraries.length; i++) {
            if (libraries[i].getName().equals("."))
                return false;
        }
        return true;
    }



}
