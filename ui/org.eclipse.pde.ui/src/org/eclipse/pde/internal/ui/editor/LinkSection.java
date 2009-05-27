/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.pde.internal.ui.parts.ILinkLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;

/**
 * This class can be used to show a standard section with an array of links.
 * Links are objects from editor pages, and each one will select the owning
 * page and reveal the element in it. If the number of objects from the content
 * provider is greated than the preset limit, only the first 'limit' number of
 * links will be shown, and a 'More...' button will show up (this is a change
 * from 2.1 where 'More...' was visible all the time).
 */
public class LinkSection extends PDESection {
	private ILinkLabelProvider labelProvider;
	private IStructuredContentProvider contentProvider;
	private Composite linkContainer;
	private Composite container;
	private Button moreButton;
	private String morePageId;
	private int linkNumberLimit = 20;
	private LinkHandler linkHandler;

	class LinkHandler implements IHyperlinkListener {
		public void linkActivated(HyperlinkEvent e) {
			doLinkActivated((Hyperlink) e.widget);
		}

		public void linkEntered(HyperlinkEvent e) {
			doEnter((Hyperlink) e.widget);
		}

		public void linkExited(HyperlinkEvent e) {
			doExit((Hyperlink) e.widget);
		}
	}

	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public LinkSection(PDEFormPage page, Composite parent, int style) {
		super(page, parent, style);
		FormToolkit toolkit = page.getManagedForm().getToolkit();
		linkHandler = new LinkHandler();
		createClient(getSection(), toolkit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		section.setClient(container);
		linkContainer = toolkit.createComposite(container);
		linkContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout linkLayout = new GridLayout();
		linkLayout.marginWidth = 0;
		linkLayout.marginHeight = 0;
		linkLayout.verticalSpacing = 0;
		linkContainer.setLayout(linkLayout);
	}

	private void createMoreButton() {
		moreButton = getManagedForm().getToolkit().createButton(container, "More...", //$NON-NLS-1$
				SWT.PUSH);
		moreButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator.showWhile(getSection().getDisplay(), new Runnable() {
					public void run() {
						getPage().getEditor().setActivePage(morePageId);
					}
				});
			}
		});
		moreButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
	}

	public void add(Object[] links) {
		for (int i = 0; i < links.length; i++) {
			createLink(links[i]);
		}
		updateMoreState(linkContainer.getChildren().length > linkNumberLimit);
		reflow();
	}

	public void remove(Object[] links) {
		for (int i = 0; i < links.length; i++) {
			disposeLink(links[i]);
		}
		updateMoreState(linkContainer.getChildren().length > linkNumberLimit);
		reflow();
	}

	private void disposeLink(Object obj) {
		Hyperlink link = find(obj);
		if (link != null)
			link.dispose();
	}

	private Hyperlink find(Object object) {
		Control[] children = linkContainer.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			if (child.getData().equals(object))
				return (Hyperlink) child;
		}
		return null;
	}

	public void update(Object[] links) {
		for (int i = 0; i < links.length; i++) {
			update(links[i]);
		}
		reflow();
	}

	private void update(Object object) {
		Hyperlink link = find(object);
		if (link != null)
			update(link, object);
	}

	private void update(Hyperlink hyperlink, Object object) {
		String text = labelProvider != null ? labelProvider.getText(object) : object.toString();
		Image image = labelProvider != null ? labelProvider.getImage(object) : null;
		String tooltip = labelProvider != null ? labelProvider.getToolTipText(object) : text;
		hyperlink.setText(text);
		hyperlink.setToolTipText(tooltip);
		if (hyperlink instanceof ImageHyperlink)
			((ImageHyperlink) hyperlink).setImage(image);
		reflow();
	}

	public void refresh() {
		// dispose old links
		Control[] children = linkContainer.getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].dispose();
		}
		createLinks();
		reflow();
	}

	private void reflow() {
		linkContainer.layout();
		container.layout();
		getManagedForm().reflow(true);
	}

	private void createLinks() {
		if (contentProvider == null)
			return;
		Object[] objects = contentProvider.getElements(getManagedForm().getInput());
		for (int i = 0; i < objects.length; i++) {
			if (i == linkNumberLimit)
				break;
			createLink(objects[i]);
		}
		if (objects.length > linkNumberLimit)
			getManagedForm().getToolkit().createLabel(linkContainer, "...", SWT.NULL); //$NON-NLS-1$
		updateMoreState(objects.length > linkNumberLimit);
	}

	private void updateMoreState(boolean needMore) {
		if (needMore && moreButton == null) {
			createMoreButton();
		} else if (!needMore && moreButton != null) {
			moreButton.dispose();
			moreButton = null;
		}
	}

	private void createLink(Object object) {
		Image image = labelProvider != null ? labelProvider.getImage(object) : null;
		Hyperlink hyperlink;
		if (image != null) {
			hyperlink = getManagedForm().getToolkit().createImageHyperlink(linkContainer, SWT.NULL);
			((ImageHyperlink) hyperlink).setImage(image);
		} else
			hyperlink = getManagedForm().getToolkit().createHyperlink(linkContainer, null, SWT.NULL);
		update(hyperlink, object);
		hyperlink.setData(object);
		hyperlink.addHyperlinkListener(linkHandler);
	}

	private void doEnter(Hyperlink link) {
		String statusText = labelProvider != null ? labelProvider.getStatusText(link.getData()) : link.getText();
		getPage().getEditorSite().getActionBars().getStatusLineManager().setMessage(statusText);
	}

	private void doExit(Hyperlink link) {
		getPage().getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
	}

	protected void doLinkActivated(Hyperlink link) {
		Object object = link.getData();
		getPage().getEditor().setActivePage(morePageId, object);
	}

	public void setMorePageId(String id) {
		this.morePageId = id;
	}

	public void setLinkNumberLimit(int limit) {
		this.linkNumberLimit = limit;
	}

	public void setContentProvider(IStructuredContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	public void setLabelProvider(ILinkLabelProvider provider) {
		this.labelProvider = provider;
	}
}
