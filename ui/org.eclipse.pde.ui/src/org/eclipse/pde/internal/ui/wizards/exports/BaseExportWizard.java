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
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;

/**
 * Insert the type's description here.
 * @see Wizard
 */
public abstract class BaseExportWizard extends Wizard implements IExportWizard, IPreferenceConstants {

	public static int EXPORT_AS_DIRECTORY = 1;
	public static int EXPORT_AS_ZIP = 2;
	public static int EXPORT_AS_UPDATE_JARS = 3;
	
	private IStructuredSelection selection;
	private BaseExportWizardPage page1;
	protected static PrintWriter writer;
	protected static File logFile;
	protected String buildTempLocation;

	/**
	 * The constructor.
	 */
	public BaseExportWizard() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setNeedsProgressMonitor(true);
		setDialogSettings(getSettingsSection(masterSettings));
		buildTempLocation =
			PDEPlugin.getDefault().getStateLocation().append("temp").toOSString();
	}

	private static void createLogWriter() {
		try {
			String path = PDEPlugin.getDefault().getStateLocation().toOSString();
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

	public void addPages() {
		page1 = createPage1();
		addPage(page1);
	}

	protected abstract BaseExportWizardPage createPage1();

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	protected abstract void doExport(
		int exportType,
		boolean exportSource,
		String destination,
		String zipFileName,
		IModel model,
		IProgressMonitor monitor) throws CoreException, InvocationTargetException;

	protected void doPerformFinish(
		int exportType,
		boolean exportSource,
		String destination,
		String zipFileName,
		Object[] items,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
			
		createDestination(destination);
		
		monitor.beginTask("", items.length + 1);
		for (int i = 0; i < items.length; i++) {
			IModel model = (IModel) items[i];
			doExport(
				exportType,
				exportSource,
				destination,
				zipFileName,
				model,
				new SubProgressMonitor(monitor, 1));
		}
		
		cleanup(zipFileName, destination, exportType, new SubProgressMonitor(monitor,1));
	}
	
	private void createDestination(String destination) throws InvocationTargetException {
		File file = new File(destination);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				throw new InvocationTargetException(
					new Exception(
						PDEPlugin.getResourceString("ExportWizard.badDirectory")));
			}
		}
	}

	public IStructuredSelection getSelection() {
		return selection;
	}

	protected abstract IDialogSettings getSettingsSection(IDialogSettings masterSettings);

	/**
	 * @see Wizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	/**
	 * @see Wizard#performFinish
	 */
	public boolean performFinish() {
		page1.saveSettings();
		final int exportType = page1.getExportType();
		final boolean exportSource = page1.getExportSource();
		final String destination = page1.getDestination();
		final String zipFileName = page1.getFileName();
		final Object[] items = page1.getSelectedItems();
		
		if (zipFileName != null) {
			File zipFile = new File(destination, zipFileName);
			if (zipFile.exists()) {
				zipFile.delete();
			}
		}
		
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException{
				try {
					doPerformFinish(exportType, exportSource, destination, zipFileName, items, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		
		try {
			createLogWriter();
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			String message = e.getTargetException().getMessage();
			if (message != null && message.length() > 0) {
				MessageDialog.openError(
					getShell(),
					getWindowTitle(),
					e.getTargetException().getMessage());
			}
			return false;
		} finally {
			if (writer != null)
				writer.close();
		}

			
		if (logFile != null && logFile.exists() && logFile.length() > 0) {
			MessageDialog
				.openError(
					getContainer().getShell(),
					getWindowTitle(),
					PDEPlugin.getFormattedMessage("ExportWizard.error.message", destination));
			return false;
		}

		return true;
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
		runner.setBuildFileLocation(
			location
				+ Path.SEPARATOR
				+ "build.xml");
		runner.addBuildListener("org.eclipse.pde.internal.ui.ant.ExportBuildListener");
		runner.setExecutionTargets(getExecutionTargets(exportType, exportSource));
		runner.run(monitor);
	}

	protected String[] getExecutionTargets(int exportType, boolean exportSource) {
		ArrayList targets = new ArrayList();
		if (exportType == BaseExportWizard.EXPORT_AS_UPDATE_JARS) {
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
	
	protected void cleanup(String filename, String destination, int exportType, IProgressMonitor monitor) {
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
			runner.setExecutionTargets((String[])targets.toArray(new String[targets.size()]));
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
		writer.println("<project name=\"temp\" default=\"clean\" basedir=\".\">");
	}
	
	private void generateCleanTarget(PrintWriter writer) {
		writer.println("<target name=\"clean\">");
		writer.println("<delete dir=\"" + buildTempLocation + "\"/>");
		writer.println("</target>");
	}
	
	private void generateCopyResultTarget(PrintWriter writer, String destination) {
		writer.println("<target name=\"copy.result\">");
		writer.println("<copy todir=\"" + destination + "\">");
		writer.println("<fileset dir=\"" + buildTempLocation + "/destination\"/>");
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
	
	private boolean generateZipLogsTarget(PrintWriter writer, String destination) {
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
			model.getUnderlyingResource().getProject().getFile("build.properties");
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
	
	
}