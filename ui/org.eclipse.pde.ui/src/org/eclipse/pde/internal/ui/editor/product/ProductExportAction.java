package org.eclipse.pde.internal.ui.editor.product;

import java.io.*;
import java.lang.reflect.*;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.build.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.*;

public class ProductExportAction extends Action {

	private PDEFormEditor fEditor;
	
	private String fZipExtension = Platform.getOS().equals("macosx") ? ".tar.gz" : ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	public ProductExportAction(PDEFormEditor editor) {
		fEditor = editor;
	}
	
	public void run() {
		if (fEditor != null)
			ensureContentSaved();
		IProductModel model = (IProductModel)fEditor.getAggregateModel();
		
		if (!validateExportDestination(model.getProduct()))
			return;
		
		ProductExportJob job = new ProductExportJob(PDEPlugin.getResourceString("ProductExportAction.jobName"), model); //$NON-NLS-1$
		job.schedule();
	}
	
	private boolean validateExportDestination(IProduct product) {
		String zipFile = product.getExportDestination();
		if (zipFile == null || zipFile.trim().length() == 0) {
			MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getResourceString("ProductExportAction.errorTitle"), PDEPlugin.getResourceString("ProductExportAction.noDestination")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		
		zipFile = zipFile.trim();
		if (!zipFile.endsWith(fZipExtension))
			zipFile += fZipExtension;
		File file = new File(zipFile);
		if (file.exists()) {
			if (file.exists()) {
				if (!MessageDialog.openQuestion(PDEPlugin.getActiveWorkbenchShell(),
						PDEPlugin.getResourceString("BaseExportWizard.confirmReplace.title"),  //$NON-NLS-1$
						PDEPlugin.getFormattedMessage("BaseExportWizard.confirmReplace.desc", //$NON-NLS-1$
								file.getAbsolutePath())))
					return false;
				file.delete();
			}
		}
		return true;	
	}
	
	private void ensureContentSaved() {
		if (fEditor.isDirty()) {
			try {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						fEditor.doSave(monitor);
					}
				};
				PlatformUI.getWorkbench().getProgressService().runInUI(
						PDEPlugin.getActiveWorkbenchWindow(), op,
						PDEPlugin.getWorkspace().getRoot());
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}


}
