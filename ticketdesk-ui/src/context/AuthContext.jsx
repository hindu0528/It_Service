import React, { createContext, useContext, useEffect, useState } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('ticketdesk_token') || null);
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('ticketdesk_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const login = (authData) => {
    // authData: { token, userId, username, email, roles }
    setToken(authData.token);
    const userData = {
      id: authData.userId,
      username: authData.username,
      email: authData.email,
      roles: authData.roles || []
    };
    setUser(userData);
    localStorage.setItem('ticketdesk_token', authData.token);
    localStorage.setItem('ticketdesk_user', JSON.stringify(userData));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('ticketdesk_token');
    localStorage.removeItem('ticketdesk_user');
  };

  const isAgentOrAdmin = () => {
    if (!user || !user.roles) return false;
    return user.roles.some(role => role === 'ROLE_AGENT' || role === 'ROLE_ADMIN');
  };

  const isAdmin = () => {
    if (!user || !user.roles) return false;
    return user.roles.includes('ROLE_ADMIN');
  };

  return (
    <AuthContext.Provider value={{ token, user, login, logout, isAgentOrAdmin, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
