import api from './api';
import axios from 'axios';

export const notificationService = {
  getHealth: async () => {
    try {
      const response = await api.get('/notifications/health');
      return response.data;
    } catch (error) {
      try {
        const directRes = await axios.get('http://localhost:8083/notifications/health');
        return directRes.data;
      } catch (err) {
        return { status: 'OFFLINE' };
      }
    }
  }
};
