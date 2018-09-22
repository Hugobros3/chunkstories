lexer grammar Tokens;

fragment LETTER: 'A'..'Z' | 'a'..'z' ;
fragment DIGIT: '0'..'9' ;

// All the characters that are allowed in a name (excl the first char that can only be a LETTER)
fragment NAME_CHARACTERS: LETTER | DIGIT | '_';

// All the characters that are allowed in a value, on top of the NAME_CHARACTERS
fragment VALUE_CHARACTERS: ',' | ';'| '.' | '?' | '!' | '~' | '/';

// All the characters that are allowed in a quoted text block
fragment TEXT_CHARACTERS: VALUE_CHARACTERS | ' ' | '\'' | '\n' ;

// A name stars with a letter and then some characters
Name: LETTER (NAME_CHARACTERS)*;

// Unquoted text is like a name but *has* to contain a special character (avoids conflicts with Name)
UnquotedText: (NAME_CHARACTERS | VALUE_CHARACTERS)* (VALUE_CHARACTERS | DIGIT | '_') (NAME_CHARACTERS | VALUE_CHARACTERS)*;

// Quoted text: almost anything goes !
Text: '"' (NAME_CHARACTERS | VALUE_CHARACTERS | TEXT_CHARACTERS)* '"';

// Comments -> ignored
MULTI_LINE_COMMENT: '/*' .*? '*/' -> skip;
SINGLE_LINE_COMMENT: '//' .*? '\r'? '\n' -> skip;
SKIPPABLE_NEWLINE: '\r'? '\n'  -> skip ;
WS: [ \t]+ -> skip ;