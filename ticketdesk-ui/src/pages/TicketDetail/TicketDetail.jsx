import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ticketService } from '../../services/ticketService';
import { attachmentService } from '../../services/attachmentService';
import { authService } from '../../services/authService';
import { useAuth } from '../../context/AuthContext';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import PriorityBadge from '../../components/PriorityBadge/PriorityBadge';
import CategoryBadge from '../../components/CategoryBadge/CategoryBadge';
import Toast from '../../components/Toast/Toast';
import { ArrowLeft, MessageSquare, Paperclip, Upload, ShieldCheck, Send, ExternalLink, Clock, UserCheck } from 'lucide-react';
import styles from './TicketDetail.module.css';

const TicketDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAgentOrAdmin } = useAuth();

  const [ticket, setTicket] = useState(null);
  const [comments, setComments] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const [agents, setAgents] = useState([]);
  const [selectedAgentId, setSelectedAgentId] = useState('');
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [submittingComment, setSubmittingComment] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [toast, setToast] = useState(null);

  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  const fetchTicketDetails = async () => {
    try {
      setLoading(true);
      const tkt = await ticketService.getTicketById(id);
      setTicket(tkt);

      const cmts = await ticketService.getCommentsByTicketId(id);
      setComments(cmts || []);

      const atts = await attachmentService.getAttachmentsByTicketId(id);
      setAttachments(atts || []);

      if (isAdmin) {
        try {
          const agentList = await authService.getAgents();
          setAgents(agentList || []);
        } catch (e) {
          console.warn('Could not fetch agent list:', e);
        }
      }
    } catch (err) {
      console.error('Failed to load ticket:', err);
      setToast({ message: err.response?.data?.message || 'Failed to fetch ticket details', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTicketDetails();
  }, [id]);

  const handleStatusChange = async (targetStatus) => {
    if (!ticket) return;

    // Sequential Validation check on Frontend
    if (ticket.status === 'OPEN' && targetStatus === 'CLOSED') {
      setToast({
        message: 'Cannot close an OPEN ticket directly! Tickets must follow the sequential lifecycle order: OPEN ➔ IN_PROGRESS ➔ RESOLVED ➔ CLOSED.',
        type: 'error'
      });
      return;
    }

    if (ticket.status === 'IN_PROGRESS' && targetStatus === 'CLOSED') {
      setToast({
        message: 'Cannot close an IN_PROGRESS ticket directly! Please mark as RESOLVED first after fix is complete.',
        type: 'error'
      });
      return;
    }

    try {
      const updated = await ticketService.updateTicketStatus(id, { status: targetStatus });
      setTicket(updated);
      setToast({ message: `Status updated to ${targetStatus}!`, type: 'success' });
    } catch (err) {
      const msg = err.response?.data?.message || `Failed to update status to ${targetStatus}`;
      setToast({ message: msg, type: 'error' });
    }
  };

  const handleAssignAgent = async () => {
    if (!selectedAgentId) {
      setToast({ message: 'Please select an agent to assign this ticket', type: 'error' });
      return;
    }

    setAssigning(true);
    try {
      const updated = await ticketService.assignTicket(id, parseInt(selectedAgentId, 10));
      setTicket(updated);
      setToast({ message: `Ticket #${id} successfully assigned to agent '${updated.assignedToUsername || 'Agent'}'!`, type: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to assign ticket', type: 'error' });
    } finally {
      setAssigning(false);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setSubmittingComment(true);
    try {
      const added = await ticketService.addComment(id, { text: newComment });
      setComments([...comments, added]);
      setNewComment('');
      setToast({ message: 'Comment posted successfully!', type: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'Failed to post comment', type: 'error' });
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    try {
      const uploaded = await attachmentService.uploadFile(file, parseInt(id, 10));
      setAttachments([...attachments, uploaded]);
      setToast({ message: `File '${file.name}' successfully uploaded & stored for Ticket #${id}!`, type: 'success' });
    } catch (err) {
      setToast({ message: err.response?.data?.message || 'File upload failed', type: 'error' });
    } finally {
      setUploading(false);
    }
  };

  if (loading) {
    return <div className={styles.page}><p>Loading ticket details...</p></div>;
  }

  if (!ticket) {
    return <div className={styles.page}><p>Ticket not found.</p></div>;
  }

  return (
    <div className={styles.page}>
      <button className={styles.backBtn} onClick={() => navigate('/tickets')}>
        <ArrowLeft size={16} /> Back to Tickets
      </button>

      <div className={styles.mainGrid}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className={styles.card}>
            <div className={styles.header}>
              <div>
                <h1 className={styles.title}>{ticket.ticketNumber || `TKT-${ticket.id}`} (Ticket #{ticket.userSequence || ticket.id}) - {ticket.title}</h1>
                <div className={styles.badges}>
                  <StatusBadge status={ticket.status} />
                  <PriorityBadge priority={ticket.priority} />
                  <CategoryBadge category={ticket.category} />
                </div>
              </div>
            </div>

            <div className={styles.descriptionBox}>
              {ticket.description}
            </div>

            {isAgentOrAdmin() && (
              <div className={styles.statusControlBox}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    <ShieldCheck size={16} style={{ color: 'var(--primary-500)' }} />
                    <span>Sequential Lifecycle Transition Control</span>
                  </div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    Sequence: <strong>OPEN ➔ IN_PROGRESS ➔ RESOLVED ➔ CLOSED</strong>
                  </span>
                </div>

                <div className={styles.statusBtns}>
                  <button
                    className={`${styles.statusBtn} ${styles.openBtn}`}
                    onClick={() => handleStatusChange('OPEN')}
                    disabled={ticket.status === 'OPEN'}
                  >
                    Mark OPEN
                  </button>
                  <button
                    className={`${styles.statusBtn} ${styles.progressBtn}`}
                    onClick={() => handleStatusChange('IN_PROGRESS')}
                    disabled={ticket.status === 'IN_PROGRESS'}
                  >
                    Set IN_PROGRESS
                  </button>
                  <button
                    className={`${styles.statusBtn} ${styles.resolveBtn}`}
                    onClick={() => handleStatusChange('RESOLVED')}
                    disabled={ticket.status === 'RESOLVED'}
                  >
                    Mark RESOLVED
                  </button>
                  <button
                    className={`${styles.statusBtn} ${styles.closeBtn}`}
                    onClick={() => handleStatusChange('CLOSED')}
                    disabled={ticket.status === 'CLOSED'}
                  >
                    Close Ticket
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className={styles.card}>
            <div className={styles.sectionTitle}>
              <MessageSquare size={18} />
              <span>Discussion Thread ({comments.length})</span>
            </div>

            <div className={styles.commentsList}>
              {comments.length === 0 ? (
                <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>No comments yet. Start the conversation below.</p>
              ) : (
                comments.map((c) => (
                  <div key={c.id} className={styles.commentCard}>
                    <div className={styles.commentHeader}>
                      <span className={styles.author}>{c.authorUsername || `User ${c.authorId}`}</span>
                      <span className={styles.time}>{c.createdAt ? new Date(c.createdAt).toLocaleString() : 'Just now'}</span>
                    </div>
                    <div className={styles.commentText}>{c.text}</div>
                  </div>
                ))
              )}
            </div>

            <form onSubmit={handleAddComment} className={styles.commentForm}>
              <textarea
                className={styles.textarea}
                placeholder="Write a response or update note..."
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                disabled={submittingComment}
              />
              <button type="submit" className={styles.commentSubmitBtn} disabled={submittingComment}>
                <Send size={14} style={{ marginRight: '6px' }} />
                {submittingComment ? 'Posting...' : 'Post Comment'}
              </button>
            </form>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className={styles.card}>
            <div className={styles.sectionTitle}>
              <Clock size={18} />
              <span>Metadata & Details</span>
            </div>

            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>Created By</span>
              <span className={styles.metaVal}>{ticket.createdByUsername || `User ${ticket.createdBy}`}</span>
            </div>

            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>Assigned Agent</span>
              <span className={styles.metaVal} style={{ color: ticket.assignedToUsername ? 'var(--primary-500)' : '#ef4444', fontWeight: 700 }}>
                {ticket.assignedToUsername || 'Unassigned'}
              </span>
            </div>

            {isAdmin && (
              <div style={{ marginTop: '0.75rem', padding: '0.75rem', background: 'rgba(59, 130, 246, 0.08)', borderRadius: '8px', border: '1px solid rgba(59, 130, 246, 0.2)' }}>
                <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--primary-500)', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
                  <UserCheck size={14} />
                  Admin Assignment Control
                </label>
                <div style={{ display: 'flex', gap: '6px' }}>
                  <select
                    value={selectedAgentId}
                    onChange={(e) => setSelectedAgentId(e.target.value)}
                    style={{ flex: 1, padding: '6px 8px', borderRadius: '6px', fontSize: '0.8rem', background: 'var(--bg-secondary)', color: 'var(--text-primary)', border: '1px solid var(--border-color)' }}
                  >
                    <option value="">-- Select Agent --</option>
                    {agents.map((ag) => (
                      <option key={ag.id} value={ag.id}>{ag.username} (ID: {ag.id})</option>
                    ))}
                  </select>
                  <button
                    onClick={handleAssignAgent}
                    disabled={assigning}
                    style={{ padding: '6px 12px', borderRadius: '6px', background: 'var(--primary-500)', color: '#ffffff', border: 'none', fontSize: '0.8rem', fontWeight: 700, cursor: 'pointer' }}
                  >
                    {assigning ? 'Assigning...' : 'Assign'}
                  </button>
                </div>
              </div>
            )}

            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>Created At</span>
              <span className={styles.metaVal}>{ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : 'N/A'}</span>
            </div>

            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>Last Updated</span>
              <span className={styles.metaVal}>{ticket.updatedAt ? new Date(ticket.updatedAt).toLocaleString() : 'N/A'}</span>
            </div>
          </div>

          <div className={styles.card}>
            <div className={styles.sectionTitle}>
              <Paperclip size={18} />
              <span>Attachments ({attachments.length})</span>
            </div>

            <div className={styles.attachmentsBox}>
              <label className={styles.uploadArea}>
                <Upload size={24} style={{ color: 'var(--primary-500)', marginBottom: '4px' }} />
                <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{uploading ? 'Generating Presigned URL...' : 'Click to Upload Attachment'}</div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Presigned S3 upload link generator</div>
                <input type="file" onChange={handleFileUpload} style={{ display: 'none' }} disabled={uploading} />
              </label>

              {attachments.map((att) => (
                <div key={att.id} className={styles.fileItem}>
                  <div>
                    <div style={{ fontWeight: 600 }}>{att.fileName}</div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{(att.fileSize / 1024).toFixed(1)} KB • {att.status}</div>
                  </div>
                  {att.presignedUrl && (
                    <a href={att.presignedUrl} target="_blank" rel="noreferrer" title="Open Presigned Upload Link">
                      <ExternalLink size={16} />
                    </a>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default TicketDetail;
