interface ErrorMessageProps {
  error: Error;
  onRetry?: () => void;
}

export default function ErrorMessage({ error, onRetry }: ErrorMessageProps) {
  return (
    <div className="card error-message">
      <p>Error: {error.message}</p>
      {onRetry && (
        <button className="button" onClick={onRetry} type="button">
          Retry
        </button>
      )}
    </div>
  );
}
