/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser;

import java.util.*;
import java.util.List;
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
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;

/**
 * Provides the PDE Image browser view which displays all icons and images from plug-ins.  The plug-ins
 * can be loaded from the target platform, the workspace or the current install.
 */
public class ImageBrowserView extends ViewPart implements IImageTarget {

	private final UpdateUI mUIJob = new UpdateUI();

	private final List<IFilter<ImageElement>> mFilters = new ArrayList<IFilter<ImageElement>>();
	private final IFilter<ImageElement> disabledIcons;
	private final IFilter<ImageElement> enabledIcons;
	private final IFilter<ImageElement> wizard;

	private ScrolledComposite scrolledComposite;
	private Composite imageComposite;
	private ComboViewer sourceCombo;
	int mImageCounter = 0;
	private Label lblPlugin;
	private Label lblPath;
	private Label lblWidth;
	private Label lblHeight;
	private Text txtReference;
	private Button nextButton;
	private Spinner spinMaxImages;

	private List<Image> displayedImages = new ArrayList<Image>();

	@SuppressWarnings("unchecked")
	public ImageBrowserView() {
		// create default filters
		final IFilter<ImageElement> iconSize = new SizeFilter(16, SizeFilter.TYPE_EXACT, 16, SizeFilter.TYPE_EXACT);
		final IFilter<ImageElement> disabled1 = new PatternFilter(".*/obj16/\\w+dis(_obj)?\\W.+"); //$NON-NLS-1$
		final IFilter<ImageElement> disabled2 = new PatternFilter(".*/d(?!ialogs)(?!ecorations)(?!nd)(?!evguide)\\w+/.+"); //$NON-NLS-1$
		final IFilter<ImageElement> disabled = new OrFilter<ImageElement>(new IFilter[] {disabled1, disabled2});
		disabledIcons = new AndFilter<ImageElement>(new IFilter[] {iconSize, disabled});

		final IFilter<ImageElement> enabled = new NotFilter<ImageElement>(disabled);
		enabledIcons = new AndFilter<ImageElement>(new IFilter[] {iconSize, enabled});

		final IFilter<ImageElement> wizardSize = new SizeFilter(75, SizeFilter.TYPE_EXACT, 66, SizeFilter.TYPE_EXACT);
		final IFilter<ImageElement> wizard1 = new PatternFilter(".*/wizban/.+"); //$NON-NLS-1$
		final IFilter<ImageElement> wizard2 = new PatternFilter(".+_wiz\\.\\w+"); //$NON-NLS-1$
		final IFilter<ImageElement> wizardName = new OrFilter<ImageElement>(new IFilter[] {wizard1, wizard2});
		wizard = new AndFilter<ImageElement>(new IFilter[] {wizardSize, wizardName});

		mFilters.add(enabledIcons);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(final Composite parent) {
		final Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.IMAGE_BROWSER_VIEW);

		Composite topComp = new Composite(composite, SWT.NONE);
		RowLayout layout = new RowLayout();
		topComp.setLayout(layout);
		topComp.setFont(parent.getFont());
		topComp.setBackground(composite.getBackground());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		topComp.setLayoutData(gd);

		Composite sourceComp = SWTFactory.createComposite(topComp, 7, 1, SWT.NONE, 0, 0);
		sourceComp.setLayoutData(new RowData());
		SWTFactory.createLabel(sourceComp, PDEUIMessages.ImageBrowserView_Source, 1);
		sourceCombo = new ComboViewer(SWTFactory.createCombo(sourceComp, SWT.READ_ONLY, 1, null));
		sourceCombo.setContentProvider(new ArrayContentProvider());
		sourceCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				scanImages();
			}
		});

		ArrayList<Object> sourceComboInput = new ArrayList<Object>();
		sourceComboInput.add(new TargetPlatformRepository(this, true));
		sourceComboInput.add(new TargetPlatformRepository(this, false));
		sourceComboInput.add(new WorkspaceRepository(this));
		sourceCombo.setInput(sourceComboInput);

		SWTFactory.createHorizontalSpacer(sourceComp, 3);

		SWTFactory.createLabel(sourceComp, PDEUIMessages.ImageBrowserView_Show, 1);
		Combo typeCombo = SWTFactory.createCombo(sourceComp, SWT.READ_ONLY, 1, new String[] {PDEUIMessages.ImageBrowserView_FilterIcons, PDEUIMessages.ImageBrowserView_FilterDisabled, PDEUIMessages.ImageBrowserView_FilterWizards, PDEUIMessages.ImageBrowserView_FilterAllImages});
		typeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mFilters.clear();
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
				scanImages();
			}
		});

		Composite pageComp = SWTFactory.createComposite(topComp, 6, 1, SWT.NONE, 0, 0);
		((GridLayout) pageComp.getLayout()).marginLeft = 20;
		pageComp.setLayoutData(new RowData());
		SWTFactory.createLabel(pageComp, PDEUIMessages.ImageBrowserView_MaxImages, 1);
		spinMaxImages = new Spinner(pageComp, SWT.BORDER);
		spinMaxImages.setMaximum(999);
		spinMaxImages.setMinimum(1);
		spinMaxImages.setSelection(250);

		SWTFactory.createHorizontalSpacer(pageComp, 3);

		nextButton = SWTFactory.createPushButton(pageComp, PDEUIMessages.ImageBrowserView_ShowMore, null);
		nextButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				scanImages();
			}
		});
		nextButton.setEnabled(false);

		scrolledComposite = new ScrolledComposite(composite, SWT.BORDER | SWT.V_SCROLL);
		scrolledComposite.setBackground(scrolledComposite.getParent().getBackground());
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		scrolledComposite.addControlListener(new ControlAdapter() {
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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		// Pressing enter when navigating the images calls this method, stealing focus
		if (sourceCombo.getSelection().isEmpty()) {
			sourceCombo.getCombo().setFocus();
		}
	}

	public void notifyImage(final ImageElement element) {
		for (final IFilter<ImageElement> filter : mFilters) {
			if (!filter.accept(element))
				return;
		}

		mUIJob.addImage(element);
		mImageCounter--;

		if (mImageCounter <= 0) {
			Display.getDefault().asyncExec(new Runnable() {

				public void run() {
					nextButton.setEnabled(true);
				}
			});
		}
	}

	public boolean needsMore() {
		return mImageCounter > 0;
	}

	private void scanImages() {
		nextButton.setEnabled(false);

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

		// initialize scan job
		if (!sourceCombo.getSelection().isEmpty()) {
			// set maximum image counter
			mImageCounter = spinMaxImages.getSelection();

			final AbstractRepository repository = (AbstractRepository) ((IStructuredSelection) sourceCombo.getSelection()).getFirstElement();
			repository.schedule();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
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

	private class UpdateUI extends FocusAdapter implements Runnable {

		Collection<ImageElement> mElements = new LinkedList<ImageElement>();
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
					button.addListener(SWT.Activate, new Listener() {
						public void handleEvent(Event e) {

						}
					});
				}

				mElements.clear();

				mPluginImageContainer.layout();
				imageComposite.layout();

				Rectangle r = scrolledComposite.getClientArea();
				scrolledComposite.setMinSize(imageComposite.computeSize(r.width, SWT.DEFAULT));
			}
		}

		public void focusGained(FocusEvent e) {
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

				// Example of how we could use a popup dialog instead
//				org.eclipse.core.runtime.Path path = new org.eclipse.core.runtime.Path(((ImageElement) data).getPath());
//
//				new PopupDialog(getSite().getShell(), PopupDialog.HOVER_SHELLSTYLE, true, false, false, false, false, path.lastSegment(), "") {
//					protected Control createInfoTextArea(Composite parent) {
//
//						Composite infoGroup = SWTFactory.createComposite(parent, 4, 1, GridData.FILL_HORIZONTAL, 0, 0);
//
//						SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Path, 1);
//						lblPath = new Label(infoGroup, SWT.NONE);
//						lblPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
//
//						SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Width, 1);
//						lblWidth = new Label(infoGroup, SWT.NONE);
//						final GridData gd_lblWidth = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
//						gd_lblWidth.widthHint = 50;
//						lblWidth.setLayoutData(gd_lblWidth);
//
//						SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Plugin, 1);
//						lblPlugin = new Label(infoGroup, SWT.NONE);
//						lblPlugin.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
//
//						SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Height, 1);
//						lblHeight = new Label(infoGroup, SWT.NONE);
//						final GridData gd_lblHeight = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
//						gd_lblHeight.widthHint = 50;
//						lblHeight.setLayoutData(gd_lblHeight);
//
//						SWTFactory.createLabel(infoGroup, PDEUIMessages.ImageBrowserView_Reference, 1);
//						txtReference = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
//						txtReference.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
//
//						lblPath.setText(((ImageElement) data).getPath());
//						lblPlugin.setText(((ImageElement) data).getFullPlugin());
//						txtReference.setText("platform:/plugin/" + ((ImageElement) data).getPlugin() + "/" + ((ImageElement) data).getPath()); //$NON-NLS-1$ //$NON-NLS-2$
//
//						lblWidth.setText(NLS.bind(PDEUIMessages.ImageBrowserView_Pixels, Integer.toString(((ImageElement) data).getImageData().width)));
//						lblHeight.setText(NLS.bind(PDEUIMessages.ImageBrowserView_Pixels, Integer.toString(((ImageElement) data).getImageData().height)));
//
//						return infoGroup;
//					}
//				}.open();
//
//				StringBuffer text = new StringBuffer();
//				text.append(NLS.bind("Image Path: {0}", path.toString())).append('\n');
//				text.append(NLS.bind("Plug-in Provider: {0}", ((ImageElement) data).getFullPlugin())).append('\n');
//				text.append(NLS.bind("Reference URL: {0}", "platform:/plugin/" + ((ImageElement) data).getPlugin() + "/" + ((ImageElement) data).getPath())).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
//				text.append(NLS.bind("Width: {0}   Height: {1}", Integer.toString(((ImageElement) data).getImageData().width), Integer.toString(((ImageElement) data).getImageData().height)));
//
//				PopupDialog popup = new PopupDialog(getSite().getShell(), SWT.NONE, false, false, false, false, false, path.lastSegment(), text.toString());
//				popup.open();

				lblPath.setText(((ImageElement) data).getPath());
				lblPlugin.setText(((ImageElement) data).getFullPlugin());
				txtReference.setText("platform:/plugin/" + ((ImageElement) data).getPlugin() + "/" + ((ImageElement) data).getPath()); //$NON-NLS-1$ //$NON-NLS-2$

				lblWidth.setText(NLS.bind(PDEUIMessages.ImageBrowserView_Pixels, Integer.toString(((ImageElement) data).getImageData().width)));
				lblHeight.setText(NLS.bind(PDEUIMessages.ImageBrowserView_Pixels, Integer.toString(((ImageElement) data).getImageData().height)));

				// update source provider
				ISourceProviderService service = (ISourceProviderService) PlatformUI.getWorkbench().getService(ISourceProviderService.class);
				ISourceProvider provider = service.getSourceProvider(ActiveImageSourceProvider.ACTIVE_IMAGE);
				if (provider instanceof ActiveImageSourceProvider)
					((ActiveImageSourceProvider) provider).setImageData(((ImageElement) data));
			}
		}

		public synchronized void reset() {
			mLastPlugin = ""; //$NON-NLS-1$
			mElements.clear();
			mPluginImageContainer = null;
		}
	}
}
