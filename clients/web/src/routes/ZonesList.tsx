import { useState, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useZones } from '../hooks/useZones';
import { useI18n } from '../i18n';
import { Button } from '../components/ui';
import CreateZoneModal from '../components/CreateZoneModal';
import type { Zone } from '../types/api';
import './ZonesList.css';

/**
 * Zones page for viewing and creating household zones.
 *
 * States:
 * - Loading: skeleton placeholders in card
 * - Empty: centered icon + title + desc + CTA
 * - Error: warning banner with retry
 * - Normal: zones list card with dividers
 *
 * Pattern follows Invites page (TmZLg, gCEwX, ZWWj9).
 * @see Pencil frames: TmZLg (list), gCEwX (empty), ZWWj9 (mobile)
 */
export default function ZonesList() {
  const { t, formatRelativeTime } = useI18n();
  const { householdId } = useAuth();
  const { zones, isLoading, error, refetch } = useZones(householdId);

  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const handleCreateSuccess = useCallback(
    () => {
      refetch();
    },
    [refetch]
  );

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  if (!householdId) {
    return (
      <div className="zones">
        <div className="zones__wrapper">
          <div className="zones__empty-page">
            <p>{t('zones.noHousehold')}</p>
          </div>
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="zones">
        <div className="zones__wrapper">
          <section className="zones__section">
            <div className="zones__section-header">
              <h2 className="zones__section-title">{t('zones.title')}</h2>
              <Button variant="primary" size="sm" disabled>
                {t('zones.create')}
              </Button>
            </div>
            <div className="zones__card">
              {[1, 2, 3].map((i, idx) => (
                <div key={i}>
                  {idx > 0 && <div className="zones__divider" />}
                  <div className="zones__skeleton">
                    <div className="zones__skeleton-icon" />
                    <div className="zones__skeleton-content">
                      <div className="zones__skeleton-line" />
                      <div className="zones__skeleton-line zones__skeleton-line--short" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="zones">
        <div className="zones__wrapper">
          <section className="zones__section">
            <div className="zones__section-header">
              <h2 className="zones__section-title">{t('zones.title')}</h2>
            </div>
            <div className="zones__card">
              <div className="zones__error">
                <svg
                  className="zones__error-icon"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                  <line x1="12" y1="9" x2="12" y2="13" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                <div className="zones__error-content">
                  <h3 className="zones__error-title">{t('zones.unableLoad')}</h3>
                  <p className="zones__error-message">{t('common.checkConnection')}</p>
                </div>
                <Button variant="primary" size="sm" onClick={handleRetry}>
                  {t('common.retry')}
                </Button>
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  // Empty state
  if (zones.length === 0) {
    return (
      <div className="zones">
        <div className="zones__wrapper">
          <section className="zones__section">
            <div className="zones__section-header">
              <h2 className="zones__section-title">{t('zones.title')}</h2>
            </div>
            <div className="zones__card">
              <div className="zones__empty">
                <svg
                  className="zones__empty-icon"
                  width="40"
                  height="40"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <rect x="3" y="3" width="7" height="7" />
                  <rect x="14" y="3" width="7" height="7" />
                  <rect x="14" y="14" width="7" height="7" />
                  <rect x="3" y="14" width="7" height="7" />
                </svg>
                <h3 className="zones__empty-title">{t('zones.noZones')}</h3>
                <p className="zones__empty-desc">
                  {t('zones.emptyDesc')}
                </p>
                <Button variant="primary" size="md" onClick={() => setIsCreateOpen(true)}>
                  {t('zones.create')}
                </Button>
              </div>
            </div>
          </section>
        </div>

        <CreateZoneModal
          householdId={householdId}
          open={isCreateOpen}
          onClose={() => setIsCreateOpen(false)}
          onSuccess={handleCreateSuccess}
        />
      </div>
    );
  }

  // Normal state with zones
  return (
    <div className="zones">
      <div className="zones__wrapper">
        <section className="zones__section">
          <div className="zones__section-header">
            <h2 className="zones__section-title">{t('zones.title')}</h2>
            <Button variant="primary" size="sm" onClick={() => setIsCreateOpen(true)}>
              {t('zones.create')}
            </Button>
          </div>
          <div className="zones__card">
            {zones.map((zone: Zone, idx: number) => (
              <div key={zone.id}>
                {idx > 0 && <div className="zones__divider" />}
                <div className="zones__item">
                  <div className="zones__item-icon">
                    <svg
                      width="18"
                      height="18"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                    >
                      <rect x="3" y="3" width="7" height="7" />
                      <rect x="14" y="3" width="7" height="7" />
                      <rect x="14" y="14" width="7" height="7" />
                      <rect x="3" y="14" width="7" height="7" />
                    </svg>
                  </div>
                  <div className="zones__item-info">
                    <span className="zones__item-name" title={zone.name}>
                      {zone.name}
                    </span>
                    <span className="zones__item-meta">
                      {t('zones.created', { time: formatRelativeTime(zone.createdAt) })}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
          {zones.length >= 10 && (
            <p className="zones__hint">
              {t('zones.countHint', { count: zones.length })}
            </p>
          )}
        </section>
      </div>

      <CreateZoneModal
        householdId={householdId}
        open={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={handleCreateSuccess}
      />
    </div>
  );
}
