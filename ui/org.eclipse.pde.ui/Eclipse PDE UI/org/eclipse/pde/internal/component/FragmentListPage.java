package org.eclipse.pde.internal.component;

import java.util.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.wizards.*;


public class FragmentListPage extends WizardPage {
	public static final String PAGE_TITLE = "NewComponentWizard.FragPage.title";
	public static final String PAGE_DESC = "NewComponentWizard.FragPage.desc";
	private CheckboxTableViewer fragmentViewer;
	private Image fragmentImage;
	private FragmentReference [] references;
	
	public class FragmentReference {
		boolean checked;
		IFragmentModel model;
		public String toString() {
			return model.getFragment().getName();
		}
	}

	class FragmentContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object [] getElements(Object parent) {
			return createFragmentReferences();
		}
	}

	class FragmentLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index==0) {
				return obj.toString();
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			return fragmentImage;
		}
	}

public FragmentListPage() {
	super("fragmentListPage");
	fragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.verticalSpacing = 9;
	container.setLayout(layout);

	fragmentViewer = new CheckboxTableViewer(container, SWT.BORDER);
	fragmentViewer.setContentProvider(new FragmentContentProvider());
	fragmentViewer.setLabelProvider(new FragmentLabelProvider());
	fragmentViewer.setSorter(ListUtil.NAME_SORTER);

	fragmentViewer.addCheckStateListener(new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent e) {
			handleFragmentChecked((FragmentReference) e.getElement(), e.getChecked());
		}
	});
	GridData gd = new GridData(GridData.FILL_BOTH);
	fragmentViewer.getTable().setLayoutData(gd);
	fragmentViewer.setInput(PDEPlugin.getDefault().getWorkspaceModelManager());
	fragmentViewer.getTable().setFocus();
	setControl(container);
}
private Object[] createFragmentReferences() {
	if (references == null) {
		WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
		IFragmentModel[] fragmentModels = manager.getWorkspaceFragmentModels();
		references = new FragmentReference[fragmentModels.length];
		for (int i = 0; i < fragmentModels.length; i++) {
			IFragmentModel model = fragmentModels[i];
			FragmentReference reference = new FragmentReference();
			reference.model = model;
			references[i] = reference;
		}
	}
	return references;
}
public void dispose() {
	fragmentImage.dispose();
	super.dispose();
}
public IFragment[] getSelectedFragments() {
	Vector result = new Vector();
	if (references!=null && references.length>0) {
		for (int i=0; i<references.length; i++) {
			if (references[i].checked) {
				IFragment fragment = references[i].model.getFragment();
				result.add(fragment);
			}
		}
	}
	IFragment [] fragments = new IFragment[result.size()];
	result.copyInto(fragments);
	return fragments;
}
private void handleFragmentChecked(FragmentReference reference, boolean checked) {
	reference.checked = checked;
}
}
