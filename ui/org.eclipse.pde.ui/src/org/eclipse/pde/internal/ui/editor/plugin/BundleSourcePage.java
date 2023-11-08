/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214511
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262622
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleClasspathHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.CompositeManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.PDEManifestElement;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.IFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.KeyValueSourcePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.actions.PDEActionConstants;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.ManifestConfiguration;
import org.eclipse.pde.internal.ui.refactoring.PDERefactoringAction;
import org.eclipse.pde.internal.ui.refactoring.RefactoringActionFactory;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.forms.editor.FormEditor;
import org.osgi.framework.Constants;

public class BundleSourcePage extends KeyValueSourcePage {

	/**
	 * Used to set the selection in the outline view with the link with editor
	 * feature is enabled
	 * Cannot use a document range object because manifest header elements
	 * do not have ranges associated with them in the bundle model
	 */
	private Object fTargetOutlineSelection;

	/**
	 * Offset used to set the current highlight range.
	 * Used to prevent cyclic event firing when a selection is made in the
	 * outline view with the link with editor feature on.  When a selection
	 * is made in the source viewer, an event is fired back to the outline
	 * view to unnecessarily update the selection.
	 */
	private int fCurrentHighlightRangeOffset;

	private static final int F_NOT_SET = -1;

	private PDERefactoringAction fRenameAction;

	/**
	 * BundleOutlineContentProvider
	 *
	 */
	private class BundleOutlineContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parent) {
			// Need an identifying class for label provider
			if (parent instanceof ImportPackageHeader) {
				return ((ImportPackageHeader) parent).getPackages();
			} else if (parent instanceof ExportPackageHeader) {
				return ((ExportPackageHeader) parent).getPackages();
			} else if (parent instanceof RequiredExecutionEnvironmentHeader) {
				return ((RequiredExecutionEnvironmentHeader) parent).getEnvironments();
			} else if (parent instanceof RequireBundleHeader) {
				return ((RequireBundleHeader) parent).getRequiredBundles();
			} else if (parent instanceof BundleClasspathHeader) {
				return getPluginLibraries();
			}
			return new Object[0];
		}

		private Object[] getPluginLibraries() {
			IPluginLibrary[] libraries = getBundleClasspathLibraries();
			if ((libraries == null) || (libraries.length == 0)) {
				return new Object[0];
			}
			return libraries;
		}

		@Override
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}

		@Override
		public Object getParent(Object child) {
			return null;
		}

		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof BundleModel model) {
				Map<String, IManifestHeader> manifest = ((Bundle) model.getBundle()).getHeaders();
				ArrayList<IDocumentKey> keys = new ArrayList<>();
				for (IManifestHeader header : manifest.values()) {
					if (header.getOffset() > -1) {
						keys.add(header);
					}
				}
				return keys.toArray();
			}
			return new Object[0];
		}
	}

	private IPluginLibrary[] getBundleClasspathLibraries() {
		// The bundle classpath header has no model data members
		// Retrieve the plug-in library equivalents from the editor model
		FormEditor editor = getEditor();
		if (editor instanceof PDEFormEditor formEditor) {
			IBaseModel baseModel = formEditor.getAggregateModel();
			if (baseModel instanceof IPluginModelBase) {
				IPluginLibrary[] libraries = ((IPluginModelBase) baseModel).getPluginBase().getLibraries();
				return libraries;
			}
		}
		return null;
	}

	private class BundleLabelProvider extends LabelProvider {
		// TODO: MP: QO: LOW: Move to PDELabelProvider
		@Override
		public String getText(Object obj) {
			if (obj instanceof PackageObject) {
				return ((PackageObject) obj).getName();
			} else if (obj instanceof ExecutionEnvironment) {
				return ((ExecutionEnvironment) obj).getName();
			} else if (obj instanceof RequireBundleObject) {
				return getTextRequireBundle(((RequireBundleObject) obj));
			} else if (obj instanceof ManifestHeader) {
				return ((ManifestHeader) obj).getName();
			}
			return super.getText(obj);
		}

		private String getTextRequireBundle(RequireBundleObject bundle) {
			StringBuilder label = new StringBuilder();
			// Append the ID
			label.append(bundle.getId());
			// Get the version
			String version = bundle.getVersion();
			// If there is no version, just return what we have
			if ((version == null) || (version.length() == 0)) {
				return label.toString();
			}
			// Append a space
			label.append(' ');
			// If the first character does not have a range indicator,
			// add a default one.  This can happen when there is only one
			// value specified for either min or max
			char firstChar = version.charAt(0);
			if ((firstChar != '(') && (firstChar != '[')) {
				label.append('(');
			}
			// Append the version
			label.append(version);
			// If the last character does not have a range indicator,
			// add a default one.  This can happen when there is only one
			// value specified for either min or max
			char lastChar = version.charAt(version.length() - 1);
			if ((lastChar != ')') && (lastChar != ']')) {
				label.append(')');
			}
			// Return what we have
			return label.toString();
		}

		@Override
		public Image getImage(Object obj) {
			PDELabelProvider labelProvider = PDEPlugin.getDefault().getLabelProvider();
			if (obj instanceof PackageObject) {
				return labelProvider.get(PDEPluginImages.DESC_PACKAGE_OBJ);
			} else if (obj instanceof ExecutionEnvironment) {
				return labelProvider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
			} else if (obj instanceof RequireBundleObject) {
				int flags = SharedLabelProvider.F_EXTERNAL;
				if (((RequireBundleObject) obj).isReexported()) {
					flags = flags | SharedLabelProvider.F_EXPORT;
				}
				return labelProvider.get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, flags);
			} else if (obj instanceof ManifestHeader) {
				return labelProvider.get(PDEPluginImages.DESC_BUILD_VAR_OBJ);
			} else if (obj instanceof IPluginLibrary) {
				return labelProvider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
			}
			return null;
		}
	}

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public BundleSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		resetTargetOutlineSelection();
		resetCurrentHighlightRangeOffset();
	}

	/**
	 * @param offset
	 */
	private void setCurrentHighlightRangeOffset(int offset) {
		fCurrentHighlightRangeOffset = offset;
	}

	/**
	 *
	 */
	private void resetCurrentHighlightRangeOffset() {
		fCurrentHighlightRangeOffset = F_NOT_SET;
	}

	private int getCurrentHighlightRangeOffset() {
		return fCurrentHighlightRangeOffset;
	}

	@Override
	public void resetHighlightRange() {
		resetCurrentHighlightRangeOffset();
		super.resetHighlightRange();
	}

	/**
	 *
	 */
	private void resetTargetOutlineSelection() {
		fTargetOutlineSelection = null;
	}

	/**
	 * @param object
	 */
	private void setTargetOutlineSelection(Object object) {
		fTargetOutlineSelection = object;
	}

	private Object getTargetOutlineSelection() {
		return fTargetOutlineSelection;
	}

	@Override
	public ILabelProvider createOutlineLabelProvider() {
		return new BundleLabelProvider();
	}

	@Override
	public ITreeContentProvider createOutlineContentProvider() {
		return new BundleOutlineContentProvider();
	}

	@Override
	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		IBundleModel model = (IBundleModel) getInputContext().getModel();
		Map<String, IManifestHeader> manifest = ((Bundle) model.getBundle()).getHeaders();
		// Reset
		resetTargetOutlineSelection();
		// Search each manifest header
		for (IManifestHeader node : manifest.values()) {
			// Check to see if the parent is within range
			if (isWithinCurrentRange(offset, node)) {
				// Search the children of composite manifest headers first if
				// specified
				if (searchChildren && (node instanceof CompositeManifestHeader)) {
					IDocumentRange child_node = getRangeElementChild(model, offset, (CompositeManifestHeader) node);
					// If the child node is specified return it; otherwise, we
					// will default to the parent node
					if (child_node != null) {
						return child_node;
					}
				}
				// A manifest header object can be used both for setting the
				// highlight range and making a selection in the outline view
				setTargetOutlineSelection(node);
				return node;
			}
		}
		return null;
	}

	/**
	 * @param offset
	 * @param range
	 * @return true if the offset is within the range; false, otherwise
	 */
	private boolean isWithinCurrentRange(int offset, IDocumentRange range) {

		if (range == null) {
			// Range not set
			return false;
		} else if (offset >= range.getOffset() && (offset <= (range.getOffset() + range.getLength()))) {
			// Offset within range
			return true;
		}
		// Offset not within range
		return false;
	}

	/**
	 * This method is required because the calculated ranges, do NOT include
	 * their parameters (e.g. x-friends, bundle-version)
	 * @param offset
	 * @param current_range
	 * @param previous_range
	 * @return true if the offset falls in between the end of the previous range
	 * and before the current range (e.g. the previous ranges parameters)
	 */
	private boolean isWithinPreviousRange(int offset, IDocumentRange current_range, IDocumentRange previous_range) {

		if ((current_range == null) || (previous_range == null)) {
			// Range not set
			return false;
		} else if ((offset >= previous_range.getOffset() + previous_range.getLength()) && ((offset <= current_range.getOffset()))) {
			// Offset within range
			return true;
		}
		// Offset not within range
		return false;
	}

	/**
	 * @param offset
	 * @param previousRange
	 */
	private boolean isBeforePreviousRange(int offset, IDocumentRange previousRange) {

		if (previousRange == null) {
			return false;
		} else if (offset < previousRange.getOffset()) {
			return true;
		}
		return false;
	}

	/**
	 * @param model
	 * @param offset
	 * @param header
	 */
	private IDocumentRange getRangeElementChild(IBundleModel model, int offset, CompositeManifestHeader header) {
		// Ensure the header has associated elements
		if (header.isEmpty()) {
			return null;
		}
		// Get the header elements
		PDEManifestElement[] elements = header.getElements();
		// Get the header elements name (assume that all elements are the same
		// as the first element)
		String headerName = getHeaderName(elements[0]);
		PDEManifestElement previousElement = null;
		PDEManifestElement currentElement = null;
		IDocumentRange previousRange = null;
		IDocumentRange currentRange = null;
		// Process each element
		for (PDEManifestElement element : elements) {
			currentElement = element;
			// Find the range for the element
			currentRange = getSpecificRange(model, headerName, currentElement.getValue());
			// Determine whether the element is within range
			if (isBeforePreviousRange(offset, previousRange)) {
				return null;
			} else if (isWithinCurrentRange(offset, currentRange)) {
				setChildTargetOutlineSelection(headerName, currentElement);
				// Use for setting the highlight range
				return currentRange;
			} else if (isWithinPreviousRange(offset, currentRange, previousRange)) {
				setChildTargetOutlineSelection(headerName, previousElement);
				// Use for setting the highlight range
				return previousRange;
			}
			// Update for the next iteration
			previousRange = currentRange;
			previousElement = currentElement;
		}

		if (isWithinLastElementParamRange(offset, currentRange, header)) {
			// No element found within range
			setChildTargetOutlineSelection(headerName, currentElement);
			// Use for setting the highlight range
			return currentRange;
		}
		return null;
	}

	/**
	 * @param offset
	 * @param currentRange
	 * @param headerRange
	 */
	private boolean isWithinLastElementParamRange(int offset, IDocumentRange currentRange, IDocumentRange headerRange) {
		if (currentRange == null) {
			return false;
		} else if ((offset >= currentRange.getOffset() + currentRange.getLength()) && (offset <= (headerRange.getOffset() + headerRange.getLength()))) {
			return true;
		}
		return false;
	}

	/**
	 * @param headerName
	 * @param element
	 */
	private void setChildTargetOutlineSelection(String headerName, PDEManifestElement element) {
		// Use for setting the outline view selection
		if (headerName.equalsIgnoreCase(Constants.BUNDLE_CLASSPATH)) {
			setTargetOutlineSelection(getBundleClasspathOutlineSelection(element));
		} else {
			setTargetOutlineSelection(element);
		}
	}

	/**
	 * Edge Case:  Cannot use the PDEManifestElement directly to select bundle
	 * classpath elements in the outline view.  Need to use IPluginLibrary
	 * objects
	 * @param manifestElement
	 */
	private Object getBundleClasspathOutlineSelection(PDEManifestElement manifestElement) {

		IPluginLibrary[] libraries = getBundleClasspathLibraries();
		// Ensure there are libraries
		if ((libraries == null) || (libraries.length == 0)) {
			return null;
		}
		// Linearly search for the equivalent library object
		for (IPluginLibrary library : libraries) {
			if (manifestElement.getValue().equals(library.getName())) {
				// Found
				return library;
			}
		}
		// None found
		return null;
	}

	/**
	 * @param element
	 */
	private String getHeaderName(PDEManifestElement element) {
		if (element instanceof ExportPackageObject) {
			return Constants.EXPORT_PACKAGE;
		} else if (element instanceof ImportPackageObject) {
			return Constants.IMPORT_PACKAGE;
		} else if (element instanceof ExecutionEnvironment) {
			return Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT;
		} else if (element instanceof RequireBundleObject) {
			return Constants.REQUIRE_BUNDLE;
		} else {
			// Bundle classpath elements do not have their own model class
			// They are created as PDEManifestElements directly whose value
			// is just a String
			// Assume that if the element is none of the above types it is this
			// type
			return Constants.BUNDLE_CLASSPATH;
		}
	}

	@Override
	protected String[] collectContextMenuPreferencePages() {
		String[] ids = super.collectContextMenuPreferencePages();
		String[] more = new String[ids.length + 1];
		more[0] = "org.eclipse.pde.ui.EditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}

	@Override
	public IDocumentRange findRange() {

		Object selection = getSelection();

		if (selection instanceof ImportObject) {
			IPluginModelBase base = ((ImportObject) selection).getImport().getPluginModel();
			if (base instanceof IBundlePluginModelBase)
				return getSpecificRange(((IBundlePluginModelBase) base).getBundleModel(), Constants.REQUIRE_BUNDLE, ((ImportObject) selection).getId());
		} else if (selection instanceof ImportPackageObject) {
			return getSpecificRange(((ImportPackageObject) selection).getModel(), Constants.IMPORT_PACKAGE, ((ImportPackageObject) selection).getValue());
		} else if (selection instanceof ExportPackageObject) {
			return getSpecificRange(((ExportPackageObject) selection).getModel(), Constants.EXPORT_PACKAGE, ((ExportPackageObject) selection).getValue());
		} else if (selection instanceof IPluginLibrary) {
			IPluginModelBase base = ((IPluginLibrary) selection).getPluginModel();
			if (base instanceof IBundlePluginModelBase)
				return getSpecificRange(((IBundlePluginModelBase) base).getBundleModel(), Constants.BUNDLE_CLASSPATH, ((IPluginLibrary) selection).getName());
		} else if (selection instanceof ExecutionEnvironment) {
			return getSpecificRange(((ExecutionEnvironment) selection).getModel(), Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, ((ExecutionEnvironment) selection).getValue());
		} else if (selection instanceof RequireBundleObject) {
			return getSpecificRange(((RequireBundleObject) selection).getModel(), Constants.REQUIRE_BUNDLE, ((RequireBundleObject) selection).getId());
		}
		return null;
	}

	public static IDocumentRange getSpecificRange(IBundleModel model, IManifestHeader header, String element) {
		if (header == null || !(model instanceof IEditingModel))
			return null;

		final int[] range = new int[] {-1, -1}; // { offset, length }
		try {
			int start = header.getOffset() + header.getName().length();
			int length = header.getLength() - header.getName().length();
			String headerValue = ((IEditingModel) model).getDocument().get(start, length);

			int i = headerValue.indexOf(element);
			int last = headerValue.lastIndexOf(element);
			if (i > 0 && i != last) {
				char[] sChar = element.toCharArray();
				char[] headerChar = headerValue.toCharArray();
				headLoop: for (; i <= last; i++) {
					// check 1st, middle and last chars to speed things up
					if (headerChar[i] != sChar[0] && headerChar[i + sChar.length / 2] != sChar[sChar.length / 2] && headerChar[i + sChar.length - 1] != sChar[sChar.length - 1])
						continue headLoop;

					for (int j = 1; j < sChar.length - 1; j++)
						if (headerChar[i + j] != sChar[j])
							continue headLoop;

					// found match
					char c = headerChar[i - 1];
					if (!Character.isWhitespace(c) && c != ',')
						// search string is contained by another
						continue headLoop;

					int index = i + sChar.length;
					if (index >= headerChar.length) {
						// Current match is longer than search
						// Occurs when match is '.' or a single character
						continue;
					}
					c = headerChar[index];
					if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '.')
						// current match is longer than search
						continue headLoop;

					break;
				}
			}
			if (i != -1) {
				range[0] = start + i;
				range[1] = element.length();
			}
		} catch (BadLocationException e) {
		}
		if (range[0] == -1) { // if un-set offset use header range
			range[0] = header.getOffset();
			// Only select the length of the header name; otherwise, the
			// header value will be included in the selection
			range[1] = header.getName().length();
		}
		return new IDocumentRange() {
			@Override
			public int getOffset() {
				return range[0];
			}

			@Override
			public int getLength() {
				return range[1];
			}
		};
	}

	public static IDocumentRange getSpecificRange(IBundleModel model, String headerName, String search) {
		IManifestHeader header = model.getBundle().getManifestHeader(headerName);
		return getSpecificRange(model, header, search);
	}

	@Override
	protected boolean isSelectionListener() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IHyperlinkDetector.class.equals(adapter))
			return (T) new BundleHyperlinkDetector(this);
		return super.getAdapter(adapter);
	}

	@Override
	public void updateSelection(Object object) {

		// Update the global selection
		setSelectedObject(object);

		// Highlight the selection if it is a manifest header
		if (object instanceof IDocumentKey) {
			setHighlightRange((IDocumentKey) object);
			setCurrentHighlightRangeOffset(((IDocumentKey) object).getOffset());
			// We don't set the selected range because it will cause the
			// manifest header and all its value to be selected
			return;
		}
		// Handle manifest header values
		// Determine the selection range
		IDocumentRange range = findRange();
		// Ensure there is a range
		if (range == null) {
			return;
		}
		// Get the model
		IBaseModel model = getInputContext().getModel();
		// Ensure we have an editing model
		if ((model instanceof AbstractEditingModel) == false) {
			return;
		}
		// If the range offset is undefined or the source viewer is dirty,
		// forcibly adjust the offsets and try to find the range again
		if ((range.getOffset() == -1) || isDirty()) {
			try {
				((AbstractEditingModel) model).adjustOffsets(((AbstractEditingModel) model).getDocument());
			} catch (CoreException e) {
				// Ignore
			}
			range = findRange();
		}
		// Set the highlight and selected range with whatever we found
		setCurrentHighlightRangeOffset(range.getOffset());
		setHighlightRange(range, true);
		setSelectedRange(range, false);
	}

	@Override
	protected void handleSelectionChangedSourcePage(SelectionChangedEvent event) {
		super.handleSelectionChangedSourcePage(event);
		ISelection selection = event.getSelection();
		// Ensure we have a selection
		if (selection.isEmpty() || ((selection instanceof ITextSelection) == false)) {
			return;
		}
		// If the page has been edited, adjust the offsets; otherwise, our
		// caculated ranges will be out of sync
		IBaseModel model = getInputContext().getModel();
		if (model instanceof AbstractEditingModel && isDirty()) {
			try {
				((AbstractEditingModel) model).adjustOffsets(((AbstractEditingModel) model).getDocument());
			} catch (CoreException e) {
				// Ignore
			}
		}
		// Synchronize using the current cursor position in this page
		synchronizeOutlinePage(((ITextSelection) selection).getOffset());
	}

	@Override
	protected void synchronizeOutlinePage(int offset) {
		// Prevent cyclical firing of events between source page and outline
		// view
		// If the previous offset is the same as the current offset, then
		// the selection does not need to be updated in the outline view
		int previous_offset = getCurrentHighlightRangeOffset();
		if (previous_offset == offset) {
			return;
		}
		// Find the range header (parent) or element (children) within range of
		// the text selection offset
		IDocumentRange rangeElement = getRangeElement(offset, true);
		// Set the highlight range
		updateHighlightRange(rangeElement);
		// Set the outline view selection
		updateOutlinePageSelection(getTargetOutlineSelection());
	}

	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		StyledText text = getViewer().getTextWidget();
		Point p = text.getSelection();
		IDocumentRange element = getRangeElement(p.x, false);
		// only activate rename when user is highlighting Bundle-SymbolicName header
		if (!(element instanceof BundleSymbolicNameHeader) || !(((BundleSymbolicNameHeader) element).getModel().isEditable()))
			return;
		if (fRenameAction == null) {
			IBaseModel base = ((PDEFormEditor) getEditor()).getAggregateModel();
			if (base instanceof IPluginModelBase) {
				fRenameAction = RefactoringActionFactory.createRefactorPluginIdAction(NLS.bind(PDEUIMessages.BundleSourcePage_renameActionText, Constants.BUNDLE_SYMBOLICNAME));
				fRenameAction.setSelection(base);
			}
		}
		if (fRenameAction != null)
			// add rename action after Outline. This is the same order as the hyperlink actions
			menu.insertAfter(PDEActionConstants.COMMAND_ID_QUICK_OUTLINE, fRenameAction);
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		// Update the text selection if this page is being activated
		if (active) {
			updateTextSelection();
		}
	}

	@Override
	protected IFoldingStructureProvider getFoldingStructureProvider(IEditingModel model) {
		return new BundleFoldingStructureProvider(this, model);
	}

	@Override
	protected ChangeAwareSourceViewerConfiguration createSourceViewerConfiguration(IColorManager colorManager) {
		return new ManifestConfiguration(colorManager, this);
	}
}
