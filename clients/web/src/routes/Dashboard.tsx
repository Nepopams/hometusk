import { useMemo, useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import InviteModal from '../components/InviteModal';
import { useAuth } from '../hooks/useAuth';
import { useMembers } from '../hooks/useMembers';
import { useRoutines } from '../hooks/useRoutines';
import { useShoppingLists } from '../hooks/useShoppingLists';
import { useTasks } from '../hooks/useTasks';
import { useI18n } from '../i18n';
import { ApiError } from '../lib/errors';
import type { HouseholdMember, Routine, ShoppingList, Task } from '../types/api';
import './Dashboard.css';

type TFunction = ReturnType<typeof useI18n>['t'];

interface SummaryCardProps {
  title: string;
  eyebrow: string;
  badge: string;
  linkTo: string;
  linkLabel: string;
  isLoading: boolean;
  error: Error | null;
  onRetry: () => void;
  children: ReactNode;
}

interface StatItem {
  label: string;
  value: number | string;
  tone?: 'warning' | 'success' | 'muted';
}

export default function Dashboard() {
  const { householdId, user } = useAuth();
  const { householdId: householdIdParam } = useParams();
  const { t } = useI18n();
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  const activeHouseholdId = householdIdParam ?? householdId ?? null;
  const currentHousehold = user?.households.find((h) => h.id === activeHouseholdId);

  const {
    tasks,
    isLoading: tasksLoading,
    error: tasksError,
    refetch: refetchTasks,
  } = useTasks(activeHouseholdId, {});
  const {
    lists,
    isLoading: listsLoading,
    error: listsError,
    refetch: refetchLists,
  } = useShoppingLists(activeHouseholdId);
  const {
    routines,
    isLoading: routinesLoading,
    error: routinesError,
    refetch: refetchRoutines,
  } = useRoutines(activeHouseholdId);
  const {
    members,
    isLoading: membersLoading,
    error: membersError,
    refetch: refetchMembers,
  } = useMembers(activeHouseholdId);

  const dashboardPath = activeHouseholdId ? `/households/${activeHouseholdId}` : '/households';
  const tasksPath = `${dashboardPath}/tasks`;
  const commandsPath = `${dashboardPath}/commands`;
  const shoppingPath = `${dashboardPath}/shopping`;
  const routinesPath = `${dashboardPath}/routines`;
  const membersPath = `${dashboardPath}/members`;

  const accessDenied = [tasksError, listsError, routinesError, membersError].some(
    (error) => error instanceof ApiError && error.status === 403
  );

  const taskSummary = useMemo(() => buildTaskSummary(tasks), [tasks]);
  const shoppingSummary = useMemo(() => buildShoppingSummary(lists), [lists]);
  const routineSummary = useMemo(() => buildRoutineSummary(routines), [routines]);
  const memberSummary = useMemo(() => buildMemberSummary(members), [members]);

  if (!activeHouseholdId) {
    return (
      <div className="dashboard-page">
        <section className="dashboard-empty-page">
          <h1>{t('dashboard.title')}</h1>
          <p>{t('tasks.noHousehold')}</p>
          <Link className="dashboard__quick-link dashboard__quick-link--primary" to="/households">
            {t('common.backToHouseholdSelector')}
          </Link>
        </section>
      </div>
    );
  }

  if (accessDenied) {
    return (
      <div className="dashboard-page">
        <section className="dashboard-empty-page">
          <h1>{t('common.accessDenied')}</h1>
          <p>{t('tasks.noAccess')}</p>
          <Link className="dashboard__quick-link dashboard__quick-link--primary" to="/households">
            {t('common.backToHouseholdSelector')}
          </Link>
        </section>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <header className="dashboard-hero">
        <div className="dashboard-hero__copy">
          <p className="dashboard-hero__eyebrow">{t('dashboard.homeEyebrow')}</p>
          <h1 className="dashboard-hero__title">
            {currentHousehold
              ? t('dashboard.titleForHousehold', { name: currentHousehold.name })
              : t('dashboard.title')}
          </h1>
          <p className="dashboard-hero__subtitle">{t('dashboard.subtitle')}</p>
        </div>
        <div className="dashboard-hero__actions" aria-label={t('dashboard.quickActions')}>
          <Link className="dashboard__quick-link dashboard__quick-link--primary" to={commandsPath}>
            <span aria-hidden="true">+</span>
            {t('dashboard.addTaskAction')}
          </Link>
          <Link className="dashboard__quick-link dashboard__quick-link--secondary" to={shoppingPath}>
            {t('dashboard.openShoppingAction')}
          </Link>
          <button
            type="button"
            className="dashboard__quick-link dashboard__quick-link--ghost"
            onClick={() => setIsInviteOpen(true)}
          >
            {t('dashboard.inviteAction')}
          </button>
        </div>
      </header>

      <div className="dashboard-grid">
        <SummaryCard
          title={t('dashboard.tasksCardTitle')}
          eyebrow={t('tasks.title')}
          badge={t('dashboard.activeCount', { count: taskSummary.activeCount })}
          linkTo={tasksPath}
          linkLabel={t('dashboard.viewTasks')}
          isLoading={tasksLoading && tasks.length === 0}
          error={tasksError}
          onRetry={() => void refetchTasks()}
        >
          <StatsGrid
            items={[
              { label: t('dashboard.overdue'), value: taskSummary.overdueCount, tone: 'warning' },
              { label: t('dashboard.dueToday'), value: taskSummary.dueTodayCount },
              { label: t('dashboard.upcoming'), value: taskSummary.upcomingCount },
              { label: t('common.done'), value: taskSummary.doneCount, tone: 'success' },
            ]}
          />
          {taskSummary.previewTasks.length === 0 ? (
            <EmptyState
              title={t('dashboard.noTasksTitle')}
              description={t('dashboard.noTasksDesc')}
              linkTo={commandsPath}
              linkLabel={t('tasks.addViaCommand')}
            />
          ) : (
            <div className="dashboard-list">
              {taskSummary.previewTasks.map((task) => (
                <Link key={task.id} to={`${tasksPath}/${task.id}`} className="dashboard-list__item">
                  <span className="dashboard-list__title">{task.title}</span>
                  <span className="dashboard-list__meta">
                    {task.deadline ? formatDeadline(task.deadline, t) : t('common.noDeadline')}
                  </span>
                </Link>
              ))}
            </div>
          )}
        </SummaryCard>

        <SummaryCard
          title={t('dashboard.shoppingCardTitle')}
          eyebrow={t('shopping.lists')}
          badge={t('dashboard.listCount', { count: shoppingSummary.listCount })}
          linkTo={shoppingPath}
          linkLabel={t('dashboard.viewShopping')}
          isLoading={listsLoading && lists.length === 0}
          error={listsError}
          onRetry={() => void refetchLists()}
        >
          <StatsGrid
            items={[
              { label: t('dashboard.shoppingLists'), value: shoppingSummary.listCount },
              { label: t('dashboard.itemsToBuy'), value: shoppingSummary.unpurchasedCount },
            ]}
          />
          {shoppingSummary.previewLists.length === 0 ? (
            <EmptyState
              title={t('dashboard.noShoppingTitle')}
              description={t('dashboard.noShoppingDesc')}
              linkTo={shoppingPath}
              linkLabel={t('shopping.createList')}
            />
          ) : (
            <div className="dashboard-list">
              {shoppingSummary.previewLists.map((list) => (
                <Link key={list.id} to={`${shoppingPath}/${list.id}`} className="dashboard-list__item">
                  <span className="dashboard-list__title">{list.name}</span>
                  <span className="dashboard-list__meta">
                    {getItemsToBuyLabel(list.unpurchasedCount, t)}
                  </span>
                </Link>
              ))}
            </div>
          )}
        </SummaryCard>

        <SummaryCard
          title={t('dashboard.routinesCardTitle')}
          eyebrow={t('routines.title')}
          badge={t('dashboard.activeCount', { count: routineSummary.activeCount })}
          linkTo={routinesPath}
          linkLabel={t('dashboard.viewRoutines')}
          isLoading={routinesLoading && routines.length === 0}
          error={routinesError}
          onRetry={() => void refetchRoutines()}
        >
          <StatsGrid
            items={[
              { label: t('routines.active'), value: routineSummary.activeCount, tone: 'success' },
              { label: t('routines.paused'), value: routineSummary.pausedCount, tone: 'muted' },
            ]}
          />
          {routineSummary.previewRoutines.length === 0 ? (
            <EmptyState
              title={t('dashboard.noRoutinesTitle')}
              description={t('dashboard.noRoutinesDesc')}
              linkTo={routinesPath}
              linkLabel={t('routines.create')}
            />
          ) : (
            <div className="dashboard-list">
              {routineSummary.previewRoutines.map((routine) => (
                <Link key={routine.id} to={routinesPath} className="dashboard-list__item">
                  <span className="dashboard-list__title">{routine.title}</span>
                  <span className="dashboard-list__meta">
                    {routine.status === 'ACTIVE' ? t('routines.active') : t('routines.paused')}
                  </span>
                </Link>
              ))}
            </div>
          )}
        </SummaryCard>

        <SummaryCard
          title={t('dashboard.membersCardTitle')}
          eyebrow={t('members.title')}
          badge={t('dashboard.memberCount', { count: memberSummary.memberCount })}
          linkTo={membersPath}
          linkLabel={t('dashboard.viewMembers')}
          isLoading={membersLoading && members.length === 0}
          error={membersError}
          onRetry={() => void refetchMembers()}
        >
          <StatsGrid
            items={[
              { label: t('members.roleAdmin'), value: memberSummary.adminCount },
              { label: t('members.roleMember'), value: memberSummary.regularCount },
            ]}
          />
          {memberSummary.previewMembers.length === 0 ? (
            <EmptyState
              title={t('dashboard.noMembersTitle')}
              description={t('dashboard.noMembersDesc')}
              linkTo={membersPath}
              linkLabel={t('members.invite')}
            />
          ) : (
            <div className="dashboard-list">
              {memberSummary.previewMembers.map((member) => (
                <Link key={member.userId} to={membersPath} className="dashboard-list__item">
                  <span className="dashboard-list__title">{member.displayName}</span>
                  <span className="dashboard-list__meta">
                    {member.role === 'admin' ? t('members.roleAdmin') : t('members.roleMember')}
                  </span>
                </Link>
              ))}
            </div>
          )}
        </SummaryCard>
      </div>

      <InviteModal
        householdId={activeHouseholdId}
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
      />
    </div>
  );
}

function SummaryCard({
  title,
  eyebrow,
  badge,
  linkTo,
  linkLabel,
  isLoading,
  error,
  onRetry,
  children,
}: SummaryCardProps) {
  return (
    <section className="dashboard-card">
      <div className="dashboard-card__header">
        <div>
          <p className="dashboard-card__eyebrow">{eyebrow}</p>
          <h2 className="dashboard-card__title">{title}</h2>
        </div>
        <span className="dashboard-card__badge">{badge}</span>
      </div>
      <div className="dashboard-card__body">
        {isLoading ? (
          <DashboardSkeleton />
        ) : error ? (
          <SectionError onRetry={onRetry} />
        ) : (
          children
        )}
      </div>
      {!isLoading && !error && (
        <Link className="dashboard-card__footer-link" to={linkTo}>
          {linkLabel}
        </Link>
      )}
    </section>
  );
}

function StatsGrid({ items }: { items: StatItem[] }) {
  return (
    <div className="dashboard-stats">
      {items.map((item) => (
        <div
          key={item.label}
          className={`dashboard-stat${item.tone ? ` dashboard-stat--${item.tone}` : ''}`}
        >
          <span className="dashboard-stat__value">{item.value}</span>
          <span className="dashboard-stat__label">{item.label}</span>
        </div>
      ))}
    </div>
  );
}

function EmptyState({
  title,
  description,
  linkTo,
  linkLabel,
}: {
  title: string;
  description: string;
  linkTo: string;
  linkLabel: string;
}) {
  return (
    <div className="dashboard-empty">
      <h3 className="dashboard-empty__title">{title}</h3>
      <p className="dashboard-empty__desc">{description}</p>
      <Link className="dashboard-empty__link" to={linkTo}>
        {linkLabel}
      </Link>
    </div>
  );
}

function SectionError({ onRetry }: { onRetry: () => void }) {
  const { t } = useI18n();

  return (
    <div className="dashboard-section-error">
      <p>{t('dashboard.sectionError')}</p>
      <button type="button" className="dashboard-section-error__button" onClick={onRetry}>
        {t('common.retry')}
      </button>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="dashboard-skeleton" aria-hidden="true">
      <div className="dashboard-skeleton__stats">
        <span />
        <span />
        <span />
      </div>
      <span className="dashboard-skeleton__line" />
      <span className="dashboard-skeleton__line dashboard-skeleton__line--short" />
      <span className="dashboard-skeleton__line" />
    </div>
  );
}

function buildTaskSummary(tasks: Task[]) {
  const now = new Date();
  const tomorrow = startOfTomorrow(now);
  const activeTasks = tasks.filter((task) => task.status !== 'done' && task.status !== 'cancelled');
  const doneCount = tasks.filter((task) => task.status === 'done').length;

  const overdueCount = activeTasks.filter((task) => {
    const deadline = parseDate(task.deadline);
    return deadline ? deadline < now : false;
  }).length;

  const dueTodayCount = activeTasks.filter((task) => {
    const deadline = parseDate(task.deadline);
    return deadline ? deadline >= now && deadline < tomorrow : false;
  }).length;

  const upcomingCount = activeTasks.filter((task) => {
    const deadline = parseDate(task.deadline);
    return deadline ? deadline >= tomorrow : false;
  }).length;

  const previewTasks = [...activeTasks]
    .sort((left, right) => compareTasksByUrgency(left, right))
    .slice(0, 3);

  return {
    activeCount: activeTasks.length,
    doneCount,
    overdueCount,
    dueTodayCount,
    upcomingCount,
    previewTasks,
  };
}

function buildShoppingSummary(lists: ShoppingList[]) {
  const sortedLists = [...lists].sort(
    (left, right) =>
      (parseDate(right.createdAt)?.getTime() ?? 0) - (parseDate(left.createdAt)?.getTime() ?? 0)
  );

  return {
    listCount: lists.length,
    unpurchasedCount: lists.reduce((total, list) => total + list.unpurchasedCount, 0),
    previewLists: sortedLists.slice(0, 3),
  };
}

function buildRoutineSummary(routines: Routine[]) {
  const visibleRoutines = routines.filter((routine) => routine.status !== 'DELETED');
  const activeCount = visibleRoutines.filter((routine) => routine.status === 'ACTIVE').length;
  const pausedCount = visibleRoutines.filter((routine) => routine.status === 'PAUSED').length;

  return {
    activeCount,
    pausedCount,
    previewRoutines: visibleRoutines.slice(0, 3),
  };
}

function buildMemberSummary(members: HouseholdMember[]) {
  const adminCount = members.filter((member) => member.role === 'admin').length;

  return {
    memberCount: members.length,
    adminCount,
    regularCount: members.length - adminCount,
    previewMembers: members.slice(0, 4),
  };
}

function compareTasksByUrgency(left: Task, right: Task): number {
  const leftDeadline = parseDate(left.deadline)?.getTime() ?? Number.POSITIVE_INFINITY;
  const rightDeadline = parseDate(right.deadline)?.getTime() ?? Number.POSITIVE_INFINITY;

  if (leftDeadline !== rightDeadline) {
    return leftDeadline - rightDeadline;
  }

  return (parseDate(right.createdAt)?.getTime() ?? 0) - (parseDate(left.createdAt)?.getTime() ?? 0);
}

function formatDeadline(deadline: string, t: TFunction): string {
  const date = parseDate(deadline);
  if (!date) return t('common.noDeadline');

  const now = new Date();
  const today = startOfDay(now);
  const tomorrow = startOfTomorrow(now);
  const nextDay = startOfTomorrow(tomorrow);

  if (date < now) {
    const overdueDays = Math.ceil((today.getTime() - startOfDay(date).getTime()) / dayMs);
    if (overdueDays <= 0) return t('dashboard.overdue');
    const diffDays = Math.max(1, overdueDays);
    return t('tasks.overdueDays', { count: diffDays });
  }

  if (date >= today && date < tomorrow) return t('common.today');
  if (date >= tomorrow && date < nextDay) return t('common.tomorrow');

  const diffDays = Math.ceil((startOfDay(date).getTime() - today.getTime()) / dayMs);
  if (diffDays <= 7) return t('tasks.inDays', { count: diffDays });

  return date.toLocaleDateString();
}

function getItemsToBuyLabel(count: number, t: TFunction): string {
  if (count === 0) return t('shopping.allPurchased');
  if (count === 1) return t('shopping.oneToBuy');
  return t('shopping.manyToBuy', { count });
}

const dayMs = 1000 * 60 * 60 * 24;

function startOfDay(date: Date): Date {
  const result = new Date(date);
  result.setHours(0, 0, 0, 0);
  return result;
}

function startOfTomorrow(date: Date): Date {
  const result = startOfDay(date);
  result.setDate(result.getDate() + 1);
  return result;
}

function parseDate(value?: string): Date | null {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
