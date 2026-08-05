import api from './api';

export const ticketService = {
  createTicket: async (ticketData) => {
    // ticketData: { title, description, category, priority }
    const response = await api.post('/tickets', ticketData);
    return response.data;
  },

  getTickets: async (params = {}) => {
    // params: { status, priority, category, page, size }
    const response = await api.get('/tickets', { params });
    return response.data;
  },

  getTicketById: async (id) => {
    const response = await api.get(`/tickets/${id}`);
    return response.data;
  },

  updateTicketStatus: async (id, statusData) => {
    // statusData: { status }
    const response = await api.patch(`/tickets/${id}/status`, statusData);
    return response.data;
  },

  assignTicket: async (id, agentId = null) => {
    const params = agentId ? { agentId } : {};
    const response = await api.patch(`/tickets/${id}/assign`, null, { params });
    return response.data;
  },

  addComment: async (ticketId, commentData) => {
    // commentData: { text }
    const response = await api.post(`/tickets/${ticketId}/comments`, commentData);
    return response.data;
  },

  getCommentsByTicketId: async (ticketId) => {
    const response = await api.get(`/tickets/${ticketId}/comments`);
    return response.data;
  },

  getDashboardSummary: async () => {
    const response = await api.get('/tickets/dashboard');
    return response.data;
  }
};
