grammar Definitions;

import Tokens;

value: Name | Text | UnquotedText;

properties: (property | compoundProperty)* ;
property: Name ':' value ';'? ;
compoundProperty: Name '{' properties '}' ;

voxelDefinitions: voxelDefinition* ;
voxelDefinition: 'voxel' Name '{' properties '}';

worldGeneratorDefinitions: worldGeneratorDefinition* ;
worldGeneratorDefinition: 'generator' Name '{' properties '}';