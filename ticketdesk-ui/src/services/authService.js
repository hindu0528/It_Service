import api from './api';

export const authService = {
  login: async (credentials) => {
    // credentials: { username, password }
    const response = await api.post('/auth/login', credentials);
    return response.data;
  },

  register: async (userData) => {
    // userData: { username, email, password, role }
    const response = await api.post('/auth/register', userData);
    return response.data;
  },

  getUserById: async (id) => {
    const response = await api.get(`/auth/users/${id}`);
    return response.data;
  },

  getAgents: async () => {
    const response = await api.get('/auth/agents');
    return response.data;
  }
};
