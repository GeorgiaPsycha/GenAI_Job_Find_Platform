import { useState } from 'react';
import { useRouter } from 'next/router';
import styles from "@/styles/Home.module.css";

export default function Login() {
  const [email, setEmail] = useState('');
  const router = useRouter();

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch('http://localhost:8080/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });

      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('token', data.token); // Αποθηκεύουμε το Token
        router.push('/'); // Πάμε στο Chat
      } else {
        alert("User not found!");
      }
    } catch (err) {
      console.error(err);
      alert("Connection failed");
    }
  };

  return (
    <div className={styles.page} style={{display:'flex', justifyContent:'center', alignItems:'center', height:'100vh'}}>
      <form onSubmit={handleLogin} style={{padding:'20px', border:'1px solid #ccc'}}>
        <h1>Login</h1>
        <input
          value={email}
          onChange={e => setEmail(e.target.value)}
          placeholder="Enter email (chris@mailinator.com)"
          style={{padding:'10px', width:'300px', display:'block', marginBottom:'10px'}}
        />
        <button type="submit" style={{padding:'10px', width:'100%'}}>Login</button>
      </form>
    </div>
  );
}