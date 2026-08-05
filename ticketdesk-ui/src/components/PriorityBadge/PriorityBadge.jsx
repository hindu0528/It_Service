import React from 'react';
import styles from './PriorityBadge.module.css';

const PriorityBadge = ({ priority }) => {
  const normalized = priority ? priority.toLowerCase() : 'low';
  const styleClass = styles[normalized] || styles.low;

  return (
    <span className={`${styles.badge} ${styleClass}`}>
      {priority || 'LOW'}
    </span>
  );
};

export default PriorityBadge;
