/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.neweditor.PDEFormPage;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class DependenciesPage extends PDEFormPage {
	public static final String PAGE_ID = "dependencies";
	private RequiresSection requiresSection;
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
		requiresSection = new RequiresSection(this, body);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 3;
		requiresSection.getSection().setLayoutData(gd);
		managedForm.addPart(requiresSection);
		//Add match
		MatchSection matchSection = new MatchSection(this, body, true);
		gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		matchSection.getSection().setLayoutData(gd);
		managedForm.addPart(matchSection);
		if (getModel().isEditable()) {
			DependencyAnalysisSection analysisSection = new DependencyAnalysisSection(
					this, body);
			gd = new GridData(GridData.FILL_HORIZONTAL
					| GridData.VERTICAL_ALIGN_BEGINNING);
			analysisSection.getSection().setLayoutData(gd);
			managedForm.addPart(analysisSection);
		}
	}
	public void contextMenuAboutToShow(IMenuManager manager) {
		IResource resource = ((IPluginModelBase) getModel())
				.getUnderlyingResource();
		if (resource != null
				&& WorkspaceModelManager.isJavaPluginProject(resource
						.getProject())) {
			manager.add(requiresSection.getBuildpathAction());
			manager.add(new Separator());
		}
	}
}