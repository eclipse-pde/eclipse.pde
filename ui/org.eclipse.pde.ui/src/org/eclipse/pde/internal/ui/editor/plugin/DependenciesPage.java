/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.editor.context.IInputContextListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class DependenciesPage extends PDEFormPage implements IInputContextListener {
	public static final String PAGE_ID = "dependencies"; //$NON-NLS-1$
	private RequiresSection requiresSection;
	private MatchSection matchSection;
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public DependenciesPage(FormEditor editor) {
		super(editor, PAGE_ID, "Dependencies"); //$NON-NLS-1$
	}
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEPlugin.getResourceString("DependenciesPage.title")); //$NON-NLS-1$
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 10;
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
		matchSection = new MatchSection(this, body, true);
		gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		matchSection.getSection().setLayoutData(gd);
		managedForm.addPart(matchSection);
		matchSection.setOsgiMode(isOsgiMode());
		
		DependencyAnalysisSection analysisSection = new DependencyAnalysisSection(
				this, body);
		gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		analysisSection.getSection().setLayoutData(gd);
		managedForm.addPart(analysisSection);
		InputContextManager contextManager = getPDEEditor().getContextManager();
		contextManager.addInputContextListener(this);
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
	private boolean isOsgiMode() {
		return (getModel() instanceof IBundlePluginModelBase);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextAdded(InputContext context) {
		if (context.getId().equals(BundleInputContext.CONTEXT_ID))
			matchSection.setOsgiMode(true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextRemoved(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextRemoved(InputContext context) {
		if (context.getId().equals(BundleInputContext.CONTEXT_ID))
			matchSection.setOsgiMode(false);

	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileAdded(org.eclipse.core.resources.IFile)
	 */
	public void monitoredFileAdded(IFile monitoredFile) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return false;
	}
}