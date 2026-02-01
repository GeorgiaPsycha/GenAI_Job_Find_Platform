import Head from "next/head";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/router";
import { Geist } from "next/font/google";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import Link from 'next/link';
import styles from "@/styles/Home.module.css";

const geistSans = Geist({
    variable: "--font-geist-sans",
    subsets: ["latin"],
});

const ACCOUNT_ID = "8c6e55a7-eee6-4c38-b78b-241e3d1b8637";

export default function Home() {
    const router = useRouter();

    const [documents, setDocuments] = useState([]);
    const [documentsLoading, setDocumentsLoading] = useState(true);
    const [activeItem, setActiveItem] = useState(null);
    const [searchInput, setSearchInput] = useState("");
    const [filteredItems, setFilteredItems] = useState([]);
    const [searchLoading, setSearchLoading] = useState(false);
    const [semanticSearchInput, setSemanticSearchInput] = useState("");
    const [semanticSearchLoading, setSemanticSearchLoading] = useState(false);
    const [selectedDocument, setSelectedDocument] = useState(null);

    const [currentUserId, setCurrentUserId] = useState(null);
    const [userRole, setUserRole] = useState(null);

    const [message, setMessage] = useState("");

    const [activeThreadId, setActiveThreadId] = useState(null);

    const [history, setHistory] = useState([
        {
            id: "1",
            author: "System",
            userId: "agent",
            text: "Hello! I am your personal career advisor. Upload your resume (CV) or tell me what kind of job you are looking for. Ask me:\n" +
                "1. \"Find me jobs based on my resume\" -> if you want me to give you a list of potential jobs based on your resume\n" +
                "2. \"Apply for job x with code x\" -> if you want to apply for a specific job\n" +
                "3. \"My application\" -> to see all the jobs you have applied for",
            createdAt: new Date().toISOString(),
        },
    ]);

    const fileInputRef = useRef(null);
    const [uploading, setUploading] = useState(false);
    const historyRef = useRef(null);

    useEffect(() => {
        const token = localStorage.getItem('token');
        const storedUserId = localStorage.getItem('userId');
        const storedRole = localStorage.getItem('role');

        if (!token) {
            router.push('/login');
        } else {
            setCurrentUserId(storedUserId);
            setUserRole(storedRole);
        }
    }, [router]);

    useEffect(() => {
        const node = historyRef.current;
        if (node) { node.scrollTop = node.scrollHeight; }
    }, [history]);

    useEffect(() => {
        const fetchDocuments = async () => {
            try {
                setDocumentsLoading(true);
                const response = await fetch("http://localhost:8080/documents");
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const data = await response.json();
                setDocuments(data);
                setFilteredItems(data);
                if (data.length > 0) setActiveItem(data[0]);
            } catch (error) {
                console.error("Error fetching documents:", error);
            } finally {
                setDocumentsLoading(false);
            }
        };
        fetchDocuments();
    }, []);

    const handleFileUpload = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setUploading(true);
        const formData = new FormData();
        formData.append("file", file);
        const token = localStorage.getItem('token');

        try {
            const uploadRes = await fetch("http://localhost:8080/files/upload", {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`
                },
                body: formData,
            });

            if (!uploadRes.ok) throw new Error("Upload failed");
            const data = await uploadRes.json();
            const fileUrl = data.url;

            setHistory((prev) => [
                ...prev,
                {
                    id: Date.now() + "-upload",
                    author: "System",
                    userId: "system",
                    text: `✅ CV Uploaded successfully! I will use it for your applications.`,
                    createdAt: new Date().toISOString(),
                }
            ]);

            const systemMsg = `I have uploaded my CV. The file is located at: ${fileUrl}. Please use this for my applications.`;

            const chatRes = await fetch("http://localhost:8080/messages", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({
                    content: systemMsg,
                    thread: activeThreadId ? { id: activeThreadId } : null,
                    account: { id: ACCOUNT_ID },
                }),
            });

            if (chatRes.ok) {
                const chatData = await chatRes.json();
                if (chatData.thread && chatData.thread.id) {
                    setActiveThreadId(chatData.thread.id);
                }
            }

        } catch (error) {
            console.error(error);
            alert("Error uploading file.");
        } finally {
            setUploading(false);
            if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (!message.trim()) return;

        const userMessage = message.trim();
        const token = localStorage.getItem('token');

        setHistory((current) => [
            ...current,
            {
                id: `${Date.now()}-user`,
                author: "You",
                userId: currentUserId,
                text: userMessage,
                createdAt: new Date().toISOString(),
            },
        ]);
        setMessage("");

        try {
            const response = await fetch("http://localhost:8080/messages", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({
                    content: userMessage,
                    thread: activeThreadId ? { id: activeThreadId } : null,
                    account: { id: ACCOUNT_ID },
                }),
            });

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem('token');
                router.push('/login');
                return;
            }

            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const data = await response.json();

            if (data.thread && data.thread.id) {
                setActiveThreadId(data.thread.id);
            }

            if (data.message) {
                setHistory((current) => [
                    ...current,
                    {
                        id: data.message.id || `${Date.now()}-agent`,
                        author: "Career Agent",
                        userId: "agent",
                        text: data.message.content || "",
                        createdAt: data.message.createdAt || new Date().toISOString(),
                    },
                ]);
            }
        } catch (error) {
            console.error("Error sending message:", error);
            setHistory((current) => [
                ...current,
                {
                    id: `${Date.now()}-error`,
                    author: "System",
                    userId: "agent",
                    text: "Error: Failed to connect. Try logging in again.",
                    createdAt: new Date().toISOString(),
                },
            ]);
        }
    };

    useEffect(() => {
        if (!searchInput.trim()) {
            if (!semanticSearchInput.trim()) setFilteredItems(documents);
            setSearchLoading(false);
            return;
        }
        const terms = searchInput.toLowerCase().split(' ').filter(t => t);
        const matches = documents.filter((item) => {
            const text = `${item.title} ${item.body || ""} ${item.company || ""}`.toLowerCase();
            return terms.every(term => text.includes(term));
        });
        setFilteredItems(matches);
    }, [searchInput, documents]);

    useEffect(() => {
        const timer = setTimeout(async () => {
            if (!semanticSearchInput.trim()) return;
            setSemanticSearchLoading(true);
            try {
                const searchText = encodeURIComponent(semanticSearchInput.trim());
                const res = await fetch(`http://localhost:8080/documents?searchText=${searchText}&accountId=${ACCOUNT_ID}`);
                if(res.ok) {
                    const data = await res.json();
                    setFilteredItems(data);
                }
            } catch(e) { console.error(e); }
            finally { setSemanticSearchLoading(false); }
        }, 800);
        return () => clearTimeout(timer);
    }, [semanticSearchInput]);

    return (
        <>
            <Head>
                <title>GenAI Job Finder</title>
                <meta name="viewport" content="width=device-width, initial-scale=1" />
            </Head>
            <div className={`${styles.page} ${geistSans.variable}`}>

                <div className={styles.sidebar}>
                    <div style={{ marginBottom: '20px', paddingBottom: '10px', borderBottom: '1px solid #333', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        {(userRole === 'ADMIN' || userRole === 'admin') ? (
                            <Link href="/admin" style={{ color: '#0070f3', textDecoration: 'none', fontWeight: 'bold', fontSize: '0.9em' }}>
                                Admin Panel
                            </Link>
                        ) : (
                            <span style={{ color: '#666', fontSize: '0.9em', fontWeight: 'bold' }}>Job Candidate</span>
                        )}
                        <button
                            onClick={() => { localStorage.clear(); router.push('/login'); }}
                            style={{ background: 'none', border: 'none', color: '#ff4444', cursor: 'pointer', fontSize: '0.9em' }}
                        >
                            Logout
                        </button>
                    </div>

                    <label className={styles.searchLabel}>Semantic Job Search</label>
                    <textarea
                        className={styles.searchArea}
                        rows={2}
                        placeholder="e.g. 'Remote Java jobs'..."
                        value={semanticSearchInput}
                        onChange={(e) => setSemanticSearchInput(e.target.value)}
                    />

                    <h1 className={styles.sidebarTitle}>Open Positions</h1>

                    <div className={styles.sidebarListWrapper}>
                        {documentsLoading ? (
                            <div className={styles.sidebarLoading}>Loading jobs...</div>
                        ) : (
                            <ul className={styles.sidebarList}>
                                {filteredItems.map((item) => (
                                    <li key={item.id}>
                                        <button
                                            className={`${styles.sidebarItem} ${activeItem?.id === item.id ? styles.sidebarItemActive : ""}`}
                                            onClick={() => setSelectedDocument(item)}
                                        >
                                            <div className={styles.sidebarItemTitle}>{item.title}</div>
                                            <div className={styles.sidebarItemDescription} style={{fontSize: '0.8em', color: '#666'}}>
                                                {item.company}
                                            </div>
                                            <div style={{fontSize: '0.7em', color: '#444', marginTop: '4px', fontFamily: 'monospace'}}>
                                                ID: {item.id}
                                            </div>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>

                <main className={styles.chatPanel}>
                    <header className={styles.chatHeader}>
                        <h2 className={styles.chatTitle}>AI Recruiter Chat</h2>
                    </header>

                    <section className={styles.chatHistory} ref={historyRef}>
                        {history.map((entry) => (
                            <article
                                key={entry.id}
                                className={`${styles.chatMessage} ${entry.userId === currentUserId ? styles.chatMessageMine : styles.chatMessageOther}`}
                            >
                                <div className={styles.chatMessageHeader}>
                                    <span className={styles.chatAuthor}>{entry.author}</span>
                                </div>
                                <div style={{ lineHeight: '1.6' }}>
                                    <ReactMarkdown remarkPlugins={[remarkGfm]}>{entry.text}</ReactMarkdown>
                                </div>
                            </article>
                        ))}
                    </section>

                    <form className={styles.chatInputArea} onSubmit={handleSubmit}>
                        <input
                            type="file"
                            ref={fileInputRef}
                            style={{display: 'none'}}
                            onChange={handleFileUpload}
                            accept=".pdf,.doc,.docx"
                        />
                        <button
                            type="button"
                            onClick={() => fileInputRef.current?.click()}
                            disabled={uploading}
                            style={{
                                marginRight: '10px',
                                background: 'none',
                                border: 'none',
                                fontSize: '1.5rem',
                                cursor: 'pointer',
                                opacity: uploading ? 0.5 : 1
                            }}
                            title="Upload CV"
                        >
                            {uploading ? "⏳" : "📎"}
                        </button>

                        <textarea
                            placeholder="Ask about jobs or upload your CV..."
                            className={styles.chatInput}
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSubmit(e)}
                            rows={1}
                        />
                    </form>
                </main>
            </div>

            {selectedDocument && (
                <div className={styles.modalOverlay} onClick={() => setSelectedDocument(null)}>
                    <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
                        <div className={styles.modalHeader}>
                            <h3>{selectedDocument.title} @ {selectedDocument.company}</h3>
                            <button className={styles.closeButton} onClick={() => setSelectedDocument(null)}>×</button>
                        </div>
                        <div className={styles.modalContent} style={{ padding: '20px', overflowY: 'auto', maxHeight: '70vh' }}>
                            <p style={{fontSize: '0.8em', color: '#555', marginBottom: '10px'}}>Job ID: {selectedDocument.id}</p>
                            <ReactMarkdown>{selectedDocument.body}</ReactMarkdown>
                            <div style={{marginTop: '20px', borderTop: '1px solid #eee', paddingTop: '10px'}}>
                                <button
                                    style={{backgroundColor: '#0070f3', color: 'white', padding: '10px 20px', border: 'none', borderRadius: '5px', cursor: 'pointer'}}
                                    onClick={() => alert(`To apply, ask the Agent: "Apply to job ID ${selectedDocument.id}"`)}
                                >
                                    Apply via Agent
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}