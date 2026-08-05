import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar/Navbar';
import Sidebar from './components/Sidebar/Sidebar';
import Landing from './pages/Landing/Landing';
import Login from './pages/Login/Login';
import Register from './pages/Register/Register';
import Dashboard from './pages/Dashboard/Dashboard';
import TicketList from './pages/TicketList/TicketList';
import TicketDetail from './pages/TicketDetail/TicketDetail';
import CreateTicket from './pages/CreateTicket/CreateTicket';
import styles from './App.module.css';

const ProtectedLayout = ({ children }) => {
  const { token } = useAuth();
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className={styles.layout}>
      <Navbar />
      <div className={styles.body}>
        <Sidebar />
        <main className={styles.mainContent}>
          {children}
        </main>
      </div>
    </div>
  );
};

const AppRoutes = () => {
  const { token, isAgentOrAdmin } = useAuth();

  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={!token ? <Login /> : <Navigate to={isAgentOrAdmin() ? "/dashboard" : "/tickets"} replace />} />
      <Route path="/register" element={!token ? <Register /> : <Navigate to="/tickets" replace />} />
      
      <Route path="/dashboard" element={
        <ProtectedLayout>
          <Dashboard />
        </ProtectedLayout>
      } />

      <Route path="/tickets" element={
        <ProtectedLayout>
          <TicketList />
        </ProtectedLayout>
      } />

      <Route path="/tickets/:id" element={
        <ProtectedLayout>
          <TicketDetail />
        </ProtectedLayout>
      } />

      <Route path="/create-ticket" element={
        <ProtectedLayout>
          <CreateTicket />
        </ProtectedLayout>
      } />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

const App = () => {
  return (
    <ThemeProvider>
      <AuthProvider>
        <Router>
          <AppRoutes />
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
