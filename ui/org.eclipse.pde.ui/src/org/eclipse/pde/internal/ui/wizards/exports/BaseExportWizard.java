package org.eclipse.pde.internal.ui.wizards.exports;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.IModel;
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
		final Object [] items = page1.getSelectedItems();
		
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doPerformFinish(exportZip, items, monitor);
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
	
	protected void doPerformFinish(boolean exportZip, Object [] items, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Exporting...", items.length);
		for (int i=0; i<items.length; i++) {
			IModel model = (IModel)items[i];
			doExport(exportZip, model, new SubProgressMonitor(monitor, 1));
		}
	}
	
	protected abstract void doExport(boolean exportZip, IModel model, IProgressMonitor monitor) throws CoreException;

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
}