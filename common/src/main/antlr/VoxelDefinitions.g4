grammar VoxelDefinitions;

import Definitions;

voxelDefinitions: voxelDefinition* ;
voxelDefinition: 'voxel' Name '{' properties '}';