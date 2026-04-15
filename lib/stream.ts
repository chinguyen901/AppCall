import { StreamChat } from "stream-chat";

const streamApiKey = process.env.STREAM_API_KEY;
const streamApiSecret = process.env.STREAM_API_SECRET;

if (!streamApiKey || !streamApiSecret) {
  throw new Error("Missing STREAM_API_KEY or STREAM_API_SECRET env.");
}

const streamServerClient = StreamChat.getInstance(streamApiKey, streamApiSecret);

export const getStreamApiKey = () => streamApiKey;

export const getStreamToken = (userId: string) => streamServerClient.createToken(userId);

export const upsertStreamUser = async (input: { id: string; name: string; image?: string }) => {
  await streamServerClient.upsertUser({
    id: input.id,
    name: input.name,
    image: input.image,
  });
};
