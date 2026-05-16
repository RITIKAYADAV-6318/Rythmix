## 🎸 RythMix: The Digital Pulse of Campus Societies
License Kotlin Firebase Material3
RythMix is more than an app; it is a digital "Tree Shed"—a centralized, high-trust ecosystem designed to shelter and organize all campus societies under one digital canopy. Built for exclusive use within university perimeters, it eliminates the "PR noise" of generic social media (WhatsApp/Instagram) and replaces it with a focused, professional environment for collaboration and talent archiving.
## 🌟 The Vision: The "Society Holder"
Campus communication is currently fragmented. Society heads struggle with PR, while students lose talent history every year. RythMix solves this by:
•
Exclusivity: Restricted institutional onboarding (College email only).
•
Consolidation: A single hub for Music, Hackathons, Gaming fests, and Workshops.
•
Archiving: A permanent digital vault for society-specific reels, compositions, and event history.
## 🚀 Key Features
🛠️ For Society Members & Creators (The Hub)
•
Dynamic Society Creation: Authenticated members can build their own society "folders" with college-specific metadata.
•
Role-Based Access Control (RBAC): Gated permissions where creators moderate memberships via a real-time Approval Workflow.
•
Administrative Broadcasting: Direct event posting with external link support (Music, Unstop, etc.) and ownership tracking.
•
Global Moderation: Ability to clear chat histories or delete specific disruptive messages in real-time.
## 👥 For Students & Artists (The Social)
•
Reactive Group Chats: Sub-second latency messaging with a modern 3D UI, @name tagging, and circular user avatars.
•
Multimedia Feed: An Instagram-style social feed for sharing and playing high-fidelity Audio/Video tracks and reels.
•
Musician Profiles: Professional digital identities showcasing roles, IDs, and society affiliations.
•
Bell Notification System: A centralized activity center for tags, likes, and administrative requests.
## 🛠️ Technical Architecture
The Stack
•
Language: Kotlin (Modern, Null-safe)
•
Database: Firebase Realtime Database (Reactive NoSQL)
•
Cloud Storage: Firebase Storage (Binary Large Object management)
•
Auth: Firebase Authentication (Institutional perimeter)
•
Image Processing: Glide (Asynchronous loading & Memory management)
Engineering Highlights
•
Sequential Task Chaining: Solved the "Object does not exist" storage error by implementing a robust continueWithTask listener logic, ensuring data integrity during high-speed uploads.
•
Window Insets Management: Mastered the Android Insets API to deliver a seamless "Edge-to-Edge" UI, preventing overlap with physical system navigation bars across diverse hardware.
•
InputStream Handling: Implemented robust file-picking logic using InputStreams for 100% reliable media processing on emulators and physical devices.
•
Reactive UI: Used persistent ValueEventListeners to create a UI that updates instantly without manual refreshes.
## 🎨 UI/UX Design Philosophy
RythMix features a custom "Lavender Sparkle" theme.
•
3D Depth: High-elevation Material CardViews for a tactile, premium feel.
•
Instagram Aesthetics: Signature gradients (Orange-Pink-Purple) and bold white branding.
•
Stability: Optimized layouts that maintain 60fps scrolling even with heavy media assets.
## 🛤️ Future Roadmap
•
[ ] Phase 2 (AI Integration): Multimodal AI for automated genre tagging and "Highlight Reel" generation using Gemini.
•
[ ] Phase 3 (Collaborative Matchmaker): LLM-based algorithm to suggest band collaborations based on student profiles.
•
[ ] Phase 4 (Ticketing): Integrated entry management for campus fests and gaming events.
## 💻 Installation
1.
Clone the repo: git clone https://github.com/yourusername/RythMix.git
2.
Open in Android Studio.
3.
Connect your Firebase Project (google-services.json).
4.
Enable Authentication (Email), Database, and Storage.
5.
Build & Run!
## 🤝 Contact
  Ritika Yadav
  "Building the digital Operating System for campus life." 🎸✨
