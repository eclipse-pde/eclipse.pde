package org.eclipse.pde.internal.ui.tests.macro;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class DefaultIndexHandler implements IIndexHandler {

	public DefaultIndexHandler() {
	}

	public IStatus processIndex(final Shell shell, String indexId) {
		final String message = "Index reached: "+indexId;
		
		final IStatus [] result = new IStatus[1];
		
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(shell, "Macro Playback", message);
				result[0] = Status.OK_STATUS;
			}
		});
		return result[0];
	}
}
