package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.actions.PDEActionConstants;
import org.eclipse.pde.internal.ui.editor.contentassist.display.HTMLTextPresenter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

public abstract class PDETextHover implements ITextHoverExtension, ITextHover {

	private static IBindingService fBindingService = (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
	
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return new Region(offset, 0);
	}

	public IInformationControlCreator getHoverControlCreator() {
		return getInformationControlCreator();
	}

	public static IInformationControlCreator getInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new PDEDefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true), getTooltipAffordanceString());
			}
		};
	}
	
	public static String getTooltipAffordanceString() {
		if (fBindingService == null)
			return null;

		String keySequence = fBindingService.getBestActiveBindingFormattedFor(PDEActionConstants.DEFN_SRC_TOOLTIP);
		if (keySequence == null)
			return null;
		
		return NLS.bind(PDEUIMessages.PDETextHover_0, keySequence);
	}
}
