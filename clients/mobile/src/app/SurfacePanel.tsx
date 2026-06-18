import { CommandSurface } from '../features/command/CommandSurface';
import { HomeSurface } from '../features/home/HomeSurface';
import { ShoppingSurface } from '../features/shopping/ShoppingSurface';
import { TasksSurface } from '../features/tasks/TasksSurface';
import { DataSurface } from '../shared/ui/DataSurface';
import { ErrorPanel } from '../shared/ui/ErrorPanel';
import { LoadingPanel } from '../shared/ui/LoadingPanel';
import { MutationFeedback } from '../shared/ui/MutationFeedback';
import type { SurfacePanelProps } from './types';

export function SurfacePanel({
  surfaceKey,
  profile,
  selectedHousehold,
  models,
  readStatus,
  readError,
  mutationMessage,
  mutationError,
  mutationControls,
  commandControls,
  highlightedTaskId,
  accent,
  onNavigate,
  onRetry,
}: SurfacePanelProps) {
  if (!selectedHousehold) {
    return (
      <DataSurface
        accent={accent}
        title="No household selected"
        body="Create or join a household, then return to this app session."
      />
    );
  }

  if (readStatus === 'loading') {
    return <LoadingPanel label="Loading household" />;
  }

  if (readStatus === 'error') {
    return <ErrorPanel message={readError ?? 'Could not load household data.'} onRetry={onRetry} />;
  }

  if (surfaceKey === 'home') {
    return (
      <>
        <MutationFeedback message={mutationMessage} error={mutationError} />
        <HomeSurface
          accent={accent}
          commandControls={commandControls}
          models={models}
          onNavigate={onNavigate}
          profile={profile}
          selectedHousehold={selectedHousehold}
        />
      </>
    );
  }
  if (surfaceKey === 'tasks') {
    return (
      <>
        <MutationFeedback message={mutationMessage} error={mutationError} />
        <TasksSurface
          accent={accent}
          controls={mutationControls}
          highlightedTaskId={highlightedTaskId}
          tasks={models.tasks}
        />
      </>
    );
  }
  if (surfaceKey === 'shopping') {
    return (
      <>
        <MutationFeedback message={mutationMessage} error={mutationError} />
        <ShoppingSurface
          accent={accent}
          controls={mutationControls}
          items={models.shoppingItems}
          lists={models.shoppingLists}
        />
      </>
    );
  }
  return (
    <>
      <MutationFeedback message={mutationMessage} error={mutationError} />
      <CommandSurface
        accent={accent}
        controls={commandControls}
        models={models}
        selectedHousehold={selectedHousehold}
      />
    </>
  );
}
