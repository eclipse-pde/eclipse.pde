/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormPage;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class OverviewPage extends PDEFormPage implements HyperlinkListener {
	public static final String PAGE_ID = "overview";
	
	private static final String contentText = 
		"<form><p>You can do the following things with this plug-in:</p>"+
		"<li style=\"image\" value=\"page\" bindent=\"5\">Make a dependency on other plug-ins in <a href=\"dependencies\">Dependencies</a></li>" +
		"<li style=\"image\" value=\"page\" bindent=\"5\">Change the run-time information in <a href=\"runtime\">Runtime</a></li>"+
		"<li style=\"image\" value=\"page\" bindent=\"5\">Extend other plug-ins in <a href=\"extensions\">Extensions</a></li>"+
		"<li style=\"image\" value=\"page\" bindent=\"5\">Create extension points in <a href=\"ex-points\">Extension Points</a></li>"+
		"</form>";

	private static final String testingText =
		"<form>" +
		"<p>You can test the plug-in in two ways:</p>"+
		"<li style=\"image\" value=\"run\" bindent=\"5\">By creating a new <a href=\"run-config\">Run configuration</a></li>"+
		"<li style=\"image\" value=\"run\" bindent=\"5\">Through the <img href=\"workbench\"/> <a href=\"run\">Run-time Workbench</a> shortcut</li>"+
		"<p>If your plug-in contains Java code, you can debug it in a similar way:</p>"+
		"<li style=\"image\" value=\"debug\" bindent=\"5\">By creating a new <a href=\"debug-config\">Debug configuration</a></li>"+
		"<li style=\"image\" value=\"debug\" bindent=\"5\">Through the <img href=\"workbench\"/> <a href=\"debug\">Run-time Workbench</a> shortcut</li>"+
		"<p><img href=\"tbs\"/> <a href=\"tbs-testing\">Troubleshooting</a></p>"+
		"</form>";

	private static final String deployingText = 
		"<form><p>To deploy the plug-in:</p>"+
		"<li style=\"text\" value=\"1.\" bindent=\"5\">Configure build properties in <a href=\"build\">Build</a></li>" +
		"<li style=\"text\" value=\"2.\" bindent=\"5\">Export the plug-in using <a href=\"export\">Export wizard</a></li>"+
		"<p><img href=\"tbs\"/> <a href=\"tbs-deploying\">Troubleshooting</a></p>"+
		"</form>";

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public OverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, "Overview");
	}
	protected String getHelpResource() {
		return "/org.eclipse.pde.doc.user/guide/pde_manifest_overview.htm";
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Overview");
		fillBody(managedForm, toolkit);
		managedForm.refresh();
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		TableWrapLayout layout = new TableWrapLayout();
		layout.bottomMargin = 10;
		layout.topMargin = 10;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 10;
		layout.numColumns = 2;
		//layout.makeColumnsEqualWidth = true;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);
		
		// alerts
		createAlertSection(managedForm, body, toolkit);
		// left column
		Composite left = toolkit.createComposite(body);
		TableWrapLayout ll = new TableWrapLayout();
		ll.topMargin = 0;
		ll.bottomMargin = 0;
		ll.leftMargin = 0;
		ll.rightMargin = 0;
		ll.verticalSpacing = 20;
		left.setLayout(ll);
		left.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// right column
		Composite right = toolkit.createComposite(body);
		TableWrapLayout rl = new TableWrapLayout();
		rl.topMargin = 0;
		rl.bottomMargin = 0;
		rl.leftMargin = 0;
		rl.rightMargin = 0;
		rl.verticalSpacing = 20;
		right.setLayout(rl);
		right.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		// sections
		createGeneralInfoSection(managedForm, left, toolkit);
		createContentSection(managedForm, left, toolkit);
		createTestingSection(managedForm, right, toolkit);
		createDeployingSection(managedForm, right, toolkit);
	}
	private void createAlertSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		AlertSection section = new AlertSection(this, parent);
		managedForm.addPart(section);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 2;
		section.getSection().setLayoutData(td);		
	}
	private void createGeneralInfoSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		GeneralInfoSection section = new GeneralInfoSection(this, parent);
		managedForm.addPart(section);
	}
	private void createContentSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);
		section.setText("Content");
		FormText text = createClient(section, contentText, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(PDEPluginImages.DESC_PAGE_OBJ, PDELabelProvider.F_EDIT));		
	}
	private void createTestingSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);		
		section.setText("Testing");		
		FormText text = createClient(section, testingText, toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("run", lp.get(PDEPluginImages.DESC_RUN_EXC));
		text.setImage("debug", lp.get(PDEPluginImages.DESC_DEBUG_EXC));
		text.setImage("workbench", lp.get(PDEPluginImages.DESC_WORKBENCH_LAUNCHER_WIZ));
		text.setImage("tbs", PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK));		
	}
	private void createDeployingSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(parent, toolkit);		
		section.setText("Deploying");
		FormText text = createClient(section, deployingText, toolkit);
		text.setImage("tbs", PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK));		
	}
	private Section createStaticSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, Section.EXPANDED|Section.TWISTIE);		
		toolkit.createCompositeSeparator(section);
		return section;
	}
	private FormText createClient(Section section, String content, FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		try {
			text.setText(content, true, false);
		}
		catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		section.setClient(text);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(td);
		text.addHyperlinkListener(this);
		return text;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
		String href = (String)e.getHref();
		// try page references
		if (href.equals("dependencies"))
			getEditor().setActivePage(DependenciesPage.PAGE_ID);
		else if (href.equals("runtime"))
			getEditor().setActivePage(RuntimePage.PAGE_ID);
		else if (href.equals("extensions"))
			getEditor().setActivePage(ExtensionsPage.PAGE_ID);
		else if (href.equals("ex-points"))
			getEditor().setActivePage(ExtensionPointsPage.PAGE_ID);
		else if (href.equals("build"))
			getEditor().setActivePage(BuildPage.PAGE_ID);
		// try running
		// try troubleshooting
		else if (href.equals("tbs-test")) {
		}
		else if (href.equals("tbs-deploy")) {
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(e.getLabel());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getEditor().getEditorSite().getActionBars().getStatusLineManager();
		mng.setMessage(null);		
	}
}