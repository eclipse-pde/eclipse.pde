/*******************************************************************************
 *  Copyright (c) 2012, 2018 Christian Pontesegger and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *     Alena Laskavaia - Bug 481613 pagination controls
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.*;
import java.util.List;
import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.views.imagebrowser.filter.*;
import org.eclipse.pde.internal.ui.views.imagebrowser.filter.IFilter;
import org.eclipse.pde.internal.ui.views.imagebrowser.repositories.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

/**
 * Provides the PDE Image browser view which displays all icons and images from plug-ins.  The plug-ins
 * can be loaded from the target platform, the workspace or the current install.
 */
public class ImageBrowserView extends ViewPart implements IImageTarget {

	private static final String COMMAND_SAVE_TO_WORKSPACE = "org.eclipse.pde.ui.imagebrowser.saveToWorkspace"; //$NON-NLS-1$
	protected final static String VIEW_ID = "org.eclipse.pde.ui.ImageBrowserView"; //$NON-NLS-1$

	private final UpdateUI mUIJob = new UpdateUI();

	private final List<IFilter> mFilters = new ArrayList<>();
	private final IFilter disabledIcons;
	private final IFilter enabledIcons;
	private final IFilter wizard;

	private ScrolledComposite scrolledComposite;
	private Composite imageComposite;
	private ComboViewer sourceCombo;
	private int maxImages; // Number of images per page
	private int page; // Zero based page number
	private int imageIndex; // Zero base index of currently available image
	private Label lblPlugin;
	private Label lblPath;
	private Label lblWidth;
	private Label lblHeight;
	private Text txtReference;
	private Spinner spinMaxImages;

	private List<Image> displayedImages = new ArrayList<>();

	private AbstractRepository repository;

	private Text txtFilter;
	private IFilter textPatternFilter;
	private PageNavigationControl pageNavigationControl;

	private ImageElement imageElement;

	private Action saveAction;

	public ImageBrowserView() {
		// create default filters
		final IFilter iconSize = new SizeFilter(16, SizeFilter.TYPE_EXACT, 16, SizeFilter.TYPE_EXACT);
		final IFilter disabled1 = new PatternFilter(".*/obj16/\\w+dis(_obj)?\\W.+"); //$NON-NLS-1$
		final IFilter disabled2 = new PatternFilter(".*/d(?!ialogs)(?!ecorations)(?!nd)(?!evguide)\\w+/.+"); //$NON-NLS-1$
		final IFilter disabled = new OrFilter(new IFilter[] {disabled1, disabled2});
		disabledIcons = new AndFilter(new IFilter[] {iconSize, disabled});

		final IFilter enabled = new NotFilter(disabled);
		enabledIcons = new AndFilter(new IFilter[] {iconSize, enabled});

		final IFilter wizardSize = new SizeFilter(75, SizeFilter.TYPE_EXACT, 66, SizeFilter.TYPE_EXACT);
		final IFilter wizard1 = new PatternFilter(".*/wizban/.+"); //$NON-NLS-1$
		final IFilter wizard2 = new PatternFilter(".+_wiz\\.\\w+"); //$NON-NLS-1$
		final IFilter wizardName = new OrFilter(new IFilter[] {wizard1, wizard2});
		wizard = new AndFilter(new IFilter[] {wizardSize, wizardName});

		mFilters.add(enabledIcons);
		textPatternFilter = new StringFilter("*"); //$NON-NLS-1$
		mFilters.add(textPatternFilter);
	}

	@Override
	public void createPartControl(final Composite parent) {
		final Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.IMAGE_BROWSER_VIEW);
		composite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		Composite topComp = new Composite(composite, SWT.NONE);
		RowLayout layout = new RowLayout();
		// need to center vertically, otherwise its looks misaligned
		layout.center = true;
		topComp.setLayout(layout);
		topComp.setFont(parent.getFont());
		topComp.setBackground(composite.getBackground());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		topComp.setLayoutData(gd);

		Composite sourceComp = SWTFactory.createComposite(topComp, 2, 1, SWT.NONE, 0, 0);
		sourceComp.setLayoutData(new RowData());
		SWTFactory.createLabel(sourceComp, PDEUIMessages.ImageBrowserView_Source, 1);
		sourceCombo = new ComboViewer(SWTFactory.createCombo(sourceComp, SWT.READ_ONLY, 1, null));
		sourceCombo.setContentProvider(ArrayContentProvider.getInstance());
		sourceCombo.addSelectionChangedListener(event -> {
			page = 0; // reset to 1st page
			scanImages();
		});

		ArrayList<Object> sourceComboInput = new ArrayList<>();
		sourceComboInput.add(new TargetPlatformRepository(this, true));
		sourceComboInput.add(new TargetPlatformRepository(this, false));
		sourceComboInput.add(new WorkspaceRepository(this));
		sourceCombo.setInput(sourceComboInput);



		// image type
		Composite typeComp = SWTFactory.createComposite(topComp, 2, 1, SWT.NONE, 0, 0);
		typeComp.setLayoutData(new RowData());
		SWTFactory.createLabel(typeComp, PDEUIMessages.ImageBrowserView_Show, 1);
		Combo typeCombo = SWTFactory.createCombo(typeComp, SWT.READ_ONLY, 1,
				new String[] { PDEUIMessages.ImageBrowserView_FilterIcons,
						PDEUIMessages.ImageBrowserView_FilterDisabled, PDEUIMessages.ImageBrowserView_FilterWizards,
						PDEUIMessages.ImageBrowserView_FilterAllImages });
		typeCombo.addSelectionListener(widgetSelectedAdapter(e -> {
			mFilters.clear();
			mFilters.add(textPatternFilter);
			Combo source = (Combo) e.getSource();
			switch (source.getSelectionIndex()) {
				case 0 :
					mFilters.add(enabledIcons);
					break;
				case 1 :
					mFilters.add(disabledIcons);
					break;
				case 2 :
					mFilters.add(wizard);
					break;
				case 3 :
				default :
			}
			page = 0; // reset to 1st page
			scanImages();
		}));

		// max images
		Composite maxComp = SWTFactory.createComposite(topComp, 2, 1, SWT.NONE, 0, 0);
		maxComp.setLayoutData(new RowData());
		SWTFactory.createLabel(maxComp, PDEUIMessages.ImageBrowserView_MaxImages, 1);
		spinMaxImages = new Spinner(maxComp, SWT.BORDER);
		spinMaxImages.setMaximum(999);
		spinMaxImages.setMinimum(1);
		spinMaxImages.setSelection(250);
		spinMaxImages.setLayoutData(GridDataFactory.fillDefaults().create());
		spinMaxImages.addModifyListener(e -> {
			page = 0; // reset to 1st page
			scanImages();
		});

		Composite filterComp = SWTFactory.createComposite(topComp, 2, 1, SWT.NONE, 0, 0);
		filterComp.setLayoutData(new RowData());
		SWTFactory.createLabel(filterComp, PDEUIMessages.ImageBrowserView_FilterText, 1)
				.setToolTipText(PDEUIMessages.ImageBrowserView_FilterTooltip);
		txtFilter = SWTFactory.createText(filterComp, SWT.BORDER | SWT.SEARCH, 1);
		((GridData) txtFilter.getLayoutData()).widthHint = 200;
		txtFilter.setToolTipText(PDEUIMessages.ImageBrowserView_FilterTooltip);
		txtFilter.addModifyListener(e -> {
			String pattern = txtFilter.getText();
			pattern = pattern.trim();
			// we match begging and end, user does not have to type *debug*
			String STAR = "*"; //$NON-NLS-1$
			if (!pattern.startsWith(STAR)) {
				pattern = STAR + pattern;
			}
			if (!pattern.endsWith(STAR)) {
				pattern += STAR;
			}
			mFilters.remove(textPatternFilter);
			textPatternFilter = new StringFilter(pattern);
			mFilters.add(textPatternFilter);
			page = 0; // reset to 1st page
			scanImages();
		});

		scrolledComposite = new ScrolledComposite(composite, SWT.BORDER | SWT.V_SCROLL);
		scrolledComposite.setBackground(scrolledComposite.getParent().getBackground());
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		scrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				Rectangle r = scrolledComposite.getClientArea();
				scrolledComposite.setMinSize(imageComposite.computeSize(r.width, SWT.DEFAULT));
			}
		});
		imageComposite = SWTFactory.createComposite(scrolledComposite, 1, 1, GridData.FILL_BOTH, 0, 0);
		((GridLayout) imageComposite.getLayout()).verticalSpacing = 0;
		imageComposite.setBackground(imageComposite.getParent().getBackground());
		scrolledComposite.setContent(imageComposite);
		scrolledComposite.setMinSize(imageComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		Composite infoGroup = SWTFactory.createComposite(composite, 4, 1, GridData.FILL_HORIZONTAL, 0, 0);
		((GridLayout) infoGroup.getLayout()).verticalSpacing = 0;

		SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Path, 1);
		lblPath = new Label(infoGroup, SWT.NONE);
		lblPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Width, 1);
		lblWidth = new Label(infoGroup, SWT.NONE);
		final GridData gd_lblWidth = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblWidth.widthHint = 50;
		lblWidth.setLayoutData(gd_lblWidth);

		SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Plugin, 1);
		lblPlugin = new Label(infoGroup, SWT.NONE);
		lblPlugin.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Height, 1);
		lblHeight = new Label(infoGroup, SWT.NONE);
		final GridData gd_lblHeight = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblHeight.widthHint = 50;
		lblHeight.setLayoutData(gd_lblHeight);

		SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Reference, 1);
		txtReference = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
		txtReference.setBackground(txtReference.getParent().getBackground());
		txtReference.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		// set original selection
		sourceCombo.setSelection(new StructuredSelection(sourceComboInput.get(0)), true);
		ImageDescriptor image = PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT);
		saveAction = new Action(PDEUIMessages.ImageBrowserView_SaveActionName, image) {

			@Override
			public void run() {
				IHandlerService handlerService = getSite().getService(IHandlerService.class);
				try {
					handlerService.executeCommand(COMMAND_SAVE_TO_WORKSPACE, null);
				} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
					PDEPlugin.log(e);
				}
			}
		};
		saveAction.setEnabled(false);
		getViewSite().getActionBars().getToolBarManager().add(saveAction);

	}

	/*-
	 * Control for page navigation, looks like this
	 *
	 *
	 *                           Previous Page 1 2 3 4 5 Next Page
	 *
	 */
	class PageNavigationControl extends Composite {
		public PageNavigationControl(Composite parent, int style) {
			super(parent, style);
			setBackground(parent.getBackground());
			RowLayoutFactory.fillDefaults().extendedMargins(0, 0, 50, 5).applyTo(this);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.END).grab(true, true).applyTo(this);
			Color hyperlinkColor = getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND);
			// Previous Page link
			Hyperlink prev = new Hyperlink(this, SWT.NONE);
			prev.setText(PDEUIMessages.ImageBrowserView_PrevPage);
			prev.setForeground(hyperlinkColor);
			prev.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					if (page > 0)
						page--;
					scanImages();
				}
			});
			prev.setBackground(getBackground());
			if (page == 0)
				prev.setEnabled(false);
			// Links to individual pages
			int currentlyAvailable = imageIndex + 1;
			int curPage = page + 1;
			int maxPage = curPage;
			boolean lastPage = currentlyAvailable <= curPage * maxImages;
			if (!lastPage)
				maxPage = Math.max(curPage + 1, 5);
			// we only show link for 5 last pages
			int start = Math.max(maxPage - 5 + 1, 1);
			for (int i = start; i <= maxPage; i++) {
				Hyperlink pageLink = new Hyperlink(this, SWT.NONE);
				pageLink.setText(String.valueOf(i));
				pageLink.setBackground(pageLink.getParent().getBackground());
				if (i != curPage) {
					final int selectedPage = i;
					pageLink.setForeground(hyperlinkColor);
					pageLink.addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							page = selectedPage - 1;
							scanImages();
						}
					});
				}

			}
			// Next Page Link
			Hyperlink next = new Hyperlink(this, SWT.NONE);
			next.setText(PDEUIMessages.ImageBrowserView_NextPage);
			next.setForeground(hyperlinkColor);
			next.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					page++;
					scanImages();
				}
			});
			next.setBackground(getBackground());
			if (lastPage)
				next.setEnabled(false);

		}
	}

	@Override
	public void setFocus() {
		scrolledComposite.getParent().setFocus();
	}

	@Override
	public synchronized void notifyImage(final ImageElement element) {
		// make a copy of filter to avoid concurrent modification exception since UI changes mFilters list
		ArrayList<IFilter> filters = new ArrayList<>(mFilters);
		for (final IFilter filter : filters) {
			if (!filter.accept(element))
				return;
		}
		if (imageIndex >= page * maxImages && imageIndex < (page + 1) * maxImages)
			mUIJob.addImage(element);
		imageIndex++;
	}

	@Override
	public boolean needsMore() {
		// we will request at least one more image that we can currently show to
		// adjust page controls properly
		int requestedImages = (page + 1) * maxImages + 1;
		int currentlyAvailable = imageIndex + 1;
		return currentlyAvailable < requestedImages;
	}

	private void scanImages() {
		if (repository != null) {
			repository.cancel();
		}

		// reset UI components
		mUIJob.reset();
		lblPath.setText(""); //$NON-NLS-1$
		lblPlugin.setText(""); //$NON-NLS-1$
		lblWidth.setText(""); //$NON-NLS-1$
		lblHeight.setText(""); //$NON-NLS-1$
		txtReference.setText(""); //$NON-NLS-1$

		// first dispose controls
		for (final Control control : imageComposite.getChildren()) {
			control.dispose();
		}

		// then dispose images used in controls
		disposeImages();

		// set maximum image counter
		maxImages = spinMaxImages.getSelection();
		imageIndex = 0;
		// initialize scan job
		IStructuredSelection ssel = sourceCombo.getStructuredSelection();
		if (!ssel.isEmpty()) {
			repository = (AbstractRepository) ssel.getFirstElement();
			repository.schedule();
		}
	}

	@Override
	public void dispose() {
		disposeImages();
	}

	private void disposeImages() {
		for (Image image : displayedImages) {
			image.dispose();
		}
		displayedImages.clear();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (ImageElement.class.equals(adapter)) {
			return adapter.cast(imageElement);
		}
		return super.getAdapter(adapter);
	}

	private class UpdateUI extends FocusAdapter implements Runnable, SelectionListener {

		Collection<ImageElement> mElements = new LinkedList<>();
		String mLastPlugin = ""; //$NON-NLS-1$
		private Composite mPluginImageContainer = null;
		private final RowLayout mRowLayout = new RowLayout(SWT.HORIZONTAL);

		public UpdateUI() {
			mRowLayout.wrap = true;
			mRowLayout.marginWidth = 0;
			mRowLayout.marginHeight = 0;
		}

		public synchronized void addImage(final ImageElement element) {
			mElements.add(element);

			if (mElements.size() == 1)
				Display.getDefault().asyncExec(this);
		}

		@Override
		public synchronized void run() {

			if (!mElements.isEmpty()) {
				for (final ImageElement element : mElements) {
					if (!mLastPlugin.equals(element.getPlugin())) {
						// new plug-in detected
						mLastPlugin = element.getPlugin();
						Label label = new Label(imageComposite, SWT.NONE);
						label.setText(mLastPlugin);
						label.setBackground(label.getParent().getBackground());

						if (mPluginImageContainer != null)
							mPluginImageContainer.layout();

						mPluginImageContainer = new Composite(imageComposite, SWT.NONE);
						mPluginImageContainer.setLayout(mRowLayout);
						mPluginImageContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
						mPluginImageContainer.setBackground(mPluginImageContainer.getParent().getBackground());
					}

					Button button = new Button(mPluginImageContainer, SWT.FLAT);
					Image image = new Image(getViewSite().getShell().getDisplay(), element.getImageData());
					displayedImages.add(image);
					button.setImage(image);
					button.setToolTipText(element.getPath());
					button.setData(element);
					button.addFocusListener(this);
					button.addSelectionListener(this);
				}

				mElements.clear();

				mPluginImageContainer.layout();

				if (pageNavigationControl!=null)
					pageNavigationControl.dispose();
				pageNavigationControl = new PageNavigationControl(imageComposite, SWT.NONE);
				pageNavigationControl.setBackground(imageComposite.getBackground());

				imageComposite.layout();

				Rectangle r = scrolledComposite.getClientArea();
				scrolledComposite.setMinSize(imageComposite.computeSize(r.width, SWT.DEFAULT));
			}
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			handleEvent(e);
		}

		@Override
		public void focusGained(FocusEvent e) {
			handleEvent(e);
		}

		private void handleEvent(TypedEvent e) {
			// Scroll the focused control into view
			Control child = (Control) e.widget;
			Rectangle bounds = child.getBounds();
			// Because we store the buttons in an additional composite, need to adjust bounds
			Rectangle pluginBounds = child.getParent().getBounds();
			bounds.x += pluginBounds.x;
			bounds.y += pluginBounds.y;
			Rectangle area = scrolledComposite.getClientArea();
			Point origin = scrolledComposite.getOrigin();
			if (origin.x > bounds.x)
				origin.x = Math.max(0, bounds.x);
			if (origin.y > bounds.y)
				origin.y = Math.max(0, bounds.y);
			if (origin.x + area.width < bounds.x + bounds.width)
				origin.x = Math.max(0, bounds.x + bounds.width - area.width);
			if (origin.y + area.height < bounds.y + bounds.height)
				origin.y = Math.max(0, bounds.y + bounds.height - area.height);
			scrolledComposite.setOrigin(origin);

			final Object data = e.widget.getData();
			if (data instanceof ImageElement) {

				lblPath.setText(((ImageElement) data).getPath());
				lblPlugin.setText(((ImageElement) data).getFullPlugin());
				txtReference.setText("platform:/plugin/" + ((ImageElement) data).getPlugin() + "/" + ((ImageElement) data).getPath()); //$NON-NLS-1$ //$NON-NLS-2$

				lblWidth.setText(NLS.bind(PDEUIMessages.ImageBrowserView_Pixels, Integer.toString(((ImageElement) data).getImageData().width)));
				lblHeight.setText(NLS.bind(PDEUIMessages.ImageBrowserView_Pixels, Integer.toString(((ImageElement) data).getImageData().height)));

				imageElement = (ImageElement) data;
				saveAction.setEnabled(true);
				getViewSite().getActionBars().getToolBarManager().update(true);
			}
		}

		public synchronized void reset() {
			mLastPlugin = ""; //$NON-NLS-1$
			mElements.clear();
			mPluginImageContainer = null;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// TODO Auto-generated method stub

		}
	}
}
