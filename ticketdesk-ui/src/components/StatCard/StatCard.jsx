import React from 'react';
import styles from './StatCard.module.css';

const StatCard = ({ title, value, icon: Icon, color = 'blue', subtitle }) => {
  const colorStyles = {
    blue: { bg: 'rgba(59, 130, 246, 0.15)', color: '#3b82f6' },
    yellow: { bg: 'rgba(234, 179, 8, 0.15)', color: '#eab308' },
    green: { bg: 'rgba(34, 197, 94, 0.15)', color: '#22c55e' },
    gray: { bg: 'rgba(148, 163, 184, 0.15)', color: '#94a3b8' },
    red: { bg: 'rgba(239, 68, 68, 0.15)', color: '#ef4444' },
    purple: { bg: 'rgba(168, 85, 247, 0.15)', color: '#a855f7' }
  };

  const currentStyle = colorStyles[color] || colorStyles.blue;

  return (
    <div className={styles.card}>
      {Icon && (
        <div
          className={styles.iconWrapper}
          style={{ backgroundColor: currentStyle.bg, color: currentStyle.color }}
        >
          <Icon size={24} />
        </div>
      )}
      <div className={styles.content}>
        <span className={styles.title}>{title}</span>
        <span className={styles.value}>{value}</span>
        {subtitle && <span className={styles.subtitle}>{subtitle}</span>}
      </div>
    </div>
  );
};

export default StatCard;
