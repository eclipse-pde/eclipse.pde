package $packageName$;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
	
	private static final String PERSPECTIVE_ID = "$pluginId$.perspective";

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
		configurer.setInitialSize(new Point(400, 300));
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(false);
%if productBranding == false		
		configurer.setTitle("$windowTitle$");
%endif
	}

	public void fillActionBars(IWorkbenchWindow window,
			IActionBarConfigurer configurer, int flags) {
		super.fillActionBars(window, configurer, flags);
		if ((flags & FILL_MENU_BAR) != 0) {
			fillMenuBar(window, configurer);
		}
	}

	private void fillMenuBar(IWorkbenchWindow window,
			IActionBarConfigurer configurer) {
		IMenuManager menuBar = configurer.getMenuManager();
		menuBar.add(createFileMenu(window));
	}

	private MenuManager createFileMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager("File", //$NON-NLS-1$
				IWorkbenchActionConstants.M_FILE);
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(ActionFactory.QUIT.create(window));
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
		return menu;
	}

}
