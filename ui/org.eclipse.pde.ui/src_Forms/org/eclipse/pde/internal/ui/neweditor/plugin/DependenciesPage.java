/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormPage;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DependenciesPage extends PDEFormPage {
	public static final String PAGE_ID="dependencies";
	private static final String linksText =
	"<form>"+
	"<p><img href=\"loops\"/> <a href=\"loops\">Look for cycles in the dependency graph</a></p>"+		
	"<p><img href=\"search\"/> <a href=\"plugins\">Find plug-ins that depend on this plug-in</a></p>"+
	"<p><img href=\"search\"/> <a href=\"fragments\">Find fragments that reference this plug-in</a></p>"+
	"</form>";
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public DependenciesPage(FormEditor editor) {
		super(editor, PAGE_ID, "Dependencies");
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Dependencies");
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);
		// add requires
		RequiresSection requiresSection = new RequiresSection(this, body);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 2;		
		requiresSection.getSection().setLayoutData(gd);
		managedForm.addPart(requiresSection);
		
		//Add match
		MatchSection matchSection = new MatchSection(this, body, true);
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		matchSection.getSection().setLayoutData(gd);
		managedForm.addPart(matchSection);
		
		// add links
		Control links = createLinks(body, toolkit);
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		links.setLayoutData(gd);
	}

	private Control createLinks(Composite body, FormToolkit toolkit) {
		FormText formText = toolkit.createFormText(body, true);
		formText.setText(linksText, true, false);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		formText.setImage("search", lp.get(PDEPluginImages.DESC_PSEARCH_OBJ));
		formText.setImage("loops", lp.get(PDEPluginImages.DESC_LOOP_OBJ));
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("loops"))
					doFindLoops();
				else
					if (e.getHref().equals("plugins"))
						doFindPlugins();
					else if (e.getHref().equals("fragments"))
						doFindFragments();
			}
		});
		return formText;
	}
	private void doFindLoops() {
	}
	private void doFindPlugins() {
	}
	private void doFindFragments() {
	}
}