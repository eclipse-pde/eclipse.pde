/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.commands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;

public class CommandList {

	protected class CommandTreeLabelProvider extends LabelProvider {
		private HashMap fImgMap = new HashMap();
		private Image fDefaultImage;

		public String getText(Object element) {
			if (element instanceof Category)
				return CommandList.getText(element);
			else if (element instanceof Command)
				return CommandList.getText(element);
			return null;
		}

		public Image getImage(Object element) {
			Image img = (Image) fImgMap.get(element);
			if (img != null)
				return img;

			if (element instanceof Category)
				img = PDEPluginImages.DESC_COMGROUP_OBJ.createImage();
			else if (element instanceof Command) {
				ImageDescriptor desc = fComImgServ.getImageDescriptor(((Command) element).getId());
				if (desc == null) {
					if (fDefaultImage == null)
						fDefaultImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
					return fDefaultImage;
				}
				img = desc.createImage();
			}

			if (img != null)
				fImgMap.put(element, img);

			return img;
		}

		public void dispose() {
			for (Iterator i = fImgMap.keySet().iterator(); i.hasNext();)
				((Image) fImgMap.get(i.next())).dispose();
			if (fDefaultImage != null)
				fDefaultImage.dispose();
			super.dispose();
		}
	}

	protected class CommandTreeComparator extends ViewerComparator {
		public int compare(Viewer viewer, Object e1, Object e2) {
			return getText(e1).compareTo(getText(e2));
		}
	}

	protected class WildcardFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			String filterText = fFilterText.getText();
			if (filterText.length() == 0)
				return true;
			String wMatch = '*' + filterText + '*';
			Pattern pattern = null;
			try {
				pattern = PatternConstructor.createPattern(wMatch, false);
			} catch (PatternSyntaxException e) {
				return false;
			}
			if (element instanceof Category) {
				ITreeContentProvider prov = (ITreeContentProvider) fTreeViewer.getContentProvider();
				Command[] commands = (Command[]) prov.getChildren(element);
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
			Command com = (Command) obj;
			try {
				return com.getName();
			} catch (NotDefinedException e) {
				return com.getId();
			}
		} else if (obj instanceof Category) {
			Category cat = (Category) obj;
			try {
				return cat.getName();
			} catch (NotDefinedException e) {
				return cat.getId();
			}
		}
		return new String();
	}

	private class CollapseAction extends Action {
		public CollapseAction() {
			super(PDEUIMessages.CommandList_collapseAll0, IAction.AS_PUSH_BUTTON);
			setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
			setToolTipText(PDEUIMessages.CommandList_collapseAll0);
		}

	}

	private CommandComposerPart fCCP;
	private FormToolkit fToolkit;
	private Text fFilterText;
	private TreeViewer fTreeViewer;
	private CommandTreeContentProvider fContentProvider;
	private ICommandImageService fComImgServ;

	public CommandList(CommandComposerPart cv, Composite parent) {
		fCCP = cv;
		fToolkit = cv.getToolkit();
		createTree(parent);
		fComImgServ = (ICommandImageService) PlatformUI.getWorkbench().getAdapter(ICommandImageService.class);
	}

	private void createTree(Composite parent) {
		Composite c = fCCP.createComposite(parent, GridData.FILL_BOTH, 1, true, 5);

		Section section = fToolkit.createSection(c, ExpandableComposite.TITLE_BAR);
		section.setText(PDEUIMessages.CommandList_groupName);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite comp = fCCP.createComposite(section);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		toolBarManager.add(new CollapseAction() {
			public void run() {
				fTreeViewer.collapseAll();
			}
		});
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				handCursor.dispose();
			}
		});
		fToolkit.adapt(toolbar, true, true);
		toolbar.setBackground(null);
		section.setTextClient(toolbar);
		toolBarManager.update(true);

		createFilterText(comp);

		Tree tree = fToolkit.createTree(comp, SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		tree.setLayoutData(gd);
		fTreeViewer = new TreeViewer(tree);
		fContentProvider = new CommandTreeContentProvider(fCCP.getCommandService());
		fTreeViewer.setContentProvider(fContentProvider);
		fTreeViewer.setLabelProvider(new CommandTreeLabelProvider());
		fTreeViewer.setComparator(new CommandTreeComparator());
		fTreeViewer.addFilter(new WildcardFilter());
		fTreeViewer.setInput(new Object());
		fTreeViewer.addSelectionChangedListener(fCCP);

		section.setClient(comp);
	}

	protected void addTreeSelectionListener(ISelectionChangedListener listener) {
		if (listener != null)
			fTreeViewer.addSelectionChangedListener(listener);
	}

	private void createFilterText(Composite parent) {
		Composite c = fCCP.createComposite(parent, GridData.FILL_HORIZONTAL, 3, false, 0);
		fFilterText = fToolkit.createText(c, "", SWT.BORDER); //$NON-NLS-1$
		fFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFilterText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN)
					fTreeViewer.getControl().setFocus();
			}
		});

		final ImageHyperlink clearButton = fToolkit.createImageHyperlink(c, SWT.NONE);
		final Image hoverImg = PDEPluginImages.DESC_CLEAR.createImage();
		clearButton.setImage(hoverImg);
		clearButton.setToolTipText(PDEUIMessages.CommandList_clearTooltip);
		clearButton.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				hoverImg.dispose();
			}
		});
		clearButton.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				fFilterText.setText(""); //$NON-NLS-1$
			}
		});
		clearButton.setVisible(false);

		fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fTreeViewer.refresh();
				clearButton.setVisible(fFilterText.getText().length() > 0);
			}
		});
	}

	public void setFocus() {
		fFilterText.setFocus();
	}

	protected void setSelection(Object object) {
		if (fTreeViewer != null && object != null)
			fTreeViewer.setSelection(new StructuredSelection(object));
	}

	public ISelection getSelection() {
		return fTreeViewer.getSelection();
	}

}
