/*
 * Created on Mar 12, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.ui.internal.samples;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.IDE;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class SampleWizard extends Wizard
		implements
			INewWizard,
			IExecutableExtension {
	private IConfigurationElement[] samples;
	private IConfigurationElement selection;
	private LastPage lastPage;
	private class ImportOverwriteQuery implements IOverwriteQuery {
		public String queryOverwrite(String file) {
			String[] returnCodes = {YES, NO, ALL, CANCEL};
			int returnVal = openDialog(file);
			return returnVal < 0 ? CANCEL : returnCodes[returnVal];
		}
		private int openDialog(final String file) {
			final int[] result = {IDialogConstants.CANCEL_ID};
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					String title = PDEPlugin
							.getResourceString("SampleWizard.overwritequery.title"); //$NON-NLS-1$
					String msg = PDEPlugin.getFormattedMessage(
							"SampleWizard.overwritequery.message", file); //$NON-NLS-1$
					String[] options = {IDialogConstants.YES_LABEL,
							IDialogConstants.NO_LABEL,
							IDialogConstants.YES_TO_ALL_LABEL,
							IDialogConstants.CANCEL_LABEL};
					MessageDialog dialog = new MessageDialog(getShell(), title,
							null, msg, MessageDialog.QUESTION, options, 0);
					result[0] = dialog.open();
				}
			});
			return result[0];
		}
	}
	/**
	 * The default constructor.
	 *  
	 */
	public SampleWizard() {
		samples = Platform.getPluginRegistry().getConfigurationElementsFor(
				"org.eclipse.pde.ui.samples");
		lastPage = new LastPage();
	}
	/**
	 *  
	 */
	public void addPages() {
		if (selection == null) {
			// need to add selection page
		}
		addPage(lastPage);
	}
	/**
	 *  
	 */
	public boolean performFinish() {
		try {
			String perspId = selection.getAttribute("perspectiveId");
			IWorkbenchPage page = PDEPlugin.getActivePage();
			if (perspId != null) {
				page = PDEPlugin.getActiveWorkbenchWindow().openPage(perspId,
						null);
			}
			SampleOperation op = new SampleOperation(selection,
					new ImportOverwriteQuery());
			getContainer().run(true, true, op);
			IFile sampleManifest = op.getSampleManifest();
			if (sampleManifest != null)
				IDE.openEditor(page, sampleManifest, true);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}
	/**
	 *  
	 */
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		String variable = data != null && data instanceof String ? data
				.toString() : null;
		if (variable != null) {
			for (int i = 0; i < samples.length; i++) {
				IConfigurationElement element = samples[i];
				String id = element.getAttribute("id");
				if (id != null && id.equals(variable)) {
					selection = element;
					lastPage.setSelection(selection);
					break;
				}
			}
		}
	}
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}
}