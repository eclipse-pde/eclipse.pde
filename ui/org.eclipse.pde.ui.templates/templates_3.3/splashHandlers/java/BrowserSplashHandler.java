
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

	private final static String F_BROWSER_URL = "http://www.google.com"; //$NON-NLS-1$
	
	private Browser fBrowser;
	
	private Button fButton;
	
	private boolean fClose;

	/**
	 * 
	 */
	public BrowserSplashHandler() {
		fBrowser = null;
		fButton = null;
		fClose = false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.internal.splash.AbstractSplashHandler#init(org.eclipse.swt.widgets.Shell,
	 *      org.eclipse.ui.IWorkbench)
	 */
	public void init(final Shell splash) {
		// Store the shell
		super.init(splash);
		// Configure the shell layout
		configureUISplash();
		// Create UI
		createUI();
		// Create UI listeners
		createUIListeners();
		// Force the UI to layout
		splash.layout(true);
		// Keep the splash screen visible and prevent the RCP application from 
		// loading until the close button is clicked.
		doEventLoop();
	}
	
	/**
	 * 
	 */
	private void doEventLoop() {
		Shell splash = getSplash();
		while (fClose == false) {
			if (splash.getDisplay().readAndDispatch() == false) {
				splash.getDisplay().sleep();
			}
		}
	}
	
	/**
	 * 
	 */
	private void createUIListeners() {
		// Create the browser listeners
		createUIListenersBrowser();
		// Create the button listeners
		createUIListenersButton();
	}

	/**
	 * 
	 */
	private void createUIListenersButton() {
		fButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// NO-OP
			}
			public void widgetSelected(SelectionEvent e) {
				fClose = true;
			}
		});		
	}

	/**
	 * 
	 */
	private void createUIListenersBrowser() {
		fBrowser.addProgressListener(new ProgressListener() {
			public void changed(ProgressEvent event) {
				// NO-OP
			}
			public void completed(ProgressEvent event) {
				// Only show the UI when the URL is fully loaded into the 
				// browser
				fBrowser.setVisible(true);
				fButton.setVisible(true);
			}
		});		
	}
	
	/**
	 * 
	 */
	private void createUI() {
		// Create the web browser
		createUIBrowser();
		// Create the close button
		createUIButton();
	}

	/**
	 * 
	 */
	private void createUIButton() {
		Shell splash = getSplash();
		fButton = new Button(splash, SWT.PUSH);
		fButton.setText("Close"); //$NON-NLS-1$
		fButton.setVisible(false);
		// Configure the button bounds
		configureUIButtonBounds();		
		// Configure layout data
		GridData data = new GridData(SWT.CENTER, SWT.FILL, false, false);
		data.widthHint = 80;			
		fButton.setLayoutData(data);
	}

	/**
	 * 
	 */
	private void configureUIButtonBounds() {
		Shell splash = getSplash();
		
		int button_x_coord = (splash.getSize().x / 2)
				- (fButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x / 2);
		int button_y_coord = splash.getSize().y
				- fButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		int button_x_width = splash.getSize().x;
		int button_y_width = fButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		
		fButton.setBounds(button_x_coord, button_y_coord, button_x_width,
				button_y_width);		
	}	
	
	/**
	 * 
	 */
	private void createUIBrowser() {
		fBrowser = new Browser(getSplash(), SWT.NONE);
		fBrowser.setUrl(F_BROWSER_URL);
		fBrowser.setVisible(false);
		// Configure layout data
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		fBrowser.setLayoutData(data);
	}

	/**
	 * 
	 */
	private void configureUISplash() {
		GridLayout layout = new GridLayout(1, true);
		getSplash().setLayout(layout);
	}

}
