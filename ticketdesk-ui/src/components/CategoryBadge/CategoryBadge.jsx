import React from 'react';
import styles from './CategoryBadge.module.css';

const CategoryBadge = ({ category }) => {
  return (
    <span className={styles.badge}>
      {category || 'OTHER'}
    </span>
  );
};

export default CategoryBadge;
