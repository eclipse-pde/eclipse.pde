package $packageName$;

import java.net.URL;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;

public class OpenViewAction extends Action implements ActionFactory.IWorkbenchAction {
	
	private final IWorkbenchWindow window;
	private int instanceNum = 0;
	private final String viewId;
	
	public OpenViewAction(IWorkbenchWindow window, String label, String viewId) {
		this.window = window;
		this.viewId = viewId;
		setText(label);
		setId(ActionBuilder.CMD_OPEN);
		setActionDefinitionId(ActionBuilder.CMD_OPEN);
		URL imageURL = Platform.getBundle("$pluginId$").getEntry("/icons/sample2.gif");
		ImageDescriptor desc = ImageDescriptor.createFromURL(imageURL);
		setImageDescriptor(desc);
	}
	
	public void run() {
		if(window != null) {	
			try {
				window.getActivePage().showView(viewId, Integer.toString(instanceNum++), IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
	}
	
	public void dispose() {	
	}
}
