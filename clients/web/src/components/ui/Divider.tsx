import './Divider.css';

interface DividerProps {
  /** Text to display in the center (e.g., "or") */
  text?: string;
}

/**
 * Horizontal divider with optional centered text.
 *
 * @example
 * // Simple line
 * <Divider />
 *
 * // With text
 * <Divider text="or" />
 */
export default function Divider({ text }: DividerProps) {
  if (!text) {
    return <hr className="divider" />;
  }

  return (
    <div className="divider-with-text">
      <div className="divider-with-text__line" />
      <span className="divider-with-text__text">{text}</span>
      <div className="divider-with-text__line" />
    </div>
  );
}
