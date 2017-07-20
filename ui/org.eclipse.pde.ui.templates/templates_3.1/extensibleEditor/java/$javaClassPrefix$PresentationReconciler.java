package $packageName$;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class $javaClassPrefix$PresentationReconciler extends PresentationReconciler {

    private final TextAttribute tagAttribute = new TextAttribute(new Color(Display.getCurrent(), new RGB(0,0, 255)));
    private final TextAttribute headerAttribute = new TextAttribute(new Color(Display.getCurrent(), new RGB(128,128,128)));

    public $javaClassPrefix$PresentationReconciler() {
        // TODO this is logic for .project file to color tags in blue. Replace with your language logic!
        RuleBasedScanner scanner= new RuleBasedScanner();
        IRule[] rules = new IRule[2];
        rules[1]= new SingleLineRule("<", ">", new Token(tagAttribute));
        rules[0]= new SingleLineRule("<?", "?>", new Token(headerAttribute));
        scanner.setRules(rules);
        DefaultDamagerRepairer dr= new DefaultDamagerRepairer(scanner);
        this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
    }
}