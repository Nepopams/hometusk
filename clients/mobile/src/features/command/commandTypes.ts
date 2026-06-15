import type { ColorValue } from 'react-native';

import type { CommandResponse, HouseholdSummary } from '../../api/types';
import type { CommandChatControls, HouseholdReadModels } from '../../app/types';

export type CommandSurfaceProps = {
  selectedHousehold: HouseholdSummary;
  models: HouseholdReadModels;
  accent: ColorValue;
  controls: CommandChatControls;
};

export type CommandOutcomeCardProps = {
  response: CommandResponse;
  accent: ColorValue;
};
