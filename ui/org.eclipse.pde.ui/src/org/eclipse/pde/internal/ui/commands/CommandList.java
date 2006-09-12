package org.eclipse.pde.internal.ui.commands;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.commands.CommandComposerPart.IDialogButtonCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class CommandList {

	protected class CommandTreeLabelProvider extends LabelProvider {
		private Image fCatImage;
		private Image fComImage;
		public String getText(Object element) {
			if (element instanceof Category)
				return CommandList.getText(element);
			else if (element instanceof Command)
				return CommandList.getText(element);
			return null;
		}
		public Image getImage(Object element) {
			if (element instanceof Category) {
				if (fCatImage == null)
					fCatImage = PDEPluginImages.DESC_CATEGORY_OBJ.createImage();
				return fCatImage;
			} else if (element instanceof Command) {
				if (fComImage == null)
					fComImage = PDEPluginImages.DESC_CATEGORY_OBJ.createImage();
				return fComImage;
			}
			return super.getImage(element);
		}
		public void dispose() {
			if (fCatImage != null)
				fCatImage.dispose();
			if (fComImage != null)
				fComImage.dispose();
			super.dispose();
		}
	}
	
	protected class CommandTreeSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object e1, Object e2) {
			return getText(e1).compareTo(getText(e2));
		}
	}
	
	protected class WildcardFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			String wMatch = '*' + fFilterText.getText() + '*';
			Pattern pattern = null;
			try {
				pattern = PatternConstructor.createPattern(wMatch, false);
			} catch (PatternSyntaxException e) {
				return false;
			}
			if (element instanceof Category) {
				ITreeContentProvider prov = (ITreeContentProvider)fTreeViewer.getContentProvider();
				Command[] commands = (Command[])prov.getChildren(element);
				for (int i = 0; i < commands.length; i++) {
					String text = getText(commands[i]);
					if (pattern.matcher(text.subSequence(0, text.length())).matches())
						return true;
				}
				return false;
			}
			String text = getText(element);
			return pattern.matcher(text.subSequence(0, text.length())).matches();
		}
	}
	
	protected static String getText(Object obj) {
		if (obj instanceof Command) {
			Command com = (Command)obj;
			try {
				return com.getName() + ' ' + '(' + com.getId() + ')';
			} catch (NotDefinedException e) {
				return com.getId();
			}
		} else if (obj instanceof Category) {
			Category cat = (Category)obj;
			try {
				return cat.getName() + ' ' + '(' + cat.getId() + ')';
			} catch (NotDefinedException e) {
				return cat.getId();
			}
		}
		return new String();
	}
	
	private CommandComposerPart fCSP;
	private FormToolkit fToolkit;
	private Text fFilterText;
	private TreeViewer fTreeViewer;
	private CommandTreeContentProvider fContentProvider;

	public CommandList(CommandComposerPart cv, Composite parent, IDialogButtonCreator creator) {
		fCSP = cv;
		fToolkit = cv.getToolkit();
		createTree(parent, creator);
	}
	private void createTree(Composite parent, IDialogButtonCreator creator) {
		Group commandGroup = new Group(parent, SWT.NONE);
		commandGroup.setLayout(new GridLayout());
		commandGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		fToolkit.adapt(commandGroup);
		commandGroup.setText(PDEUIMessages.CommandList_groupName);
		
		createFilterText(commandGroup);
		
		Tree tree = fToolkit.createTree(commandGroup, SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLayout(new GridLayout());
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTreeViewer = new TreeViewer(tree);
		fContentProvider = new CommandTreeContentProvider(fCSP.getCommandService());
		fTreeViewer.setContentProvider(fContentProvider);
		fTreeViewer.setLabelProvider(new CommandTreeLabelProvider());
		fTreeViewer.setSorter(new CommandTreeSorter());
		fTreeViewer.addFilter(new WildcardFilter());
		fTreeViewer.setInput(new Object());
		fTreeViewer.addSelectionChangedListener(fCSP);
		fTreeViewer.addSelectionChangedListener(creator.getButtonEnablementListener());
	}
	
	private void createFilterText(Composite parent) {
		Composite c = fCSP.createComposite(parent, GridData.FILL_HORIZONTAL, 2, false);
		fFilterText = fToolkit.createText(c, "", SWT.BORDER); //$NON-NLS-1$
		fFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFilterText.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN) fTreeViewer.getControl().setFocus();
            }
        });
		
		ToolBar toolBar = new ToolBar(c, SWT.FLAT | SWT.HORIZONTAL);
		fToolkit.adapt(toolBar, true, true);
        ToolBarManager filterToolkbar = new ToolBarManager(toolBar);

        final IAction clearTextAction = new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$
            public void run() {
            	fFilterText.setText(""); //$NON-NLS-1$
            }
        };

        clearTextAction.setToolTipText(PDEUIMessages.CommandList_clearTooltip);
        clearTextAction.setImageDescriptor(PDEPluginImages.DESC_CLEAR);
        clearTextAction.setDisabledImageDescriptor(PDEPluginImages.DESC_DCLEAR);
        clearTextAction.setEnabled(false);
        
        filterToolkbar.add(clearTextAction);
        filterToolkbar.update(false);
        
        fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fTreeViewer.refresh();
				if (clearTextAction != null)
					clearTextAction.setEnabled(fFilterText.getText().length() > 0);
			}
        });
	}
	
	public void setFocus() {
		fFilterText.setFocus();
	}
}
