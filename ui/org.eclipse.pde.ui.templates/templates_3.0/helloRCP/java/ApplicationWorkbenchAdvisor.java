package $packageName$;

import org.eclipse.swt.graphics.Point;
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
}
