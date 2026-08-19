grammar Spell;

/*
 * Parser
 */

spell: statement+;

statement: propertyStatement | attachedStatement;

// json property values
propertyBlock: '{' propertyStatement* '}' | propertyStatement;
propertyStatement: assignment | subobject;

assignment: propertyName '=' propertyValue ';';
propertyName: Identifier;
propertyValue: literal | literalArrayValue | objectArrayValue;
literalArrayValue: '[' (literal (',' literal)* ','?)? ']';
objectArrayValue: '[' (arrayElement (',' arrayElement)* ','?)? ']';
arrayElement: '{' propertyStatement* '}';

subobject: subobjectName propertyBlock;
subobjectName: Identifier;

// spell attached action blocks
attachedStatement: eventHandler | entity;
eventHandler: eventName scriptBlock;
eventName: OnCast | OnTick | OnCastEnd;
entity: 'entity' entityName scriptBlock;
entityName: Identifier;

// spell action syntax
scriptBlock: '{' scriptStatement* '}' | scriptStatement;
scriptStatement: ifBlock | filterBlock | selectBlock | actions;
ifBlock: 'if' '(' conditionExpr ')' scriptBlock elseBlock?;
filterBlock: 'filter' '(' conditionExpr ')' scriptBlock elseBlock?; // en_preds
elseBlock: 'else' scriptBlock;
selectBlock: 'select' '(' selectorList ')' scriptBlock; // targets
actions: mapHolder+ (';' | perEntityHit); // spell action list
perEntityHit: 'per_entity_hit' scriptBlock;

selectorList: selector ('||' selector)*;
selector: mapHolder; // spell targets entry

condition: mapHolder; // single condition
conditionParen: '(' conditionExpr ')' | condition;
conditionNot: Not conditionParen | conditionParen;
conditionAnd: conditionNot ('&&' conditionNot)*;
conditionExpr: conditionAnd;

// corresponds to MapHolder with type and map
mapHolder: mapHolderType '(' mapHolderArguments? ')';
mapHolderType: Identifier;
mapHolderArguments: argumentPositional (',' argumentPositional)* (',' argumentKeyValue)*
                    | argumentKeyValue (',' argumentKeyValue)*;
argumentPositional: literal;
argumentKeyValue: argumentKey '=' argumentValue;
argumentKey: Identifier;
argumentValue: literal;

literal: String | Double | Int | bool;
bool: True | False;

/*
 * Lexer
 */

Not: '!';

OnCast: 'on_cast';
OnTick: 'on_tick';
OnCastEnd: 'on_cast_end';

True: 'true';
False: 'false';

Double: Number? '.' Number;
Int: Number;
fragment Number: '0'..'9'+;

String: '"' ~[\r\n"]* '"';

Identifier: ('a'..'z' | 'A'..'Z' | '0'..'9' | [_$])+;

Comment: '//' ~[\r\n]* [\r]? [\n] -> skip;
BlockComment: '/*' .*? '*/' -> skip;

WS: [ \t\r\n]+ -> skip;