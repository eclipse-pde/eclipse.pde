/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.target;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;

public class TargetStateView extends PageBookView {

	public static final String VIEW_ID = "org.eclipse.pde.ui.TargetPlatformState"; //$NON-NLS-1$
	private Map<IPageBookViewPage, IWorkbenchPart> fPagesToParts;
	private Map<IWorkbenchPart, IPageBookViewPage> fPartsToPages;
	private IWorkbenchPart fPartState;

	static class DummyPart implements IWorkbenchPart {
		private IWorkbenchPartSite fSite;

		public DummyPart(IWorkbenchPartSite site) {
			fSite = site;
		}

		@Override
		public void addPropertyListener(IPropertyListener listener) {/* dummy */
		}

		@Override
		public void createPartControl(Composite parent) {/* dummy */
		}

		@Override
		public void dispose() {
			fSite = null;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public IWorkbenchPartSite getSite() {
			return fSite;
		}

		@Override
		public String getTitle() {
			return null;
		}

		@Override
		public Image getTitleImage() {
			return null;
		}

		@Override
		public String getTitleToolTip() {
			return null;
		}

		@Override
		public void removePropertyListener(IPropertyListener listener) {/* dummy */
		}

		@Override
		public void setFocus() {/* dummy */
		}
	}

	public TargetStateView() {
		fPartsToPages = new HashMap<>(4);
		fPagesToParts = new HashMap<>(4);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		fPartState = new DummyPart(site);
	}

	@Override
	public void dispose() {
		super.dispose();
		fPartState.dispose();
		fPartState = null;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.TARGET_STATE_VIEW);
	}

	@Override
	protected IPage createDefaultPage(PageBook book) {
		return createPage(getDefaultPart());
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		IPageBookViewPage page = fPartsToPages.get(part);
		if (page == null && !fPartsToPages.containsKey(part)) {
			page = createPage(part);
		}
		if (page != null) {
			return new PageRec(part, page);
		}
		return null;
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		IPage page = pageRecord.page;
		page.dispose();
		pageRecord.dispose();

		// empty cross-reference cache
		fPartsToPages.remove(part);
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		return getDefaultPart();
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof DummyPart;
	}

	/**
	 * part of the part constants
	 */
	private IPageBookViewPage createPage(IWorkbenchPart part) {
		IPageBookViewPage page = new StateViewPage(this);
		initPage(page);
		page.createControl(getPageBook());
		fPartsToPages.put(part, page);
		fPagesToParts.put(page, part);
		return page;
	}

	private IWorkbenchPart getDefaultPart() {
		return fPartState;
	}

}
