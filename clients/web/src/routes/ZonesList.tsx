import { useState, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useZones } from '../hooks/useZones';
import { Button } from '../components/ui';
import CreateZoneModal from '../components/CreateZoneModal';
import type { Zone } from '../types/api';
import './ZonesList.css';

/**
 * Zones page for viewing and creating household zones.
 *
 * States:
 * - Loading: skeleton placeholders
 * - Empty: welcome message with create CTA
 * - Error: warning banner with retry
 * - Normal: zones list with create button
 *
 * Pattern follows Dashboard/Invites pages.
 * @see Pencil CreateHouseholdModal pattern: NmuGH, D5bh0, w7hAD, DMAll
 */
export default function ZonesList() {
  const { householdId } = useAuth();
  const { zones, isLoading, error, refetch } = useZones(householdId);

  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const handleCreateSuccess = useCallback(
    (_zone: { id: string; name: string }) => {
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
            <p>Please select a household to view zones.</p>
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
          <div className="zones__header">
            <h1 className="zones__title">Zones</h1>
            <Button variant="primary" size="md" disabled>
              Create zone
            </Button>
          </div>
          <div className="zones__list">
            {[1, 2, 3].map((i) => (
              <div key={i} className="zones__skeleton">
                <div className="zones__skeleton-icon" />
                <div className="zones__skeleton-content">
                  <div className="zones__skeleton-line" />
                  <div className="zones__skeleton-line zones__skeleton-line--short" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="zones">
        <div className="zones__wrapper">
          <div className="zones__header">
            <h1 className="zones__title">Zones</h1>
          </div>
          <div className="zones__error">
            <div className="zones__error-box">
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
                <h3 className="zones__error-title">Unable to load zones</h3>
                <p className="zones__error-message">Check your connection and try again.</p>
              </div>
            </div>
            <Button variant="primary" size="md" onClick={handleRetry}>
              Retry
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // Empty state
  if (zones.length === 0) {
    return (
      <div className="zones">
        <div className="zones__wrapper">
          <div className="zones__header">
            <h1 className="zones__title">Zones</h1>
          </div>
          <div className="zones__empty">
            <div className="zones__empty-icon">
              <svg
                width="32"
                height="32"
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
            <h2 className="zones__empty-title">No zones yet</h2>
            <p className="zones__empty-desc">
              Zones help organize tasks by area of your home. Create your first zone to get
              started.
            </p>
            <Button variant="primary" size="md" onClick={() => setIsCreateOpen(true)}>
              Create zone
            </Button>
          </div>
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
        <div className="zones__header">
          <h1 className="zones__title">Zones</h1>
          <Button variant="primary" size="md" onClick={() => setIsCreateOpen(true)}>
            Create zone
          </Button>
        </div>

        <div className="zones__list">
          {zones.map((zone: Zone) => (
            <div key={zone.id} className="zones__item">
              <div className="zones__item-icon">
                <svg
                  width="20"
                  height="20"
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
                <span className="zones__item-name">{zone.name}</span>
                <span className="zones__item-meta">
                  Created {formatRelativeTime(zone.createdAt)}
                </span>
              </div>
            </div>
          ))}
        </div>
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

function formatRelativeTime(timestamp: string): string {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diffMs = now - then;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  return `${diffDays}d ago`;
}
