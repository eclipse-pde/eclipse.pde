/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.ui.ILaunchWizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.launching.*;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

public class WorkbenchLauncherWizard extends Wizard implements ILaunchWizard {

	private static final String STORE_SECTION = "WorkbenchLauncherWizard";

	private String mode;
	private ILauncher launcher;

	private WorkbenchLauncherWizardBasicPage page1;
	private WorkbenchLauncherWizardAdvancedPage page2;
	private IStructuredSelection selection;

	public WorkbenchLauncherWizard() {
		setDialogSettings(getSettingsSection());
	}

	/*
	 * @see org.eclipse.jface.wizard.IWizard#addPages
	 */
	public void addPages() {
		setNeedsProgressMonitor(true);
		String title;
		if ("debug".equals(mode)) {
			title = "Run-time Workbench Launcher (Debug Mode)";
		} else {
			title = "Run-time Workbench Launcher (Run Mode)";
		}
		page1 = new WorkbenchLauncherWizardBasicPage(title);
		addPage(page1);
		page2 = new WorkbenchLauncherWizardAdvancedPage(title);
		addPage(page2);
	}

	private static IDialogSettings getSettingsSection() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	/**
	 * Sets the chosen launcher and elements.
	 */
	public boolean performFinish() {
		try {
			page1.storeSettings(true);
			page2.storeSettings();
			final LauncherData data = createLauncherData();
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						delegateLaunch(data, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			String title = "Launch Eclipse Workbench";
			String message = "Launch failed. See log for details.";
			PDEPlugin.logException(e, title, message);
			return true; // exception handled
		}
		return true;
	}
	
	private LauncherData createLauncherData() {
		LauncherData data = new LauncherData();
		data.setWorkspaceLocation(page1.getWorkspaceLocation());
		data.setClearWorkspace(page1.doClearWorkspace());
		IVMInstall install = page1.getVMInstall();
		data.setVmInstall(install);
		data.setPlugins(page2.getPlugins());
		data.setProgramArguments(page1.getProgramArguments());
		data.setVmArguments(page1.getVMArguments());
		data.setApplicationName(page1.getApplicationName());
		data.setTracingEnabled(page1.isTracingEnabled());
		return data;
	}

	/*
	 * @see Wizard#performCancel()
	 */
	public boolean performCancel() {
		page1.storeSettings(false);
		return super.performCancel();
	}

	private void delegateLaunch(LauncherData data, IProgressMonitor monitor) throws CoreException {
		IPath targetWorkbenchLocation = data.getWorkspaceLocation();
		boolean clearWorkspace = data.getClearWorkspace();
		IVMInstall install = data.getVmInstall();
		IVMRunner runner = install.getVMRunner(mode);
		IPluginModelBase[] plugins = data.getPlugins();
		ExecutionArguments args =
			new ExecutionArguments(data.getVmArguments(), data.getProgramArguments());
		String appname = data.getApplicationName();
		boolean tracing = data.getTracingEnabled();

		WorkbenchLauncherDelegate delegate =
			(WorkbenchLauncherDelegate) launcher.getDelegate();
		delegate.doLaunch(
			launcher,
			mode,
			runner,
			targetWorkbenchLocation,
			clearWorkspace,
			args,
			plugins,
			appname,
			tracing,
			monitor);
	}

	private static void delegateHeadlessLaunch(
		ILauncher launcher,
		String mode,
		IStructuredSelection selection,
		LauncherData data,
		IProgressMonitor monitor)
		throws CoreException {
		IPath targetWorkbenchLocation = data.getWorkspaceLocation();
		boolean clearWorkspace = data.getClearWorkspace();
		IVMInstall install = data.getVmInstall();
		IVMRunner runner = install.getVMRunner(mode);
		IPluginModelBase[] plugins = data.getPlugins();
		ExecutionArguments args =
			new ExecutionArguments(data.getVmArguments(), data.getProgramArguments());
		String appname = data.getApplicationName();
		boolean tracing = data.getTracingEnabled();

		WorkbenchLauncherDelegate delegate =
			(WorkbenchLauncherDelegate) launcher.getDelegate();
		delegate.doLaunch(
			launcher,
			mode,
			runner,
			targetWorkbenchLocation,
			clearWorkspace,
			args,
			plugins,
			appname,
			tracing,
			monitor);
	}

	/*
	 * @see ILaunchWizard#init
	 */
	public void init(
		ILauncher launcher,
		String mode,
		IStructuredSelection selection) {
		this.mode = mode;
		this.launcher = launcher;
		this.selection = selection;
		initializeDefaultPageImageDescriptor();
	}

	public static boolean runHeadless(
		final ILauncher launcher,
		final String mode,
		final IStructuredSelection selection) {
		try {
			IDialogSettings settings = getSettingsSection();
			final LauncherData launcherData = new LauncherData();
			// Collect stored launcher data from the dialog settings
			WorkbenchLauncherWizardBasicPage.setLauncherData(settings, launcherData);
			WorkbenchLauncherWizardAdvancedPage.setLauncherData(settings, launcherData);
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());

			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						delegateHeadlessLaunch(launcher, mode, selection, launcherData, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
			return true;
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			String title = "Launch Eclipse Workbench";
			String message = "Launch failed. See log for details.";
			PDEPlugin.logException(e, title, message);
			return true; // exception handled
		}
	}

	/**
	 * Initializes the default page image descriptor to an appropriate banner.
	 * <p>
	 * Subclasses may reimplement.
	 * </p>
	 */
	protected void initializeDefaultPageImageDescriptor() {
		if ("debug".equals(mode)) {
			setDefaultPageImageDescriptor(PDEPluginImages.DESC_DEBUG_WIZ);
		} else {
			setDefaultPageImageDescriptor(PDEPluginImages.DESC_RUN_WIZ);
		}
	}

}