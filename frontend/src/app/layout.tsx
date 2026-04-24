import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "하루 한 질문 교환일기",
  description: "커플이 매일 하나의 질문을 나누는 조용한 기록장",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className="min-h-screen">{children}</body>
    </html>
  );
}
