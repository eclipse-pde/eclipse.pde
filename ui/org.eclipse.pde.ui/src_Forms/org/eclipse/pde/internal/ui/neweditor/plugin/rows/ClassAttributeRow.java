/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin.rows;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.manifest.*;
import org.eclipse.pde.internal.ui.neweditor.IContextPart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ClassAttributeRow extends ReferenceAttributeRow {
	public ClassAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#openReference()
	 */
	protected void openReference() {
		String name = text.getText();
		IProject project = part.getPage().getPDEEditor().getCommonProject();
		IJavaProject javaProject = JavaCore.create(project);
		String path = name.replace('.', '/') + ".java";
		try {
			IJavaElement result = javaProject.findElement(new Path(path));
			if (result != null) {
				JavaUI.openInEditor(result);
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#browse()
	 */
	protected void browse() {
		BusyIndicator.showWhile(text.getDisplay(), new Runnable() {
			public void run() {
				JavaAttributeValue value = createJavaAttributeValue();
				JavaAttributeWizard wizard = new JavaAttributeWizard(value);
				WizardDialog dialog = new WizardDialog(PDEPlugin
						.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 500);
				int result = dialog.open();
				if (result == WizardDialog.OK) {
					String newValue = wizard.getClassName();
					text.setText(newValue);
					//markDirty();
				}
			}
		});
	}
	private JavaAttributeValue createJavaAttributeValue() {
		IProject project = part.getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) part.getPage().getModel();
		String value = text.getText();
		return new JavaAttributeValue(project, model, att, value);
	}
}