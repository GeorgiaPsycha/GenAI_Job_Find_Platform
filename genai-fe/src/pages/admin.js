import { useState } from 'react';
import Link from 'next/link';
import styles from "@/styles/Home.module.css"; // Χρησιμοποιούμε τα ίδια styles

// TODO: ΒΑΛΕ ΤΑ ID ΣΟΥ ΕΔΩ
const ACCOUNT_ID = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX";
const ADMIN_ID = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX";

export default function Admin() {
    const [form, setForm] = useState({
        title: '',
        company: '',
        location: '',
        seniority: '',
        body: ''
    });
    const [status, setStatus] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setStatus('Publishing...');

        try {
            const res = await fetch('http://localhost:8080/documents', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ...form,
                    account: { id: ACCOUNT_ID },
                    createdBy: { id: ADMIN_ID },
                    status: 'active'
                })
            });

            if (res.ok) {
                setStatus('✅ Job Posted Successfully!');
                setForm({ title: '', company: '', location: '', seniority: '', body: '' });
            } else {
                setStatus('❌ Error posting job');
            }
        } catch (err) {
            console.error(err);
            setStatus('❌ Connection Error');
        }
    };

    return (
        <div className={styles.page} style={{ padding: '40px', display: 'block', maxWidth: '800px', margin: '0 auto' }}>
            <Link href="/" style={{ color: '#0070f3', textDecoration: 'none', marginBottom: '20px', display: 'block' }}>
                &larr; Back to Candidate Chat
            </Link>

            <h1 style={{ marginBottom: '20px' }}>Admin Dashboard: Post a Job</h1>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <input
                    className={styles.chatInput}
                    placeholder="Job Title (e.g. Senior Java Dev)"
                    value={form.title}
                    onChange={e => setForm({...form, title: e.target.value})}
                    required
                />
                <input
                    className={styles.chatInput}
                    placeholder="Company Name"
                    value={form.company}
                    onChange={e => setForm({...form, company: e.target.value})}
                    required
                />
                <input
                    className={styles.chatInput}
                    placeholder="Location"
                    value={form.location}
                    onChange={e => setForm({...form, location: e.target.value})}
                />
                <input
                    className={styles.chatInput}
                    placeholder="Seniority (Junior, Mid, Senior)"
                    value={form.seniority}
                    onChange={e => setForm({...form, seniority: e.target.value})}
                />
                <textarea
                    className={styles.chatInput}
                    placeholder="Job Description (Paste full text here...)"
                    rows={10}
                    value={form.body}
                    onChange={e => setForm({...form, body: e.target.value})}
                    required
                />

                <button
                    type="submit"
                    style={{
                        padding: '15px',
                        backgroundColor: '#0070f3',
                        color: 'white',
                        fontWeight: 'bold',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: 'pointer'
                    }}
                >
                    Post Job
                </button>
            </form>

            {status && <div style={{ marginTop: '20px', fontSize: '1.2em' }}>{status}</div>}
        </div>
    );
}