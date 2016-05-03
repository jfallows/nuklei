/*
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
grammar Nuklei;

specification
   : scope
   ;

scope
   : KW_SCOPE ID LEFT_BRACE definition * RIGHT_BRACE
   ;

scoped_name
   : (DOUBLE_COLON)? ID (DOUBLE_COLON ID)*
   ;

definition
   : type_decl
   | scope
   ;

positive_int_const
   : HEX_LITERAL
   | INTEGER_LITERAL
   ;

type_decl
   : constr_type_spec
   ;

type_declarator
   : type_spec declarators
   ;

type_spec
   : simple_type_spec
   | constr_type_spec
   ;

simple_type_spec
   : base_type_spec
   | template_type_spec
   | scoped_name
   ;

base_type_spec
   : integer_type
   | octets_type
   | string_type
   ;

template_type_spec
   : list_type
   | string_type
   ;

constr_type_spec
   : struct_type
   | union_type
   ;

declarators
   : declarator (COMMA declarator)*
   ;

declarator
   : ID
   ;

integer_type
   : int8_type
   | int16_type
   | int32_type
   | int64_type
   | uint8_type
   | uint16_type
   | uint32_type
   | uint64_type
   ;

int8_type
   : KW_INT8
   ;

int16_type
   : KW_INT16
   ;

int32_type
   : KW_INT32
   ;

int64_type
   : KW_INT64
   ;

uint8_type
   : KW_UINT8
   ;

uint16_type
   : KW_UINT16
   ;

uint32_type
   : KW_UINT32
   ;

uint64_type
   : KW_UINT64
   ;

octets_type
   : KW_OCTETS LEFT_SQUARE_BRACKET positive_int_const RIGHT_SQUARE_BRACKET
   | KW_OCTETS
   ;

struct_type
   : KW_STRUCT ID (KW_EXTENDS scoped_name)? (LEFT_SQUARE_BRACKET int_literal RIGHT_SQUARE_BRACKET)? LEFT_BRACE member_list RIGHT_BRACE
   ;

member_list
   : member *
   ;

member
   : type_spec declarators SEMICOLON
   ;

union_type
   : KW_UNION ID KW_SWITCH LEFT_BRACKET KW_UINT8 RIGHT_BRACKET LEFT_BRACE case_list RIGHT_BRACE
   ;

case_list
   : case_member *
   ;

case_member
   : KW_CASE int_literal COLON member
   ;

list_type
   : KW_LIST LEFT_ANG_BRACKET simple_type_spec COMMA positive_int_const RIGHT_ANG_BRACKET
   | KW_LIST LEFT_ANG_BRACKET simple_type_spec RIGHT_ANG_BRACKET
   ;

string_type
   : KW_STRING LEFT_ANG_BRACKET positive_int_const RIGHT_ANG_BRACKET
   | KW_STRING
   ;

int_literal
   : INTEGER_LITERAL
   | HEX_LITERAL
   ;

INTEGER_LITERAL
   : ('0' | '1' .. '9' '0' .. '9'*) INTEGER_TYPE_SUFFIX?
   ;


HEX_LITERAL
   : '0' ('x' | 'X') HEX_DIGIT + INTEGER_TYPE_SUFFIX?
   ;


fragment HEX_DIGIT
   : ('0' .. '9' | 'a' .. 'f' | 'A' .. 'F')
   ;


fragment INTEGER_TYPE_SUFFIX
   : ('l' | 'L')
   ;


fragment LETTER
   : '\u0024' | '\u0041' .. '\u005a' | '\u005f' | '\u0061' .. '\u007a' | '\u00c0' .. '\u00d6' | '\u00d8' .. '\u00f6' | '\u00f8' .. '\u00ff' | '\u0100' .. '\u1fff' | '\u3040' .. '\u318f' | '\u3300' .. '\u337f' | '\u3400' .. '\u3d2d' | '\u4e00' .. '\u9fff' | '\uf900' .. '\ufaff'
   ;


fragment ID_DIGIT
   : '\u0030' .. '\u0039' | '\u0660' .. '\u0669' | '\u06f0' .. '\u06f9' | '\u0966' .. '\u096f' | '\u09e6' .. '\u09ef' | '\u0a66' .. '\u0a6f' | '\u0ae6' .. '\u0aef' | '\u0b66' .. '\u0b6f' | '\u0be7' .. '\u0bef' | '\u0c66' .. '\u0c6f' | '\u0ce6' .. '\u0cef' | '\u0d66' .. '\u0d6f' | '\u0e50' .. '\u0e59' | '\u0ed0' .. '\u0ed9' | '\u1040' .. '\u1049'
   ;


SEMICOLON
   : ';'
   ;


COLON
   : ':'
   ;


COMMA
   : ','
   ;


LEFT_BRACE
   : '{'
   ;


RIGHT_BRACE
   : '}'
   ;


LEFT_SQUARE_BRACKET
   : '['
   ;


RIGHT_SQUARE_BRACKET
   : ']'
   ;


LEFT_BRACKET
   : '('
   ;


RIGHT_BRACKET
   : ')'
   ;


SLASH
   : '/'
   ;


LEFT_ANG_BRACKET
   : '<'
   ;


RIGHT_ANG_BRACKET
   : '>'
   ;


DOUBLE_COLON
   : '::'
   ;


KW_STRING
   : 'string'
   ;


KW_SWITCH
   : 'switch'
   ;


KW_CASE
   : 'case'
   ;


KW_DEFAULT
   : 'default'
   ;


KW_LIST
   : 'list'
   ;


KW_OCTETS
   : 'octets'
   ;


KW_STRUCT
   : 'struct'
   ;


KW_EXTENDS
   : 'extends'
   ;


KW_READONLY
   : 'readonly'
   ;


KW_INT8
   : 'int8'
   ;


KW_INT16
   : 'int16'
   ;


KW_INT32
   : 'int32'
   ;


KW_INT64
   : 'int64'
   ;


KW_UINT8
   : 'uint8'
   ;


KW_UINT16
   : 'uint16'
   ;


KW_UINT32
   : 'uint32'
   ;


KW_UINT64
   : 'uint64'
   ;


KW_UNION
   : 'union'
   ;


KW_SCOPE
   : 'scope'
   ;


ID
   : LETTER (LETTER | ID_DIGIT)*
   ;


WS
   : (' ' | '\r' | '\t' | '\u000C' | '\n') -> channel (HIDDEN)
   ;


COMMENT
   : '/*' .*? '*/' -> channel (HIDDEN)
   ;


LINE_COMMENT
   : '//' ~ ('\n' | '\r')* '\r'? '\n' -> channel (HIDDEN)
   ;
