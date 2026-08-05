import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ticketService } from '../../services/ticketService';
import { PlusCircle, ArrowLeft } from 'lucide-react';
import Toast from '../../components/Toast/Toast';
import styles from './CreateTicket.module.css';

const CreateTicket = () => {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('SOFTWARE');
  const [priority, setPriority] = useState('MEDIUM');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title || !description) {
      setToast({ message: 'Title and description are required', type: 'error' });
      return;
    }

    setLoading(true);
    try {
      const created = await ticketService.createTicket({ title, description, category, priority });
      setToast({ message: `Ticket #${created.id} created successfully!`, type: 'success' });
      setTimeout(() => navigate(`/tickets/${created.id}`), 1000);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to create ticket';
      setToast({ message: msg, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.icon}>
            <PlusCircle size={24} />
          </div>
          <div>
            <h1 className={styles.title}>Submit Support Request</h1>
            <p className={styles.subtitle}>Describe your technical issue or support request</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.formGroup}>
            <label className={styles.label}>Ticket Title</label>
            <input
              type="text"
              className={styles.input}
              placeholder="e.g. Outlook email sync error on laptop"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={loading}
            />
          </div>

          <div className={styles.row}>
            <div className={styles.formGroup}>
              <label className={styles.label}>Category</label>
              <select
                className={styles.select}
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                disabled={loading}
              >
                <option value="HARDWARE">HARDWARE</option>
                <option value="SOFTWARE">SOFTWARE</option>
                <option value="NETWORK">NETWORK</option>
                <option value="ACCESS">ACCESS</option>
                <option value="OTHER">OTHER</option>
              </select>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>Priority</label>
              <select
                className={styles.select}
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                disabled={loading}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>
            </div>
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Detailed Description</label>
            <textarea
              className={styles.textarea}
              placeholder="Provide steps to reproduce, error messages, and system details..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={loading}
            />
          </div>

          <div className={styles.actions}>
            <button type="button" className={styles.cancelBtn} onClick={() => navigate('/tickets')} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className={styles.submitBtn} disabled={loading}>
              {loading ? 'Submitting...' : 'Create Ticket'}
            </button>
          </div>
        </form>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default CreateTicket;
