import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "하루 한 질문 교환일기",
  description: "커플이 매일 하나의 질문을 나누는 교환일기",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
