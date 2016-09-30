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
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;

public class $presentationClass$ extends PresentationReconciler {

	public $presentationClass$() {
		RuleBasedScanner scanner= new RuleBasedScanner();
		IRule[] rules = new IRule[4];
		rules[0] = new TargetPlatformRule();
		rules[1] = new SingleLineRule("\"", "\"",new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(139,69,19)))));
		rules[2] = new SingleLineRule("<?", "?>",new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(176,176,176)))));
		rules[3] = new MultiLineRule("<!--", "-->",new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(0,100,0)))));
		scanner.setRules(rules);
		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(scanner);
		this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	}
}
