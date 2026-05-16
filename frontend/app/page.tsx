import { cookies } from "next/headers";
import { YoutubeSearchShell } from "../components/youtube-search-shell";

const accountCookieName = "cip_account_id";

export default async function HomePage() {
  const cookieStore = await cookies();
  const accountId = cookieStore.get(accountCookieName)?.value || "";

  return <YoutubeSearchShell accountId={accountId} />;
}
