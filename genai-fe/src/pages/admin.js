import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import styles from "@/styles/Home.module.css";

const ACCOUNT_ID = "8c6e55a7-eee6-4c38-b78b-241e3d1b8637";

export default function Admin() {
    const router = useRouter();

    // Απλά states για τη φόρμα
    const [form, setForm] = useState({
        title: '',
        company: '',
        location: '',
        seniority: '',
        body: ''
    });
    const [status, setStatus] = useState('');

    // State για να ξέρουμε πότε φορτώθηκε η σελίδα στον browser
    const [isMounted, setIsMounted] = useState(false);

    // 1. Τρέχει ΜΙΑ ΦΟΡΑ όταν ανοίξει η σελίδα
    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setIsMounted(true);

        const role = localStorage.getItem('role');
        const token = localStorage.getItem('token');

        // Αν δεν είναι ADMIN, τον στέλνουμε στο login χωρίς πολλά πολλά
        if (!token || role !== 'ADMIN') {
            router.push('/login');
        }
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setStatus('Publishing...');

        // 2. Διαβάζουμε τα στοιχεία ΤΗ ΣΤΙΓΜΗ ΠΟΥ ΠΑΤΑΣ ΤΟ ΚΟΥΜΠΙ
        const token = localStorage.getItem('token');
        const adminId = localStorage.getItem('userId');

        try {
            const res = await fetch('http://localhost:8080/documents', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    ...form,
                    account: { id: ACCOUNT_ID },
                    createdBy: { id: adminId },
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

    if (!isMounted) return null;

    return (
        <div className={styles.page} style={{ display: 'flex', justifyContent: 'center', minHeight: '100vh' }}>
            <div style={{ padding: '40px', width: '100%', maxWidth: '800px' }}>

                {/* Header - Χωρίς το Back Link πλέον */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', marginBottom: '30px' }}>
                    <button
                        onClick={() => { localStorage.clear(); router.push('/login'); }}
                        style={{ background: 'none', border: '1px solid #ff4444', color: '#ff4444', padding: '5px 10px', borderRadius: '5px', cursor: 'pointer' }}
                    >
                        Logout
                    </button>
                </div>

                <h1 style={{ marginBottom: '20px', borderBottom: '1px solid #333', paddingBottom: '10px' }}>
                    🛡️ Recruiter Dashboard
                </h1>

                {/* Form */}
                <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                    <div style={{ display: 'flex', gap: '10px' }}>
                        <input
                            className={styles.chatInput}
                            style={{ flex: 1 }}
                            placeholder="Job Title (e.g. Senior Java Dev)"
                            value={form.title}
                            onChange={e => setForm({...form, title: e.target.value})}
                            required
                        />
                        <input
                            className={styles.chatInput}
                            style={{ flex: 1 }}
                            placeholder="Company Name"
                            value={form.company}
                            onChange={e => setForm({...form, company: e.target.value})}
                            required
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '10px' }}>
                        <input
                            className={styles.chatInput}
                            style={{ flex: 1 }}
                            placeholder="Location (e.g. Remote, Athens)"
                            value={form.location}
                            onChange={e => setForm({...form, location: e.target.value})}
                        />
                        <input
                            className={styles.chatInput}
                            style={{ flex: 1 }}
                            placeholder="Seniority (Junior, Mid, Senior)"
                            value={form.seniority}
                            onChange={e => setForm({...form, seniority: e.target.value})}
                        />
                    </div>

                    <textarea
                        className={styles.chatInput}
                        placeholder="Job Description (Paste full text here...)"
                        rows={12}
                        value={form.body}
                        onChange={e => setForm({...form, body: e.target.value})}
                        required
                        style={{ resize: 'vertical' }}
                    />

                    <button
                        type="submit"
                        className={styles.uploadButton}
                        style={{
                            padding: '15px',
                            fontWeight: 'bold',
                            fontSize: '1.1em',
                            marginTop: '10px'
                        }}
                    >
                        Post Job Position
                    </button>
                </form>

                {status && (
                    <div style={{
                        marginTop: '20px',
                        padding: '15px',
                        borderRadius: '8px',
                        backgroundColor: status.includes('✅') ? 'rgba(0, 255, 0, 0.1)' : 'rgba(255, 0, 0, 0.1)',
                        border: status.includes('✅') ? '1px solid green' : '1px solid red',
                        textAlign: 'center'
                    }}>
                        {status}
                    </div>
                )}
            </div>
        </div>
    );
}