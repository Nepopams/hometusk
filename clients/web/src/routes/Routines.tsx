import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useMembers } from '../hooks/useMembers';
import { useRoutines } from '../hooks/useRoutines';
import { useZones } from '../hooks/useZones';
import { useI18n } from '../i18n';
import { deleteRoutine, pauseRoutine, resumeRoutine } from '../lib/api';
import { ApiError } from '../lib/errors';
import { Button } from '../components/ui';
import {
  DeleteRoutineModal,
  RoutineForm,
  RoutineRow,
  UpcomingInstances,
} from '../components/routines';
import type { Routine } from '../types/api';
import './Routines.css';

export default function Routines() {
  const { t } = useI18n();
  const { householdId } = useAuth();
  const { routines, isLoading, error, refetch } = useRoutines(householdId);
  const { zones, isLoading: zonesLoading } = useZones(householdId);
  const { members, isLoading: membersLoading } = useMembers(householdId);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRoutine, setEditingRoutine] = useState<Routine | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Routine | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [pausingId, setPausingId] = useState<string | null>(null);
  const [resumingId, setResumingId] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const openCreate = useCallback(() => {
    setEditingRoutine(null);
    setIsFormOpen(true);
  }, []);

  const openEdit = useCallback((routine: Routine) => {
    setEditingRoutine(routine);
    setIsFormOpen(true);
  }, []);

  const closeForm = useCallback(() => {
    setIsFormOpen(false);
    setEditingRoutine(null);
  }, []);

  const handleSaved = useCallback(() => {
    closeForm();
    refetch();
  }, [closeForm, refetch]);

  const handleDeleteOpen = useCallback((routine: Routine) => {
    setDeleteTarget(routine);
    setDeleteError(null);
  }, []);

  const handleDeleteClose = useCallback(() => {
    setDeleteTarget(null);
    setDeleteError(null);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    if (!householdId || !deleteTarget) return;

    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteRoutine(householdId, deleteTarget.id);
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      if (err instanceof ApiError) {
        const msg =
          typeof err.body === 'object' && err.body !== null && 'message' in err.body
            ? (err.body as { message?: string }).message
            : undefined;
        setDeleteError(msg || t('routines.deleteUnable'));
      } else {
        setDeleteError(t('routines.deleteUnable'));
      }
    } finally {
      setIsDeleting(false);
    }
  }, [householdId, deleteTarget, refetch, t]);

  const handlePause = useCallback(
    async (routine: Routine) => {
      if (!householdId) return;
      setPausingId(routine.id);
      try {
        await pauseRoutine(householdId, routine.id);
        refetch();
      } catch (err) {
        console.error('Failed to pause routine:', err);
      } finally {
        setPausingId(null);
      }
    },
    [householdId, refetch]
  );

  const handleResume = useCallback(
    async (routine: Routine) => {
      if (!householdId) return;
      setResumingId(routine.id);
      try {
        await resumeRoutine(householdId, routine.id);
        refetch();
      } catch (err) {
        console.error('Failed to resume routine:', err);
      } finally {
        setResumingId(null);
      }
    },
    [householdId, refetch]
  );

  const handleToggleExpand = useCallback((routine: Routine) => {
    setExpandedId((prev) => (prev === routine.id ? null : routine.id));
  }, []);

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  if (!householdId) {
    return (
      <div className="routines">
        <div className="routines__wrapper">
          <div className="routines__empty-page">
            <p>{t('routines.noHousehold')}</p>
          </div>
        </div>
      </div>
    );
  }

  if (error instanceof ApiError && error.status === 403) {
    return (
      <div className="routines">
        <div className="routines__wrapper">
          <section className="routines__section">
            <div className="routines__section-header">
              <h2 className="routines__section-title">{t('routines.title')}</h2>
            </div>
            <div className="routines__card">
              <div className="routines__error">
                <div className="routines__error-content">
                  <h3 className="routines__error-title">{t('common.accessDenied')}</h3>
                  <p className="routines__error-message">
                    {t('error.accessDeniedSubtitle')}
                  </p>
                </div>
                <Link to="/households">
                  <Button variant="primary" size="sm">
                    {t('common.backToHouseholds')}
                  </Button>
                </Link>
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="routines">
        <div className="routines__wrapper">
          <section className="routines__section">
            <div className="routines__section-header">
              <h2 className="routines__section-title">{t('routines.title')}</h2>
            </div>
            <div className="routines__card">
              <div className="routines__error">
                <div className="routines__error-content">
                  <h3 className="routines__error-title">{t('routines.unableLoad')}</h3>
                  <p className="routines__error-message">{t('common.checkConnection')}</p>
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

  if (isLoading && routines.length === 0) {
    return (
      <div className="routines">
        <div className="routines__wrapper">
          <section className="routines__section">
            <div className="routines__section-header">
              <h2 className="routines__section-title">{t('routines.title')}</h2>
              <Button variant="primary" size="sm" disabled>
                {t('routines.create')}
              </Button>
            </div>
            <div className="routines__card">
              {[1, 2, 3].map((idx) => (
                <div key={idx}>
                  {idx > 1 && <div className="routines__divider" />}
                  <div className="routines__skeleton">
                    <div className="routines__skeleton-line" />
                    <div className="routines__skeleton-line routines__skeleton-line--short" />
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    );
  }

  if (routines.length === 0) {
    return (
      <div className="routines">
        <div className="routines__wrapper">
          <section className="routines__section">
            <div className="routines__section-header">
              <h2 className="routines__section-title">{t('routines.title')}</h2>
            </div>
            <div className="routines__card">
              <div className="routines__empty">
                <h3 className="routines__empty-title">{t('routines.noRoutines')}</h3>
                <p className="routines__empty-desc">
                  {t('routines.emptyDesc')}
                </p>
                <Button variant="primary" size="md" onClick={openCreate}>
                  {t('routines.create')}
                </Button>
              </div>
            </div>
          </section>
        </div>

        <RoutineForm
          open={isFormOpen}
          householdId={householdId}
          routine={editingRoutine}
          zones={zones}
          members={members}
          isLookupsLoading={zonesLoading || membersLoading}
          onClose={closeForm}
          onSaved={handleSaved}
        />
      </div>
    );
  }

  return (
    <div className="routines">
      <div className="routines__wrapper">
        <section className="routines__section">
          <div className="routines__section-header">
            <h2 className="routines__section-title">{t('routines.title')}</h2>
            <Button variant="primary" size="sm" onClick={openCreate}>
              {t('routines.create')}
            </Button>
          </div>
          <div className="routines__card">
            {routines.map((routine, idx) => (
              <div key={routine.id}>
                {idx > 0 && <div className="routines__divider" />}
                <RoutineRow
                  routine={routine}
                  isPausing={pausingId === routine.id}
                  isResuming={resumingId === routine.id}
                  isExpanded={expandedId === routine.id}
                  onEdit={openEdit}
                  onDelete={handleDeleteOpen}
                  onPause={handlePause}
                  onResume={handleResume}
                  onToggleExpand={handleToggleExpand}
                />
                {expandedId === routine.id && (
                  <UpcomingInstances
                    householdId={householdId}
                    routineId={routine.id}
                    routineStatus={routine.status}
                    assignmentPolicy={routine.assignmentPolicy}
                  />
                )}
              </div>
            ))}
          </div>
          {routines.length >= 10 && (
            <p className="routines__hint">{t('routines.countHint', { count: routines.length })}</p>
          )}
        </section>
      </div>

      <RoutineForm
        open={isFormOpen}
        householdId={householdId}
        routine={editingRoutine}
        zones={zones}
        members={members}
        isLookupsLoading={zonesLoading || membersLoading}
        onClose={closeForm}
        onSaved={handleSaved}
      />

      <DeleteRoutineModal
        open={Boolean(deleteTarget)}
        routine={deleteTarget}
        isDeleting={isDeleting}
        error={deleteError}
        onClose={handleDeleteClose}
        onConfirm={handleDeleteConfirm}
      />
    </div>
  );
}
