import api from './api';

export const attachmentService = {
  uploadFile: async (file, ticketId) => {
    const formData = new FormData();
    formData.append('file', file);
    if (ticketId) formData.append('ticketId', ticketId);

    const response = await api.post('/attachments/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  },

  generatePresignedUrl: async (presignData) => {
    // presignData: { fileName, fileType, fileSize, ticketId }
    const response = await api.post('/attachments/presign', presignData);
    return response.data;
  },

  confirmAttachment: async (ticketId, attachmentId) => {
    const response = await api.post(`/attachments/${ticketId}/confirm?attachmentId=${attachmentId}`);
    return response.data;
  },

  getAttachmentsByTicketId: async (ticketId) => {
    const response = await api.get(`/attachments/ticket/${ticketId}`);
    return response.data;
  }
};
