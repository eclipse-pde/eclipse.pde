/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Display;

public abstract class BaseExportJob
	extends Job
	implements IPreferenceConstants {

	public static final int EXPORT_AS_DIRECTORY = 1;
	public static final int EXPORT_AS_ZIP = 2;
	public static final int EXPORT_AS_UPDATE_JARS = 3;
	private static final ISchedulingRule nonConcurentPDEExportRule =
		new ISchedulingRule() {
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return this == rule;
		}
	};
	private static final IStatus OK_STATUS =
		new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null);

	protected static PrintWriter writer;
	protected static File logFile;

	protected int exportType;
	protected boolean exportSource;
	protected String destination;
	protected String zipFileName;
	protected Object[] items;

	protected String buildTempLocation;

	/**
	 * @param name
	 */
	public BaseExportJob(
		int exportType,
		boolean exportSource,
		String destination,
		String zipFileName,
		Object[] items) {
		super(PDEPlugin.getResourceString("ExportJob.jobTitle"));
		this.exportType = exportType;
		this.exportSource = exportSource;
		this.destination = destination;
		this.zipFileName = zipFileName;
		this.items = items;
		setSchedulingRules();
		buildTempLocation =
			PDEPlugin
				.getDefault()
				.getStateLocation()
				.append("temp")
				.toOSString();
	}
	private void setSchedulingRules() {
		java.util.List rules = new ArrayList();
		// Prevent concurent PDE exports
		// since files and writer are shared
		rules.add(nonConcurentPDEExportRule);
		// Prevent modyfication of resources being exported
		for (int i = 0; i < items.length; i++) {
			IModel model = (IModel) items[i];
			IResource resource = model.getUnderlyingResource();
			if (resource != null) {
				rules.add(resource);
			}
		}
		ISchedulingRule[] rulesArray =
			(ISchedulingRule[]) rules.toArray(
				new ISchedulingRule[rules.size()]);
		this.setRule(new MultiRule(rulesArray));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		if (zipFileName != null) {
			File zipFile = new File(destination, zipFileName);
			if (zipFile.exists()) {
				zipFile.delete();
			}
		}

		String errorMessage = null;
		try {
			createLogWriter();
			try {
				doExports(monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		} catch (InvocationTargetException e) {
			String message = e.getTargetException().getMessage();
			if (message != null && message.length() > 0) {
				errorMessage = e.getTargetException().getMessage();
			}
		} finally {
			if (writer != null)
				writer.close();
		}

		if (errorMessage == null
			&& logFile != null
			&& logFile.exists()
			&& logFile.length() > 0) {
			errorMessage =
				PDEPlugin.getFormattedMessage(
					"ExportJob.error.message",
					destination);
		}
		if (errorMessage != null) {
			// TODO Once platform has a mean to notify users
			// of errors, do not do this, just return error status
			final String em = errorMessage;
			getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					asyncNotifyExportException(em);
				}
			});
			return Job.ASYNC_FINISH;
		}

		return OK_STATUS;
	}

	protected void doExports(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		createDestination(destination);

		monitor.beginTask("", items.length + 1);
		for (int i = 0; i < items.length; i++) {
			IModel model = (IModel) items[i];
			doExport(model, new SubProgressMonitor(monitor, 1));
		}

		cleanup(
			zipFileName,
			destination,
			exportType,
			new SubProgressMonitor(monitor, 1));
	}

	protected abstract void doExport(IModel model, IProgressMonitor monitor)
		throws CoreException, InvocationTargetException;

	private static void createLogWriter() {
		try {
			String path =
				PDEPlugin.getDefault().getStateLocation().toOSString();
			logFile = new File(path, "exportLog.txt");
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

	private void createDestination(String destination)
		throws InvocationTargetException {
		File file = new File(destination);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				throw new InvocationTargetException(
					new Exception(
						PDEPlugin.getResourceString(
							"ExportWizard.badDirectory")));
			}
		}
	}

	protected void runScript(
		String location,
		String destination,
		int exportType,
		boolean exportSource,
		Map properties,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		AntRunner runner = new AntRunner();
		runner.addUserProperties(properties);
		runner.setAntHome(location);
		runner.setBuildFileLocation(location + Path.SEPARATOR + "build.xml");
		runner.addBuildListener(
			"org.eclipse.pde.internal.ui.ant.ExportBuildListener");
		runner.setExecutionTargets(
			getExecutionTargets(exportType, exportSource));
		runner.run(monitor);
	}

	protected String[] getExecutionTargets(
		int exportType,
		boolean exportSource) {
		ArrayList targets = new ArrayList();
		if (exportType == BaseExportJob.EXPORT_AS_UPDATE_JARS) {
			targets.add("build.update.jar");
		} else {
			targets.add("build.jars");
			targets.add("gather.bin.parts");
			if (exportSource) {
				targets.add("build.sources");
				targets.add("gather.sources");
			}
		}
		return (String[]) targets.toArray(new String[targets.size()]);
	}

	protected void cleanup(
		String filename,
		String destination,
		int exportType,
		IProgressMonitor monitor) {
		File scriptFile = null;
		try {
			scriptFile = createScriptFile();
			writer = new PrintWriter(new FileWriter(scriptFile), true);
			generateHeader(writer);
			generateCleanTarget(writer);
			boolean errors = generateZipLogsTarget(writer, destination);
			if (exportType == EXPORT_AS_ZIP)
				generateZipFolderTarget(writer, destination, filename);
			else if (exportType == EXPORT_AS_DIRECTORY)
				generateCopyResultTarget(writer, destination);
			generateClosingTag(writer);
			writer.close();

			ArrayList targets = new ArrayList();
			if (errors)
				targets.add("zip.logs");
			if (exportType == EXPORT_AS_ZIP)
				targets.add("zip.folder");
			else if (exportType == EXPORT_AS_DIRECTORY)
				targets.add("copy.result");
			targets.add("clean");

			AntRunner runner = new AntRunner();
			runner.setBuildFileLocation(scriptFile.getAbsolutePath());
			runner.setExecutionTargets(
				(String[]) targets.toArray(new String[targets.size()]));
			runner.run(monitor);
		} catch (IOException e) {
		} catch (CoreException e) {
		} finally {
			if (scriptFile != null && scriptFile.exists())
				scriptFile.delete();
		}

	}

	private File createScriptFile() throws IOException {
		String path = PDEPlugin.getDefault().getStateLocation().toOSString();
		File zip = new File(path, "zip.xml");
		if (zip.exists()) {
			zip.delete();
			zip.createNewFile();
		}
		return zip;
	}

	private void generateHeader(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println(
			"<project name=\"temp\" default=\"clean\" basedir=\".\">");
	}

	private void generateCleanTarget(PrintWriter writer) {
		writer.println("<target name=\"clean\">");
		writer.println("<delete dir=\"" + buildTempLocation + "\"/>");
		writer.println("</target>");
	}

	private void generateCopyResultTarget(
		PrintWriter writer,
		String destination) {
		writer.println("<target name=\"copy.result\">");
		writer.println("<copy todir=\"" + destination + "\">");
		writer.println(
			"<fileset dir=\"" + buildTempLocation + "/destination\"/>");
		writer.println("</copy>");
		writer.println("</target>");
	}

	private void generateZipFolderTarget(
		PrintWriter writer,
		String destination,
		String filename) {
		writer.println("<target name=\"zip.folder\">");
		writer.println(
			"<zip zipfile=\""
				+ destination
				+ "/"
				+ filename
				+ "\" basedir=\""
				+ buildTempLocation
				+ "/destination\"/>");
		writer.println("</target>");
	}

	private boolean generateZipLogsTarget(
		PrintWriter writer,
		String destination) {
		if (logFile != null && logFile.exists() && logFile.length() > 0) {
			writer.println("<target name=\"zip.logs\">");
			writer.println("<delete file=\"" + destination + "/logs.zip\"/>");
			writer.println(
				"<zip zipfile=\""
					+ destination
					+ "/logs.zip\" basedir=\""
					+ buildTempLocation
					+ "/temp.folder\"/>");
			writer.println("</target>");
			return true;
		}
		return false;
	}

	private void generateClosingTag(PrintWriter writer) {
		writer.println("</project>");
	}

	protected boolean isCustomBuild(IModel model) throws CoreException {
		IBuildModel buildModel = null;
		IFile buildFile =
			model.getUnderlyingResource().getProject().getFile(
				"build.properties");
		if (buildFile.exists()) {
			buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
		}
		if (buildModel != null) {
			IBuild build = buildModel.getBuild();
			IBuildEntry entry = build.getEntry("custom");
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].equals("true"))
						return true;
				}
			}
		}
		return false;
	}
	/**
	 * Returns the standard display to be used. The method first checks, if the
	 * thread calling this method has an associated disaply. If so, this
	 * display is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display;
		display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	private void asyncNotifyExportException(String errorMessage) {
		// ask the user to install updates
		getStandardDisplay().beep();
		MessageDialog.openError(
			PDEPlugin.getActiveWorkbenchShell(),
			PDEPlugin.getResourceString("ExportJob.jobTitle"),
			errorMessage);
		done(OK_STATUS);
	}

}
