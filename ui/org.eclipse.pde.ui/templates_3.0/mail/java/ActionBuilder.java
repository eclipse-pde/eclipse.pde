package $packageName$;

import java.net.URL;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchConfigurer;

/**
 * An action builder is responsible for creating, adding, and disposing of the
 * actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ActionBuilder {
	
	// Command ids - these are used for keybindings
	public static final String CMD_OPEN = "$pluginId$.open";
	public static final String CMD_EXIT = "org.eclipse.ui.file.exit";
	public static final String CMD_OPEN_MESSAGE = "$pluginId$.openMessage";
	
	// Actions - important to dispose when when this builder is disposed. The actions
	// are kept so that they can be used to populate other action bars when the 
	// workbench advisor is called with FILL_PROXY.
	private IWorkbenchAction exitAction;
	private IWorkbenchAction aboutAction;
	private IWorkbenchAction newWindow;
	private OpenViewAction openViewAction;
	private Action messagePopup;
	
	// The window this action builder will add actions to
	private final IWorkbenchWindow window;
	
	public ActionBuilder(IWorkbenchWindow window) {
		this.window = window;
	}
	
	public void populateMenuBar(IActionBarConfigurer configurer) {
		IMenuManager mgr = configurer.getMenuManager();
		
		MenuManager fileMenu = new MenuManager("&File", "file");
		MenuManager aboutMenu = new MenuManager("&About", "about");
		
		// Allow contributions to the top-level menu
		mgr.add(fileMenu);
		mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		mgr.add(aboutMenu);
		
		// File
		fileMenu.add(newWindow);
		fileMenu.add(new Separator());
		fileMenu.add(messagePopup);
		fileMenu.add(openViewAction);
		fileMenu.add(new Separator());
		fileMenu.add(exitAction);
		
		// About
		aboutMenu.add(aboutAction);
	}
	
	public void populateCoolBar(IActionBarConfigurer configurer) {
		ICoolBarManager mgr = configurer.getCoolBarManager();
		IToolBarManager toolbar = new ToolBarManager(mgr.getStyle());
        mgr.add(toolbar);	
        toolbar.add(openViewAction);
        toolbar.add(messagePopup);
	}
	
	public void makeAndPopulateActions(IWorkbenchConfigurer workbenchConfigurer, IActionBarConfigurer configurer) {
		// Create the actions
		exitAction = ActionFactory.QUIT.create(window);
		aboutAction = ActionFactory.ABOUT.create(window);
		newWindow = ActionFactory.OPEN_NEW_WINDOW.create(window);
		openViewAction = new OpenViewAction(window, "Open Another Message View", $closeable$.ID);
		messagePopup = new Action("Open Message") {
			public void run() {
				MessageDialog.openInformation(window.getShell(), "Open", "Open Message Dialog!");
			}
		};
		URL imageURL = Platform.getBundle("$pluginId$").getEntry("/icons/sample3.gif");
		ImageDescriptor desc = ImageDescriptor.createFromURL(imageURL);
		messagePopup.setImageDescriptor(desc);
		
		// Associate the action with a pre-defined command. The commands are
		// defined in the plug.xml file.
		messagePopup.setActionDefinitionId(CMD_OPEN_MESSAGE);
		
		// Register actions for keybindings. The keybindings are defined in the plugin.xml file.
		configurer.registerGlobalAction(messagePopup);
		configurer.registerGlobalAction(exitAction);	
		configurer.registerGlobalAction(openViewAction);
		
		// Add the actions to the actions bars
		populateCoolBar(configurer);
		populateMenuBar(configurer);			
	}
	
	public void dispose() {
		exitAction.dispose();
		aboutAction.dispose();
		newWindow.dispose();
	}
}
