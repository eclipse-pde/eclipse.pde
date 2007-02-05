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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
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
	
	/**
	 * @param infoControl
	 * @param control
	 * @param provider
	 */
	public static void addHoverListenerToControl(
			final IInformationControl infoControl, final Control control,
			final IControlHoverContentProvider provider) {
		
		control.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent e) {
			}
			public void mouseExit(MouseEvent e) {
				if (infoControl instanceof PDEDefaultInformationControl && ((PDEDefaultInformationControl)infoControl).isDisposed())
					return;
				infoControl.setVisible(false);
			}
			public void mouseHover(MouseEvent e) {
				if (infoControl instanceof PDEDefaultInformationControl && ((PDEDefaultInformationControl)infoControl).isDisposed())
					return;
				String text = provider.getHoverContent(control);
				if (text == null || text.trim().length() == 0)
					return;
				updateHover(infoControl, text);
				infoControl.setLocation(control.toDisplay(new Point(10, 25)));
				infoControl.setVisible(true);
			}
		});
	}
	
	/**
	 * @param infoControl
	 * @param text
	 */
	public static void updateHover(IInformationControl infoControl, String text) {
		infoControl.setInformation(text);
		Point p = infoControl.computeSizeHint();
		infoControl.setSize(p.x, p.y);
		if (text == null || text.trim().length() == 0)
			infoControl.setVisible(false);
	}
	
}
