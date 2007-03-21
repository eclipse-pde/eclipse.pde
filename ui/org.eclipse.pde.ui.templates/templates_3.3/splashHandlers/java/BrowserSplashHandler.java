/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package $packageName$;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.AbstractSplashHandler;

/**
 * @since 3.3
 * 
 */
public class BrowserSplashHandler extends AbstractSplashHandler {

	private boolean movieLoaded = false, done = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.splash.AbstractSplashHandler#init(org.eclipse.swt.widgets.Shell,
	 *      org.eclipse.ui.IWorkbench)
	 */
	public void init(final Shell splash) {
		super.init(splash);
		splash.setLayout(new GridLayout(1, true));
		final Browser browser = new Browser(splash, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final Button button = new Button(splash, SWT.PUSH);
		button.setText("Close"); //$NON-NLS-1$
		button.setBounds(
				570 / 2 - button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x / 2,
				splash.getSize().y - 20, splash.getSize().x, 20);
		button.setVisible(false);
		GridData data = new GridData(SWT.CENTER, SWT.FILL, false, false);
		data.widthHint = 80;			
		button.setLayoutData(data);
		browser.addProgressListener(new ProgressListener() {

			public void changed(ProgressEvent event) {
			}

			public void completed(ProgressEvent event) {
				movieLoaded = true;
				button.setVisible(true);
				button.addSelectionListener(new SelectionListener() {

					public void widgetDefaultSelected(SelectionEvent e) {
						
					}

					public void widgetSelected(SelectionEvent e) {
						done = true;
					}
				});
			}
		});
		browser.setUrl("http://www.google.com"); //$NON-NLS-1$		
		splash.layout(true);
		while (!movieLoaded)
			while (getSplash().getDisplay().readAndDispatch())
				;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.splash.AbstractSplashHandler#endSplash()
	 */
	public void dispose() {
		getSplash().setActive();
		if (movieLoaded) {
			while (!done)
				getSplash().getDisplay().readAndDispatch();
		}
		super.dispose();
	}
}
