import Head from "next/head";
import { useEffect, useRef, useState } from "react";
import { Geist } from "next/font/google";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import Link from 'next/link'; // <--- ΠΡΟΣΘΗΚΗ
import styles from "@/styles/Home.module.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

// --- ΡΥΘΜΙΣΗ ΤΟΥ AGENT ΓΙΑ JOB SEARCH ---
const initialAgents = [
  {
    id: "career-agent",
    name: "AI Career Coach",
    settings: {
      llmModel: "llama3.1:latest", // ή "gpt-4o-mini" αν έχεις OpenAI
      embeddingsModel: "nomic-embed-text:latest", // ή "text-embedding-3-small"
      rerankingModel: "voyage-large-2-instruct", // Το Voyage που έχεις στο backend
      maxTokens: "1024",
      temperature: "0.3",
      behavior: "You are a helpful AI Career Recruiter. Help the user find job openings from the available documents. You can use the 'search' tool to find jobs and the 'apply_to_job' tool to submit applications.",
    },
  },
];

// --- TODO: ΑΝΤΙΚΑΤΕΣΤΗΣΕ ΜΕ ΤΑ ID ΑΠΟ ΤΗ ΒΑΣΗ ΣΟΥ (Πίνακες: app_user, account) ---
const ACCOUNT_ID = "8c6e55a7-eee6-4c38-b78b-241e3d1b8637";
const USER_ID = "edbe9124-8811-43a6-ae66-1c773d9e8c73"; // Το ID του Chris
const THREAD_ID = null; // null για νέο thread κάθε φορά, ή βάλε ένα UUID

export default function Home() {
  const [documents, setDocuments] = useState([]);
  const [documentsLoading, setDocumentsLoading] = useState(true);
  const [activeItem, setActiveItem] = useState(null);
  const [searchInput, setSearchInput] = useState("");
  const [filteredItems, setFilteredItems] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [semanticSearchInput, setSemanticSearchInput] = useState("");
  const [semanticSearchLoading, setSemanticSearchLoading] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState(null);
  const [documentModalOpen, setDocumentModalOpen] = useState(false);
  const [agents, setAgents] = useState(initialAgents);
  const [activeAgentId, setActiveAgentId] = useState(initialAgents[0].id);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [settingsAgentId, setSettingsAgentId] = useState(initialAgents[0].id);

  // Settings Form State
  const [settingsForm, setSettingsForm] = useState({
    name: initialAgents[0].name,
    llmModel: initialAgents[0].settings.llmModel,
    embeddingsModel: initialAgents[0].settings.embeddingsModel,
    rerankingModel: initialAgents[0].settings.rerankingModel,
    maxTokens: initialAgents[0].settings.maxTokens,
    temperature: initialAgents[0].settings.temperature,
    behavior: initialAgents[0].settings.behavior,
  });

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
  const fileInputRef = useRef(null);
  const [uploading, setUploading] = useState(false);

  // Scroll to bottom
  useEffect(() => {
    const node = historyRef.current;
    if (node) {
      node.scrollTop = node.scrollHeight;
    }
  }, [history]);

  // Fetch Documents (Job Postings)
  useEffect(() => {
    const fetchDocuments = async () => {
      try {
        setDocumentsLoading(true);
        const response = await fetch("http://localhost:8080/documents"); // Ή /api/documents αν έχεις proxy
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        setDocuments(data);
        setFilteredItems(data); // Αρχικά δείχνουμε τα πάντα
        if (data.length > 0) setActiveItem(data[0]);
      } catch (error) {
        console.error("Error fetching documents:", error);
      } finally {
        setDocumentsLoading(false);
      }
    };
    fetchDocuments();
  }, []);

  // --- SEND MESSAGE ---
  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!message.trim()) return;

    const userMessage = message.trim();

    setHistory((current) => [
      ...current,
      {
        id: `${Date.now()}-user`,
        author: "You",
        userId: USER_ID,
        text: userMessage,
        createdAt: new Date().toISOString(),
      },
    ]);
    setMessage("");

    try {
      // Κλήση στο Backend
      const response = await fetch("http://localhost:8080/messages", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          content: userMessage,
          thread: THREAD_ID ? { id: THREAD_ID } : null,
          account: { id: ACCOUNT_ID },
          user: { id: USER_ID },
        }),
      });

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
          text: "Error: Failed to connect to the AI Recruiter.",
          createdAt: new Date().toISOString(),
        },
      ]);
    }
  };

  const activeAgent = agents.find((agent) => agent.id === activeAgentId) ?? agents[0];

  // --- CLIENT SIDE SEARCH (Simple) ---
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

  // --- SEMANTIC SEARCH (Server Side) ---
  useEffect(() => {
    const timer = setTimeout(async () => {
        if (!semanticSearchInput.trim()) return;

        setSemanticSearchLoading(true);
        try {
            const searchText = encodeURIComponent(semanticSearchInput.trim());
            // Κλήση στο Search Endpoint
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


     // ... (Κώδικας για Settings/Modal παραμένει ίδιος ή παρόμοιος, τον παραλείπω για συντομία, κράτα τον δικό σου) ...
     // Εδώ συμπεριλαμβάνω μόνο το render return με τις αλλαγές

     return (
       <>
         <Head>
           <title>GenAI Job Finder</title>
           <meta name="viewport" content="width=device-width, initial-scale=1" />
         </Head>
         <div className={`${styles.page} ${geistSans.variable}`}>

           {/* SIDEBAR */}
           <div className={styles.sidebar}>
               {/* LINK ΓΙΑ ADMIN PANEL */}
               <div style={{ marginBottom: '20px', paddingBottom: '10px', borderBottom: '1px solid #333' }}>
                   <Link href="/admin" style={{ color: '#0070f3', textDecoration: 'none', fontWeight: 'bold' }}>
                       &larr; Go to Admin / Post Jobs
                   </Link>
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

             {/* Document List */}
             <div className={styles.sidebarListWrapper}>
               {documentsLoading ? (
                 <div className={styles.sidebarLoading}>Loading jobs...</div>
               ) : (
                 <ul className={styles.sidebarList}>
                   {filteredItems.map((item) => (
                     <li key={item.id}>
                       <button
                         className={`${styles.sidebarItem} ${activeItem?.id === item.id ? styles.sidebarItemActive : ""}`}
                         onClick={() => setSelectedDocument(item)} // Άνοιγμα Modal
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

           {/* CHAT PANEL */}
           <main className={styles.chatPanel}>
             <header className={styles.chatHeader}>
                <h2 className={styles.chatTitle}>AI Recruiter Chat</h2>
             </header>

             <section className={styles.chatHistory} ref={historyRef}>
               {history.map((entry) => (
                 <article
                   key={entry.id}
                   className={`${styles.chatMessage} ${entry.userId === USER_ID ? styles.chatMessageMine : styles.chatMessageOther}`}
                 >
                   <div className={styles.chatMessageHeader}>
                     <span className={styles.chatAuthor}>{entry.author}</span>
                   </div>
                   {/* Markdown Support για ωραίες απαντήσεις */}
                   <div style={{ lineHeight: '1.6' }}>
                       <ReactMarkdown remarkPlugins={[remarkGfm]}>{entry.text}</ReactMarkdown>
                   </div>
                 </article>
               ))}
             </section>

             <form className={styles.chatInputArea} onSubmit={handleSubmit}>
               <textarea
                 placeholder="Ask about jobs (e.g., 'Find me a Java Developer role')..."
                 className={styles.chatInput}
                 value={message}
                 onChange={(e) => setMessage(e.target.value)}
                 onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSubmit(e)}
                 rows={1}
               />
             </form>
           </main>
         </div>

         {/* DOCUMENT MODAL (Job Description) */}
         {selectedDocument && (
           <div className={styles.modalOverlay} onClick={() => setSelectedDocument(null)}>
             <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
               <div className={styles.modalHeader}>
                 <h3>{selectedDocument.title} @ {selectedDocument.company}</h3>
                 <button className={styles.closeButton} onClick={() => setSelectedDocument(null)}>×</button>
               </div>
               <div className={styles.modalContent} style={{ padding: '20px', overflowY: 'auto', maxHeight: '70vh' }}>
                 <ReactMarkdown>{selectedDocument.body}</ReactMarkdown>
                 {/* Κουμπί Apply (Manual) */}
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
