/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.launcher;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.ui.ILaunchWizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.launching.*;

import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.pde.internal.PDEPluginImages;
import org.eclipse.pde.internal.base.model.plugin.IPluginModelBase;

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

	private IDialogSettings getSettingsSection() {
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
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						delegateLaunch(monitor);
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

	/*
	 * @see Wizard#performCancel()
	 */
	public boolean performCancel() {
		page1.storeSettings(false);
		return super.performCancel();
	}

	private void delegateLaunch(IProgressMonitor monitor) throws CoreException {
		IPath targetWorkbenchLocation= page1.getWorkspaceLocation();
		boolean clearWorkspace= page1.doClearWorkspace();
		IVMInstall install= page1.getVMInstall();
		IVMRunner runner= install.getVMRunner(mode);
		IPluginModelBase[] plugins= page2.getPlugins();
		ExecutionArguments args= new ExecutionArguments(page1.getVMArguments(), page1.getProgramArguments());
		String appname= page1.getApplicationName();
		/*
		NewWorkbenchLauncher delegate= (NewWorkbenchLauncher) launcher.getDelegate();
		delegate.initialize(runner, targetWorkbenchLocation, clearWorkspace, args, plugins, appname);
		delegate.launch(selection.toArray(), mode, launcher, monitor);
		*/
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