package $packageName$;

import java.io.*;
import java.util.*;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.intro.config.*;
import org.w3c.dom.*;

//

public class DynamicContentProvider implements IIntroXHTMLContentProvider {


    public void init(IIntroContentProviderSite site) {
    }


    public void createContent(String id, PrintWriter out) {
    }

    public void createContent(String id, Composite parent, FormToolkit toolkit) {
    }

    private String getCurrentTimeString() {
        StringBuffer content = new StringBuffer(
                "Dynamic content from $className$: ");
        content.append("Current Time is: ");
        content.append(new Date(System.currentTimeMillis()));
        return content.toString();
    }

    public void createContent(String id, Element parent) {
        Document dom = parent.getOwnerDocument();
        Element para = dom.createElement("p");
        para.setAttribute("id", "dynamicContent");
        para.appendChild(dom.createTextNode(getCurrentTimeString()));
        parent.appendChild(para);

    }


    public void dispose() {

    }



}
