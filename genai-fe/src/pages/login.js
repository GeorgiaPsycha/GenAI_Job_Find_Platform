import { useState } from 'react';
import { useRouter } from 'next/router';
import styles from "@/styles/Home.module.css";

export default function Login() {
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('user'); // Default ρόλος
  const [error, setError] = useState('');
  const router = useRouter();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const res = await fetch('http://localhost:8080/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, role })
      });

      if (res.ok) {
        const data = await res.json();
        // Αποθήκευση στο Browser
        localStorage.setItem('token', data.token);
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('email', data.email);
        localStorage.setItem('role', data.role);

        // Redirect ανάλογα με τον ρόλο (προαιρετικό, τώρα πάνε όλα στο chat)
        if (data.role === 'admin') {
          await router.push('/admin'); // Οι Recruiters πάνε στο Admin Panel
        } else {
          await router.push('/'); // Οι Candidates πάνε στο Chat
        }
      } else {
        const errorData = await res.json();
        setError(errorData.error || 'Login failed. Check details.');
      }
    } catch (err) {
      console.error(err);
      setError('Connection error.');
    }
  };

  // Στυλ για τα κουτιά επιλογής
  const boxStyle = (isSelected) => ({
    flex: 1,
    padding: '15px',
    border: isSelected ? '2px solid #0070f3' : '1px solid #444',
    backgroundColor: isSelected ? 'rgba(0, 112, 243, 0.1)' : '#1a1a1a',
    borderRadius: '8px',
    cursor: 'pointer',
    textAlign: 'center',
    transition: 'all 0.2s ease',
    margin: '0 5px',
    color: isSelected ? '#fff' : '#888'
  });

  return (
      <div className={styles.page} style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', flexDirection: 'column' }}>
        <form onSubmit={handleLogin} style={{ padding: '2.5rem', border: '1px solid #333', borderRadius: '12px', background: '#0a0a0a', minWidth: '400px', boxShadow: '0 4px 20px rgba(0,0,0,0.5)' }}>
          <h1 style={{ marginBottom: '10px', textAlign: 'center', fontSize: '1.8rem' }}>Welcome Back</h1>
          <p style={{ textAlign: 'center', color: '#666', marginBottom: '30px' }}>Log in to GenAI Job Platform</p>

          {/* Email Input */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.9rem', color: '#ccc' }}>Email Address</label>
            <input
                className={styles.chatInput}
                style={{ width: '100%', padding: '12px', borderRadius: '6px', border: '1px solid #333', backgroundColor: '#111', color: '#fff' }}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="e.g. chris@mailinator.com"
                required
            />
          </div>

          {/* Role Selection Boxes */}
          <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.9rem', color: '#ccc' }}>Select Role</label>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '30px' }}>

            {/* USER BOX */}
            <div
                style={boxStyle(role === 'user')}
                onClick={() => setRole('user')}
            >
              <div style={{ fontSize: '1.5rem', marginBottom: '5px' }}>👤</div>
              <div style={{ fontWeight: 'bold' }}>Candidate</div>
            </div>

            {/* ADMIN BOX */}
            <div
                style={boxStyle(role === 'admin')}
                onClick={() => setRole('admin')}
            >
              <div style={{ fontSize: '1.5rem', marginBottom: '5px' }}>🛡️</div>
              <div style={{ fontWeight: 'bold' }}>Recruiter/Admin</div>
            </div>

          </div>

          <button className={styles.uploadButton} type="submit" style={{ width: '100%', padding: '12px', fontSize: '1rem', fontWeight: 'bold' }}>
            Login
          </button>

          {error && <p style={{ color: '#ff4444', marginTop: '20px', textAlign: 'center', fontSize: '0.9rem' }}>{error}</p>}
        </form>
      </div>
  );
}