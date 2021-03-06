/*
 * Copyright 2017 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.basheditor.document;

import static de.jcup.basheditor.document.BashDocumentIdentifiers.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

import de.jcup.basheditor.document.keywords.BashGnuCommandKeyWords;
import de.jcup.basheditor.document.keywords.BashIncludeKeyWords;
import de.jcup.basheditor.document.keywords.BashLanguageKeyWords;
import de.jcup.basheditor.document.keywords.BashSpecialVariableKeyWords;
import de.jcup.basheditor.document.keywords.BashSystemKeyWords;
import de.jcup.basheditor.document.keywords.DocumentKeyWord;

public class BashDocumentPartitionScanner extends RuleBasedPartitionScanner {

	private OnlyLettersKeyWordDetector onlyLettersWordDetector = new OnlyLettersKeyWordDetector();
	private VariableDefKeyWordDetector variableDefKeyWordDetector = new VariableDefKeyWordDetector();

	public BashDocumentPartitionScanner() {

		IToken parameters = createToken(PARAMETER);
		IToken comment = createToken(COMMENT);
		IToken simpleString = createToken(SINGLE_STRING);
		IToken doubleString = createToken(DOUBLE_STRING);
		IToken backtickString = createToken(BACKTICK_STRING);

		IToken systemKeyword = createToken(BASH_SYSTEM_KEYWORD);
		IToken bashKeyword = createToken(BASH_KEYWORD);

		IToken knownVariables = createToken(KNOWN_VARIABLES);
		IToken variables = createToken(VARIABLES);
		IToken includeKeyword = createToken(INCLUDE_KEYWORD);
		IToken bashCommand = createToken(BASH_COMMAND);

		List<IPredicateRule> rules = new ArrayList<>();
		buildWordRules(rules, systemKeyword, BashSystemKeyWords.values());
		rules.add(new BashVariableRule(variables));
		rules.add(new SingleLineRule("#", "", comment, (char) -1, true));

		rules.add(new BashStringRule("\"", "\"", doubleString));
		rules.add(new BashStringRule("\'", "\'", simpleString));
		rules.add(new BashStringRule("`", "`", backtickString));

		rules.add(new CommandParameterRule(parameters));

		buildWordRules(rules, includeKeyword, BashIncludeKeyWords.values());
		buildWordRules(rules, bashKeyword, BashLanguageKeyWords.values());
		buildWordRules(rules, bashCommand, BashGnuCommandKeyWords.values());

		buildVarDefRules(rules, knownVariables, BashSpecialVariableKeyWords.values());

		setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
	}

	private void buildWordRules(List<IPredicateRule> rules, IToken token, DocumentKeyWord[] values) {
		for (DocumentKeyWord keyWord : values) {
			rules.add(new ExactWordPatternRule(onlyLettersWordDetector, createWordStart(keyWord), token,
					keyWord.isBreakingOnEof()));
		}
	}

	private void buildVarDefRules(List<IPredicateRule> rules, IToken token, DocumentKeyWord[] values) {
		for (DocumentKeyWord keyWord : values) {
			rules.add(new VariableDefKeyWordPatternRule(variableDefKeyWordDetector, createWordStart(keyWord), token,
					keyWord.isBreakingOnEof()));
		}
	}

	private String createWordStart(DocumentKeyWord keyWord) {
		return keyWord.getText();
	}

	private IToken createToken(BashDocumentIdentifier identifier) {
		return new Token(identifier.getId());
	}
}
