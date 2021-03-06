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
package de.jcup.basheditor.script.parser;

import static de.jcup.basheditor.script.parser.AssertParseTokens.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.jcup.basheditor.script.parser.ParseToken;
import de.jcup.basheditor.script.parser.TokenParser;

public class TokenParserTest {

	private TokenParser parserToTest;

	@Before
	public void before() {
		parserToTest = new TokenParser();
	}

	@Test
	public void bracket_bracket_1_plus_1_bracket_close_close__recognized() {
		/* prepare */
		String string = "echo $((1+1))";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
			assertThat(tokens).
				containsTokens(
						"echo", "$((1+1))"
						);	/* @formatter:on*/
	}
	
	@Test
	public void sbracket_sbracket_1_plus_1_sbracket_close_close__recognized() {
		/* prepare */
		String string = "echo $[[1+1]]";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
			assertThat(tokens).
				containsTokens(
						"echo", "$[[1+1]]"
						);	/* @formatter:on*/
	}

	@Test
	public void $1_has_start_0_end_2() {
		/* prepare */
		String string = "$1";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		ParseToken token = assertThat(tokens).resolveToken("$1");
		assertEquals(0, token.start);
		assertEquals(2, token.end);
	}

	@Test
	public void moveUntilNextCharWillBeNoStringContent_no_string_contend_handled_as_expected() {
		/* prepare */
		ParseContext context = new ParseContext();
		context.chars = "$(tput 'STRING')".toCharArray();
		context.pos = 2;// at t(put)

		/* execute */
		parserToTest.moveUntilNextCharWillBeNoStringContent(context);
		context.moveForward(); // we must simulate the for next move forwarding!
		parserToTest.moveUntilNextCharWillBeNoStringContent(context);
		context.moveForward();
		parserToTest.moveUntilNextCharWillBeNoStringContent(context);
		context.moveForward();
		parserToTest.moveUntilNextCharWillBeNoStringContent(context);
		context.moveForward();

		/* test */
		assertEquals("tput", context.sb.toString());

		/* execute */
		parserToTest.moveUntilNextCharWillBeNoStringContent(context);
		context.moveForward();

		/* test */
		assertEquals("tput ", context.sb.toString());

		/* execute */
		parserToTest.moveUntilNextCharWillBeNoStringContent(context);
		context.moveForward();

		/* test */
		assertEquals("tput 'STRING'", context.sb.toString());
	}

	@Test
	public void moveUntilNextCharWillBeNoStringContent_tests() {
		assertMoveUntilNextCharWillBeNoStringContent("$('nonsense ;-)'", 2, "'nonsense ;-)'", 15);
		assertMoveUntilNextCharWillBeNoStringContent("'abc'd", 0, "'abc'", 4);
		assertMoveUntilNextCharWillBeNoStringContent("'abc'd", 1, "a", 1);
		assertMoveUntilNextCharWillBeNoStringContent("('abc'd", 0, "(", 0);
		assertMoveUntilNextCharWillBeNoStringContent("('abc'd", 1, "'abc'", 5);
	}

	@Test
	public void moveUntilNextCharWillBeNoStringContent_tests_with_escaped_strings() {
		assertMoveUntilNextCharWillBeNoStringContent("$('non\\\'sense ;-)'", 2, "'non\\\'sense ;-)'", 17);
	}

	@Test
	public void moveUntilNextCharWillBeNoStringContent_tests_with_other_stringtype_inside() {
		assertMoveUntilNextCharWillBeNoStringContent("$('non\"sense ;-)'", 2, "'non\"sense ;-)'", 16);
	}

	private void assertMoveUntilNextCharWillBeNoStringContent(String code, int codePos, String expectedContent,
			int expectedNextPos) {
		/* prepare */
		ParseContext context = new ParseContext();
		context.chars = code.toCharArray();
		context.pos = codePos;

		/* execute */
		parserToTest.moveUntilNextCharWillBeNoStringContent(context);

		/* test */
		assertEquals(expectedContent, context.sb.toString());
		assertEquals(expectedNextPos, context.pos);
	}

	@Test
	public void a_variable_array_with_string_inside_and_escaped_string_char_having_bracket() {
		/* prepare */
		String string = "$abc['\\\'nonsense]']";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"$abc['\\\'nonsense]']"
					);	/* @formatter:on*/
	}

	@Test
	public void a_variable_array_with_string_inside_having_bracket() {
		/* prepare */
		String string = "$abc['nonsense]']";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"$abc['nonsense]']"
					);	/* @formatter:on*/
	}

	@Test
	public void a_variable_curly_braced_with_string_inside_having_curly_bracket() {
		/* prepare */
		String string = "${'nonsense }'}";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"${'nonsense }'}"
					);	/* @formatter:on*/
	}

	@Test
	public void a_variable_group_with_string_inside_having_close_bracket_like_group() {
		/* prepare */
		String string = "$('nonsense ;-)')";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"$('nonsense ;-)')"
					);	/* @formatter:on*/
	}

	@Test
	public void complex_variable_with_group() {
		/* prepare */
		String string = "DIST=$(grep \"DISTRIB_ID\" /etc/lsb-release|awk -F \"=\" '{print $2}'|tr -d \"\\\"', \\n\")";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"DIST=",
					"$(grep \"DISTRIB_ID\" /etc/lsb-release|awk -F \"=\" '{print $2}'|tr -d \"\\\"', \\n\")"
					);	/* @formatter:on*/
	}

	@Test
	public void bugfix_54_a_variable_having_braces_and_a_string_inside_is_closed_by_braces() {

		/* prepare */
		String string = "BLACK=$(tput setaf 0 'STRING')";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"BLACK=",
					"$(tput setaf 0 'STRING')"
					);	/* @formatter:on*/
	}

	@Test
	public void bugfix_54_a_variable_having_braces_is_closed_by_braces() {

		/* prepare */
		String string = "BLACK=$(tput setaf 0)";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
					assertThat(tokens).
						containsTokens(
								"BLACK=",
								"$(tput setaf 0)"
								);	/* @formatter:on*/
	}

	@Test
	public void bugfix_41_3__variable_with_array_having_string_containing_space_recognized_correct() {

		/* prepare */
		String string = "x=${y[`z1 z2`]}";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"x=",
					"${y[`z1 z2`]}"
					);	/* @formatter:on*/
	}

	@Test
	public void bugfix_47__$$_is_no_longer_a_problem() {
		/* prepare */
		String string = "export DB2CLP=**$$**";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"export",
					"DB2CLP=",
					"**",
					"$$",
					"**");
		/* @formatter:on*/
	}

	@Test
	public void bugfix_46__variable_containing_multiple_curly_end_brackets_are_supported() {
		/* prepare */
		String string = "${NAWK:=${awk:=awk}}";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"${NAWK:=${awk:=awk}}");
		/* @formatter:on*/
	}

	@Test
	public void bugfix_45() throws Exception {
		/* prepare */
		String string = "cd \"$(dirname \"$BASH_SOURCE\")\"\n\n# Check if the database exists";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"cd",
					"\"$(dirname \"",
					"$BASH_SOURCE",
					"\")\"",
					"# Check if the database exists");
		/* @formatter:on*/
	}

	@Test
	public void bugfix_45_simplified() throws Exception {
		/* prepare "a"$z"b" # x */
		String string = "\"a\"$z\"b\" # x";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens( 
					"\"a\"", //"a"
					"$z",    //$z
					"\"b\"", //"b"
					"# x");  //"#x"
		/* @formatter:on*/
	}

	@Test
	public void bugfix_43() throws Exception {
		/* prepare */
		String string = "alpha() { eval  \"a\"=${_e#*=} }";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"alpha()",
					"{",
						"eval",
						"\"a\"=",
						"${_e#*=}",
					"}");
		/* @formatter:on*/
	}

	@Test
	public void bugfix_41_2_handle_arrays() throws Exception {
		/* prepare */
		String string = "alpha() { a=${b[`id`]} } beta(){ }";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */ /* @formatter:off*/
		assertThat(tokens).
			containsTokens(
					"alpha()",
					"{",
					"a=",
					"${b[`id`]}",
					"}",
					"beta()",
					"{",
					"}");
		/* @formatter:on*/
	}

	@Test
	public void $bracketPIDbracket_create_databaseDOTsql_is_recognizedas_two_tokens() {

		/* prepare */
		String string = "${PID}_create_database.sql";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("${PID}", "_create_database.sql");
	}

	@Test
	public void echo_$myVar1_echo_$myVar2_parsed_correctly() {
		/* prepare */
		String string = "echo $myVar1 echo $myVar2";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("echo", "$myVar1", "echo", "$myVar2");
	}

	@Test
	public void bugfix_39__a_variable_containing_hash_is_not_recognized_as_comment() {
		/* prepare */
		String string = "if [ ${#TitleMap[*]} -eq 0 ]";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsNotToken("#TitleMap[*]} -eq 0 ]");

	}

	@Test
	public void for_abc_10_newlines_x_token_x_has_position_13() {
		/* prepare */
		String string = "abc\n\n\n\n\n\n\n\n\n\nx";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("x").hasStart(13);

	}

	@Test
	public void for_a_cariage_return_newline_x__token_x_has_position_3() {
		/* prepare */
		String string = "a\r\nx";
		System.out.println(string);
		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("x").hasStart(3);

	}

	@Test
	public void for_ab_cariage_return_newline_x__token_x_has_position_4() {
		/* prepare */
		String string = "ab\r\nx";
		System.out.println(string);
		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("x").hasStart(4);

	}

	@Test
	public void for_ab_cariage_return_newline_cariage_return_newline_x__token_x_has_position_4() {
		/* prepare */
		String string = "ab\r\n\r\nx";
		System.out.println(string);
		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("x").hasStart(6);

	}

	@Test
	public void for_abc_10_cariage_return_newlines_x_token_x_has_position_13() {
		/* prepare */
		String string = "abc\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\nx";
		System.out.println(string);
		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("x").hasStart(23);

	}

	@Test
	public void for_abc__token_abc_has_pos_0() {
		/* prepare */
		String string = "abc";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("abc").hasStart(0);

	}

	@Test
	public void for_abc__token_abc_has_end_2() {
		/* prepare */
		String string = "abc";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("abc").hasEnd(3);

	}

	@Test
	public void for_space_abc__token_abc_has_pos_1() {
		/* prepare */
		String string = " abc";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("abc").hasStart(1);

	}

	@Test
	public void for_space_abc__token_abc_has_end_3() {
		/* prepare */
		String string = " abc";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).token("abc").hasEnd(4);

	}

	@Test
	public void token_abc_followed_by_open_curly_brace_results_in_two_tokens() {
		/* prepare */
		String string = "abc{";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("abc", "{");
	}

	@Test
	public void token_abc_followed_by_close_curly_brace_results_in_two_tokens() {
		/* prepare */
		String string = "abc}";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("abc", "}");
	}

	@Test
	public void token_abc_followed_by_open_and_close_curly_brace_results_in_three_tokens() {
		/* prepare */
		String string = "abc{}";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("abc", "{", "}");
	}

	@Test
	public void semicolon_abc_results_in_token_abc_only() {
		/* prepare */
		String string = ";abc";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("abc");
	}

	@Test
	public void semicolon_abc_semicolon_def_results_in_tokens_abc_and_def() {
		/* prepare */
		String string = ";abc;def";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("abc", "def");
	}

	@Test
	public void semicolon_abc_space_def_results_in_tokens_abc_and_def() {
		/* prepare */
		String string = ";abc def";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsTokens("abc", "def");
	}

	@Test
	public void do_x_do_y_done() {
		/* prepare */
		String string = "do\nx\ndo\ny\ndone";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		assertThat(tokens).containsToken("do", 2).containsOneToken("x").containsOneToken("y").containsOneToken("done");
	}

	@Test
	public void do_x_done_do_y_done_do_done_done_do() {
		/* prepare */
		String string = "do\nx\ndone\ndo\ny\ndone\n\ndo\ndone\ndone\ndo\n\n";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		/* @formatter:off*/
		assertThat(tokens).
			containsTokens("do","x","done","do","y","done","do","done","done","do");
		/* @formatter:on */
	}

	@Test
	public void do_x_done_do_y_done_do_done_done_do__with_backslash_r_inside() {
		/* prepare */
		String string = "do\nx\ndone\ndo\r\ny\ndone\n\ndo\ndone\ndone\ndo\n\n";

		/* execute */
		List<ParseToken> tokens = parserToTest.parse(string);

		/* test */
		/* @formatter:off*/
		assertThat(tokens).
			containsTokens("do","x","done","do","y","done","do","done","done","do");
		/* @formatter:on */
	}

	@Test
	public void a_simple_string_containing_space_do_space_does_not_result_in_a_token_do() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("' do '");

		/* test */
		assertThat(tokens).containsNotToken("do");

	}

	@Test
	public void a_double_string_containing_space_do_space_does_not_result_in_a_token_do() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("\" do \"");

		/* test */
		assertThat(tokens).containsNotToken("do");
	}

	@Test
	public void a_double_ticked_string_containing_space_do_space_does_not_result_in_a_token_do() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("` do `");

		/* test */
		assertThat(tokens).containsNotToken("do");
	}

	@Test
	public void a_double_ticked_do_string_followed_by_space_and_do_does_result_in_do_token() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("`do` do `");

		/* test */
		assertThat(tokens).containsOneToken("do");

	}

	@Test
	public void a_double_ticked_do_string_followed_by_space_and_ESCAPE_and_do_does_result_in_NO_do_token() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("`do\\` do `");

		/* test */
		assertThat(tokens).containsNotToken("do");

	}

	@Test
	public void a_single_do_string_followed_by_space_and_ESCAPE_and_do_does_result_in_NO_do_token() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("'do\\' do '");

		/* test */
		assertThat(tokens).containsNotToken("do");

	}

	@Test
	public void a_double_do_string_followed_by_space_and_ESCAPE_and_do_does_result_in_NO_do_token() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("\"do\\\" do \"");

		/* test */
		assertThat(tokens).containsNotToken("do");

	}

	@Test
	public void a_double_string_containing_single_string_has_token_with_singlestring_contained() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("\" This is the 'way' it is \"");

		/* test */
		assertThat(tokens).containsOneToken("\" This is the 'way' it is \"");

	}

	@Test
	public void a_double_string_containing_double_ticked_string_has_token_with_singlestring_contained() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("\" This is the `way` it is \"");

		/* test */
		assertThat(tokens).containsOneToken("\" This is the `way` it is \"");

	}

	@Test
	public void a_single_string_containing_double_string_has_token_with_singlestring_contained() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("' This is the \\\"way\\\" it is '");

		/* test */
		assertThat(tokens).containsOneToken("' This is the \\\"way\\\" it is '");

	}

	@Test
	public void a_single_string_containing_double_ticked_string_has_token_with_singlestring_contained() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("' This is the `way` it is '");

		/* test */
		assertThat(tokens).containsOneToken("' This is the `way` it is '");

	}

	@Test
	public void abc_def_ghji_is_parsed_as_three_tokens() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("abc def ghji");

		/* test */
		assertNotNull(tokens);
		assertEquals(3, tokens.size());

		Iterator<ParseToken> it = tokens.iterator();
		ParseToken token1 = it.next();
		ParseToken token2 = it.next();
		ParseToken token3 = it.next();

		assertEquals("abc", token1.text);
		assertEquals("def", token2.text);
		assertEquals("ghji", token3.text);
	}

	@Test
	public void some_spaces_abc_def_ghji_is_parsed_as_three_tokens() {

		/* execute */
		List<ParseToken> tokens = parserToTest.parse("    abc def ghji");

		/* test */
		assertNotNull(tokens);
		assertEquals(3, tokens.size());

		Iterator<ParseToken> it = tokens.iterator();
		ParseToken token1 = it.next();
		ParseToken token2 = it.next();
		ParseToken token3 = it.next();

		assertEquals("abc", token1.text);
		assertEquals("def", token2.text);
		assertEquals("ghji", token3.text);
	}

	@Test
	public void abc_def_ghji_is_parsed_as_three_tokens__and_correct_positions() {
		/* execute */
		List<ParseToken> tokens = parserToTest.parse("abc def ghji");
		// ............................................01234567890

		/* test */
		assertThat(tokens).containsTokens("abc", "def", "ghji");
		assertThat(tokens).token("abc").hasStart(0);
		assertThat(tokens).token("def").hasStart(4);
		assertThat(tokens).token("ghji").hasStart(8);

	}

	@Test
	public void comment1_returns_one_tokens() {
		List<ParseToken> tokens = parserToTest.parse("#comment1");

		assertThat(tokens).containsOneToken("#comment1");

	}

	@Test
	public void comment1_new_line_returns_one_tokens() {
		List<ParseToken> tokens = parserToTest.parse("#comment1\n");

		assertThat(tokens).containsOneToken("#comment1");

	}

	@Test
	public void comment1_new_line_function_space_name_returns_3_tokens_comment1_function_and_name() {
		List<ParseToken> tokens = parserToTest.parse("#comment1\nfunction name");

		assertThat(tokens).containsTokens("#comment1", "function", "name");
	}

	@Test
	public void comment1_new_line_function_space_name_directly_followed_by_brackets_returns_3_tokens_comment1_function_and_name() {
		List<ParseToken> tokens = parserToTest.parse("#comment1\nfunction name()");

		assertThat(tokens).containsTokens("#comment1", "function", "name()");
	}

}
