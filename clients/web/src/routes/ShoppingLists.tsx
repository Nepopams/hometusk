import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useShoppingLists } from '../hooks/useShoppingLists';
import { useI18n } from '../i18n';
import { Button } from '../components/ui';
import type { ShoppingList } from '../types/api';
import './ShoppingLists.css';

/**
 * Shopping Lists page for viewing household shopping lists.
 *
 * States:
 * - Loading: skeleton placeholders in card
 * - Empty: centered icon + title + desc
 * - Error: warning banner with retry
 * - Normal: shopping lists card with dividers
 *
 * Pattern follows Zones/Members pages.
 * @see Pencil frames: dataShopping (shopping item interface)
 */
export default function ShoppingLists() {
  const { t } = useI18n();
  const { householdId } = useAuth();
  const { lists, isLoading, error, refetch } = useShoppingLists(householdId);

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  if (!householdId) {
    return (
      <div className="shopping-lists">
        <div className="shopping-lists__wrapper">
          <div className="shopping-lists__empty-page">
            <p>{t('shopping.noHouseholdLists')}</p>
          </div>
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="shopping-lists">
        <div className="shopping-lists__wrapper">
          <section className="shopping-lists__section">
            <div className="shopping-lists__section-header">
              <h2 className="shopping-lists__section-title">{t('shopping.lists')}</h2>
            </div>
            <div className="shopping-lists__card">
              {[1, 2, 3].map((i, idx) => (
                <div key={i}>
                  {idx > 0 && <div className="shopping-lists__divider" />}
                  <div className="shopping-lists__skeleton">
                    <div className="shopping-lists__skeleton-icon" />
                    <div className="shopping-lists__skeleton-content">
                      <div className="shopping-lists__skeleton-line" />
                      <div className="shopping-lists__skeleton-line shopping-lists__skeleton-line--short" />
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
      <div className="shopping-lists">
        <div className="shopping-lists__wrapper">
          <section className="shopping-lists__section">
            <div className="shopping-lists__section-header">
              <h2 className="shopping-lists__section-title">{t('shopping.lists')}</h2>
            </div>
            <div className="shopping-lists__card">
              <div className="shopping-lists__error">
                <svg
                  className="shopping-lists__error-icon"
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
                <div className="shopping-lists__error-content">
                  <h3 className="shopping-lists__error-title">{t('shopping.unableLoadLists')}</h3>
                  <p className="shopping-lists__error-message">{t('common.checkConnection')}</p>
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
  if (lists.length === 0) {
    return (
      <div className="shopping-lists">
        <div className="shopping-lists__wrapper">
          <section className="shopping-lists__section">
            <div className="shopping-lists__section-header">
              <h2 className="shopping-lists__section-title">{t('shopping.lists')}</h2>
            </div>
            <div className="shopping-lists__card">
              <div className="shopping-lists__empty">
                <svg
                  className="shopping-lists__empty-icon"
                  width="40"
                  height="40"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <circle cx="9" cy="21" r="1" />
                  <circle cx="20" cy="21" r="1" />
                  <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
                </svg>
                <h3 className="shopping-lists__empty-title">{t('shopping.noLists')}</h3>
                <p className="shopping-lists__empty-desc">
                  {t('shopping.noListsDesc')}
                </p>
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  // Normal state with lists
  return (
    <div className="shopping-lists">
      <div className="shopping-lists__wrapper">
        <section className="shopping-lists__section">
          <div className="shopping-lists__section-header">
            <h2 className="shopping-lists__section-title">{t('shopping.lists')}</h2>
          </div>
          <div className="shopping-lists__card">
            {lists.map((list: ShoppingList, idx: number) => (
              <div key={list.id}>
                {idx > 0 && <div className="shopping-lists__divider" />}
                <Link
                  to={`/households/${householdId}/shopping/${list.id}`}
                  className="shopping-lists__item"
                >
                  <div className="shopping-lists__item-icon">
                    <svg
                      width="18"
                      height="18"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                    >
                      <circle cx="9" cy="21" r="1" />
                      <circle cx="20" cy="21" r="1" />
                      <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
                    </svg>
                  </div>
                  <div className="shopping-lists__item-info">
                    <span className="shopping-lists__item-name" title={list.name}>
                      {list.name}
                    </span>
                    <span className="shopping-lists__item-meta">
                      {getItemsToBuyLabel(list.unpurchasedCount, t)}
                    </span>
                  </div>
                  <div className="shopping-lists__item-badge">
                    {list.unpurchasedCount > 0 && (
                      <span className="shopping-lists__badge">{list.unpurchasedCount}</span>
                    )}
                    <svg
                      className="shopping-lists__item-chevron"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                    >
                      <path d="M9 18l6-6-6-6" />
                    </svg>
                  </div>
                </Link>
              </div>
            ))}
          </div>
          {lists.length >= 10 && (
            <p className="shopping-lists__hint">
              {t('shopping.listsCountHint', { count: lists.length })}
            </p>
          )}
        </section>
      </div>
    </div>
  );
}

function getItemsToBuyLabel(count: number, t: ReturnType<typeof useI18n>['t']): string {
  if (count === 0) return t('shopping.allPurchased');
  if (count === 1) return t('shopping.oneToBuy');
  return t('shopping.manyToBuy', { count });
}
