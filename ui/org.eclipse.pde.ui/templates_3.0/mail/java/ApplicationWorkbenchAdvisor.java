package $packageName$;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
	
	private static final String PERSPECTIVE_ID = "$pluginId$.perspective";

    private IWorkbenchAction exitAction;
    private IWorkbenchAction aboutAction;
    private IWorkbenchAction newWindowAction;
    private OpenViewAction openViewAction;
    private Action messagePopupAction;

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	} 
	
	public void fillActionBars(IWorkbenchWindow window, IActionBarConfigurer configurer, int flags) {
		if ((flags & FILL_PROXY) != 0) {
			// Filling in fake action bars, for example when showing the customize perspective dialog.
			// At this point, we don't have to create new actions, and instead simply add them to the
			// provided actions bars.
			if ((flags & FILL_MENU_BAR) != 0) {
				fillMenuBar(configurer.getMenuManager());
			}
			if ((flags & FILL_COOL_BAR) != 0) {
				fillCoolBar(configurer.getCoolBarManager());
			}
		} else {
			makeActions(window, configurer);
			fillMenuBar(configurer.getMenuManager());
			fillCoolBar(configurer.getCoolBarManager());
		}
	}
	
	private void fillMenuBar(IMenuManager menuBar) {
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
        
        menuBar.add(fileMenu);
        // Add a group marker indicating where action set menus will appear.
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(helpMenu);
        
        // File
        fileMenu.add(newWindowAction);
        fileMenu.add(new Separator());
        fileMenu.add(messagePopupAction);
        fileMenu.add(openViewAction);
        fileMenu.add(new Separator());
        fileMenu.add(exitAction);
        
        // Help
        helpMenu.add(aboutAction);		
	}
	
    private void fillCoolBar(ICoolBarManager coolBar) {
        IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        coolBar.add(new ToolBarContributionItem(toolbar, "main"));   
        toolbar.add(openViewAction);
        toolbar.add(messagePopupAction);
    }

	
	private void makeActions(IWorkbenchWindow window, IActionBarConfigurer configurer) {
        exitAction = ActionFactory.QUIT.create(window);
        configurer.registerGlobalAction(exitAction);
        
        aboutAction = ActionFactory.ABOUT.create(window);
        configurer.registerGlobalAction(aboutAction);
        
        newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
        configurer.registerGlobalAction(newWindowAction);
        
        openViewAction = new OpenViewAction(window, "Open Another Message View", View.ID);
        configurer.registerGlobalAction(openViewAction);
        
        messagePopupAction = new MessagePopupAction("Open Message", window);
        configurer.registerGlobalAction(messagePopupAction);
		
	}
	
	public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
		configurer.setInitialSize(new Point(600, 400));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(false);
	}
	
	public void postWindowClose(IWorkbenchWindowConfigurer configurer) {
		aboutAction.dispose();
		exitAction.dispose();
		newWindowAction.dispose();
	}
}
