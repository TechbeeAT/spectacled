# Privacy Policy

Last updated: [[DATE OF PUBLICATION]]

## 1. Who is responsible

The controller within the meaning of the EU General Data Protection Regulation (GDPR) is:

Techbee e.U.
Stuwerstraße 43/15
1020 Wien
Austria

Email: [privacy@techbee.at](mailto:privacy@techbee.at)
Website: [https://spectacled.techbee.at](https://spectacled.techbee.at)

This Privacy Policy covers:

- the apps **spectacled Journals**, **spectacled Notes** and **spectacled Tasks** for Android, iOS, desktop (Windows, macOS, Linux) and the web,
- the web app version of these apps,
- the optional hosted proxy service for the web app (see section 5),
- this website, spectacled.techbee.at.

## 2. The short version

- spectacled has **no user accounts with us**, **no analytics**, **no crash reporting**, **no advertising** and **no tracking**. The apps contain no third-party tracking or advertising libraries.
- Your journals, notes and tasks are stored **on your device** and on the **CalDAV server you choose**. The apps connect to that server directly; we do not see or store your entries.
- There are only two cases in which data can pass through a service run by us or by a third party on your behalf. Both are optional and off by default:
  - the **hosted proxy** for the web app (section 5), and
  - the **AI feature**, if you enable it with your own API key (section 6).

## 3. Data processed by the apps on your device

To provide their functionality, the apps process the following data locally on your device:

- the content of your journal entries, notes and tasks, including attachments,
- your CalDAV account configuration (server address, username, password or app password),
- synchronization metadata (e.g. which entries have changed since the last sync),
- app settings and preferences, including any AI provider settings and API keys you enter.

**Credentials and API keys** are stored encrypted on your device on all platforms. In the web app, data is stored in your browser's local storage (IndexedDB) and stays there until you delete it, for example by removing the account in the app or clearing your browser's site data.

The apps may write **diagnostic log messages** to your device's system log for troubleshooting. These logs stay on your device and are never sent to us.

We do not have access to any of this data. It is not processed by us and therefore no legal basis on our side is required for it.

## 4. Your CalDAV server

spectacled synchronizes your entries with a CalDAV server that you configure: a server you run yourself or a provider you choose. The apps send your credentials and your entries **directly** to that server.

The operator of that server processes your data on its own responsibility and under its own privacy policy. We have no influence on, and no access to, that processing. If you want to access, correct or delete data stored on your CalDAV server, please contact your server operator or use the tools it provides.

## 5. Web app and hosted proxy

### 5.1 Delivery of the web app (GitHub Pages)

The web app is delivered via **GitHub Pages**, a service of GitHub, Inc., 88 Colin P. Kelly Jr. Street, San Francisco, CA 94107, USA. When you open the web app, GitHub processes your IP address and technical request data (e.g. time of request, requested file, browser information) in order to deliver the files and to protect its service. GitHub processes this data in the USA. GitHub is certified under the EU-U.S. Data Privacy Framework. For details see the [GitHub Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).

Legal basis: our legitimate interest in delivering the web app reliably and securely (Art. 6(1)(f) GDPR).

Once loaded, the web app runs entirely in your browser. Your entries and credentials are not sent to GitHub.

### 5.2 Why a proxy is needed

For security reasons, browsers only allow a web app to talk to another server if that server explicitly permits it (known as "CORS"). Most CalDAV servers do not do this, so the web app needs a small relay, a **proxy**, between your browser and your CalDAV server. The native apps for Android, iOS and desktop do **not** use a proxy.

You can:

1. **run your own proxy** (recommended; the source code and instructions are published on [GitHub](https://github.com/TechbeeAT/spectacled/tree/main/server)), or
2. use the **hosted proxy** we operate at `https://spectacled-proxy.fly.dev`.

The hosted proxy is **off by default**. You can only enable it in the web app's settings after confirming a dialog that explains what is described here.

### 5.3 What the hosted proxy processes

The proxy forwards your requests to your CalDAV server and returns the responses. To do this, the encrypted (HTTPS) connection from your browser ends at the proxy and a new encrypted connection to your CalDAV server is opened. This means that **while a request passes through, the proxy technically has access to its full content**, including your CalDAV username and password and the entries being synchronized.

We operate the proxy solely to relay this traffic. It does **not** store your credentials or entries. It only logs:

- the HTTP method of a request (e.g. `GET`, `PUT`),
- the host name of the CalDAV server the request is sent to,
- rejected requests and connection errors (including the requested target address).

No credentials, request headers or entry contents are logged.

Our hosting provider (section 5.4) also processes your IP address and connection data to operate and protect its platform.

You can check how the proxy works in its published source code, but you cannot verify from the outside which version is running. If you do not want to place this trust in us, please run your own proxy or use one of the native apps.

Legal basis: provision of the service you requested (Art. 6(1)(b) GDPR) and our legitimate interest in operating the service securely and preventing abuse (Art. 6(1)(f) GDPR).

### 5.4 Hosting of the proxy (Fly.io)

The hosted proxy runs on infrastructure provided by **Fly.io, Inc.**, USA, in its data center in **Frankfurt, Germany**. Fly.io acts as our processor. Because Fly.io is a US company, access from the USA cannot be ruled out. Such transfers are based on [[the EU-U.S. Data Privacy Framework / the EU Standard Contractual Clauses in Fly.io's Data Processing Agreement — please verify which applies]].

Proxy logs are retained for [[X days — Fly.io's default log retention; please verify in the Fly.io dashboard/documentation]] and then deleted automatically.

## 6. Optional AI feature

The apps offer an optional feature that suggests new entries derived from text you select. It is **off by default** and only works after you choose a provider and enter your own access data:

- **Anthropic (Claude):** the selected text and instructions for the task are sent **directly from your device** to the API of Anthropic, PBC, USA (`api.anthropic.com`), using **your own API key**. Anthropic processes this data as your provider, under the contract between you and Anthropic and under [Anthropic's privacy policy](https://www.anthropic.com/legal/privacy). The data is processed in the USA.
- **OpenAI-compatible server:** the same data is sent directly to the server address you enter, for example a model you run yourself (Ollama, LM Studio, etc.). The operator of that server is responsible for the processing.

The data does not pass through any server operated by us, and we do not receive it. Please do not use this feature for content you do not want to share with the provider you selected.

## 7. This website

### 7.1 Hosting (IONOS)

This website is hosted by **IONOS SE**, Elgendorfer Str. 57, 56410 Montabaur, Germany, as our processor under a data processing agreement. When you visit the website, the web server automatically records technical data in server log files:

- IP address,
- date and time of the request,
- requested page or file,
- referrer URL,
- browser and operating system information.

This data is needed to deliver the website and to keep it secure (e.g. to detect attacks). Log files are deleted after [[X days — please check your IONOS hosting settings]].

Legal basis: our legitimate interest in the secure and reliable operation of the website (Art. 6(1)(f) GDPR).

### 7.2 Cookies, analytics and external content

This website uses **no analytics or tracking tools** and **no tracking cookies**. All fonts, images and scripts are loaded from our own server, so no data is sent to third parties such as Google Fonts or content delivery networks when you view a page.

[[If you ever log in to WordPress or enable comments, WordPress sets technically necessary cookies for that; mention this here if it applies to visitors.]]

### 7.3 Links to other websites

The website links to external services such as GitHub, Mastodon, PayPal (donations) and NLnet. These are ordinary links: no data is sent to these services until you click them. After that, the privacy policy of the respective service applies.

## 8. App stores and downloads

When you download the apps from Google Play, the Apple App Store, F-Droid or GitHub, the respective platform processes data (e.g. your account, device information, download and usage statistics) on its own responsibility and under its own privacy policy. We may receive aggregated, non-personal statistics from these platforms (e.g. the number of installations).

## 9. Contacting us and contributing

If you contact us by email, we process your email address and the content of your message to answer your request (Art. 6(1)(b) or (f) GDPR). We delete this data when it is no longer needed, unless we are legally required to keep it.

If you report issues or contribute via GitHub, your contributions (username, profile information, comments, commits) are public and processed by GitHub under its own privacy policy. Please do not include personal or sensitive information, such as credentials or private entries, in public reports.

## 10. Your rights

Under the GDPR you have the right to:

- access the personal data we process about you (Art. 15),
- rectification (Art. 16),
- erasure (Art. 17),
- restriction of processing (Art. 18),
- data portability (Art. 20),
- object to processing based on legitimate interests (Art. 21),
- withdraw consent at any time with effect for the future, where processing is based on consent.

To exercise your rights, please email [privacy@techbee.at](mailto:privacy@techbee.at). Please note that we do not have access to the data stored in the apps on your device or on your CalDAV server (see sections 3 and 4).

You also have the right to lodge a complaint with a supervisory authority. In Austria this is the **Österreichische Datenschutzbehörde**, Barichgasse 40–42, 1030 Wien, [www.dsb.gv.at](https://www.dsb.gv.at).

## 11. Children

spectacled is not directed at children under 14. We do not knowingly process personal data of children.

## 12. Security

We take appropriate technical and organizational measures to protect the data we process. All connections to our services use encryption (HTTPS). The hosted proxy only accepts HTTPS targets and blocks requests to private and internal network addresses.

## 13. Changes to this Privacy Policy

We update this Privacy Policy when the apps or our services change. The current version is always available at [https://spectacled.techbee.at/privacy-policy/](https://spectacled.techbee.at/privacy-policy/). The date at the top shows when it was last updated.
