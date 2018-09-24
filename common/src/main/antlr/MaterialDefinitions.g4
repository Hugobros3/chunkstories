grammar MaterialDefinitions;

import Definitions;

voxelMaterialDefinitions: voxelMaterialDefinition* ;
voxelMaterialDefinition: 'material' Name '{' properties '}';