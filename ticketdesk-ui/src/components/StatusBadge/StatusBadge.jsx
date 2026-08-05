import React from 'react';
import styles from './StatusBadge.module.css';

const StatusBadge = ({ status }) => {
  const normalized = status ? status.toLowerCase() : 'open';
  const styleClass = styles[normalized] || styles.open;

  const displayLabel = status ? status.replace('_', ' ') : 'OPEN';

  return (
    <span className={`${styles.badge} ${styleClass}`}>
      <span className={styles.dot}></span>
      {displayLabel}
    </span>
  );
};

export default StatusBadge;
