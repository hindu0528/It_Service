import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { notificationService } from '../../services/notificationService';
import { Sun, Moon, LogOut, ShieldCheck, Ticket } from 'lucide-react';
import styles from './Navbar.module.css';

const Navbar = () => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [kafkaHealth, setKafkaHealth] = useState('UP');

  useEffect(() => {
    const checkNotificationHealth = async () => {
      const data = await notificationService.getHealth();
      setKafkaHealth(data.status || 'UP');
    };
    checkNotificationHealth();
    const interval = setInterval(checkNotificationHealth, 15000);
    return () => clearInterval(interval);
  }, []);

  const rolesText = user?.roles ? user.roles.map(r => r.replace('ROLE_', '')).join(', ') : 'USER';

  return (
    <header className={styles.navbar}>
      <div className={styles.brand}>
        <div className={styles.logoIcon}>
          <Ticket size={20} />
        </div>
        <span className={styles.title}>TicketDesk</span>
      </div>

      <div className={styles.actions}>
        <div className={styles.healthBadge} title="Kafka Notification Listener Status">
          <span className={styles.healthDot}></span>
          <span>Kafka Consumer: {kafkaHealth}</span>
        </div>

        <button
          className={styles.themeToggle}
          onClick={toggleTheme}
          title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
        >
          {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
        </button>

        {user && (
          <div className={styles.userInfo}>
            <div className={styles.avatar}>
              {user.username ? user.username.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className={styles.userText}>
              <span className={styles.username}>{user.username}</span>
              <span className={styles.roleTag}>{rolesText}</span>
            </div>
          </div>
        )}

        {user && (
          <button className={styles.logoutBtn} onClick={logout} title="Sign Out">
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        )}
      </div>
    </header>
  );
};

export default Navbar;
