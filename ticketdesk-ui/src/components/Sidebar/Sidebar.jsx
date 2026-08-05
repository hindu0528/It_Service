import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LayoutDashboard, Ticket, PlusCircle } from 'lucide-react';
import styles from './Sidebar.module.css';

const Sidebar = () => {
  const { isAgentOrAdmin } = useAuth();

  return (
    <aside className={styles.sidebar}>
      <NavLink to="/create-ticket" className={styles.createBtn}>
        <PlusCircle size={18} />
        <span>Create Ticket</span>
      </NavLink>

      <div>
        <div className={styles.sectionTitle}>Navigation</div>
        <ul className={styles.navList}>
          {isAgentOrAdmin() && (
            <li>
              <NavLink
                to="/dashboard"
                className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
              >
                <LayoutDashboard size={18} />
                <span>Dashboard</span>
              </NavLink>
            </li>
          )}

          <li>
            <NavLink
              to="/tickets"
              className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
            >
              <Ticket size={18} />
              <span>Tickets</span>
            </NavLink>
          </li>
        </ul>
      </div>
    </aside>
  );
};

export default Sidebar;
