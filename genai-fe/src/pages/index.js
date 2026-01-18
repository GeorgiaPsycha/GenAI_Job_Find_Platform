import Head from "next/head";
import { useEffect, useRef, useState } from "react";
import { Geist } from "next/font/google";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import styles from "@/styles/Home.module.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});


const initialAgents = [
  {
    id: "my-gpt-5.1-mini",
    name: "my-gpt-5.1-mini",
    settings: {
      llmModel: "gpt-4o-mini",
      embeddingsModel: "text-embedding-3-small",
      rerankingModel: "colbert-latest",
      maxTokens: "1024",
      temperature: "0.3",
      behavior: "Respond concisely with actionable next steps.",
    },
  },
  {
    id: "support-pro",
    name: "support-pro",
    settings: {
      llmModel: "gpt-4.1",
      embeddingsModel: "text-embedding-3-large",
      rerankingModel: "bge-large",
      maxTokens: "2048",
      temperature: "0.2",
      behavior: "Sound professional and detail every diagnostic step.",
    },
  },
  {
    id: "concierge-lite",
    name: "concierge-lite",
    settings: {
      llmModel: "gpt-3.5-turbo",
      embeddingsModel: "text-embedding-3-small",
      rerankingModel: "mini-lm-rerank",
      maxTokens: "768",
      temperature: "0.6",
      behavior: "Friendly tone, focus on travel and hospitality questions.",
    },
  },
];

const MY_USER_ID = "user-primary";
const ACCOUNT_ID = "12f515a8-d8c5-479f-8255-1e33c815014f";
const THREAD_ID = "fece8b42-b645-4f19-ac7b-0246c9f4880e";
const USER_ID = "efaf535d-0cee-4abe-87fa-803d8a3b49d8";

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
      text: "Welcome! Pick a document to start chatting.",
      createdAt: new Date(Date.now() - 1000 * 60 * 5).toISOString(),
    },
    {
      id: "2",
      author: "You",
      userId: MY_USER_ID,
      text: "Can you summarize the onboarding checklist?",
      createdAt: new Date(Date.now() - 1000 * 60 * 4).toISOString(),
    },
    {
      id: "3",
      author: "Agent",
      userId: "agent",
      text: "Absolutely. It covers hardware pickup, HR docs, and first-week buddies.",
      createdAt: new Date(Date.now() - 1000 * 60 * 3).toISOString(),
    },
  ]);
  const historyRef = useRef(null);
  const fileInputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({ total: 0, completed: 0, failed: 0 });

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
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setDocuments(data);
        if (data.length > 0) {
          setActiveItem(data[0]);
        }
      } catch (error) {
        console.error("Error fetching documents:", error);
      } finally {
        setDocumentsLoading(false);
      }
    };

    fetchDocuments();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!message.trim()) {
      return;
    }
    
    const userMessage = message.trim();
    
    // Add user message to history
    setHistory((current) => [
      ...current,
      {
        id: `${Date.now()}-${Math.random()}`,
        author: "You",
        userId: MY_USER_ID,
        text: userMessage,
        createdAt: new Date().toISOString(),
      },
    ]);
    setMessage("");
    
    try {
      // Send POST request to messages endpoint
      const response = await fetch("http://localhost:8080/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          content: userMessage,
          thread: {
            id: THREAD_ID,
          },
          account: {
            id: ACCOUNT_ID,
          },
          user: {
            id: USER_ID,
          },
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      
      // Add agent response to history
      if (data.message) {
        setHistory((current) => [
          ...current,
          {
            id: data.message.id || `${Date.now()}-${Math.random()}`,
            author: "Agent",
            userId: "agent",
            text: data.message.content || "",
            createdAt: data.message.createdAt || new Date().toISOString(),
          },
        ]);
      }
      
      // Extract supporting document IDs and set them to searchInput
      if (data.supportingDocuments && data.supportingDocuments.length > 0) {
        const documentIds = data.supportingDocuments
          .map((doc) => doc.document?.id)
          .filter((id) => id != null)
          .join(";");
        
        if (documentIds) {
          setSearchInput(documentIds);
        }
      }
    } catch (error) {
      console.error("Error sending message:", error);
      // Optionally add an error message to the chat
      setHistory((current) => [
        ...current,
        {
          id: `${Date.now()}-${Math.random()}`,
          author: "System",
          userId: "agent",
          text: "Error: Failed to send message. Please try again.",
          createdAt: new Date().toISOString(),
        },
      ]);
    }
  };

  const activeAgent = agents.find((agent) => agent.id === activeAgentId) ?? agents[0];
  const orderedHistory = [...history].sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  );

  useEffect(() => {
    let debounceTimer;

    if (!searchInput.trim()) {
      setFilteredItems(documents);
      setSearchLoading(false);
      return () => {
        clearTimeout(debounceTimer);
      };
    }

    setSearchLoading(true);
    debounceTimer = setTimeout(() => {
      // Split by semicolon and trim each term
      const searchTerms = searchInput
        .split(';')
        .map(term => term.trim().toLowerCase())
        .filter(term => term.length > 0);
      
      const matches = documents.filter((item) => {
        const haystack = `${item.title} ${item.summary || ""} ${item.id || ""}`.toLowerCase();
        // Return true if ANY of the search terms matches (OR logic)
        return searchTerms.some(term => haystack.includes(term));
      });
      setFilteredItems(matches);
      setSearchLoading(false);
    }, 200);

    return () => {
      clearTimeout(debounceTimer);
    };
  }, [searchInput, documents]);

  useEffect(() => {
    let debounceTimer;

    debounceTimer = setTimeout(() => {
      const fetchSemanticSearch = async () => {
        try {
          setSemanticSearchLoading(true);
          const searchText = encodeURIComponent(semanticSearchInput.trim());
          const response = await fetch(`http://localhost:8080/documents?searchText=${searchText}&accountId=${ACCOUNT_ID}`);
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }
          const data = await response.json();
          setDocuments(data);
          setFilteredItems(data);
          if (data.length > 0) {
            setActiveItem(data[0]);
          }
        } catch (error) {
          console.error("Error fetching semantic search results:", error);
        } finally {
          setSemanticSearchLoading(false);
        }
      };
      fetchSemanticSearch();
    }, 1000);

    return () => {
      clearTimeout(debounceTimer);
    };
  }, [semanticSearchInput]);

  const openSettings = () => {
    const current = agents.find((agent) => agent.id === activeAgentId);
    if (current) {
      setSettingsAgentId(current.id);
      setSettingsForm({
        name: current.name,
        ...current.settings,
      });
    }
    setSettingsOpen(true);
  };

  const handleAgentSelect = (event) => {
    const agentId = event.target.value;
    setSettingsAgentId(agentId);
    const agent = agents.find((entry) => entry.id === agentId);
    if (agent) {
      setSettingsForm({
        name: agent.name,
        ...agent.settings,
      });
    }
  };

  const handleCreateNew = () => {
    setSettingsAgentId("new");
    setSettingsForm({
      name: "",
      llmModel: "",
      embeddingsModel: "",
      rerankingModel: "",
      maxTokens: "",
      temperature: "",
      behavior: "",
    });
  };

  const handleSettingsChange = (event) => {
    const { name, value } = event.target;
    setSettingsForm((current) => ({ ...current, [name]: value }));
  };

  const handleSaveSettings = () => {
    if (settingsAgentId === "new") {
      const generatedId =
        settingsForm.name?.trim().replace(/\s+/g, "-").toLowerCase() ||
        `agent-${Date.now()}`;
      const nextAgent = {
        id: generatedId,
        name: settingsForm.name || generatedId,
        settings: {
          llmModel: settingsForm.llmModel,
          embeddingsModel: settingsForm.embeddingsModel,
          rerankingModel: settingsForm.rerankingModel,
          maxTokens: settingsForm.maxTokens,
          temperature: settingsForm.temperature,
          behavior: settingsForm.behavior,
        },
      };
      setAgents((current) => [...current, nextAgent]);
      setActiveAgentId(nextAgent.id);
    } else {
      setAgents((current) =>
        current.map((agent) =>
          agent.id === settingsAgentId
            ? {
                ...agent,
                name: settingsForm.name || agent.name,
                settings: {
                  llmModel: settingsForm.llmModel,
                  embeddingsModel: settingsForm.embeddingsModel,
                  rerankingModel: settingsForm.rerankingModel,
                  maxTokens: settingsForm.maxTokens,
                  temperature: settingsForm.temperature,
                  behavior: settingsForm.behavior,
                },
              }
            : agent
        )
      );
      setActiveAgentId(settingsAgentId);
    }
    setSettingsOpen(false);
  };

  useEffect(() => {
    if (!settingsOpen) {
      return;
    }

    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        setSettingsOpen(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [settingsOpen]);

  useEffect(() => {
    if (!documentModalOpen) {
      return;
    }

    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        setDocumentModalOpen(false);
        setSelectedDocument(null);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [documentModalOpen]);

  const readFileContent = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result);
      reader.onerror = (e) => reject(e);
      reader.readAsText(file);
    });
  };

  const uploadDocument = async (fileName, fileContent) => {
    try {
      const response = await fetch("http://localhost:8080/documents", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          account: {
            id: "12f515a8-d8c5-479f-8255-1e33c815014f",
          },
          title: fileName,
          body: fileContent,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return { success: true, fileName };
    } catch (error) {
      console.error(`Error uploading ${fileName}:`, error);
      return { success: false, fileName, error: error.message };
    }
  };

  const handleFileSelect = async (event) => {
    const files = Array.from(event.target.files);
    if (files.length === 0) {
      return;
    }

    setUploading(true);
    setUploadProgress({ total: files.length, completed: 0, failed: 0 });

    // Read all file contents first
    const fileData = [];
    for (const file of files) {
      try {
        const content = await readFileContent(file);
        fileData.push({ fileName: file.name, content });
      } catch (error) {
        console.error(`Error reading ${file.name}:`, error);
        setUploadProgress((prev) => ({
          ...prev,
          failed: prev.failed + 1,
        }));
      }
    }

    // Upload in batches of 10
    const batchSize = 10;
    for (let i = 0; i < fileData.length; i += batchSize) {
      const batch = fileData.slice(i, i + batchSize);
      
      // Upload all files in the batch concurrently
      const uploadPromises = batch.map(({ fileName, content }) =>
        uploadDocument(fileName, content)
      );

      // Wait for all uploads in the batch to complete
      const results = await Promise.all(uploadPromises);

      // Update progress
      results.forEach((result) => {
        setUploadProgress((prev) => ({
          ...prev,
          completed: result.success ? prev.completed + 1 : prev.completed,
          failed: result.success ? prev.failed : prev.failed + 1,
        }));
      });
    }

    setUploading(false);
    
    // Reset file input
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleDocumentClick = (document) => {
    setSelectedDocument(document);
    setDocumentModalOpen(true);
  };

  const handleCloseDocumentModal = () => {
    setDocumentModalOpen(false);
    setSelectedDocument(null);
  };

  return (
    <>
      <Head>
        <title>Chat Console</title>
        <meta name="description" content="Sidebar plus chat playground" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <div className={`${styles.page} ${geistSans.variable}`}>
        <div className={styles.sidebar}>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            style={{ display: "none" }}
            onChange={handleFileSelect}
          />
          <button
            type="button"
            className={styles.uploadButton}
            onClick={handleUploadClick}
            disabled={uploading}
          >
            {uploading ? "Uploading..." : "Upload Document"}
          </button>
          {uploading && (
            <div className={styles.uploadProgress}>
              <span>
                {uploadProgress.completed + uploadProgress.failed} / {uploadProgress.total} files
              </span>
              {uploadProgress.failed > 0 && (
                <span className={styles.uploadError}>
                  ({uploadProgress.failed} failed)
                </span>
              )}
            </div>
          )}
          <label className={styles.searchLabel} htmlFor="simple-search">
            Workspace search
          </label>
          <textarea
            id="simple-search"
            className={styles.searchArea}
            rows={3}
            placeholder="Simple search"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
          />
          <label className={styles.searchLabel} htmlFor="semantic-search">
            Semantic Search
          </label>
          <textarea
            id="semantic-search"
            className={styles.searchArea}
            rows={3}
            placeholder="Semantic Search"
            value={semanticSearchInput}
            onChange={(event) => setSemanticSearchInput(event.target.value)}
          />
          <h1 className={styles.sidebarTitle}>Documents</h1>
          <div className={styles.sidebarListWrapper}>
            {documentsLoading ? (
              <div className={styles.sidebarLoading}>
                <div className={styles.spinner} />
                <span>Loading documents...</span>
              </div>
            ) : (
              <ul className={styles.sidebarList}>
                {filteredItems.map((item) => (
                  <li key={item.id}>
                    <button
                      type="button"
                      className={`${styles.sidebarItem} ${
                        activeItem?.id === item.id ? styles.sidebarItemActive : ""
                      }`}
                      onClick={() => {
                        setActiveItem(item);
                        handleDocumentClick(item);
                      }}
                    >
                      <span className={styles.sidebarItemTitle}>{item.title}</span>
                      {item.summary && (
                        <span className={styles.sidebarItemDescription}>
                          {item.summary}
                        </span>
                      )}
                    </button>
                  </li>
                ))}
                {filteredItems.length === 0 && !searchLoading && !semanticSearchLoading && (
                  <li className={styles.emptyState}>No documents match that query.</li>
                )}
              </ul>
            )}
            {(searchLoading || semanticSearchLoading) && !documentsLoading && (
              <div className={styles.sidebarLoading}>
                <div className={styles.spinner} />
                <span>Searching...</span>
              </div>
            )}
          </div>
        </div>

        <main className={styles.chatPanel}>
          <header className={styles.chatHeader}>
            <div>
              <p className={styles.chatEyebrow}>
                model: {activeAgent?.name ?? "loading..."}
              </p>
              <h2 className={styles.chatTitle}>{activeItem?.title || "No document selected"}</h2>
              {activeItem?.summary && (
                <p className={styles.chatSubtitle}>{activeItem.summary}</p>
              )}
            </div>
            <button
              type="button"
              className={styles.settingsButton}
              onClick={openSettings}
              aria-label="Open agent settings"
            >
              ⚙️
            </button>
          </header>

          <section className={styles.chatHistory} ref={historyRef}>
            {orderedHistory.map((entry) => {
              const isMine = entry.userId === MY_USER_ID;
              return (
                <article
                  key={entry.id}
                  className={`${styles.chatMessage} ${
                    isMine ? styles.chatMessageMine : styles.chatMessageOther
                  }`}
                >
                  <div className={styles.chatMessageHeader}>
                    <span className={styles.chatAuthor}>{entry.author}</span>
                    <time className={styles.chatTimestamp}>
                      {new Date(entry.createdAt).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </time>
                  </div>
                  <p>{entry.text}</p>
                </article>
              );
            })}
          </section>

          <form className={styles.chatInputArea} onSubmit={handleSubmit}>
            <textarea
              placeholder="Type a message and hit enter..."
              className={styles.chatInput}
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  handleSubmit(event);
                }
              }}
              rows={1}
            />
          </form>
        </main>
      </div>
      {settingsOpen && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <div className={styles.modalHeader}>
              <h3>Agent settings</h3>
              <button
                type="button"
                className={styles.closeButton}
                onClick={() => setSettingsOpen(false)}
                aria-label="Close settings"
              >
                ×
              </button>
            </div>
            <div className={styles.modalRow}>
              <label className={styles.modalLabel}>Agent</label>
              <div className={styles.modalAgentControls}>
                <select
                  className={styles.modalSelect}
                  value={settingsAgentId}
                  onChange={handleAgentSelect}
                >
                  {agents.map((agent) => (
                    <option key={agent.id} value={agent.id}>
                      {agent.name}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className={styles.createButton}
                  onClick={handleCreateNew}
                >
                  Create new
                </button>
              </div>
            </div>
            <div className={styles.modalGrid}>
            <label className={styles.modalLabel}>
                Agent name
                <input
                  type="text"
                  name="name"
                  value={settingsForm.name}
                  onChange={handleSettingsChange}
                />
              </label>
              <label>
                LLM model
                <input
                  type="text"
                  name="llmModel"
                  value={settingsForm.llmModel}
                  onChange={handleSettingsChange}
                />
              </label>
              <label>
                Embeddings model
                <input
                  type="text"
                  name="embeddingsModel"
                  value={settingsForm.embeddingsModel}
                  onChange={handleSettingsChange}
                />
              </label>
              <label>
                ReRanking model
                <input
                  type="text"
                  name="rerankingModel"
                  value={settingsForm.rerankingModel}
                  onChange={handleSettingsChange}
                />
              </label>
              <label>
                Max tokens
                <input
                  type="number"
                  name="maxTokens"
                  value={settingsForm.maxTokens}
                  onChange={handleSettingsChange}
                />
              </label>
              <label>
                Temperature
                <input
                  type="number"
                  step="0.1"
                  name="temperature"
                  value={settingsForm.temperature}
                  onChange={handleSettingsChange}
                />
              </label>
            </div>
            <label className={styles.behaviorLabel}>
              Agent behavior
              <textarea
                rows={4}
                name="behavior"
                value={settingsForm.behavior}
                onChange={handleSettingsChange}
              />
            </label>
            <div className={styles.modalFooter}>
              <button
                type="button"
                className={styles.saveButton}
                onClick={handleSaveSettings}
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
      {documentModalOpen && selectedDocument && (
        <div className={styles.modalOverlay} onClick={handleCloseDocumentModal}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3>{selectedDocument.title}</h3>
              <button
                type="button"
                className={styles.closeButton}
                onClick={handleCloseDocumentModal}
                aria-label="Close document"
              >
                ×
              </button>
            </div>
            <div className={styles.modalContent} style={{ maxHeight: "70vh", overflowY: "auto", padding: "1rem" }}>
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {selectedDocument.body || ""}
              </ReactMarkdown>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
