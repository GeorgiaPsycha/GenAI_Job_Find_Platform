import Head from "next/head";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/router"; // <--- 1. Import Router
import { Geist } from "next/font/google";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import Link from 'next/link';
import styles from "@/styles/Home.module.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const initialAgents = [
  {
    id: "career-agent",
    name: "AI Career Coach",
    settings: {
      llmModel: "llama3.1:latest",
      embeddingsModel: "nomic-embed-text:latest",
      rerankingModel: "voyage-large-2-instruct",
      maxTokens: "1024",
      temperature: "0.3",
      behavior: "You are a helpful AI Career Recruiter...",
    },
  },
];

// Κρατάμε μόνο το ACCOUNT_ID hardcoded (ή το παίρνεις από localStorage αν το αποθηκεύεις στο login)
const ACCOUNT_ID = "8c6e55a7-eee6-4c38-b78b-241e3d1b8637";
const THREAD_ID = null;

export default function Home() {
  const router = useRouter(); // <--- 2. Init Router

  const [documents, setDocuments] = useState([]);
  const [documentsLoading, setDocumentsLoading] = useState(true);
  const [activeItem, setActiveItem] = useState(null);
  const [searchInput, setSearchInput] = useState("");
  const [filteredItems, setFilteredItems] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [semanticSearchInput, setSemanticSearchInput] = useState("");
  const [semanticSearchLoading, setSemanticSearchLoading] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState(null);

  // State για τον τρέχοντα χρήστη (για το UI)
  const [currentUserId, setCurrentUserId] = useState(null);

  const [message, setMessage] = useState("");
  const [history, setHistory] = useState([
    {
      id: "1",
      author: "System",
      userId: "agent",
      text: "Γεια σου! Είμαι ο προσωπικός σου σύμβουλος καριέρας. Πες μου τι δουλειά ψάχνεις;",
      createdAt: new Date().toISOString(),
    },
  ]);

  const historyRef = useRef(null);

  // --- 3. AUTH CHECK & INITIALIZATION ---
  useEffect(() => {
    // Έλεγχος αν υπάρχει token
    const token = localStorage.getItem('token');
    const storedUserId = localStorage.getItem('userId');

    if (!token) {
      // Αν δεν υπάρχει, πίσω στο Login
      router.push('/login');
    } else {
      // Αν υπάρχει, κρατάμε το User ID για να ξεχωρίζουμε τα μηνύματά μας στο chat
      setCurrentUserId(storedUserId);
    }
  }, [router]);

  useEffect(() => {
    const node = historyRef.current;
    if (node) {
      node.scrollTop = node.scrollHeight;
    }
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

  // --- 4. SEND MESSAGE (ΜΕ TOKEN) ---
  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!message.trim()) return;

    const userMessage = message.trim();
    const token = localStorage.getItem('token'); // Πάρε το token

    // Προσθήκη στο UI (Optimistic update)
    setHistory((current) => [
      ...current,
      {
        id: `${Date.now()}-user`,
        author: "You",
        userId: currentUserId, // Χρήση του ID από το login
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
            "Authorization": `Bearer ${token}` // <--- ΤΟ ΚΛΕΙΔΙ: Στέλνουμε το Token
        },
        body: JSON.stringify({
          content: userMessage,
          thread: THREAD_ID ? { id: THREAD_ID } : null,
          account: { id: ACCOUNT_ID },
          // ΔΕΝ στέλνουμε user: { id: ... }, το βρίσκει το Backend από το token!
        }),
      });

      if (response.status === 401 || response.status === 403) {
          // Αν το token έληξε, redirect στο login
          localStorage.removeItem('token');
          router.push('/login');
          return;
      }

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

      const data = await response.json();

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
          text: "Error: Failed to connect (or session expired). Try logging in again.",
          createdAt: new Date().toISOString(),
        },
      ]);
    }
  };

  // ... (Υπόλοιπος κώδικας Search παραμένει ίδιος) ...
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
                   <Link href="/admin" style={{ color: '#0070f3', textDecoration: 'none', fontWeight: 'bold', fontSize: '0.9em' }}>
                       Admin Panel
                   </Link>
                   {/* Logout Button */}
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
                         <span className={styles.sidebarItemTitle}>{item.title}</span>
                         <span className={styles.sidebarItemDescription} style={{fontSize: '0.8em', color: '#666'}}>
                           {item.company}
                         </span>
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
                   // Ελέγχουμε το ID του μηνύματος με το ID του συνδεδεμένου χρήστη
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
               <textarea
                 placeholder="Ask about jobs..."
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
                 <ReactMarkdown>{selectedDocument.body}</ReactMarkdown>
                 <div style={{marginTop: '20px', borderTop: '1px solid #eee', paddingTop: '10px'}}>
                     <button
                       style={{backgroundColor: '#0070f3', color: 'white', padding: '10px 20px', border: 'none', borderRadius: '5px', cursor: 'pointer'}}
                       onClick={() => alert(`To apply, ask the Agent: "Apply to ${selectedDocument.title}"`)}
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