/*
 * Created on Feb 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AlertSection extends PDESection {
	private FormText text;
	private String noItemsText = "<form><p>No alerts at this time.</p></form>";
	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public AlertSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());

	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Alerts and Action Items");
		//toolkit.createCompositeSeparator(section);
		text = toolkit.createFormText(section, true);
		text.setImage("warning", PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		text.setImage("error", PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		section.setClient(text);
	}
	public void refresh() {
		text.setText(getAlerts(), true, false);
		super.refresh();
	}
	private String getAlerts() {
		/*StringBuffer buf = new StringBuffer();
		buf.append("<form>");
		buf.append("<li style=\"image\" value=\"warning\"><a>This plug-in has a version that is not compatible with the target platform.</a></li>");
		buf.append("<li style=\"image\" value=\"warning\"><a>This plug-in does not have build.properties file and cannot be deployed properly.</a></li>");
		buf.append("<li style=\"image\" value=\"error\"><a>You realize, of course, that coding in this plug-in is pretty pathetic, don't you?</a></li>");		
		buf.append("</form>");
		return buf.toString();*/
		return noItemsText;
	}
}