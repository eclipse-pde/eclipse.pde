package org.eclipse.pde.internal.ui.wizards.exports;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;

/**
 * Insert the type's description here.
 * @see Wizard
 */
public abstract class BaseExportWizard extends Wizard implements IExportWizard {
	private IStructuredSelection selection;
	private BaseExportWizardPage page1;
	
	/**
	 * The constructor.
	 */
	public BaseExportWizard() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		IDialogSettings masterSettings =
			PDEPlugin.getDefault().getDialogSettings();
		setNeedsProgressMonitor(true);
		setDialogSettings(getSettingsSection(masterSettings));
	}
	
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	
	public void addPages() {
		page1 = createPage1();
		addPage(page1);
	}
	
	protected abstract IDialogSettings getSettingsSection(IDialogSettings masterSettings);
	protected abstract BaseExportWizardPage createPage1();

	/**
	 * Insert the method's description here.
	 * @see Wizard#performFinish
	 */
	public boolean performFinish()  {
		page1.saveSettings();
		final boolean exportZip = page1.getExportZip();
		final String destination = page1.getDestination();
		final Object [] items = page1.getSelectedItems();
		
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doPerformFinish(exportZip, destination, items, monitor);
				}
				catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				finally {
					monitor.done();
				}
			}
		};
		
		try {
			getContainer().run(true, true, op);
		}
		catch (InterruptedException e) {
			return false;
		}
		catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}
	
	protected void doPerformFinish(
		boolean exportZip,
		String destination,
		Object[] items,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString("ExportWizard.exporting"),
			items.length);
		ArrayList statusEntries = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			IModel model = (IModel) items[i];
			IStatus status = ensureValid(model, monitor);
			if (status == null) {
				doExport(
					exportZip,
					destination,
					model,
					new SubProgressMonitor(monitor, 1));
			} else {
				statusEntries.add(status);
				monitor.worked(1);
			}
		}
		if (statusEntries.size() > 0) {
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.PLUGIN_ID,
					IStatus.ERROR,
					(IStatus[]) statusEntries.toArray(new IStatus[statusEntries.size()]),
					"Errors encountered while building.",
					null);
					throw new CoreException(multiStatus);
		}
	}
	
	protected abstract void doExport(boolean exportZip, String destination, IModel model, IProgressMonitor monitor) throws InvocationTargetException, CoreException;

	/**
	 * Insert the method's description here.
	 * @see Wizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection)  {
		this.selection = selection;
	}
	
	public IStructuredSelection getSelection() {
		return selection;
	}
	
	private IStatus ensureValid(IModel model, IProgressMonitor monitor) throws CoreException {
		// Force the build if autobuild is off
		IResource file = model.getUnderlyingResource();
		IProject project = file.getProject();
		if (!project.getWorkspace().isAutoBuilding()) {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		}

		IStatus status = null;
		if (hasErrors(file)) {
			StringBuffer buffer = new StringBuffer(" contains errors");
			 
			if (model instanceof IPluginModel) {
				buffer.insert(0,"plugin " + ((IPluginModel)model).getPluginBase().getId());
			} else if (model instanceof IFragmentModel) {
				buffer.insert(0, "fragment " + ((IFragmentModel)model).getPluginBase().getId());
			} else if (model instanceof IFeatureModel) {
				buffer.insert(0, "feature " + ((IFeatureModel)model).getFeature().getId());
			}
			
			status = new Status(IStatus.ERROR, PDEPlugin.PLUGIN_ID, IStatus.ERROR, buffer.toString(), null);

		}
		return status;
	}
	
	private boolean hasErrors(IResource file) throws CoreException {
		// Check if there are errors against feature file
		IMarker[] markers =file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		for (int i=0; i<markers.length; i++) {
			IMarker marker = markers[i];
			Object att = marker.getAttribute(IMarker.SEVERITY);
			if (att!=null && att instanceof Integer) {
				Integer severity = (Integer)att;
				if (severity.intValue()==IMarker.SEVERITY_ERROR) return true;
			}
		}
		return false;
	}
	
	private void makeScript(IModel model, IProgressMonitor monitor) {}
	
}