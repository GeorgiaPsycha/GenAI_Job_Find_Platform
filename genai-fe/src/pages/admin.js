import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import styles from "@/styles/Home.module.css";

const ACCOUNT_ID = "8c6e55a7-eee6-4c38-b78b-241e3d1b8637";

export default function Admin() {
    const router = useRouter();

    // States για τη φόρμα
    const [form, setForm] = useState({
        title: '',
        company: '',
        location: '',
        seniority: '',
        body: ''
    });
    const [status, setStatus] = useState('');

    // States για το AI Dashboard
    const [myJobs, setMyJobs] = useState([]);
    const [selectedJob, setSelectedJob] = useState(null);
    const [rankedApplicants, setRankedApplicants] = useState([]);
    const [loadingRank, setLoadingRank] = useState(false);

    const [isMounted, setIsMounted] = useState(false);

    // 1. Initial Load & Auth Check
    useEffect(() => {
        setIsMounted(true);
        const role = localStorage.getItem('role');
        const token = localStorage.getItem('token');

        if (!token || role !== 'ADMIN') {
            router.push('/login');
        } else {
            // Αν είναι admin, φέρε τα jobs του
            fetchMyJobs(token);
        }
    }, []);

    // 2. Fetch Admin's Jobs
    const fetchMyJobs = async (token) => {
        try {
            const res = await fetch('http://localhost:8080/admin-ai/my-jobs', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) {
                const data = await res.json();
                setMyJobs(data);
            }
        } catch (e) { console.error(e); }
    };

    // 3. Post Job Logic
    const handleSubmit = async (e) => {
        e.preventDefault();
        setStatus('Publishing...');
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
                fetchMyJobs(token); // Ανανέωση λίστας
            } else {
                setStatus('❌ Error posting job');
            }
        } catch (err) {
            setStatus('❌ Connection Error');
        }
    };

    // 4. AI Reranking Call
    const handleJobClick = async (job) => {
        setSelectedJob(job);
        setRankedApplicants([]);
        setLoadingRank(true);
        const token = localStorage.getItem('token');

        try {
            const res = await fetch(`http://localhost:8080/admin-ai/job/${job.id}/rank-candidates`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (res.ok) {
                const data = await res.json();
                setRankedApplicants(data);
            }
        } catch (e) {
            console.error("Ranking failed", e);
        } finally {
            setLoadingRank(false);
        }
    };

    if (!isMounted) return null;

    return (
        <div className={styles.page} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minHeight: '100vh', padding: '20px' }}>

            {/* Header */}
            <div style={{ width: '100%', maxWidth: '1200px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
                <div style={{display: 'flex', alignItems: 'center', gap: '20px'}}>
                    <Link href="/" style={{ color: '#0070f3', textDecoration: 'none', fontWeight: 'bold' }}>&larr; Home</Link>
                    <h1>🛡️ Recruiter Dashboard</h1>
                </div>
                <button
                    onClick={() => { localStorage.clear(); router.push('/login'); }}
                    style={{ background: 'none', border: '1px solid #ff4444', color: '#ff4444', padding: '5px 15px', borderRadius: '5px', cursor: 'pointer' }}
                >
                    Logout
                </button>
            </div>

            <div style={{ display: 'flex', gap: '40px', width: '100%', maxWidth: '1200px', flexWrap: 'wrap' }}>

                {/* LEFT COLUMN: Post Job */}
                <div style={{ flex: 1, minWidth: '400px', borderRight: '1px solid #333', paddingRight: '40px' }}>
                    <h2 style={{marginBottom: '20px'}}>1. Post a New Position</h2>
                    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                        <div style={{ display: 'flex', gap: '10px' }}>
                            <input className={styles.chatInput} style={{ flex: 1 }} placeholder="Job Title" value={form.title} onChange={e => setForm({...form, title: e.target.value})} required />
                            <input className={styles.chatInput} style={{ flex: 1 }} placeholder="Company" value={form.company} onChange={e => setForm({...form, company: e.target.value})} required />
                        </div>
                        <div style={{ display: 'flex', gap: '10px' }}>
                            <input className={styles.chatInput} style={{ flex: 1 }} placeholder="Location" value={form.location} onChange={e => setForm({...form, location: e.target.value})} />
                            <input className={styles.chatInput} style={{ flex: 1 }} placeholder="Seniority" value={form.seniority} onChange={e => setForm({...form, seniority: e.target.value})} />
                        </div>
                        <textarea className={styles.chatInput} placeholder="Job Description (Paste full requirements here)..." rows={10} value={form.body} onChange={e => setForm({...form, body: e.target.value})} required />
                        <button type="submit" className={styles.uploadButton} style={{ padding: '15px', fontWeight: 'bold' }}>Post Job Position</button>
                    </form>
                    {status && <div style={{ marginTop: '10px', padding: '10px', borderRadius:'5px', backgroundColor: status.includes('✅') ? 'rgba(0,255,0,0.1)' : 'rgba(255,0,0,0.1)', border: status.includes('✅') ? '1px solid green' : '1px solid red' }}>{status}</div>}
                </div>

                {/* RIGHT COLUMN: My Jobs & AI Ranking */}
                <div style={{ flex: 1, minWidth: '400px' }}>
                    <h2 style={{marginBottom: '20px'}}>2. AI Candidate Review</h2>
                    <p style={{color: '#888', marginBottom: '15px'}}>Select a job to rerank applicants by relevance:</p>

                    {/* List of Jobs Buttons */}
                    <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '30px' }}>
                        {myJobs.length === 0 && <p style={{color: '#666'}}>No jobs posted yet.</p>}
                        {myJobs.map(job => (
                            <button
                                key={job.id}
                                onClick={() => handleJobClick(job)}
                                style={{
                                    padding: '10px 15px',
                                    backgroundColor: selectedJob?.id === job.id ? '#0070f3' : '#222',
                                    color: 'white',
                                    border: '1px solid #444',
                                    borderRadius: '5px',
                                    cursor: 'pointer',
                                    transition: 'all 0.2s'
                                }}
                            >
                                {job.title}
                            </button>
                        ))}
                    </div>

                    {/* Applicants Panel */}
                    {selectedJob && (
                        <div style={{ border: '1px solid #444', borderRadius: '10px', padding: '20px', backgroundColor: '#111' }}>
                            <h3 style={{ borderBottom: '1px solid #333', paddingBottom: '10px', marginBottom: '15px' }}>
                                Candidates for: <span style={{color: '#0070f3'}}>{selectedJob.title}</span>
                            </h3>

                            {loadingRank && <div style={{textAlign: 'center', padding: '30px', color: '#0070f3'}}>⚡ AI is comparing CVs with Job Description...</div>}

                            {!loadingRank && rankedApplicants.length === 0 && (
                                <p style={{color: '#888'}}>No candidates have applied yet (or candidates have no CV text).</p>
                            )}

                            <ul style={{ listStyle: 'none', padding: 0 }}>
                                {rankedApplicants.map((applicant, index) => (
                                    <li key={applicant.applicationId} style={{
                                        marginBottom: '15px',
                                        padding: '15px',
                                        backgroundColor: index === 0 ? 'rgba(0, 255, 0, 0.08)' : '#1a1a1a',
                                        border: index === 0 ? '1px solid #006400' : '1px solid #333',
                                        borderRadius: '8px'
                                    }}>
                                        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                                            <div>
                                                <span style={{fontWeight: 'bold', fontSize: '1.1em', color: '#fff'}}>
                                                    #{index + 1} {applicant.candidateName}
                                                </span>
                                                <div style={{fontSize: '0.85em', color: '#666'}}>{applicant.candidateEmail}</div>
                                            </div>

                                            <div style={{textAlign: 'right'}}>
                                                <span style={{
                                                    backgroundColor: applicant.score > 0.75 ? '#006400' : applicant.score > 0.5 ? '#b8860b' : '#555',
                                                    color: 'white',
                                                    padding: '4px 8px',
                                                    borderRadius: '4px',
                                                    fontWeight: 'bold',
                                                    fontSize: '0.9em'
                                                }}>
                                                    Match: {(applicant.score * 100).toFixed(1)}%
                                                </span>
                                            </div>
                                        </div>

                                        <div style={{fontSize: '0.9em', color: '#aaa', marginTop: '10px', fontStyle: 'italic', borderTop: '1px solid #333', paddingTop: '8px'}}>
                                            &quot;{applicant.motivation}&quot;                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}