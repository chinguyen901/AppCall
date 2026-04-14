export type SipProfile = {
  authName: string;
  password: string;
  domain: string;
  displayName: string;
  transport: "UDP" | "TCP" | "TLS";
  port: number;
};

export const buildSipConfig = (input: {
  sipUsername: string;
  sipPassword: string;
  displayName: string;
}): SipProfile => {
  const domain = process.env.PORTSIP_DOMAIN;
  const transport = (process.env.PORTSIP_TRANSPORT || "TLS").toUpperCase() as
    | "UDP"
    | "TCP"
    | "TLS";
  const port = Number(process.env.PORTSIP_PORT || 5061);

  if (!domain) {
    throw new Error("Missing PORTSIP_DOMAIN env.");
  }

  return {
    authName: input.sipUsername,
    password: input.sipPassword,
    displayName: input.displayName,
    domain,
    transport,
    port,
  };
};
