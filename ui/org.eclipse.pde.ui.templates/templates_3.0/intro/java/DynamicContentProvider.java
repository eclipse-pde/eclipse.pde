package $packageName$;

import java.io.*;
import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.intro.config.*;


public class DynamicContentProvider implements IIntroContentProvider {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.config.IIntroContentProvider#init(org.eclipse.ui.intro.config.IIntroContentProviderSite)
     */
    public void init(IIntroContentProviderSite site) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.config.IIntroContentProvider#createContent(java.lang.String,
     *      java.io.PrintWriter)
     */
    public void createContent(String id, PrintWriter out) {
        String content = getCurrentTimeString();
        content = "<p>" + content + "</p>";
        out.write(content);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.config.IIntroContentProvider#createContent(java.lang.String,
     *      org.eclipse.swt.widgets.Composite,
     *      org.eclipse.ui.forms.widgets.FormToolkit)
     */
    public void createContent(String id, Composite parent, FormToolkit toolkit) {
        String content = getCurrentTimeString();
        Label label1 = toolkit.createLabel(parent, content, SWT.WRAP);
        Label label2 = toolkit.createLabel(parent,
                "Some content from SWT presentation", SWT.WRAP);


    }

    private String getCurrentTimeString() {
        StringBuffer content = new StringBuffer(
                "Dynamic content from Intro ContentProvider: ");
        content.append("Current time is: ");
        content.append(new Date(System.currentTimeMillis()));
        return content.toString();
    }



    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.config.IIntroContentProvider#dispose()
     */
    public void dispose() {

    }

}
