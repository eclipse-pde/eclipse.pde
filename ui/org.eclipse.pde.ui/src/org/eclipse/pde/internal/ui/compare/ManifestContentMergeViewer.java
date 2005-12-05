package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.ManifestConfiguration;
import org.eclipse.pde.internal.ui.editor.text.ManifestPartitionScanner;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

public class ManifestContentMergeViewer extends TextMergeViewer {

	public ManifestContentMergeViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);
	}

	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			IColorManager colorManager = ColorManager.getDefault();
			((SourceViewer)textViewer).configure(new ManifestConfiguration(colorManager));
			String symbolicFontName = ManifestContentMergeViewer.class.getName();
			Font font = JFaceResources.getFont(symbolicFontName);
			if (font != null)
				((SourceViewer)textViewer).getTextWidget().setFont(font);
		}
	}

	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new ManifestPartitionScanner(), ManifestPartitionScanner.PARTITIONS);
	}
	
	public String getTitle() {
		return PDEUIMessages.ManifestContentMergeViewer_title; 
	}
	
}
