package $packageName$;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

public class TargetPlatformRule extends WordRule {

String[] tagWords = new String[] {"unit","locations","target","location","targetJRE","launcherArgs"};
String[] attributeWords = new String[] {"id","version","location", "path", "name","sequenceNumber","includeAllPlatforms","includeConfigurePhase","includeMode","includeSource","type","repository"};

	public TargetPlatformRule(IWordDetector detector) {
		super(detector);
	}

	public TargetPlatformRule() {
		this(new Detector());
		setupWords();
	}

	private void setupWords() {
		for (String word : attributeWords) {
			this.addWord(word, blue);
		}

		for (String word : tagWords) {
			this.addWord(word, red);
		}

	}

	private IToken blue= new Token(new TextAttribute(Display.getDefault().getSystemColor(SWT.COLOR_BLUE)));

	private IToken red = new Token(new TextAttribute(Display.getDefault().getSystemColor(SWT.COLOR_RED)));

}
class Detector implements IWordDetector{

	@Override
	public boolean isWordStart(char c) {

		return Character.isAlphabetic(c);
	}

	@Override
	public boolean isWordPart(char c) {

		return Character.isAlphabetic(c);
	}

}
