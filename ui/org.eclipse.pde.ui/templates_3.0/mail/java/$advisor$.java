package $packageName$;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.*;

/**
 * This advisor simply creates action builders for each new window and ensures
 * that the action builder is disposed when the window is closed.
 */
public class $advisor$ extends WorkbenchAdvisor {

	// Key for caching the action builder with the window configurer
	private static final String ACTION_BUILDER = "actionBuilder";
	
	public String getInitialWindowPerspectiveId() {
		return "$pluginId$.perspective";
	} 
	
	public void fillActionBars(IWorkbenchWindow window, IActionBarConfigurer configurer, int flags) {
		ActionBuilder actionBuilder = null;
		IWorkbenchWindowConfigurer windowConfigurer = getWorkbenchConfigurer().getWindowConfigurer(window);
		actionBuilder = (ActionBuilder) windowConfigurer.getData(ACTION_BUILDER);
		if (actionBuilder == null) {
			actionBuilder = new ActionBuilder(window);
		}
		if ((flags & FILL_PROXY) != 0) {
			// Filling in fake action bars, for example when showing the customize perspective dialog.
			// At this point, we don't have to create new actions, and instead simply add them to the
			// provided actions bars.
			if ((flags & FILL_MENU_BAR) != 0) {
				actionBuilder.populateMenuBar(configurer);
			}
			if ((flags & FILL_COOL_BAR) != 0) {
				actionBuilder.populateCoolBar(configurer);
			}
		} else {
			// Filling in real action bars, ok to create action instances here since it will
			// only be called once per workbench window.
			windowConfigurer.setData(ACTION_BUILDER, actionBuilder);
			actionBuilder.makeAndPopulateActions(getWorkbenchConfigurer(), configurer);
		}
	}
	
	public void postWindowClose(IWorkbenchWindowConfigurer configurer) {
		ActionBuilder a = (ActionBuilder) configurer.getData(ACTION_BUILDER);
		if (a != null) {
			configurer.setData(ACTION_BUILDER, null);
			a.dispose();
		}
	}

	public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
		super.preWindowOpen(configurer);
		configurer.setInitialSize(new Point(600, 400));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(false);
	}
}
