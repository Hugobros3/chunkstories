grammar EntityDefinitions;

import Definitions;


entitiesDefinitions: entitiesDefinition* ;
entitiesDefinition: 'entity' Name '{' properties '}';
