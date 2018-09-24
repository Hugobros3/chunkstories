grammar ItemDefinitions;

import Definitions;

itemDefinitions: itemDefinition* ;
itemDefinition: 'item' Name '{' properties '}';