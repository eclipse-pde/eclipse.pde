/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public abstract class SyntaxColorTab {

	protected IColorManager fColorManager;
	private TableViewer fElementViewer;
	private SourceViewer fPreviewViewer;
	private ChangeAwareSourceViewerConfiguration fSourceViewerConfiguration;
	private Button fBoldButton;
	private Button fItalicButton;
	private ColorSelector fColorSelector;

	class ColorElement {
		private final String fDisplayName;
		private final String fColorKey;
		private RGB fColorValue;
		private boolean fBold;
		private boolean fItalic;

		public ColorElement(String displayName, String colorKey, RGB colorValue, boolean bold, boolean italic) {
			fDisplayName = displayName;
			fColorKey = colorKey;
			fColorValue = colorValue;
			fBold = bold;
			fItalic = italic;
		}

		public String getColorKey() {
			return fColorKey;
		}

		public String getDisplayName() {
			return fDisplayName;
		}

		public RGB getColorValue() {
			return fColorValue;
		}

		public void setColorValue(RGB rgb) {
			if (fColorValue.equals(rgb))
				return;
			RGB oldrgb = fColorValue;
			fColorValue = rgb;
			firePropertyChange(new PropertyChangeEvent(this, fColorKey, oldrgb, rgb));
		}

		public void setBold(boolean bold) {
			if (bold == fBold)
				return;
			Boolean oldValue = Boolean.valueOf(fBold);
			fBold = bold;
			Boolean newValue = Boolean.valueOf(bold);
			String property = fColorKey + IPDEColorConstants.P_BOLD_SUFFIX;
			firePropertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
		}

		public boolean isBold() {
			return fBold;
		}

		public void setItalic(boolean italic) {
			if (italic == fItalic)
				return;
			Boolean oldValue = Boolean.valueOf(fItalic);
			fItalic = italic;
			Boolean newValue = Boolean.valueOf(italic);
			String property = fColorKey + IPDEColorConstants.P_ITALIC_SUFFIX;
			firePropertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
		}

		public boolean isItalic() {
			return fItalic;
		}

		@Override
		public String toString() {
			return getDisplayName();
		}

		public void firePropertyChange(PropertyChangeEvent event) {
			if (fSourceViewerConfiguration != null) {
				fSourceViewerConfiguration.adaptToPreferenceChange(event);
				fPreviewViewer.invalidateTextPresentation();
			}
		}
	}

	public SyntaxColorTab(IColorManager manager) {
		fColorManager = manager;
	}

	protected abstract String[][] getColorStrings();

	private ColorElement[] getColorData() {
		String[][] colors = getColorStrings();
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		ColorElement[] list = new ColorElement[colors.length];
		for (int i = 0; i < colors.length; i++) {
			String displayName = colors[i][0];
			String key = colors[i][1];
			RGB setting = PreferenceConverter.getColor(store, key);
			boolean bold = store.getBoolean(key + IPDEColorConstants.P_BOLD_SUFFIX);
			boolean italic = store.getBoolean(key + IPDEColorConstants.P_ITALIC_SUFFIX);
			list[i] = new ColorElement(displayName, key, setting, bold, italic);
		}
		return list;
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		createElementTable(container);
		createPreviewer(container);
		return container;
	}

	private void createElementTable(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.LEFT);
		label.setText(PDEUIMessages.SyntaxColorTab_elements);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		fElementViewer = new TableViewer(container, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		fElementViewer.setLabelProvider(new LabelProvider());
		fElementViewer.setContentProvider(ArrayContentProvider.getInstance());
		fElementViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite colorComposite = new Composite(container, SWT.NONE);
		colorComposite.setLayout(new GridLayout(2, false));
		colorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		label = new Label(colorComposite, SWT.LEFT);
		label.setText(PDEUIMessages.SyntaxColorTab_color);

		fColorSelector = new ColorSelector(colorComposite);
		Button colorButton = fColorSelector.getButton();
		colorButton.setLayoutData(new GridData(GridData.BEGINNING));

		colorButton.addSelectionListener(widgetSelectedAdapter(e -> {
			ColorElement item = getColorElement(fElementViewer);
			item.setColorValue(fColorSelector.getColorValue());
		}));

		fBoldButton = new Button(colorComposite, SWT.CHECK);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fBoldButton.setLayoutData(gd);
		fBoldButton.setText(PDEUIMessages.SyntaxColorTab_bold);
		fBoldButton.addSelectionListener(widgetSelectedAdapter(e -> {
			ColorElement item = getColorElement(fElementViewer);
			item.setBold(fBoldButton.getSelection());
		}));

		fItalicButton = new Button(colorComposite, SWT.CHECK);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fItalicButton.setLayoutData(gd);
		fItalicButton.setText(PDEUIMessages.SyntaxColorTab_italic);
		fItalicButton.addSelectionListener(widgetSelectedAdapter(e -> {
			ColorElement item = getColorElement(fElementViewer);
			item.setItalic(fItalicButton.getSelection());
		}));

		fElementViewer.addSelectionChangedListener(event -> {
			ColorElement item = getColorElement(fElementViewer);
			fColorSelector.setColorValue(item.getColorValue());
			fBoldButton.setSelection(item.isBold());
			fItalicButton.setSelection(item.isItalic());
		});
		fElementViewer.setInput(getColorData());
		fElementViewer.setComparator(new ViewerComparator());
		fElementViewer.setSelection(new StructuredSelection(fElementViewer.getElementAt(0)));
	}

	private void createPreviewer(Composite parent) {
		Composite previewComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		previewComp.setLayout(layout);
		previewComp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(previewComp, SWT.NONE);
		label.setText(PDEUIMessages.SyntaxColorTab_preview);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fPreviewViewer = new SourceViewer(previewComp, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		fSourceViewerConfiguration = getSourceViewerConfiguration();

		if (fSourceViewerConfiguration != null)
			fPreviewViewer.configure(fSourceViewerConfiguration);

		fPreviewViewer.setEditable(false);
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		fPreviewViewer.setDocument(getDocument());

		Control control = fPreviewViewer.getControl();
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	protected abstract ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration();

	public void performOk() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		int count = fElementViewer.getTable().getItemCount();
		for (int i = 0; i < count; i++) {
			ColorElement item = (ColorElement) fElementViewer.getElementAt(i);
			PreferenceConverter.setValue(store, item.getColorKey(), item.getColorValue());
			store.setValue(item.getColorKey() + IPDEColorConstants.P_BOLD_SUFFIX, item.isBold());
			store.setValue(item.getColorKey() + IPDEColorConstants.P_ITALIC_SUFFIX, item.isItalic());
		}
	}

	public void performDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		int count = fElementViewer.getTable().getItemCount();
		for (int i = 0; i < count; i++) {
			ColorElement item = (ColorElement) fElementViewer.getElementAt(i);
			RGB rgb = PreferenceConverter.getDefaultColor(store, item.getColorKey());
			item.setColorValue(rgb);
			item.setBold(store.getDefaultBoolean(item.getColorKey() + IPDEColorConstants.P_BOLD_SUFFIX));
			item.setItalic(store.getDefaultBoolean(item.getColorKey() + IPDEColorConstants.P_ITALIC_SUFFIX));
		}
		ColorElement element = getColorElement(fElementViewer);
		fColorSelector.setColorValue(element.fColorValue);
		fBoldButton.setSelection(element.isBold());
		fItalicButton.setSelection(element.isItalic());
	}

	public void dispose() {
		fSourceViewerConfiguration.dispose();
	}

	protected abstract IDocument getDocument();

	private ColorElement getColorElement(TableViewer viewer) {
		IStructuredSelection selection = viewer.getStructuredSelection();
		return (ColorElement) selection.getFirstElement();
	}

}
