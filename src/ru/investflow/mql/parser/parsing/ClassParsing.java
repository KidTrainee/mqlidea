package ru.investflow.mql.parser.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import ru.investflow.mql.parser.parsing.util.ParsingErrors;
import ru.investflow.mql.psi.MQL4Elements;

import static ru.investflow.mql.parser.parsing.BracketBlockParsing.parseBracketsBlock;
import static ru.investflow.mql.parser.parsing.CommentParsing.parseComment;
import static ru.investflow.mql.parser.parsing.FunctionsParsing.parseFunction;
import static ru.investflow.mql.parser.parsing.preprocessor.PreprocessorParsing.parsePreprocessorBlock;
import static ru.investflow.mql.parser.parsing.statement.EnumParsing.parseEnum;
import static ru.investflow.mql.parser.parsing.statement.StatementParsing.parseEmptyStatement;
import static ru.investflow.mql.parser.parsing.util.ParsingUtils.advanceLexerUntil;

public class ClassParsing implements MQL4Elements {

    public static final TokenSet ON_ERROR_CLASS_ADVANCE_TOKENS = TokenSet.create(R_CURLY_BRACKET);
    public static final TokenSet ON_ERROR_CLASS_DO_NOT_ADVANCE_TOKENS = TokenSet.create(SEMICOLON);

    /**
     * Form: (class|struct) name { }
     */
    public static boolean parseClassOrStruct(PsiBuilder b, int l) {
        IElementType t1 = b.getTokenType();
        if (t1 != CLASS_KEYWORD && t1 != STRUCT_KEYWORD && t1 != INTERFACE_KEYWORD) {
            return false;
        }
        boolean isClass = t1 == CLASS_KEYWORD;
        Marker m = b.mark();
        try {
            b.advanceLexer(); // 'class|struct'
            if (b.getTokenType() != IDENTIFIER) {
                b.error(isClass ? "Class name is expected" : "Struct name is expected");
                b.mark().done(MQL4Elements.CLASS_INNER_BLOCK);
                return true;
            }
            b.advanceLexer(); // name

            if (b.getTokenType() != L_CURLY_BRACKET) {
                b.error(ParsingErrors.UNEXPECTED_TOKEN);
                b.mark().done(MQL4Elements.CLASS_INNER_BLOCK);
                return true;
            }
            b.advanceLexer(); // '{'
            if (!parseClassInnerBlock(b, l)) {
                advanceLexerUntil(b, ON_ERROR_CLASS_ADVANCE_TOKENS, ON_ERROR_CLASS_DO_NOT_ADVANCE_TOKENS);
                return true;
            }
            b.advanceLexer(); // '}'
        } finally {
            m.done(CLASS_DEFINITION);
        }
        return true;
    }

    /**
     * Form: as of today: set of anything until '}'
     */
    private static boolean parseClassInnerBlock(PsiBuilder b, int l) {
        Marker classInnerBlock = b.mark();
        try {
            while (b.getTokenType() != R_CURLY_BRACKET && !b.eof()) {
                boolean r = parseEmptyStatement(b)
                        || parseComment(b)
                        || parsePreprocessorBlock(b)
                        || parseFunction(b)
                        || parseEnum(b, l + 1)
                        || parseClassOrStruct(b, l + 1)
                        || parseBracketsBlock(b, l + 1);
                if (!r) {
                    b.advanceLexer();
                }
            }
            return !b.eof();
        } finally {
            classInnerBlock.done(CLASS_INNER_BLOCK);
        }
    }


}
