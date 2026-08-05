import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ticketService } from '../../services/ticketService';
import StatCard from '../../components/StatCard/StatCard';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import PriorityBadge from '../../components/PriorityBadge/PriorityBadge';
import CategoryBadge from '../../components/CategoryBadge/CategoryBadge';
import { Ticket, Clock, CheckCircle2, XCircle, AlertTriangle, ArrowRight } from 'lucide-react';
import styles from './Dashboard.module.css';

const Dashboard = () => {
  const navigate = useNavigate();
  const { user, isAgentOrAdmin } = useAuth();
  const [summary, setSummary] = useState(null);
  const [recentTickets, setRecentTickets] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        if (isAgentOrAdmin()) {
          const sumData = await ticketService.getDashboardSummary();
          setSummary(sumData);
        }
        const ticketsData = await ticketService.getTickets({ page: 0, size: 5 });
        setRecentTickets(ticketsData.content || []);
      } catch (err) {
        console.error('Failed to load dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const total = summary?.totalTickets || recentTickets.length;
  const statusCounts = summary?.countsByStatus || {};
  const priorityCounts = summary?.countsByPriority || {};

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>System Analytics Dashboard</h1>
          <p className={styles.subtitle}>Overview of support ticket operations, metrics, and state transitions</p>
        </div>
      </div>

      <div className={styles.gridStats}>
        <StatCard
          title="Total Tickets"
          value={total}
          icon={Ticket}
          color="blue"
          subtitle="All system tickets"
        />
        <StatCard
          title="Open"
          value={statusCounts.OPEN || 0}
          icon={Clock}
          color="yellow"
          subtitle="Awaiting agent pick"
        />
        <StatCard
          title="In Progress"
          value={statusCounts.IN_PROGRESS || 0}
          icon={AlertTriangle}
          color="purple"
          subtitle="Active investigation"
        />
        <StatCard
          title="Resolved"
          value={statusCounts.RESOLVED || 0}
          icon={CheckCircle2}
          color="green"
          subtitle="Fix verified"
        />
        <StatCard
          title="Closed"
          value={statusCounts.CLOSED || 0}
          icon={XCircle}
          color="gray"
          subtitle="Archived"
        />
      </div>

      <div className={styles.section}>
        <div className={styles.card}>
          <div className={styles.cardTitle}>
            <span>Recent Tickets</span>
            <button
              onClick={() => navigate('/tickets')}
              style={{ background: 'none', border: 'none', color: 'var(--primary-500)', fontSize: '0.85rem', fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
            >
              View All <ArrowRight size={14} />
            </button>
          </div>

          {loading ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Loading recent tickets...</p>
          ) : recentTickets.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No tickets found in the system.</p>
          ) : (
            <table className={styles.recentTable}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Category</th>
                  <th>Priority</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {recentTickets.map(t => (
                  <tr key={t.id} onClick={() => navigate(`/tickets/${t.id}`)}>
                    <td style={{ fontWeight: 700, color: 'var(--primary-500)' }}>{t.ticketNumber || `TKT-${t.id}`}</td>
                    <td style={{ fontWeight: 600 }}>{t.title}</td>
                    <td><CategoryBadge category={t.category} /></td>
                    <td><PriorityBadge priority={t.priority} /></td>
                    <td><StatusBadge status={t.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className={styles.card}>
          <div className={styles.cardTitle}>
            <span>Priority Distribution</span>
          </div>

          <div className={styles.priorityList}>
            <div className={styles.priorityItem}>
              <div className={styles.priorityLabel}>
                <PriorityBadge priority="CRITICAL" />
              </div>
              <span className={styles.priorityCount}>{priorityCounts.CRITICAL || 0}</span>
            </div>

            <div className={styles.priorityItem}>
              <div className={styles.priorityLabel}>
                <PriorityBadge priority="HIGH" />
              </div>
              <span className={styles.priorityCount}>{priorityCounts.HIGH || 0}</span>
            </div>

            <div className={styles.priorityItem}>
              <div className={styles.priorityLabel}>
                <PriorityBadge priority="MEDIUM" />
              </div>
              <span className={styles.priorityCount}>{priorityCounts.MEDIUM || 0}</span>
            </div>

            <div className={styles.priorityItem}>
              <div className={styles.priorityLabel}>
                <PriorityBadge priority="LOW" />
              </div>
              <span className={styles.priorityCount}>{priorityCounts.LOW || 0}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
