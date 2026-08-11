import api from './api';

export const notificationService = {
  getHealth: async () => {
    try {
      const response = await api.get('/notifications/health');
      return response.data;
    } catch (error) {
      return { status: 'OFFLINE' };
    }
  }
};
