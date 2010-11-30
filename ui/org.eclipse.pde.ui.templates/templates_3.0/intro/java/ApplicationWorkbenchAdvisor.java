package $packageName$;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.IWorkbenchWindow;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "$pluginId$.perspective";
    
    private ActionFactory.IWorkbenchAction introAction;
    
    public void initialize(IWorkbenchConfigurer configurer) {
        super.initialize(configurer);
        configurer.setSaveAndRestore(true);
    }

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

    public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
        configurer.setInitialSize(new Point(700, 550));
        configurer.setShowCoolBar(false);
        configurer.setShowStatusLine(false);
        configurer.setTitle("$productName$");
    }

    public void fillActionBars(IWorkbenchWindow window, IActionBarConfigurer configurer, int flags) {
        super.fillActionBars(window, configurer, flags);
        if ((flags & WorkbenchAdvisor.FILL_PROXY) == 0) {
            introAction = ActionFactory.INTRO.create(window);
            configurer.registerGlobalAction(introAction);
        }
        
        if ((flags & WorkbenchAdvisor.FILL_MENU_BAR) != 0) {
            IMenuManager menuManager = configurer.getMenuManager();
            IMenuManager helpMenu = new MenuManager("&Help", "help");
            menuManager.add(helpMenu);
            helpMenu.add(introAction);
        }
        
        introAction = ActionFactory.INTRO.create(window);
        configurer.registerGlobalAction(introAction);

    }

}
