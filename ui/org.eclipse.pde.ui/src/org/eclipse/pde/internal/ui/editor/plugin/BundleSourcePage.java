/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.KeyValueSourcePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.SourceOutlinePage;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.ManifestConfiguration;
import org.eclipse.pde.internal.ui.editor.text.ReconcilingStrategy;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.FormatManifestAction;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Constants;

public class BundleSourcePage extends KeyValueSourcePage {
	
	class BundleSourceViewerConfiguration extends ManifestConfiguration {
		public BundleSourceViewerConfiguration(IColorManager manager) {
			super(manager);
		}
		
		public IReconciler getReconciler(ISourceViewer sourceViewer) {
			ReconcilingStrategy strategy = new ReconcilingStrategy();
			strategy.addParticipant((IReconcilingParticipant) getInputContext()
					.getModel());
			strategy.addParticipant((SourceOutlinePage)getContentOutline());
			MonoReconciler reconciler = new MonoReconciler(strategy, false);
			reconciler.setDelay(500);
			return reconciler;
		}
	}

	class BundleOutlineContentProvider extends DefaultContentProvider
			implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			return false;
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object parent) {
			if (parent instanceof BundleModel) {
				BundleModel model = (BundleModel) parent;
				Dictionary manifest = ((Bundle)model.getBundle()).getHeaders();
				ArrayList keys = new ArrayList();
				for (Enumeration elements = manifest.keys(); elements.hasMoreElements();) {
					IDocumentKey key = (IDocumentKey) manifest.get(elements.nextElement());
					if (key.getOffset() > -1)
						keys.add(key);
				}
				return keys.toArray();
			}
			return new Object[0];
		}
	}
	class BundleLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof ManifestHeader) {
				return ((ManifestHeader) obj).getName();
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof ManifestHeader)
				return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_BUILD_VAR_OBJ);
			return null;
		}
	}
	private IColorManager fColorManager;
	private BundleSourceViewerConfiguration fConfiguration;
	
	public BundleSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		fColorManager = ColorManager.getDefault();
		fConfiguration = new BundleSourceViewerConfiguration(fColorManager);
		setSourceViewerConfiguration(fConfiguration);
	}
	
	protected ILabelProvider createOutlineLabelProvider() {
		return new BundleLabelProvider();
	}
	
	protected ITreeContentProvider createOutlineContentProvider() {
		return new BundleOutlineContentProvider();
	}
	protected IDocumentRange getRangeElement(ITextSelection selection) {
		if (selection.isEmpty())
			return null;
		IBundleModel model = (IBundleModel) getInputContext().getModel();
		Dictionary manifest = ((Bundle) model.getBundle()).getHeaders();
		int offset = selection.getOffset();
		
		for (Enumeration elements = manifest.elements(); elements.hasMoreElements();) {
		    IDocumentRange node = (IDocumentRange) elements.nextElement();

		    if (offset >= node.getOffset() &&
		        offset < node.getOffset() + node.getLength()) {
		        return node;
		    }
		}
		return null;
	}
	
	public void dispose() {
		fColorManager.dispose();
		fConfiguration.dispose();
		super.dispose();
	}
	
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		try {
			ISourceViewer sourceViewer = getSourceViewer();
			if (sourceViewer != null)
				fConfiguration.handlePropertyChangeEvent(event);
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}
	
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return fConfiguration.affectsTextPresentation(event) || super.affectsTextPresentation(event);
	}
	
	protected String[] collectContextMenuPreferencePages() {
		String[] ids= super.collectContextMenuPreferencePages();
		String[] more= new String[ids.length + 1];
		more[0]= "org.eclipse.pde.ui.EditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}
	
	public IDocumentRange findRange() {
		if (fSel instanceof ImportObject) {
			IPluginModelBase base = ((ImportObject)fSel).getImport().getPluginModel();
			if (base instanceof IBundlePluginModelBase)
				return getSpecificRange(
						((IBundlePluginModelBase)base).getBundleModel(),
						Constants.REQUIRE_BUNDLE,
						((ImportObject)fSel).getId());
		} else if (fSel instanceof ImportPackageObject) {
			return getSpecificRange(
					((ImportPackageObject)fSel).getModel(),
					Constants.IMPORT_PACKAGE,
					((ImportPackageObject)fSel).getValue());
		} else if (fSel instanceof ExportPackageObject) {
			return getSpecificRange(
					((ExportPackageObject)fSel).getModel(),
					Constants.EXPORT_PACKAGE,
					((ExportPackageObject)fSel).getValue());
		} else if (fSel instanceof IPluginLibrary) {
			IPluginModelBase base = ((IPluginLibrary)fSel).getPluginModel();
			if (base instanceof IBundlePluginModelBase)
				return getSpecificRange(
						((IBundlePluginModelBase)base).getBundleModel(),
						Constants.BUNDLE_CLASSPATH,
						((IPluginLibrary)fSel).getName());
		} else if (fSel instanceof ExecutionEnvironment) {
			return getSpecificRange(
					((ExecutionEnvironment)fSel).getModel(),
					Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
					((ExecutionEnvironment)fSel).getValue());
		}
		return null;
	}
	
	protected IDocumentRange getSpecificRange(IBundleModel model, String headerName, String search) {
		IManifestHeader header = model.getBundle().getManifestHeader(headerName);
		if (header != null) {
			final int[] range = new int[] { -1, -1 }; // { offset, length }
			if (model instanceof BundleModel) {
				try {
					int start = header.getOffset() + header.getName().length();
					int length = header.getLength() - header.getName().length();
					String headerValue = ((BundleModel)model).getDocument().get(start, length);
					
					int i = headerValue.indexOf(search);
					int last = headerValue.lastIndexOf(search);
					if (i > 0 && i != last) {
						char[] sChar = search.toCharArray();
						char[] headerChar = headerValue.toCharArray();
						headLoop: for (; i <= last; i++) {
							// check 1st, middle and last chars to speed things up
							if (headerChar[i] != sChar[0] && 
									headerChar[i + sChar.length / 2] != sChar[sChar.length / 2] && 
									headerChar[i + sChar.length - 1] != sChar[sChar.length - 1])
								continue headLoop;
							
							for (int j = 1; j < sChar.length - 1; j++)
								if (headerChar[i + j] != sChar[j])
									continue headLoop;
	
							// found match
							char c = headerChar[i - 1];
							if (!Character.isWhitespace(c) && c != ',')
								// search string is contained by another
								continue headLoop;
							
							c = headerChar[i + sChar.length];
							if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '.')
								// current match is longer than search
								continue headLoop;
							
							break;
						}
					} 
					if (i != -1) {
						range[0] = start + i;
						range[1] = search.length();
					}
				} catch (BadLocationException e) {
				}
			} 
			if (range[0] == -1) { // if un-set offset use header range
				range[0] = header.getOffset();
				range[1] = header.getLength();
			}
			return new IDocumentRange() {
				public int getOffset() { return range[0]; }
				public int getLength() { return range[1]; }
			};
		}
		return null;
	}

	protected boolean isSelectionListener() {
		return true;
	}
	
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		FormatManifestAction action = new FormatManifestAction();
		action.setSourceInput(getEditorInput());
		menu.add(action);
	}
}
