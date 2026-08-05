import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ticketService } from '../../services/ticketService';
import { useAuth } from '../../context/AuthContext';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import PriorityBadge from '../../components/PriorityBadge/PriorityBadge';
import CategoryBadge from '../../components/CategoryBadge/CategoryBadge';
import { Filter, RefreshCw, ChevronLeft, ChevronRight, Plus, UserCheck } from 'lucide-react';
import styles from './TicketList.module.css';

const TicketList = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [category, setCategory] = useState('');
  const [assignedStatus, setAssignedStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  const fetchTickets = async () => {
    try {
      setLoading(true);
      const params = { page, size: 8 };
      if (status) params.status = status;
      if (priority) params.priority = priority;
      if (category) params.category = category;
      if (assignedStatus) params.assignedStatus = assignedStatus;

      const data = await ticketService.getTickets(params);
      setTickets(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch tickets:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTickets();
  }, [status, priority, category, assignedStatus, page]);

  const handleReset = () => {
    setStatus('');
    setPriority('');
    setCategory('');
    setAssignedStatus('');
    setPage(0);
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Support Tickets</h1>
          <p className={styles.subtitle}>Filter, inspect, and manage support tickets</p>
        </div>
        <button
          onClick={() => navigate('/create-ticket')}
          style={{ padding: '10px 16px', borderRadius: '8px', background: 'linear-gradient(135deg, #2563eb, #3b82f6)', color: '#ffffff', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px', border: 'none', cursor: 'pointer' }}
        >
          <Plus size={18} />
          New Ticket
        </button>
      </div>

      <div className={styles.filterCard}>
        <div className={styles.filterGroup}>
          <Filter size={16} style={{ color: 'var(--text-muted)' }} />
          <span className={styles.label}>Status:</span>
          <select className={styles.select} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All Statuses</option>
            <option value="OPEN">OPEN</option>
            <option value="IN_PROGRESS">IN PROGRESS</option>
            <option value="RESOLVED">RESOLVED</option>
            <option value="CLOSED">CLOSED</option>
          </select>
        </div>

        <div className={styles.filterGroup}>
          <span className={styles.label}>Priority:</span>
          <select className={styles.select} value={priority} onChange={(e) => { setPriority(e.target.value); setPage(0); }}>
            <option value="">All Priorities</option>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>
        </div>

        <div className={styles.filterGroup}>
          <span className={styles.label}>Category:</span>
          <select className={styles.select} value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }}>
            <option value="">All Categories</option>
            <option value="HARDWARE">HARDWARE</option>
            <option value="SOFTWARE">SOFTWARE</option>
            <option value="NETWORK">NETWORK</option>
            <option value="ACCESS">ACCESS</option>
            <option value="OTHER">OTHER</option>
          </select>
        </div>

        {isAdmin && (
          <div className={styles.filterGroup}>
            <UserCheck size={16} style={{ color: 'var(--primary-500)' }} />
            <span className={styles.label} style={{ color: 'var(--primary-500)', fontWeight: 700 }}>Assignment:</span>
            <select className={styles.select} value={assignedStatus} onChange={(e) => { setAssignedStatus(e.target.value); setPage(0); }}>
              <option value="">All Assignment Statuses</option>
              <option value="UNASSIGNED">Unassigned Tickets</option>
              <option value="ASSIGNED">Assigned Tickets</option>
            </select>
          </div>
        )}

        <button className={styles.resetBtn} onClick={handleReset} title="Clear Filters">
          <RefreshCw size={14} style={{ marginRight: '4px' }} />
          Reset Filters
        </button>
      </div>

      <div className={styles.tableCard}>
        {loading ? (
          <p style={{ padding: '2rem', textStyle: 'italic', color: 'var(--text-muted)' }}>Fetching ticket records...</p>
        ) : tickets.length === 0 ? (
          <p style={{ padding: '2rem', textStyle: 'italic', color: 'var(--text-muted)' }}>No tickets matched your filter criteria.</p>
        ) : (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Category</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Created By</th>
                <th>Assigned To</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <tr key={t.id} className={styles.row} onClick={() => navigate(`/tickets/${t.id}`)}>
                  <td style={{ fontWeight: 700, color: 'var(--primary-500)' }}>{t.ticketNumber || `TKT-${t.id}`}</td>
                  <td style={{ fontWeight: 600 }}>{t.title}</td>
                  <td><CategoryBadge category={t.category} /></td>
                  <td><PriorityBadge priority={t.priority} /></td>
                  <td><StatusBadge status={t.status} /></td>
                  <td>{t.createdByUsername || `User ${t.createdBy}`}</td>
                  <td>
                    {t.assignedToUsername ? (
                      <span style={{ color: 'var(--primary-500)', fontWeight: 600 }}>{t.assignedToUsername}</span>
                    ) : (
                      <span style={{ color: '#ef4444', fontStyle: 'italic', fontSize: '0.85rem' }}>Unassigned</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className={styles.pagination}>
          <span className={styles.pageInfo}>
            Showing page {page + 1} of {totalPages} ({totalElements} total items)
          </span>
          <div className={styles.pageBtns}>
            <button
              className={styles.pageBtn}
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              <ChevronLeft size={16} />
            </button>
            <button
              className={styles.pageBtn}
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TicketList;
