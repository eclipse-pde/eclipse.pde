/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.launcher;

import java.net.URL;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.SWT;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.events.*;

public class PluginPathDialog extends Dialog {

	private URL[] urls;
	private boolean blockResize;

	class URLContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return urls;
		}
	}

	class URLLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return ((URL) obj).toString();
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	public PluginPathDialog(Shell shell, URL[] urls) {
		super(shell);
		this.urls = urls;
		//setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	protected Control createDialogArea(Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = layout.marginHeight = 9;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);
		
		final Label label = new Label(container, SWT.WRAP);
		label.setText(
		"Based on selections in the launcher, the following plug-ins and/or fragments will be passed to the run-time workbench instance:");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		Point lsize = label.computeSize(container.getSize().x, SWT.DEFAULT, true);
		gd.heightHint = lsize.y;
		label.setLayoutData(gd);
		
		container.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (blockResize) return;
				int width = container.getSize().x;
				width -= ((GridLayout)container.getLayout()).marginWidth * 2;
				Point lsize = label.computeSize(width, SWT.DEFAULT, true);
				GridData gd = (GridData)label.getLayoutData();
				gd.heightHint = lsize.y;
				blockResize = true;
				container.layout();
				blockResize = false;
			}
		});
		
		TableViewer viewer = new TableViewer(container);
		viewer.setContentProvider(new URLContentProvider());
		viewer.setLabelProvider(new URLLabelProvider());

		gd = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(gd);
		viewer.setInput(PDEPlugin.getDefault());
		return container;
	}
}