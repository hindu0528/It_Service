import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTheme } from '../../context/ThemeContext';
import {
  Ticket,
  ShieldCheck,
  Zap,
  Layers,
  Cloud,
  CheckCircle2,
  ArrowRight,
  Sun,
  Moon,
  Lock,
  Cpu,
  RefreshCw,
  Sparkles
} from 'lucide-react';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import PriorityBadge from '../../components/PriorityBadge/PriorityBadge';
import CategoryBadge from '../../components/CategoryBadge/CategoryBadge';
import styles from './Landing.module.css';

const Landing = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className={styles.landing}>
      {/* Navbar Header */}
      <header className={styles.header}>
        <div className={styles.brand}>
          <div className={styles.logoIcon}>
            <Ticket size={22} />
          </div>
          <span className={styles.brandName}>TicketDesk</span>
        </div>

        <div className={styles.headerActions}>
          <button
            className={styles.themeToggle}
            onClick={toggleTheme}
            title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
          >
            {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
          </button>

          <Link to="/login" className={styles.loginBtn}>
            Sign In
          </Link>
          <Link to="/register" className={styles.registerBtn}>
            Get Started
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.badge}>
            <Sparkles size={14} />
            <span>Production Java 21 & Spring Boot 3.3 Platform</span>
          </div>

          <h1 className={styles.heroTitle}>
            Enterprise <span className={styles.highlight}>IT Incident & Support</span> Tracker
          </h1>

          <p className={styles.heroDesc}>
            Deploy a real-world, high-availability ticket management system powered by Spring Cloud Microservices, OpenFeign Resilience4j Circuit Breakers, Apache Kafka Event Consumers, and AWS S3 Presigned Uploads.
          </p>

          <div className={styles.heroCtas}>
            <button className={styles.primaryCta} onClick={() => navigate('/register')}>
              Launch Support Workspace <ArrowRight size={18} />
            </button>
            <button className={styles.secondaryCta} onClick={() => navigate('/login')}>
              Quick Demo Login
            </button>
          </div>
        </div>

        {/* Interactive Preview Mockup Card */}
        <div className={styles.previewCard}>
          <div className={styles.previewHeader}>
            <span className={styles.previewTitle}>Live Support Operations Feed</span>
            <StatusBadge status="IN_PROGRESS" />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <div style={{ fontSize: '1.1rem', fontWeight: 800 }}>#1042 - VPN Connection Drops on Corporate Wi-Fi</div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <CategoryBadge category="NETWORK" />
              <PriorityBadge priority="HIGH" />
            </div>
          </div>

          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', backgroundColor: 'var(--bg-input)', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
            <strong>Assigned Agent:</strong> Sarah Jenkins (ID: 4) <br />
            <strong>State Machine:</strong> Validated transition from <code>OPEN</code> &rarr; <code>IN_PROGRESS</code>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            <span>Kafka Topic: <code>ticket.status-changed</code></span>
            <span style={{ color: '#22c55e', fontWeight: 600 }}>✓ Presigned S3 Attached</span>
          </div>
        </div>
      </section>

      {/* Feature Section */}
      <section className={styles.featuresSection}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>Engineered for Scale & Security</h2>
          <p className={styles.sectionSubtitle}>
            Built strictly following industrial microservices design standards and domain security rules.
          </p>
        </div>

        <div className={styles.featureGrid}>
          <div className={styles.featureCard}>
            <div className={styles.featureIcon}><Layers size={24} /></div>
            <h3 className={styles.featureName}>Spring Cloud Microservices</h3>
            <p className={styles.featureDesc}>
              Decoupled architecture featuring Config Server, Eureka Service Registry, and Spring Cloud Gateway for edge routing.
            </p>
          </div>

          <div className={styles.featureCard}>
            <div className={styles.featureIcon}><Lock size={24} /></div>
            <h3 className={styles.featureName}>Strict Role-Based Access</h3>
            <p className={styles.featureDesc}>
              JWT authentication enforcing role boundaries: regular users create/track tickets, while Agents and Admins control status workflows.
            </p>
          </div>

          <div className={styles.featureCard}>
            <div className={styles.featureIcon}><RefreshCw size={24} /></div>
            <h3 className={styles.featureName}>Status Lifecycle Machine</h3>
            <p className={styles.featureDesc}>
              Enforces valid state transitions (OPEN &rarr; IN_PROGRESS &rarr; RESOLVED &rarr; CLOSED), rejecting invalid jumps with HTTP 400.
            </p>
          </div>

          <div className={styles.featureCard}>
            <div className={styles.featureIcon}><Cloud size={24} /></div>
            <h3 className={styles.featureName}>S3 Presigned URL Uploads</h3>
            <p className={styles.featureDesc}>
              Generates AWS-compatible presigned S3 upload URLs for direct client uploads, with metadata confirmed via OpenFeign.
            </p>
          </div>

          <div className={styles.featureCard}>
            <div className={styles.featureIcon}><Zap size={24} /></div>
            <h3 className={styles.featureName}>Kafka Asynchronous Events</h3>
            <p className={styles.featureDesc}>
              Publishes domain events (created, status-changed, comment-added) consumed asynchronously by Notification Service listeners.
            </p>
          </div>

          <div className={styles.featureCard}>
            <div className={styles.featureIcon}><ShieldCheck size={24} /></div>
            <h3 className={styles.featureName}>Resilience4j Circuit Breakers</h3>
            <p className={styles.featureDesc}>
              Inter-service OpenFeign calls are protected with sliding-window circuit breakers and automatic fallback handling.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <div className={styles.brand}>
            <div className={styles.logoIcon} style={{ width: 32, height: 32 }}>
              <Ticket size={18} />
            </div>
            <span className={styles.brandName} style={{ fontSize: '1.1rem' }}>TicketDesk</span>
          </div>

          <div className={styles.footerText}>
            © 2026 TicketDesk IT Support System • Built with Java 21, Spring Boot 3.3 & React
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Landing;
