grammar WorldGeneratorDefinitions;

import Definitions;

worldGeneratorDefinitions: worldGeneratorDefinition* ;
worldGeneratorDefinition: 'generator' Name '{' properties '}';