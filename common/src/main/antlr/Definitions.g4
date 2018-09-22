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

itemDefinitions: itemDefinition* ;
itemDefinition: 'generator' Name '{' properties '}';

entitiesDefinitions: entitiesDefinition* ;
entitiesDefinition: 'generator' Name '{' properties '}';

voxelMaterialDefinitions: voxelMaterialDefinition* ;
voxelMaterialDefinition: 'generator' Name '{' properties '}';
