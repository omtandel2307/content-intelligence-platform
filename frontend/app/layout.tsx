import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Content Intelligence",
  description:
    "Search YouTube, save videos, summarize transcripts, and chat with local RAG.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
