import type { GamificationSettings } from '../../types/api';

interface PrivacySettingsCardProps {
  settings: GamificationSettings;
  onUpdate: (settings: Partial<GamificationSettings>) => void;
  isUpdating: boolean;
}

export function PrivacySettingsCard({
  settings,
  onUpdate,
  isUpdating,
}: PrivacySettingsCardProps) {
  return (
    <div className="progress__card privacy-settings">
      <h2>Privacy Settings</h2>

      <div className="privacy-settings__option">
        <label className="privacy-settings__label">
          <input
            type="checkbox"
            checked={settings.showProgressToOthers}
            onChange={(e) => onUpdate({ showProgressToOthers: e.target.checked })}
            disabled={isUpdating}
          />
          <span>Show my progress to household members</span>
        </label>
      </div>

      <div className="privacy-settings__option">
        <label className="privacy-settings__label">
          <input
            type="checkbox"
            checked={settings.gamificationEnabled}
            onChange={(e) => onUpdate({ gamificationEnabled: e.target.checked })}
            disabled={isUpdating}
          />
          <span>Enable gamification</span>
        </label>
        {!settings.gamificationEnabled && (
          <p className="privacy-settings__warning">
            You will not earn points or badges while gamification is disabled.
          </p>
        )}
      </div>

      {isUpdating && <p className="privacy-settings__saving">Saving...</p>}
    </div>
  );
}
